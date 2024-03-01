<?php

$redis = new Redis();
$redis->connect('127.0.0.1', 6379);

echo $redis->ping('PONG'), "\n";
$infoResult = $redis->info();
var_dump($infoResult['redis_version']);
