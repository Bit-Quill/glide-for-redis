<?php

namespace Glide;

class Connection
{
    /**
     * Client object provided
     * @var FFI\CData
     */
    protected $client;

    protected $successCallback;
    protected $failureCallback;

    public function __construct($client, $successCallback, $failureCallback)
    {
        $this->client = $client;
        $this->successCallback = $successCallback;
        $this->failureCallback = $failureCallback;
    }

    public function getClient()
    {
        return $this->client;
    }
}
