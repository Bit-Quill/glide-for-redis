<?php

declare(strict_types = 1);

require __DIR__ . '/../vendor/autoload.php';

//$leveltype = \FFI::new($ffi->Level);
//var_dump($leveltype);
//
//$loglevel = $ffi->init($ffi->Option_Level->Warn, 'log.output');
//var_dump($loglevel);

use \Glide\RedisClient;

$host = '127.0.0.1';
$port = 6379;
try {
    $client = new RedisClient($host, $port, false);
} catch (ErrorException $e) {
    print "Unable to create client: " . $e->getMessage() . PHP_EOL;
    exit(0);
}

$key = "apples";
$randomNumber = rand();
$value = md5("$randomNumber");

$tasks = [
    // $client->get($key),
    // $client->get($key),
    // $client->get($key),
    // $client->get($key),
    // $client->get($key),
    $client->set($key, $value)
];
try {
    $responses = \Amp\Future\await($tasks);
    var_dump($responses);
} catch (Exception $e) {
    // If any one of the requests fails the combo will fail
    echo $e->getMessage(), "\n";
}

//echo "GET($key): " . PHP_EOL;
//$tasks[] = $client->get($key);
//$getResult = $future->await();
//echo ">>> $getResult" . PHP_EOL;
//
//echo "SET($key, $value): " . PHP_EOL;
//$result = $client->set($key, $value)->await();
//echo ">>> $result" . PHP_EOL;

$client->close();

exit();
