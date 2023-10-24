/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package javababushka.benchmarks.lettuce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import io.lettuce.core.RedisFuture;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class LettuceAsyncClientIT {

  private static LettuceAsyncClient lettuceClient;

  private static LettuceAsyncClient otherLettuceClient;

  @BeforeAll
  static void initializeJedisClient() {
    lettuceClient = new LettuceAsyncClient();
    lettuceClient.connectToRedis();

    otherLettuceClient = new LettuceAsyncClient();
    otherLettuceClient.connectToRedis();
  }

  @AfterAll
  static void closeConnection() {
    lettuceClient.closeConnection();
    otherLettuceClient.closeConnection();
  }

  @Test
  public void testResourceSetGet() {
    String key = "key1";
    String value = "my-value-1";

    String otherKey = "key2";
    String otherValue = "my-value-2";

    RedisFuture setResult = lettuceClient.asyncSet(key, value);
    RedisFuture otherSetResult = otherLettuceClient.asyncSet(otherKey, otherValue);

    // and wait for both clients
    try {
      lettuceClient.waitForResult(setResult);
    } catch (Exception e) {
      fail("Can SET redis result without Exception");
    }
    try {
      otherLettuceClient.waitForResult(otherSetResult);
    } catch (Exception e) {
      fail("Can SET other redis result without Exception");
    }

    RedisFuture getResult = lettuceClient.asyncGet(key);
    RedisFuture otherGetResult = otherLettuceClient.asyncGet(otherKey);
    String result = "invalid";
    String otherResult = "invalid";
    try {
      result = (String) lettuceClient.waitForResult(getResult);
    } catch (Exception e) {
      fail("Can GET redis result without Exception");
    }

    try {
      otherResult = (String) otherLettuceClient.waitForResult(otherGetResult);
    } catch (Exception e) {
      fail("Can GET other redis result without Exception");
    }

    assertEquals(value, result);
    assertEquals(otherValue, otherResult);
  }
}
