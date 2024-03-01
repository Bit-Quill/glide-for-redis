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

// use function Amp\delay;

class RedisClient
{
    protected static $ffi;
    protected static $lock;

    /**
     * @var Connection
     */
    protected Connection $connection;

    /**
     * @var AsyncCommandTransport
     */
    protected AsyncCommandTransport $asyncCommandTransport;

    /**
     * @var PromiseContainer
     */
    protected PromiseContainer $promiseContainer;

    protected function successCallback(): \Closure
    {
        return function (int $index, ?string $message) {

            echo memory_get_usage() . PHP_EOL;
            $messageCpy = null;
            if (is_array($message)) {
                print "DETECTED AS AN ARRAY" . PHP_EOL;
                var_dump($message);
            }
            if ($message != null) {
                $messageCpy = preg_replace('/[[:^print:]]/', '', $message);
                print "SUCCESS(idx: $index, msg: $messageCpy)" . PHP_EOL;
            } else {
                print "SUCCESS(idx: $index)" . PHP_EOL;
            }
            echo memory_get_usage() . PHP_EOL;

            $promise = $this->promiseContainer->getPromise($index);
            // mark the async operation as complete
            if ($messageCpy == null) {
                $promise->complete("OK");
            } else {
                $promise->complete($messageCpy);
            }

            if (RedisClient::$lock->acquire()) {
                $this->promiseContainer->freePromise($index);
                RedisClient::$lock->release();
            }
        };
    }

    protected function failureCallback(): \Closure
    {
        return function (int $index) {
            $indexCpy = $index;
            print "FAILURE (idx: $indexCpy)" . PHP_EOL;

            $promise = $this->promiseContainer->getPromise($index);
            if (!isset($promise)) {
                // something went wrong!
                throw new ErrorException("Promise not available");
            }
            // mark the async operation as error with a request error
            $promise->error(new ErrorException("Request Error"));
            $this->promiseContainer->freePromise($index);
        };
    }

    /**
     * @param $host string
     * @param $port int
     * @param $useTls bool
     * @throws ErrorException
     */
    public function __construct($host, $port, $useTls)
    {
        // TODO move to static initiation
        if (RedisClient::$ffi == null) {
            $header = file_get_contents('target/debug/glide_rs.h');
            RedisClient::$ffi = FFI::cdef(
                $header,
                'target/debug/libglide_rs.dylib'
            );
        }
        $ffi = RedisClient::$ffi;

        // TODO move to static initiation
        if (RedisClient::$lock == null) {
            $store = new SemaphoreStore();
            $factory = new LockFactory($store);
            RedisClient::$lock = $factory->createLock('promise-lock');
        }

        // connect to Redis

        // TODO move this work to the Connection class
        // TODO apply async promises
        // TODO update to use protobuf connection objects
        $client = $ffi->create_client(
            $host,
            $port,
            $useTls,
            $this->successCallback(),
            $this->failureCallback()
        );
        var_dump($client);

        if ($client == null) {
            throw new ErrorException("Failed creating a client");
        }

        // TODO return a promise of a client connection
        $this->connection = new Connection(
            $client,
            $this->successCallback(),
            $this->failureCallback()
        );

        $this->asyncCommandTransport = new AsyncCommandTransport();
        $this->promiseContainer = new PromiseContainer();
    }

    public function close()
    {
        print "Closing client" . PHP_EOL;
        RedisClient::$ffi->close_client($this->connection->getClient());
    }

    public function set($key, $value): Future
    {
        $deferredFuture = new DeferredFuture();
        $client = $this->connection->getClient();

        $callbackId = 0;
        if (RedisClient::$lock->acquire()) {
            $callbackId = $this->promiseContainer->getFreeCallbackId();
            print "savePromise($callbackId, future)" . PHP_EOL;
            $this->promiseContainer->savePromise($callbackId, $deferredFuture);

            RedisClient::$lock->release();
        }

        $callbackIdCData = RedisClient::$ffi->new();
        $callbackIdCData->cdata = $callbackId;
        $keyCData = RedisClient::$ffi->new('const char *');
        $keyCData->cdata = $key;
        $valueCData = RedisClient::$ffi->new('const char *');
        $valueCData->cdata = $value;
        RedisClient::$ffi->set($client, $callbackIdCData, $keyCData, $valueCData);

        return $deferredFuture->getFuture();
    }

    public function get($key): Future
    {
        $deferredFuture = new DeferredFuture();
        $client = $this->connection->getClient();

        $callbackId = 0;
        if (RedisClient::$lock->acquire()) {
            $callbackId = $this->promiseContainer->getFreeCallbackId();
            print "savePromise($callbackId, future)" . PHP_EOL;
            $this->promiseContainer->savePromise($callbackId, $deferredFuture);

            RedisClient::$lock->release();
        }

        RedisClient::$ffi->get($client, $callbackId, $key);

        return $deferredFuture->getFuture();
    }
}
