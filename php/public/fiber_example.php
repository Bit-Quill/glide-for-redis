<?php

declare(strict_types=1);

require __DIR__ . '/../vendor/autoload.php';

$channels = [];

$successCallback = function ($index, ?string $message) use (&$channels) {
    // spawn callback in a separate thread
    \Amp\async(function () use ($index, $message, &$channels) {
        if ($message != null) {
            $message = preg_replace('/[[:^print:]]/', '', $message);
            print "SUCCESS(idx: $index, msg: $message)" . PHP_EOL;
        } else {
            print "SUCCESS(idx: $index)" . PHP_EOL;
            $message = "OK";
        }
        try {
            $channel = $channels[$index][1];
            if (!$channel->isClosed()) {
                $channel->send($message);
                print "DONE" . PHP_EOL;
            } else {
                throw new \Amp\Sync\ChannelException("Channel unexpectedly closed");
            }
        } catch (\Amp\Sync\ChannelException $e) {
            print "Unable to send: ChannelException $e" . PHP_EOL;
        } catch (\Amp\Serialization\SerializationException $e) {
            print "Unable to send: SerializationException $e" . PHP_EOL;
        }
    });
};

$failureCallback = function (int $index) {
    $indexCpy = $index;
    print "FAILURE (idx: $indexCpy)" . PHP_EOL;

    $promise = $this->promiseContainer->getPromise($index);
    if (!isset($promise)) {
        // something went wrong!
        print "Promise not available" . PHP_EOL;
    }
    // mark the async operation as error with a request error
    $promise->error(new ErrorException("Request Error"));
    $this->promiseContainer->freePromise($index);
};

$header = file_get_contents('target/debug/glide_rs.h');
$ffi = FFI::cdef(
    $header,
    'target/debug/libglide_rs.dylib'
);

$host = '127.0.0.1';
$port = 6379;
$useTls = false;

$client = $ffi->create_client(
    $host,
    $port,
    $useTls,
    $successCallback,
    $failureCallback
);

//$randomNumber = rand();
//$value = md5("$randomNumber");

$getFunction = function ($key, $callbackId, \Amp\Sync\Channel $channel, $client, $ffi) {
    print "call ffi->get ($callbackId)" . PHP_EOL;
    $ffi->get($client, $callbackId, $key);

    print "WAITING TO RECEIVE DATA ($callbackId): " . PHP_EOL;
    try {
        \Amp\delay(2);
        $result = $channel->receive(new \Amp\TimeoutCancellation(5));
        print "RECEIVED: $result" . PHP_EOL;
        return $result;
    } catch (\Amp\Sync\ChannelException $e) {
        print "Unable to receive: ChannelException $e" . PHP_EOL;
    } catch (\Amp\Serialization\SerializationException $e) {
        print "Unable to receive: SerializationException $e" . PHP_EOL;
    }
    return "OK";
};

$channels[] = \Amp\Sync\createChannelPair();
$channels[] = \Amp\Sync\createChannelPair();
$channels[] = \Amp\Sync\createChannelPair();
$channels[] = \Amp\Sync\createChannelPair();

$results[] = \Amp\async($getFunction, "oranges", 0, $channels[0][0], $client, $ffi);
$results[] = \Amp\async($getFunction, "apples", 1, $channels[1][0], $client, $ffi);
$results[] = \Amp\async($getFunction, "oranges", 2, $channels[2][0], $client, $ffi);
$results[] = \Amp\async($getFunction, "apples", 3, $channels[3][0], $client, $ffi);

\Amp\delay(1);
print ">>> GET RESULT: " . $results[0]->await() . PHP_EOL;
print ">>> GET RESULT: " . $results[1]->await() . PHP_EOL;
print ">>> GET RESULT: " . $results[2]->await() . PHP_EOL;
print ">>> GET RESULT: " . $results[3]->await() . PHP_EOL;

print "Closing all" . PHP_EOL;
$ffi->close_client($client);
foreach ($channels as $channelPair) {
    $channelPair[0]->close();
    $channelPair[1]->close();
}
