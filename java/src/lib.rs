use babushka::start_socket_listener;

use jni::objects::{JClass, JObject, JThrowable, JObjectArray};
use jni::JNIEnv;
use jni::sys::jlong;
use std::sync::mpsc;
use log::error;
use redis::Value;

fn redis_value_to_java<'local>(env: &mut JNIEnv<'local>, val: Value) -> JObject<'local> {
    match val {
        Value::Nil => JObject::null(),
        Value::Status(str) => JObject::from(env.new_string(str).unwrap()),
        Value::Okay => JObject::from(env.new_string("OK").unwrap()),
        // TODO use primitive integer
        Value::Int(num) => env.new_object("java/lang/Integer", "(I)V", &[num.into()]).unwrap(),
        Value::Data(data) => match std::str::from_utf8(data.as_ref()) {
            Ok(val) => JObject::from(env.new_string(val).unwrap()),
            Err(_err) => {
                let _ = env.throw("Error decoding Unicode data");
                JObject::null()
            },
        },
        Value::Bulk(bulk) => {
            // TODO: Consider caching the method ID here in a static variable (might need RwLock to mutate)
            let items: JObjectArray = env
                .new_object_array(bulk.len() as i32, "java/lang/Object", JObject::null())
                .unwrap();

            for (i, item) in bulk.into_iter().enumerate() {
                let java_value = redis_value_to_java(env, item);
                env.set_object_array_element(&items, i as i32, java_value)
                    .unwrap();
            }

            items.into()
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_babushka_FFI_BabushkaCoreNativeDefinitions_valueFromPointer<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    pointer: jlong
) -> JObject<'local> {
    let value = unsafe { Box::from_raw(pointer as *mut Value) };
    redis_value_to_java(&mut env, *value)
}

#[no_mangle]
pub extern "system" fn Java_babushka_FFI_BabushkaCoreNativeDefinitions_startSocketListenerExternal<'local>(
    env: JNIEnv<'local>,
    _class: JClass<'local>
) -> JObject<'local> {
    let (tx, rx) = mpsc::channel::<Result<String, String>>();

    start_socket_listener(move |socket_path : Result<String, String>| {
        // Signals that thread has started
        let _ = tx.send(socket_path);
    });

    // Wait until the thread has started
    let socket_path = rx.recv();

    match socket_path {
        Ok(Ok(path)) => {
            env.new_string(path).unwrap().into()
        },
        Ok(Err(error_message)) => {
            throw_java_exception(env, error_message);
            JObject::null()
        },
        Err(error) => {
            throw_java_exception(env, error.to_string());
            JObject::null()
        }
    }
}

fn throw_java_exception(mut env: JNIEnv, message: String) {
    let res = env.new_object(
        "java/lang/Exception",
        "(Ljava/lang/String;)V",
        &[
            (&env.new_string(message.clone()).unwrap()).into(),
        ]);

    match res {
        Ok(res) => {
            let _ = env.throw(JThrowable::from(res));
        },
        Err(err) => {
            error!("Failed to create exception with string {}: {}", message, err.to_string());
        }
    };
}
