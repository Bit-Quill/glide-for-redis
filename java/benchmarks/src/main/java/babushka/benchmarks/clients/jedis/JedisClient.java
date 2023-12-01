package babushka.benchmarks.clients.jedis;

import babushka.benchmarks.clients.SyncClient;
import babushka.benchmarks.utils.ConnectionSettings;
import redis.clients.jedis.Jedis;

/** A Jedis client with sync capabilities. See: https://github.com/redis/jedis */
public class JedisClient implements SyncClient {

  public static final String DEFAULT_HOST = "localhost";
  public static final int DEFAULT_PORT = 6379;

  protected Jedis jedisResource;

  @Override
  public void closeConnection() {
    try {
      jedisResource.close();
    } catch (Exception ignored) {
    }
  }

  @Override
  public String getName() {
    return "Jedis";
  }

  @Override
  public void connectToRedis(ConnectionSettings connectionSettings) {
    jedisResource =
        new Jedis(connectionSettings.host, connectionSettings.port, connectionSettings.useSsl);
    jedisResource.connect();
    if (!jedisResource.isConnected()) {
      throw new RuntimeException("failed to connect to jedis");
    }
  }

  public String info() {
    return jedisResource.info();
  }

  public String info(String section) {
    return jedisResource.info(section);
  }

  @Override
  public void set(String key, String value) {
    jedisResource.set(key, value);
  }

  @Override
  public String get(String key) {
    return jedisResource.get(key);
  }
}
