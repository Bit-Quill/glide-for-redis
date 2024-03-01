<?php

namespace Glide\Handlers;

use ErrorException;
use Glide\Connection;

class ConnectionTransport
{
    /**
     * @var \FFI
     */
    protected $ffi;

    /**
     * @var string
     */
    protected $host;

    /**
     * @var int
     */
    protected $port;

    /**
     * @var bool
     */
    protected $useTls;

    /**
     * @var bool
     */
    protected $clusterMode;

    /**
     * @var PromiseContainer
     */
    protected $messageContainer;

    public function __construct(\FFI $ffi, $host, $port, $useTls, $clusterMode)
    {
        $this->ffi = $ffi;
        $this->host = $host;
        $this->port = $port;
        $this->useTls = $useTls;
        $this->clusterMode = $clusterMode;
    }

    public function connectToRedis() {
        $successCallback = $this->ffi->new('SuccessCallback');
        $failureCallback = $this->ffi->new('FailureCallback');

        // TODO apply async promises
        $client = $this->ffi->create_client($this->host, $this->port, $this->useTls, $successCallback, $failureCallback);
        var_dump($client);
        if ($client == null) {
            throw new ErrorException("Failed creating a client");
        }

        // TODO return a promise of a client connection
        return new Connection($client, $successCallback, $failureCallback);
    }

    public function getCallbackRequest() {

    }
}
