<?php

namespace Glide;

use ErrorException;
use FFI;
use FFI\CType;
use Glide\Handlers\AsyncCommandTransport;
use Glide\Handlers\ConnectionTransport;
use Glide\Handlers\PromiseContainer;
use Amp\Future;
use Amp\DeferredFuture;
use Symfony\Component\Lock\LockFactory;
use Symfony\Component\Lock\Store\SemaphoreStore;

use function Amp\async;

$channels = [];

class RedisClient
{
    // use Amp\Sync\Mutex instead
    protected static $lock;
    protected FFI $ffi;
    private static int $nextIndex = 0;

    /**
     * Client object provided
     * @var FFI\CData
     */
    protected FFI\CData $clientPtr;

    /**
     * @var PromiseContainer
     */
    public PromiseContainer $promiseContainer;

    /**
     * @var array
     */
    protected array $channels;

    /**
     * @param $host string
     * @param $port int
     * @param $useTls bool
     * @throws ErrorException
     */
    public function __construct($host, $port, $useTls)
    {
        $header = file_get_contents('target/debug/glide_rs.h');
        $this->ffi = FFI::cdef(
            $header,
            'target/debug/libglide_rs.dylib'
        );

        // TODO move to static initiation
        if (RedisClient::$lock == null) {
            $store = new SemaphoreStore();
            $factory = new LockFactory($store);
            RedisClient::$lock = $factory->createLock('promise-lock');
        }

        $this->channels = array();
        $channels = &$this->channels;

        $successCallback = function ($index, $message) use (&$channels) {
            // spawn callback in a separate thread
            async(function () use ($index, $message, &$channels) {
                if ($message != null) {
                    print "SUCCESS(idx: $index, msg: $message)" . PHP_EOL;
                } else {
                    print "SUCCESS(idx: $index)" . PHP_EOL;
                    $message = "OK";
                }
                try {
                    $channel = $channels[$index][1];
                    if (!$channel->isClosed()) {
                        delay(2);
                        $channel->send($message);
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
            // spawn callback in a separate thread
            async(function () use ($index, &$channels) {
                $indexCpy = $index;
                print "FAILURE (idx: $indexCpy)" . PHP_EOL;

                // TODO handle error case
            });
        };

        // TODO move this work to the Connection class
        // TODO apply async promises
        // TODO update to use protobuf connection objects
        $this->clientPtr = $this->ffi->create_client(
            $host,
            $port,
            $useTls,
            $successCallback,
            $failureCallback
        );

        if ($this->clientPtr == null) {
            throw new ErrorException("Failed creating a client");
        }
    }

    public function close()
    {
        print "Closing client" . PHP_EOL;
        $this->ffi->close_client($this->clientPtr);
        foreach ($this->channels as $channel) {
            [$left, $right] = $channel;
            $left->close();
            $right->close();
        }
    }

    public function set($key, $value): Future
    {
        $getFunction = function ($key, $value, $callbackId, \Amp\Sync\Channel $channel, $client, $ffi) {
            print "call ffi->set ($callbackId)" . PHP_EOL;
            $ffi->set($client, $callbackId, $key, $value);

            try {
                return $channel->receive(new \Amp\TimeoutCancellation(5));
            } catch (\Amp\Sync\ChannelException $e) {
                print "Unable to receive: ChannelException $e" . PHP_EOL;
            } catch (\Amp\Serialization\SerializationException $e) {
                print "Unable to receive: SerializationException $e" . PHP_EOL;
            }
            return "OK";
        };

        $callbackId = 0;
        if (RedisClient::$lock->acquire()) {
            $callbackId = self::$nextIndex++;
            RedisClient::$lock->release();
        }
        $this->channels[$callbackId] = createChannelPair();

        return \Amp\async($getFunction, $key, $value, $callbackId, $this->channels[$callbackId][0], $this->clientPtr, $this->ffi);
    }

    public function get($key): Future
    {
        $getFunction = function ($key, $callbackId, \Amp\Sync\Channel $channel, $client, $ffi) {
            print "call ffi->get ($callbackId)" . PHP_EOL;
            $ffi->get($client, $callbackId, $key);

            try {
                return $channel->receive(new \Amp\TimeoutCancellation(5));
            } catch (\Amp\Sync\ChannelException $e) {
                print "Unable to receive: ChannelException $e" . PHP_EOL;
            } catch (\Amp\Serialization\SerializationException $e) {
                print "Unable to receive: SerializationException $e" . PHP_EOL;
            }
            return "OK";
        };

        $callbackId = 0;
        if (RedisClient::$lock->acquire()) {
            $callbackId = self::$nextIndex++;
            RedisClient::$lock->release();
        }
        $this->channels[$callbackId] = createChannelPair();

        return \Amp\async($getFunction, $key, $callbackId, $this->channels[$callbackId][0], $this->clientPtr, $this->ffi);
    }
}
