package babushka.benchmarks.jedis;

import static org.junit.jupiter.api.Assertions.assertTrue;

import babushka.benchmarks.clients.jedis.JedisClient;
import babushka.benchmarks.utils.Benchmarking;
import babushka.benchmarks.utils.ChosenAction;
import babushka.benchmarks.utils.ConnectionSettings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class JedisClientIT {

  private static JedisClient jedisClient;

  @BeforeAll
  static void initializeJedisClient() {
    jedisClient = new JedisClient();
    jedisClient.connectToRedis(new ConnectionSettings("localhost", 6379, false, false));
  }

  @Test
  public void testResourceInfo() {
    String result = jedisClient.info();

    assertTrue(result.length() > 0);
  }

  @Test
  public void testResourceInfoBySection() {
    String section = "Server";
    String result = jedisClient.info(section);

    assertTrue(result.length() > 0);
    assertTrue(result.startsWith("# " + section));
  }

  @Test
  public void testResourceSetGet() {
    int iterations = 100000;
    String value = "my-value";

    Map<ChosenAction, Benchmarking.Operation> actions = new HashMap<>();
    actions.put(ChosenAction.GET_EXISTING, () -> jedisClient.get(Benchmarking.generateKeySet()));
    actions.put(
        ChosenAction.GET_NON_EXISTING, () -> jedisClient.get(Benchmarking.generateKeyGet()));
    actions.put(ChosenAction.SET, () -> jedisClient.set(Benchmarking.generateKeySet(), value));

    Map<ChosenAction, List<Long>> latencies =
        Map.of(
            ChosenAction.GET_EXISTING, new ArrayList<>(),
            ChosenAction.GET_NON_EXISTING, new ArrayList<>(),
            ChosenAction.SET, new ArrayList<>());
    for (int i = 0; i < iterations; i++) {
      var latency = Benchmarking.measurePerformance(actions);
      latencies.get(latency.getKey()).add(latency.getValue());
    }

    Benchmarking.printResults(Benchmarking.calculateResults(latencies), 0, iterations);
  }
}
