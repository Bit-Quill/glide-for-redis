use babushka::client::Client as BabushkaClient;
use babushka::connection_request;
use babushka::connection_request::AddressInfo;
use redis::{Cmd, FromRedisValue, RedisResult};
use std::{
    ffi::{c_void, CStr, CString},
    os::raw::c_char,
};
use tokio::runtime::Builder;
use tokio::runtime::Runtime;

pub enum Level {
    Error = 0,
    Warn = 1,
    Info = 2,
    Debug = 3,
    Trace = 4,
}

pub struct Connection {
    connection: BabushkaClient,
    success_callback: unsafe extern "C" fn(usize, *const c_char, usize) -> (),
    failure_callback: unsafe extern "C" fn(usize) -> (), // TODO - add specific error codes
    runtime: Runtime,
}

fn create_connection_request(
    host: String,
    port: u32,
    use_tls: bool,
    use_cluster_mode: bool,
) -> connection_request::ConnectionRequest {
    let mut address_info = AddressInfo::new();
    address_info.host = host.to_string().into();
    address_info.port = port;
    let addresses_info = vec![address_info];
    let mut connection_request = connection_request::ConnectionRequest::new();
    connection_request.addresses = addresses_info;
    connection_request.cluster_mode_enabled = use_cluster_mode;
    connection_request.tls_mode = if use_tls {
        connection_request::TlsMode::InsecureTls
    } else {
        connection_request::TlsMode::NoTls
    }
        .into();

    connection_request
}

fn create_connection_internal(
    host: *const c_char,
    port: u32,
    use_tls: bool,
    use_cluster_mode: bool,
    success_callback: unsafe extern "C" fn(usize, *const c_char, usize) -> (),
    failure_callback: unsafe extern "C" fn(usize) -> (),
) -> RedisResult<Connection> {
    let host_cstring = unsafe { CStr::from_ptr(host as *mut c_char) };
    let host_string = host_cstring.to_str()?.to_string();
    let request = create_connection_request(host_string, port, use_tls, use_cluster_mode);
    let runtime = Builder::new_multi_thread()
        .enable_all()
        .thread_name("Babushka go thread")
        .build()?;
    let _runtime_handle = runtime.enter();
    let connection = runtime.block_on(BabushkaClient::new(request)).unwrap();
    Ok(Connection {
        connection,
        success_callback,
        failure_callback,
        runtime,
    })
}

/// Creates a new connection to the given address. The success callback needs to copy the given string synchronously, since it will be dropped by Rust once the callback returns. All callbacks should be offloaded to separate threads in order not to exhaust the connection's thread pool.
#[no_mangle]
pub extern "C" fn create_connection(
    host: *const c_char,
    port: u32,
    use_tls: bool,
    use_cluster_mode: bool,
    success_callback: unsafe extern "C" fn(usize, *const c_char, usize) -> (),
    failure_callback: unsafe extern "C" fn(usize) -> (),
) -> *const c_void {
    match create_connection_internal(host, port, use_tls, use_cluster_mode, success_callback, failure_callback) {
        Err(_) => std::ptr::null(), // TODO - log errors
        Ok(connection) => Box::into_raw(Box::new(connection)) as *const c_void,
    }
}

#[no_mangle]
pub extern "C" fn close_connection(connection_ptr: *const c_void) {
    let connection_ptr = unsafe { Box::from_raw(connection_ptr as *mut Connection) };
    let _runtime_handle = connection_ptr.runtime.enter();
    drop(connection_ptr);
}

/// Expects that key and value will be kept valid until the callback is called.
#[no_mangle]
pub extern "C" fn set(
    connection_ptr: *const c_void,
    callback_index: usize,
    key: *const c_char,
    value: *const c_char,
    channel: usize
) {
    let connection = unsafe { Box::leak(Box::from_raw(connection_ptr as *mut Connection)) };
    // The safety of this needs to be ensured by the calling code. Cannot dispose of the pointer before all operations have completed.
    let ptr_address = connection_ptr as usize;

    let key_cstring = unsafe { CStr::from_ptr(key as *mut c_char) };
    let value_cstring = unsafe { CStr::from_ptr(value as *mut c_char) };
    let mut connection_clone = connection.connection.clone();
    connection.runtime.spawn(async move {
        let key_bytes = key_cstring.to_bytes();
        let value_bytes = value_cstring.to_bytes();
        let mut cmd = Cmd::new();
        cmd.arg("SET").arg(key_bytes).arg(value_bytes);
        let result = connection_clone.req_packed_command(&cmd, None).await;
        unsafe {
            let client = Box::leak(Box::from_raw(ptr_address as *mut Connection));
            match result {
                Ok(_) => (client.success_callback)(callback_index, std::ptr::null(), channel),
                Err(_) => (client.failure_callback)(callback_index), // TODO - report errors
            };
        }
    });
}

/// Expects that key will be kept valid until the callback is called. If the callback is called with a string pointer, the pointer must
/// be used synchronously, because the string will be dropped after the callback.
#[no_mangle]
pub extern "C" fn get(connection_ptr: *const c_void, callback_index: usize, key: *const c_char, channel: usize) {
    let connection = unsafe { Box::leak(Box::from_raw(connection_ptr as *mut Connection)) };
    // The safety of this needs to be ensured by the calling code. Cannot dispose of the pointer before all operations have completed.
    let ptr_address = connection_ptr as usize;

    let key_cstring = unsafe { CStr::from_ptr(key as *mut c_char) };
    let mut connection_clone = connection.connection.clone();
    connection.runtime.spawn(async move {
        let key_bytes = key_cstring.to_bytes();
        let mut cmd = Cmd::new();
        cmd.arg("GET").arg(key_bytes);
        let result = connection_clone.req_packed_command(&cmd, None).await;
        let connection = unsafe { Box::leak(Box::from_raw(ptr_address as *mut Connection)) };
        let value = match result {
            Ok(value) => value,
            Err(_) => {
                unsafe { (connection.failure_callback)(callback_index) }; // TODO - report errors,
                return;
            }
        };
        let result = Option::<CString>::from_redis_value(&value);

        unsafe {
            match result {
                Ok(None) => (connection.success_callback)(callback_index, std::ptr::null(), channel),
                Ok(Some(c_str)) => (connection.success_callback)(callback_index, c_str.as_ptr(), channel),
                Err(_) => (connection.failure_callback)(callback_index), // TODO - report errors
            };
        }
    });
}

impl From<logger_core::Level> for Level {
    fn from(level: logger_core::Level) -> Self {
        match level {
            logger_core::Level::Error => Level::Error,
            logger_core::Level::Warn => Level::Warn,
            logger_core::Level::Info => Level::Info,
            logger_core::Level::Debug => Level::Debug,
            logger_core::Level::Trace => Level::Trace,
        }
    }
}

impl From<Level> for logger_core::Level {
    fn from(level: Level) -> logger_core::Level {
        match level {
            Level::Error => logger_core::Level::Error,
            Level::Warn => logger_core::Level::Warn,
            Level::Info => logger_core::Level::Info,
            Level::Debug => logger_core::Level::Debug,
            Level::Trace => logger_core::Level::Trace,
        }
    }
}

#[no_mangle]
#[allow(improper_ctypes_definitions)]
/// # Safety
/// Unsafe function because creating string from pointer
pub unsafe extern "C" fn log(
    log_level: Level,
    log_identifier: *const c_char,
    message: *const c_char,
) {
    unsafe {
        logger_core::log(
            log_level.into(),
            CStr::from_ptr(log_identifier)
                .to_str()
                .expect("Can not read log_identifier argument."),
            CStr::from_ptr(message)
                .to_str()
                .expect("Can not read message argument."),
        );
    }
}

#[no_mangle]
#[allow(improper_ctypes_definitions)]
/// # Safety
/// Unsafe function because creating string from pointer
pub unsafe extern "C" fn init(level: Option<Level>, file_name: *const c_char) -> Level {
    let file_name_as_str;
    unsafe {
        file_name_as_str = if file_name.is_null() {
            None
        } else {
            Some(
                CStr::from_ptr(file_name)
                    .to_str()
                    .expect("Can not read string argument."),
            )
        };

        let logger_level = logger_core::init(level.map(|level| level.into()), file_name_as_str);
        logger_level.into()
    }
}
