package babushka.benchmarks.lettuce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import babushka.benchmarks.clients.lettuce.LettuceAsyncClient;
import babushka.benchmarks.utils.ConnectionSettings;
import io.lettuce.core.RedisFuture;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class LettuceAsyncClientIT {

  private static LettuceAsyncClient lettuceClient;

  private static LettuceAsyncClient otherLettuceClient;

  @BeforeAll
  static void initializeLettuceClient() {
    lettuceClient = new LettuceAsyncClient();
    lettuceClient.connectToRedis(new ConnectionSettings("localhost", 6379, false, false));

    otherLettuceClient = new LettuceAsyncClient();
    otherLettuceClient.connectToRedis(new ConnectionSettings("localhost", 6379, false, false));
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
      fail("SET result failed with exception " + e);
    }
    try {
      otherLettuceClient.waitForResult(otherSetResult);
    } catch (Exception e) {
      fail("SET result on other client failed with exception " + e);
    }

    RedisFuture getResult = lettuceClient.asyncGet(key);
    RedisFuture otherGetResult = otherLettuceClient.asyncGet(otherKey);
    String result = "invalid";
    String otherResult = "invalid";
    try {
      result = (String) lettuceClient.waitForResult(getResult);
    } catch (Exception e) {
      fail("GET result failed with exception " + e);
    }

    try {
      otherResult = (String) otherLettuceClient.waitForResult(otherGetResult);
    } catch (Exception e) {
      fail("GET result on other client failed with exception " + e);
    }

    assertEquals(value, result);
    assertEquals(otherValue, otherResult);
  }
}
