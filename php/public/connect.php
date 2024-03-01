<?php

$redis = new Redis();
//Connecting to Redis
$redis->connect('hostname', port);
$redis->auth('password');

if ($redis->ping()) {
    echo "PONG";
}

?>


