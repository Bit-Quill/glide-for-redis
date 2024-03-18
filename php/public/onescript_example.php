<?php

declare(strict_types=1);

require __DIR__ . '/../vendor/autoload.php';

use Glide\RedisClient;

$host = '127.0.0.1';
$port = 6379;
try {
    $client = new RedisClient($host, $port, false);
} catch (ErrorException $e) {
    print "Unable to create client: " . $e->getMessage() . PHP_EOL;
    exit(0);
}

$randomNumber = rand();
$value = md5("$randomNumber");

$resultB = \Amp\async($client->get(...), "apples");
$resultA = \Amp\async($client->get(...), "oranges");
print ">>> GET RESULT:" . PHP_EOL;
\Amp\delay(1);
$resultB->await();
$resultA->await();

$client->close();

exit();
