<?php

namespace Glide\Handlers;

class Message
{
    /**
     * @var int
     */
    public $index;

    public function __construct($index)
    {
        $this->index = $index;
    }

}
