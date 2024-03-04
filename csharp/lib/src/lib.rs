/**
 * Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0
 */
mod connection;
pub use connection::{
    AuthenticationInfo, ConnectionConfig, ConnectionRetryStrategy, NodeAddress, ProtocolVersion,
    ReadFrom, TlsMode,
};

use glide_core::client::Client as GlideClient;
use glide_core::connection_request;
use redis::{Cmd, FromRedisValue, RedisResult};
use std::{
    ffi::{c_void, CStr, CString},
    os::raw::c_char,
};
use tokio::runtime::{Builder, Runtime};

pub enum Level {
    Error = 0,
    Warn = 1,
    Info = 2,
    Debug = 3,
    Trace = 4,
}

pub struct Client {
    client: GlideClient,
    success_callback: unsafe extern "C" fn(usize, *const c_char) -> (),
    failure_callback: unsafe extern "C" fn(usize) -> (), // TODO - add specific error codes
    runtime: Runtime,
}

/// Convert raw C string to a rust string.
/// # Safety
/// Unsafe function because creating a string from a pointer.
unsafe fn ptr_to_str(ptr: *const c_char) -> &'static str {
    if ptr as i64 != 0 {
        unsafe { CStr::from_ptr(ptr) }.to_str().unwrap()
    } else {
        ""
    }
}

/// Convert raw array pointer to a vector of `NodeAddress`es.
/// # Safety
/// Unsafe function because dereferencing a pointer.
pub unsafe fn node_addresses_to_proto(
    data: *const *const NodeAddress,
    len: usize,
) -> Vec<connection_request::NodeAddress> {
    unsafe { std::slice::from_raw_parts(data as *mut NodeAddress, len) }
        .iter()
        .map(|addr| {
            let mut address_info = connection_request::NodeAddress::new();
            address_info.host = unsafe { ptr_to_str(addr.host) }.into();
            address_info.port = addr.port as u32;
            address_info
        })
        .collect()
}

/// Convert connection configuration to a corresponding protobuf object.
/// # Safety
/// Unsafe function because dereferencing a pointer.
unsafe fn create_connection_request(
    config: *const ConnectionConfig,
) -> connection_request::ConnectionRequest {
    let mut connection_request = connection_request::ConnectionRequest::new();

    let config_ref = unsafe { &*config };

    connection_request.addresses =
        unsafe { node_addresses_to_proto(config_ref.addresses, config_ref.address_count) };

    connection_request.tls_mode = match config_ref.tls_mode {
        TlsMode::SecureTls => connection_request::TlsMode::SecureTls,
        TlsMode::InsecureTls => connection_request::TlsMode::InsecureTls,
        TlsMode::NoTls => connection_request::TlsMode::NoTls,
    }
    .into();
    connection_request.cluster_mode_enabled = config_ref.cluster_mode;
    connection_request.request_timeout = config_ref.request_timeout;

    connection_request.read_from = match config_ref.read_from {
        ReadFrom::AZAffinity => connection_request::ReadFrom::AZAffinity,
        ReadFrom::PreferReplica => connection_request::ReadFrom::PreferReplica,
        ReadFrom::Primary => connection_request::ReadFrom::Primary,
        ReadFrom::LowestLatency => connection_request::ReadFrom::LowestLatency,
    }
    .into();

    let mut retry_strategy = connection_request::ConnectionRetryStrategy::new();
    retry_strategy.number_of_retries = config_ref.connection_retry_strategy.number_of_retries;
    retry_strategy.factor = config_ref.connection_retry_strategy.factor;
    retry_strategy.exponent_base = config_ref.connection_retry_strategy.exponent_base;
    connection_request.connection_retry_strategy = Some(retry_strategy).into();

    let mut auth_info = connection_request::AuthenticationInfo::new();
    auth_info.username = unsafe { ptr_to_str(config_ref.authentication_info.username) }.into();
    auth_info.password = unsafe { ptr_to_str(config_ref.authentication_info.password) }.into();
    connection_request.authentication_info = Some(auth_info).into();

    connection_request.database_id = config_ref.database_id;
    connection_request.protocol = match config_ref.protocol {
        ProtocolVersion::RESP2 => connection_request::ProtocolVersion::RESP2,
        ProtocolVersion::RESP3 => connection_request::ProtocolVersion::RESP3,
    }
    .into();

    connection_request.client_name = unsafe { ptr_to_str(config_ref.client_name) }.into();

    connection_request
}

/// # Safety
/// Unsafe function because calling other unsafe function.
unsafe fn create_client_internal(
    config: *const ConnectionConfig,
    success_callback: unsafe extern "C" fn(usize, *const c_char) -> (),
    failure_callback: unsafe extern "C" fn(usize) -> (),
) -> RedisResult<Client> {
    let request = unsafe { create_connection_request(config) };
    let runtime = Builder::new_multi_thread()
        .enable_all()
        .thread_name("GLIDE for Redis C# thread")
        .build()?;
    let _runtime_handle = runtime.enter();
    let client = runtime.block_on(GlideClient::new(request)).unwrap(); // TODO - handle errors.
    Ok(Client {
        client,
        success_callback,
        failure_callback,
        runtime,
    })
}

/// Creates a new client to the given address. The success callback needs to copy the given string synchronously, since it will be dropped by Rust once the callback returns. All callbacks should be offloaded to separate threads in order not to exhaust the client's thread pool.
/// # Safety
/// Unsafe function because calling other unsafe function.
#[no_mangle]
pub unsafe extern "C" fn create_client(
    config: *const ConnectionConfig,
    success_callback: unsafe extern "C" fn(usize, *const c_char) -> (),
    failure_callback: unsafe extern "C" fn(usize) -> (),
) -> *const c_void {
    match unsafe { create_client_internal(config, success_callback, failure_callback) } {
        Err(_) => std::ptr::null(), // TODO - log errors
        Ok(client) => Box::into_raw(Box::new(client)) as *const c_void,
    }
}

/// # Safety
/// Unsafe function because dereferencing a pointer.
#[no_mangle]
pub unsafe extern "C" fn close_client(client_ptr: *const c_void) {
    let client_ptr = unsafe { Box::from_raw(client_ptr as *mut Client) };
    let _runtime_handle = client_ptr.runtime.enter();
    drop(client_ptr);
}

/// Expects that key and value will be kept valid until the callback is called.
#[no_mangle]
pub extern "C" fn set(
    client_ptr: *const c_void,
    callback_index: usize,
    key: *const c_char,
    value: *const c_char,
) {
    let client = unsafe { Box::leak(Box::from_raw(client_ptr as *mut Client)) };
    // The safety of this needs to be ensured by the calling code. Cannot dispose of the pointer before all operations have completed.
    let ptr_address = client_ptr as usize;

    let key_cstring = unsafe { CStr::from_ptr(key as *mut c_char) };
    let value_cstring = unsafe { CStr::from_ptr(value as *mut c_char) };
    let mut client_clone = client.client.clone();
    client.runtime.spawn(async move {
        let key_bytes = key_cstring.to_bytes();
        let value_bytes = value_cstring.to_bytes();
        let mut cmd = Cmd::new();
        cmd.arg("SET").arg(key_bytes).arg(value_bytes);
        let result = client_clone.send_command(&cmd, None).await;
        unsafe {
            let client = Box::leak(Box::from_raw(ptr_address as *mut Client));
            match result {
                Ok(_) => (client.success_callback)(callback_index, std::ptr::null()), // TODO - should return "OK" string.
                Err(_) => (client.failure_callback)(callback_index), // TODO - report errors
            };
        }
    });
}

/// Expects that key will be kept valid until the callback is called. If the callback is called with a string pointer, the pointer must
/// be used synchronously, because the string will be dropped after the callback.
#[no_mangle]
pub extern "C" fn get(client_ptr: *const c_void, callback_index: usize, key: *const c_char) {
    let client = unsafe { Box::leak(Box::from_raw(client_ptr as *mut Client)) };
    // The safety of this needs to be ensured by the calling code. Cannot dispose of the pointer before all operations have completed.
    let ptr_address = client_ptr as usize;

    let key_cstring = unsafe { CStr::from_ptr(key as *mut c_char) };
    let mut client_clone = client.client.clone();
    client.runtime.spawn(async move {
        let key_bytes = key_cstring.to_bytes();
        let mut cmd = Cmd::new();
        cmd.arg("GET").arg(key_bytes);
        let result = client_clone.send_command(&cmd, None).await;
        let client = unsafe { Box::leak(Box::from_raw(ptr_address as *mut Client)) };
        let value = match result {
            Ok(value) => value,
            Err(_) => {
                unsafe { (client.failure_callback)(callback_index) }; // TODO - report errors,
                return;
            }
        };
        let result = Option::<CString>::from_owned_redis_value(value);

        unsafe {
            match result {
                Ok(None) => (client.success_callback)(callback_index, std::ptr::null()),
                Ok(Some(c_str)) => (client.success_callback)(callback_index, c_str.as_ptr()),
                Err(_) => (client.failure_callback)(callback_index), // TODO - report errors
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
