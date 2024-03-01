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

$successCallback = $ffi->new('SuccessCallback');
$failureCallback = $ffi->new('FailureCallback');

$client = $ffi->create_client('127.0.0.1', 6379, false, $successCallback, $failureCallback);
var_dump($client);
if ($client == null) {
    throw new ErrorException("Failed creating a client");
}

// make a set call
$randomNumber = rand();
$value = md5("$randomNumber");
echo "SET(apples, $value)\n";
$ffi->set($client, 1, 'apples', $value);

// have to sleep because we haven't set up callbacks yet
sleep(1);

$ffi->get($client, 2, 'apples');
echo "GET(apples)\n";

$ffi->close_client($client);



