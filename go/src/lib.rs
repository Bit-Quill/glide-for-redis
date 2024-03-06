/**
 * Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0
 */
use glide_core::client::Client as GlideClient;
use glide_core::connection_request;
use protobuf::Message;
use std::{
    ffi::{c_void, CString},
    os::raw::c_char,
};
use tokio::runtime::Builder;
use tokio::runtime::Runtime;

/// Success callback that is called when a Redis command succeeds.
pub type SuccessCallback =
    unsafe extern "C" fn(channel_address: usize, message: *const c_char) -> ();

/// Failure callback that is called when a Redis command fails.
///
/// `error` should be manually freed by calling `free_error` after this callback is invoked, otherwise a memory leak will occur.
pub type FailureCallback =
    unsafe extern "C" fn(channel_address: usize, error: *const RedisErrorFFI) -> ();

/// The connection response.
///
/// It contains either a connection or an error. It is represented as a struct instead of an enum for ease of use in the wrapper language.
#[repr(C)]
pub struct ConnectionResponse {
    conn_ptr: *const c_void,
    error: *const RedisErrorFFI,
}

/// A Redis error.
#[repr(C)]
pub struct RedisErrorFFI {
    message: *const c_char,
    error_type: ErrorType,
}

/// FFI compatible version of the ErrorType enum defined in protobuf.
// TODO: Update when command errors are implemented
#[repr(C)]
pub enum ErrorType {
    ClosingError = 0,
    RequestError = 1,
    TimeoutError = 2,
    ExecAbortError = 3,
    ConnectionError = 4,
}

/// The glide client.
#[allow(dead_code)]
pub struct Client {
    client: GlideClient,
    success_callback: SuccessCallback,
    failure_callback: FailureCallback,
    runtime: Runtime,
}

fn create_client_internal(
    connection_request_bytes: &[u8],
    success_callback: SuccessCallback,
    failure_callback: FailureCallback,
) -> Result<Client, RedisErrorFFI> {
    let request = connection_request::ConnectionRequest::parse_from_bytes(connection_request_bytes)
        .map_err(|err| {
            let msg = CString::new(err.to_string()).unwrap();
            RedisErrorFFI {
                message: msg.into_raw(),
                error_type: ErrorType::ConnectionError,
            }
        })?;
    let runtime = Builder::new_multi_thread()
        .enable_all()
        .thread_name("GLIDE for Redis Go thread")
        .build()
        .map_err(|err| {
            let msg = CString::new(err.to_string()).unwrap();
            RedisErrorFFI {
                message: msg.into_raw(),
                error_type: ErrorType::ConnectionError,
            }
        })?;
    let _runtime_handle = runtime.enter();
    let client = runtime.block_on(GlideClient::new(request)).map_err(|err| {
        let msg = CString::new(err.to_string()).unwrap();
        RedisErrorFFI {
            message: msg.into_raw(),
            error_type: ErrorType::ConnectionError,
        }
    })?;
    Ok(Client {
        client,
        success_callback,
        failure_callback,
        runtime,
    })
}

/// Creates a new client to the given address. The success callback needs to copy the given string synchronously, since it will be dropped by Rust once the callback returns. All callbacks should be offloaded to separate threads in order not to exhaust the client's thread pool.
///
/// The returned `ConnectionResponse` should be manually freed by calling `free_connection_response`, otherwise a memory leak will occur. It should be freed whether or not an error occurs.
///
/// # Safety
///
/// * `connection_request_bytes` must point to `connection_request_len` consecutive properly initialized bytes.
/// * `connection_request_len` must not be greater than `isize::MAX`. See the safety documentation of [`std::slice::from_raw_parts`](https://doc.rust-lang.org/std/slice/fn.from_raw_parts.html).
#[no_mangle]
pub unsafe extern "C" fn create_client(
    connection_request_bytes: *const u8,
    connection_request_len: usize,
    success_callback: SuccessCallback,
    failure_callback: FailureCallback,
) -> *const ConnectionResponse {
    let request_bytes =
        unsafe { std::slice::from_raw_parts(connection_request_bytes, connection_request_len) };
    let response = match create_client_internal(request_bytes, success_callback, failure_callback) {
        Err(err) => ConnectionResponse {
            conn_ptr: std::ptr::null(),
            error: Box::into_raw(Box::new(err)) as *const RedisErrorFFI,
        },
        Ok(client) => ConnectionResponse {
            conn_ptr: Box::into_raw(Box::new(client)) as *const c_void,
            error: std::ptr::null(),
        },
    };
    Box::into_raw(Box::new(response))
}

/// Closes the given client, deallocating it from the heap.
///
/// # Safety
///
/// * `client_ptr` must be able to be safely casted to a valid `Box<Client>` via `Box::from_raw`. See the safety documentation of [`std::boxed::Box::from_raw`](https://doc.rust-lang.org/std/boxed/struct.Box.html#method.from_raw).
/// * `client_ptr` must not be null.
#[no_mangle]
pub unsafe extern "C" fn close_client(client_ptr: *const c_void) {
    let client_ptr = unsafe { Box::from_raw(client_ptr as *mut Client) };
    let _runtime_handle = client_ptr.runtime.enter();
    drop(client_ptr);
}

/// Deallocates a `ConnectionResponse`.
///
/// # Safety
///
/// * `connection_response_ptr` must be able to be safely casted to a valid `Box<RedisErrorFFI>` via `Box::from_raw`. See the safety documentation of [`std::boxed::Box::from_raw`](https://doc.rust-lang.org/std/boxed/struct.Box.html#method.from_raw).
/// * `connection_response_ptr` must not be null.
#[no_mangle]
pub unsafe extern "C" fn free_connection_response(
    connection_response_ptr: *const ConnectionResponse,
) {
    let connection_response =
        unsafe { Box::from_raw(connection_response_ptr as *mut ConnectionResponse) };
    drop(connection_response);
}

/// Deallocates a `RedisErrorFFI`.
///
/// # Safety
///
/// * `error_ptr` must be able to be safely casted to a valid `Box<RedisErrorFFI>` via `Box::from_raw`. See the safety documentation of [`std::boxed::Box::from_raw`](https://doc.rust-lang.org/std/boxed/struct.Box.html#method.from_raw).
/// * The error message must be able to be safely casted to a valid `CString` via `CString::from_raw`. See the safety documentation of [`std::ffi::CString::from_raw`](https://doc.rust-lang.org/std/ffi/struct.CString.html#method.from_raw).
/// * `error_ptr` must not be null.
/// * The error message pointer must not be null.
#[no_mangle]
pub unsafe extern "C" fn free_error(error_ptr: *const RedisErrorFFI) {
    let error = unsafe { Box::from_raw(error_ptr as *mut RedisErrorFFI) };
    let error_msg = unsafe { CString::from_raw(error.message as *mut c_char) };
    drop(error);
    drop(error_msg);
}
