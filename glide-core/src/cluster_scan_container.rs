/**
 * Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0
 */
use logger_core::log_info;
use once_cell::sync::Lazy;
use redis::{RedisResult, ScanStateRC};
use sha1_smol::Sha1;
use std::{collections::HashMap, sync::Mutex};

// This is a container for storing the cursor of a cluster scan.
// The cursor for a cluster scan is a ref to the actual ScanState struct in redis-rs.
// In order to avoid dropping it when it is passed between layers of the application,
// we store it in this container and only pass the hash of the cursor.
// The cursor is stored in the container and can be retrieved using the hash.
// In wrapper layer we wrap the hash in an object, which, when dropped, trigger the removal of the cursor from the container.
// When the ref is removed from the container, the actual ScanState struct is dropped by Rust GC.

static CONTAINER: Lazy<Mutex<HashMap<String, ScanStateRC>>> =
    Lazy::new(|| Mutex::new(HashMap::new()));

pub fn insert_cluster_scan_cursor(scan_state: ScanStateRC) -> String {
    let hash = Sha1::new();
    let hash = hash.digest().to_string();
    CONTAINER.lock().unwrap().insert(hash.clone(), scan_state);
    hash
}

pub fn get_cluster_scan_cursor(hash: String) -> RedisResult<ScanStateRC> {
    let scan_state_rc = CONTAINER.lock().unwrap().get(&hash).cloned();
    match scan_state_rc {
        Some(scan_state_rc) => Ok(scan_state_rc),
        None => Err(redis::RedisError::from((
            redis::ErrorKind::ResponseError,
            "Invalid scan_state_cursor hash",
        ))),
    }
}

pub fn remove_scan_state_cursor(hash: String) {
    log_info(
        "scan_state_cursor lifetime",
        format!("Removed scan_state_cursor with hash: `{hash}`"),
    );
    CONTAINER.lock().unwrap().remove(&hash);
}
