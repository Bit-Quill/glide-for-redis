<?php declare(strict_types = 1);

$header = file_get_contents('target/debug/glide_rs.h');
$ffi = \FFI::cdef(
    $header,
    'target/debug/libglide_rs.dylib'
);

//$leveltype = \FFI::new($ffi->Level);
//var_dump($leveltype);
//
//$loglevel = $ffi->init($ffi->Option_Level->Warn, 'log.output');
//var_dump($loglevel);

$successCallbackFunc = function ($index, $message) {
    print "SUCCESS" . PHP_EOL;
    print "index: " . PHP_EOL;
    var_dump($index);
    print "message: " . PHP_EOL;
    var_dump($message);
};

$failureCallbackFunc = function ($message) {
    print "FAILURE" . PHP_EOL;
    print "message: " . PHP_EOL;
    var_dump($message);
};

$client = $ffi->create_client('127.0.0.1', 6379, false, $successCallbackFunc, $failureCallbackFunc);
var_dump($client);
if ($client == null) {
    throw new ErrorException("Failed creating a client");
}

$result = $ffi->get($client, 1, 'apples');

// have to sleep because we haven't set up callbacks yet
print "sleep " . PHP_EOL;
sleep(2);

print "result: " . PHP_EOL;
var_dump($result);

$result = $ffi->get($client, 1, 'invalid');

// have to sleep because we haven't set up callbacks yet
print "sleep " . PHP_EOL;
sleep(2);

print "result: " . PHP_EOL;
var_dump($result);

$ffi->close_client($client);



