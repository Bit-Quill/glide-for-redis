<?php

namespace Glide\Handlers;

use Amp\DeferredFuture;
use Amp\Future;
use ErrorException;
use PHPUnit\TextUI\RuntimeException;
use SplDoublyLinkedList;
use SplQueue;

class PromiseContainer
{
    private static function mutex()
    {
        static $mutex = null;
        if ($mutex == null) {
            $mutex = new SyncMutex("concurrentLock");
        }
        return $mutex;
    }
    private static $mutex;

    // TODO make this thread safe
    // use an atomic counter or concurrent list
    private static int $nextIndex = 0;

    /**
     * @var array
     */
    public array $promises;

    /**
     * @var array
     */
    public array $availablePromises;

    public function __construct()
    {
        $this->promises = array();
        $this->availablePromises = array();
    }

    /**
     * @param $index int
     * @return DeferredFuture
     * @throws ErrorException
     */
    public function getPromise(int $index): ?DeferredFuture
    {
        if ($index == null) {
            $index = 0;
        }
        if (!isset($index)) {
            throw new ErrorException("Index not set");
        }
        if (!is_int($index)) {
            throw new ErrorException("Index not an int");
        }
        if (!is_integer($index)) {
            throw new ErrorException("Index is not an integer");
        }
        if (!array_key_exists($index, $this->promises)) {
            throw new ErrorException("Promise not available: $index");
        }
        return $this->promises[$index];
    }

    public function getFreeCallbackId(): int
    {
        // add mutex lock around this
        //        if (empty($this->availablePromises)) {
        return self::$nextIndex++;
        //        }
        //        return array_pop($this->availablePromises);
    }

    /**
     * @param $id
     * @param DeferredFuture $future
     * @return void
     */
    public function savePromise($id, DeferredFuture $future): void
    {
        $this->promises[$id] = $future;
    }

    /**
     * @param $index
     */
    public function freePromise($index): void
    {
        if (array_key_exists($index, $this->promises)) {
            $f = $this->promises[$index];
            unset($this->promises[$index]);
            $this->availablePromises[] = $index;
            var_dump($f);
        }
    }
}
