/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package javabushka.client.jedis;

import javabushka.client.SyncClient;
import javabushka.client.utils.ConnectionSettings;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class JedisClient implements SyncClient {

  public static final String DEFAULT_HOST = "localhost";
  public static final int DEFAULT_PORT = 6379;

  protected Jedis jedisResource;

  public boolean someLibraryMethod() {
    return true;
  }

  @Override
  public void connectToRedis() {
    JedisPool pool = new JedisPool(DEFAULT_HOST, DEFAULT_PORT);
    jedisResource = pool.getResource();
  }

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
