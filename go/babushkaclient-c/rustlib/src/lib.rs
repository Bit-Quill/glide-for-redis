#![allow(non_upper_case_globals)]
#![allow(non_camel_case_types)]
#![allow(non_snake_case)]

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

include!(concat!(env!("OUT_DIR"), "/bindings.rs"));
pub type SuccessCallback =
    unsafe extern "C" fn(message: *const c_char, channel_address: usize) -> ();
pub type FailureCallback =
    unsafe extern "C" fn(err_message: *const c_char, channel_address: usize) -> ();

pub struct Connection {
    connection: BabushkaClient,
    success_callback: SuccessCallback,
    failure_callback: FailureCallback,
    runtime: Runtime,
}


fn create_connection_request_c(
    request: *const ConnectionRequest,
) -> connection_request::ConnectionRequest {
    if let Some(request_ref) = unsafe { request.as_ref() } {
        unsafe {
            let mut addresses = request_ref.addresses;
            let mut addresses_info = Vec::new();
            while !(*addresses).is_null() {
                let address_info_c = &**addresses;
                let mut address_info = AddressInfo::new();
                address_info.host =convert_to_string(address_info_c.host).unwrap().into();
                address_info.port = address_info_c.port;
                addresses_info.push(address_info);
                addresses = addresses.add(1);
            }

            let mut connection_request = connection_request::ConnectionRequest::new();
            connection_request.addresses = addresses_info;
            connection_request.cluster_mode_enabled = request_ref.cluster_mode_enabled;
            connection_request.tls_mode = if request_ref.tls_mode == 2 {
                connection_request::TlsMode::InsecureTls
            } else {
                connection_request::TlsMode::NoTls
            }
            .into();

            return connection_request;
        }
    }
    connection_request::ConnectionRequest::new()
}

fn create_connection_internal_c(
    request: *const ConnectionRequest,
    success_callback: SuccessCallback,
    failure_callback: FailureCallback,
) -> RedisResult<Connection> {
    let request = create_connection_request_c(request);
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
pub extern "C" fn create_connection_c(
    request: *const ConnectionRequest,
    success_callback: SuccessCallback,
    failure_callback: FailureCallback,
) -> *const c_void {
    match create_connection_internal_c(request, success_callback, failure_callback) {
        Err(_) => std::ptr::null(),
        Ok(connection) => Box::into_raw(Box::new(connection)) as *const c_void,
    }
}

#[no_mangle]
pub extern "C" fn close_connection_c(connection_ptr: *const c_void) {
    let connection_ptr = unsafe { Box::from_raw(connection_ptr as *mut Connection) };
    let _runtime_handle = connection_ptr.runtime.enter();
    drop(connection_ptr);
}

#[no_mangle]
pub extern "C" fn execute_command_rust(connection_ptr: *const c_void, request: *const RedisRequestC, channel: usize) {
    let connection = unsafe { Box::leak(Box::from_raw(connection_ptr as *mut Connection)) };
    let ptr_address = connection_ptr as usize;
    let mut connection_clone = connection.connection.clone();

    let mut request_args: Vec<&[u8]> = vec![];
    let mut request_name_cstr: &[u8] = &[];

    if let Some(request_ref) = unsafe { request.as_ref() } {
        unsafe {
            request_name_cstr = CStr::from_ptr(request_ref.command_name).to_bytes();
            let mut argument_array = request_ref.argument_array;
            if !argument_array.is_null() {
                while !(*argument_array).is_null() {
                    request_args.push(CStr::from_ptr(*argument_array).to_bytes());
                    argument_array = argument_array.add(1);
                }
            }
        }
    }

    connection.runtime.spawn(async move {
        let mut cmd = Cmd::new();
        cmd.arg(request_name_cstr);
        for argument_bytes in &request_args {
            cmd.arg(argument_bytes);
        }
        let result = connection_clone.req_packed_command(&cmd, None).await;
        let connection = unsafe { Box::leak(Box::from_raw(ptr_address as *mut Connection)) };
        let value = match result {
            Ok(value) => value,
            Err(err) => {
                let c_err_str = CString::new(err.to_string()).expect("CString::new failed");
                unsafe { (connection.failure_callback)(c_err_str.as_ptr(), channel) };
                return;
            }
        };
        let result = Option::<CString>::from_redis_value(&value);

        unsafe {
            match result {
                Ok(None) => (connection.success_callback)(std::ptr::null(), channel),
                Ok(Some(c_str)) => (connection.success_callback)(c_str.as_ptr(), channel),
                Err(err) =>{
                    let c_err_str = CString::new(err.to_string()).expect("CString::new failed");
                    (connection.failure_callback)(c_err_str.as_ptr(), channel)
                }
            };
        }
    });
}
fn convert_to_string(raw_ptr: *mut c_char) -> Option<String> {
    unsafe {
        let c_str = CStr::from_ptr(raw_ptr);
        Some(c_str.to_string_lossy().into_owned())
    }
}

