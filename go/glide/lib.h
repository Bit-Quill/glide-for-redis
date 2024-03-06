#include <stdarg.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdlib.h>

/**
 * FFI compatible version of the ErrorType enum defined in protobuf.
 */
typedef enum ErrorType {
  ClosingError = 0,
  RequestError = 1,
  TimeoutError = 2,
  ExecAbortError = 3,
  ConnectionError = 4,
} ErrorType;

/**
 * A Redis error.
 */
typedef struct RedisErrorFFI {
  const char *message;
  enum ErrorType error_type;
} RedisErrorFFI;

/**
 * The connection response.
 *
 * It contains either a connection or an error. It is represented as a struct instead of an enum for ease of use in the wrapper language.
 */
typedef struct ConnectionResponse {
  const void *conn_ptr;
  const struct RedisErrorFFI *error;
} ConnectionResponse;

/**
 * Success callback that is called when a Redis command succeeds.
 */
typedef void (*SuccessCallback)(uintptr_t channel_address, const char *message);

/**
 * Failure callback that is called when a Redis command fails.
 */
typedef void (*FailureCallback)(uintptr_t channel_address, const struct RedisErrorFFI *error);

/**
 * Creates a new client to the given address. The success callback needs to copy the given string synchronously, since it will be dropped by Rust once the callback returns. All callbacks should be offloaded to separate threads in order not to exhaust the client's thread pool.
 *
 * # Safety
 *
 * * `connection_request_bytes` must point to `connection_request_len` consecutive properly initialized bytes.
 * * `connection_request_len` must not be greater than `isize::MAX`. See the safety documentation of [`std::slice::from_raw_parts`](https://doc.rust-lang.org/std/slice/fn.from_raw_parts.html).
 */
const struct ConnectionResponse *create_client(const uint8_t *connection_request_bytes,
                                               uintptr_t connection_request_len,
                                               SuccessCallback success_callback,
                                               FailureCallback failure_callback);

/**
 * Closes the given client, deallocating it from the heap.
 *
 * # Safety
 *
 * * `client_ptr` must be able to be safely casted to a valid `Box<Client>` via `Box::from_raw`. See the safety documentation of [`std::boxed::Box::from_raw`](https://doc.rust-lang.org/std/boxed/struct.Box.html#method.from_raw).
 * * `client_ptr` must not be null.
 */
void close_client(const void *client_ptr);

/**
 * Deallocates a `ConnectionResponse`.
 *
 * # Safety
 *
 * * `connection_response_ptr` must be able to be safely casted to a valid `Box<RedisErrorFFI>` via `Box::from_raw`. See the safety documentation of [`std::boxed::Box::from_raw`](https://doc.rust-lang.org/std/boxed/struct.Box.html#method.from_raw).
 * * `connection_response_ptr` must not be null.
 */
void free_connection_response(const struct ConnectionResponse *connection_response_ptr);

/**
 * Deallocates a `RedisErrorFFI`.
 *
 * # Safety
 *
 * * `error_ptr` must be able to be safely casted to a valid `Box<RedisErrorFFI>` via `Box::from_raw`. See the safety documentation of [`std::boxed::Box::from_raw`](https://doc.rust-lang.org/std/boxed/struct.Box.html#method.from_raw).
 * * The error message must be able to be safely casted to a valid `CString` via `CString::from_raw`. See the safety documentation of [`std::ffi::CString::from_raw`](https://doc.rust-lang.org/std/ffi/struct.CString.html#method.from_raw).
 * * `error_ptr` must not be null.
 * * The error message pointer must not be null.
 */
void free_error(const struct RedisErrorFFI *error_ptr);
