package javababushka.benchmarks.clients;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javababushka.benchmarks.utils.ConnectionSettings;

/**
 * A Lettuce client with async capabilities
 * see: https://lettuce.io/
 */
public class LettuceAsyncClient implements AsyncClient {

  RedisClient client;
  RedisAsyncCommands asyncCommands;
  StatefulRedisConnection<String, String> connection;

  @Override
  public void connectToRedis() {
    connectToRedis(new ConnectionSettings("localhost", 6379, false));
  }

  @Override
  public void connectToRedis(ConnectionSettings connectionSettings) {
    client =
        RedisClient.create(
            String.format(
                "%s://%s:%d",
                connectionSettings.useSsl ? "rediss" : "redis",
                connectionSettings.host,
                connectionSettings.port));
    connection = client.connect();
    asyncCommands = connection.async();
  }

  @Override
  public RedisFuture<?> asyncSet(String key, String value) {
    return asyncCommands.set(key, value);
  }

  @Override
  public RedisFuture<String> asyncGet(String key) {
    return asyncCommands.get(key);
  }

  @Override
  public Object waitForResult(Future future) {
    return waitForResult(future, DEFAULT_TIMEOUT);
  }

  @Override
  public Object waitForResult(Future future, long timeoutMS) {
    try {
      return future.get(timeoutMS, TimeUnit.MILLISECONDS);
    } catch (Exception ignored) {
      return null;
    }
  }

  @Override
  public void closeConnection() {
    connection.close();
    client.shutdown();
  }

  @Override
  public String getName() {
    return "Lettuce Async";
  }
}
