/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.standalone;

import static glide.TestConfiguration.REDIS_VERSION;
import static glide.TestUtilities.checkFunctionStatsResponse;
import static glide.TestUtilities.commonClientConfig;
import static glide.TestUtilities.getValueFromInfo;
import static glide.TestUtilities.parseInfoResponseToMap;
import static glide.api.BaseClient.OK;
import static glide.api.models.commands.InfoOptions.Section.CLUSTER;
import static glide.api.models.commands.InfoOptions.Section.CPU;
import static glide.api.models.commands.InfoOptions.Section.EVERYTHING;
import static glide.api.models.commands.InfoOptions.Section.MEMORY;
import static glide.api.models.commands.InfoOptions.Section.SERVER;
import static glide.api.models.commands.InfoOptions.Section.STATS;
import static glide.cluster.CommandTests.DEFAULT_INFO_SECTIONS;
import static glide.cluster.CommandTests.EVERYTHING_INFO_SECTIONS;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import glide.api.RedisClient;
import glide.api.models.commands.FlushMode;
import glide.api.models.commands.InfoOptions;
import glide.api.models.exceptions.RequestException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(10) // seconds
public class CommandTests {

    private static final String INITIAL_VALUE = "VALUE";

    private static RedisClient regularClient = null;

    @BeforeAll
    @SneakyThrows
    public static void init() {
        regularClient =
                RedisClient.CreateClient(commonClientConfig().requestTimeout(7000).build()).get();
    }

    @AfterAll
    @SneakyThrows
    public static void teardown() {
        regularClient.close();
    }

    @Test
    @SneakyThrows
    public void custom_command_info() {
        Object data = regularClient.customCommand(new String[] {"info"}).get();
        assertTrue(((String) data).contains("# Stats"));
    }

    @Test
    @SneakyThrows
    public void custom_command_del_returns_a_number() {
        String key = "custom_command_del_returns_a_number";
        regularClient.set(key, INITIAL_VALUE).get();
        var del = regularClient.customCommand(new String[] {"DEL", key}).get();
        assertEquals(1L, del);
        var data = regularClient.get(key).get();
        assertNull(data);
    }

    @Test
    @SneakyThrows
    public void ping() {
        String data = regularClient.ping().get();
        assertEquals("PONG", data);
    }

    @Test
    @SneakyThrows
    public void ping_with_message() {
        String data = regularClient.ping("H3LL0").get();
        assertEquals("H3LL0", data);
    }

    @Test
    @SneakyThrows
    public void info_without_options() {
        String data = regularClient.info().get();
        for (String section : DEFAULT_INFO_SECTIONS) {
            assertTrue(data.contains("# " + section), "Section " + section + " is missing");
        }
    }

    @Test
    @SneakyThrows
    public void info_with_multiple_options() {
        InfoOptions.InfoOptionsBuilder builder = InfoOptions.builder().section(CLUSTER);
        if (REDIS_VERSION.isGreaterThanOrEqualTo("7.0.0")) {
            builder.section(CPU).section(MEMORY);
        }
        InfoOptions options = builder.build();
        String data = regularClient.info(options).get();
        for (String section : options.toArgs()) {
            assertTrue(
                    data.toLowerCase().contains("# " + section.toLowerCase()),
                    "Section " + section + " is missing");
        }
    }

    @Test
    @SneakyThrows
    public void info_with_everything_option() {
        InfoOptions options = InfoOptions.builder().section(EVERYTHING).build();
        String data = regularClient.info(options).get();
        for (String section : EVERYTHING_INFO_SECTIONS) {
            assertTrue(data.contains("# " + section), "Section " + section + " is missing");
        }
    }

    @Test
    @SneakyThrows
    public void simple_select_test() {
        assertEquals(OK, regularClient.select(0).get());

        String key = UUID.randomUUID().toString();
        String value = UUID.randomUUID().toString();
        assertEquals(OK, regularClient.set(key, value).get());

        assertEquals(OK, regularClient.select(1).get());
        assertNull(regularClient.get(key).get());

        assertEquals(OK, regularClient.select(0).get());
        assertEquals(value, regularClient.get(key).get());
    }

    @Test
    @SneakyThrows
    public void select_test_gives_error() {
        ExecutionException e =
                assertThrows(ExecutionException.class, () -> regularClient.select(-1).get());
        assertTrue(e.getCause() instanceof RequestException);
    }

    @Test
    @SneakyThrows
    public void clientId() {
        var id = regularClient.clientId().get();
        assertTrue(id > 0);
    }

    @Test
    @SneakyThrows
    public void clientGetName() {
        // TODO replace with the corresponding command once implemented
        regularClient.customCommand(new String[] {"client", "setname", "clientGetName"}).get();

        var name = regularClient.clientGetName().get();

        assertEquals("clientGetName", name);
    }

    @Test
    @SneakyThrows
    public void config_reset_stat() {
        String data = regularClient.info(InfoOptions.builder().section(STATS).build()).get();
        int value_before = getValueFromInfo(data, "total_net_input_bytes");

        var result = regularClient.configResetStat().get();
        assertEquals(OK, result);

        data = regularClient.info(InfoOptions.builder().section(STATS).build()).get();
        int value_after = getValueFromInfo(data, "total_net_input_bytes");
        assertTrue(value_after < value_before);
    }

    @Test
    @SneakyThrows
    public void config_rewrite_non_existent_config_file() {
        var info = regularClient.info(InfoOptions.builder().section(SERVER).build()).get();
        var configFile = parseInfoResponseToMap(info).get("config_file");

        if (configFile.isEmpty()) {
            ExecutionException executionException =
                    assertThrows(ExecutionException.class, () -> regularClient.configRewrite().get());
            assertTrue(executionException.getCause() instanceof RequestException);
        } else {
            assertEquals(OK, regularClient.configRewrite().get());
        }
    }

    @Test
    @SneakyThrows
    public void configGet_with_no_args_returns_error() {
        var exception =
                assertThrows(
                        ExecutionException.class, () -> regularClient.configGet(new String[] {}).get());
        assertTrue(exception.getCause() instanceof RequestException);
        assertTrue(exception.getCause().getMessage().contains("wrong number of arguments"));
    }

    @Test
    @SneakyThrows
    public void configGet_with_wildcard() {
        var data = regularClient.configGet(new String[] {"*file"}).get();
        assertTrue(data.size() > 5);
        assertTrue(data.containsKey("pidfile"));
        assertTrue(data.containsKey("logfile"));
    }

    @Test
    @SneakyThrows
    public void configGet_with_multiple_params() {
        assumeTrue(REDIS_VERSION.isGreaterThanOrEqualTo("7.0.0"), "This feature added in redis 7");
        var data = regularClient.configGet(new String[] {"pidfile", "logfile"}).get();
        assertAll(
                () -> assertEquals(2, data.size()),
                () -> assertTrue(data.containsKey("pidfile")),
                () -> assertTrue(data.containsKey("logfile")));
    }

    @Test
    @SneakyThrows
    public void configSet_with_unknown_parameter_returns_error() {
        var exception =
                assertThrows(
                        ExecutionException.class,
                        () -> regularClient.configSet(Map.of("Unknown Option", "Unknown Value")).get());
        assertTrue(exception.getCause() instanceof RequestException);
    }

    @Test
    @SneakyThrows
    public void configSet_a_parameter() {
        var oldValue = regularClient.configGet(new String[] {"maxclients"}).get().get("maxclients");

        var response = regularClient.configSet(Map.of("maxclients", "42")).get();
        assertEquals(OK, response);
        var newValue = regularClient.configGet(new String[] {"maxclients"}).get();
        assertEquals("42", newValue.get("maxclients"));

        response = regularClient.configSet(Map.of("maxclients", oldValue)).get();
        assertEquals(OK, response);
    }

    @SneakyThrows
    @Test
    public void echo() {
        String message = "GLIDE";
        String response = regularClient.echo(message).get();
        assertEquals(message, response);
    }

    @Test
    @SneakyThrows
    public void time() {
        // Take the time now, convert to 10 digits and subtract 1 second
        long now = Instant.now().getEpochSecond() - 1L;
        String[] result = regularClient.time().get();

        assertEquals(2, result.length);

        assertTrue(
                Long.parseLong(result[0]) > now,
                "Time() result (" + result[0] + ") should be greater than now (" + now + ")");
        assertTrue(Long.parseLong(result[1]) < 1000000);
    }

    @Test
    @SneakyThrows
    public void lastsave() {
        long result = regularClient.lastsave().get();
        var yesterday = Instant.now().minus(1, ChronoUnit.DAYS);
        assertTrue(Instant.ofEpochSecond(result).isAfter(yesterday));
    }

    @Test
    @SneakyThrows
    public void lolwut_lolwut() {
        var response = regularClient.lolwut().get();
        System.out.printf("%nLOLWUT standalone client standard response%n%s%n", response);
        assertTrue(response.contains("Redis ver. " + REDIS_VERSION));

        response = regularClient.lolwut(new int[] {30, 4, 4}).get();
        System.out.printf(
                "%nLOLWUT standalone client standard response with params 30 4 4%n%s%n", response);
        assertTrue(response.contains("Redis ver. " + REDIS_VERSION));

        response = regularClient.lolwut(5).get();
        System.out.printf("%nLOLWUT standalone client ver 5 response%n%s%n", response);
        assertTrue(response.contains("Redis ver. " + REDIS_VERSION));

        response = regularClient.lolwut(6, new int[] {50, 20}).get();
        System.out.printf(
                "%nLOLWUT standalone client ver 6 response with params 50 20%n%s%n", response);
        assertTrue(response.contains("Redis ver. " + REDIS_VERSION));
    }

    @Test
    @SneakyThrows
    public void objectFreq() {
        String key = UUID.randomUUID().toString();
        String maxmemoryPolicy = "maxmemory-policy";
        String oldPolicy =
                regularClient.configGet(new String[] {maxmemoryPolicy}).get().get(maxmemoryPolicy);
        try {
            assertEquals(OK, regularClient.configSet(Map.of(maxmemoryPolicy, "allkeys-lfu")).get());
            assertEquals(OK, regularClient.set(key, "").get());
            assertTrue(regularClient.objectFreq(key).get() >= 0L);
        } finally {
            regularClient.configSet(Map.of(maxmemoryPolicy, oldPolicy)).get();
        }
    }

    @Test
    @SneakyThrows
    public void flushall() {
        assertEquals(OK, regularClient.flushall(FlushMode.SYNC).get());

        // TODO replace with KEYS command when implemented
        Object[] keysAfter = (Object[]) regularClient.customCommand(new String[] {"keys", "*"}).get();
        assertEquals(0, keysAfter.length);

        assertEquals(OK, regularClient.flushall().get());
        assertEquals(OK, regularClient.flushall(FlushMode.ASYNC).get());
    }

    @SneakyThrows
    @Test
    public void functionLoad() {
        assumeTrue(REDIS_VERSION.isGreaterThanOrEqualTo("7.0.0"), "This feature added in redis 7");
        String libName = "mylib1C";
        String code =
                "#!lua name="
                        + libName
                        + " \n redis.register_function('myfunc1c', function(keys, args) return args[1] end)";
        assertEquals(libName, regularClient.functionLoad(code).get());
        // TODO test function with FCALL when fixed in redis-rs and implemented
        // TODO test with FUNCTION LIST

        // re-load library without overwriting
        var executionException =
                assertThrows(ExecutionException.class, () -> regularClient.functionLoad(code).get());
        assertInstanceOf(RequestException.class, executionException.getCause());
        assertTrue(
                executionException.getMessage().contains("Library '" + libName + "' already exists"));

        // re-load library with overwriting
        assertEquals(libName, regularClient.functionLoadReplace(code).get());
        String newCode =
                code + "\n redis.register_function('myfunc2c', function(keys, args) return #args end)";
        assertEquals(libName, regularClient.functionLoadReplace(newCode).get());
        // TODO test with FCALL
    }

    // @Test
    @SneakyThrows
    public void functionStats_and_functionKill() {
        assumeTrue(REDIS_VERSION.isGreaterThanOrEqualTo("7.0.0"), "This feature added in redis 7");
        String libName = "functionStats_and_functionKill";
        String funcName = "deadlock";
        // function runs an endless loop
        String code =
                "#!lua name="
                        + libName
                        + "\n"
                        + "local function sleep(keys, args)\n"
                        + "  local step = 0\n"
                        + "  while (true) do\n"
                        + "    struct.pack('HH', 1, 2)\n"
                        + "  end\n"
                        + "  return 'OK'\n"
                        + "end\n"
                        + "redis.register_function{\n"
                        + "function_name='"
                        + funcName
                        + "',\n"
                        + "callback=sleep,\n"
                        + "flags={ 'no-writes' }\n"
                        + "}";

        // nothing to kill
        var exception =
                assertThrows(ExecutionException.class, () -> regularClient.functionKill().get());
        assertInstanceOf(RequestException.class, exception.getCause());
        assertTrue(exception.getMessage().toLowerCase().contains("notbusy"));

        // load the lib
        assertEquals(libName, regularClient.functionLoadReplace(code).get());

        try (var testClient =
                RedisClient.CreateClient(commonClientConfig().requestTimeout(7000).build()).get()) {
            // call the function without await
            // TODO use FCALL
            var before = System.currentTimeMillis();
            var promise = testClient.customCommand(new String[] {"FCALL", funcName, "0"});
            Thread.sleep(404);

            // redis kills a function with 5 sec delay
            assertEquals(OK, regularClient.functionKill().get());

            exception = assertThrows(ExecutionException.class, () -> regularClient.functionKill().get());
            assertInstanceOf(RequestException.class, exception.getCause());
            assertTrue(exception.getMessage().toLowerCase().contains("notbusy"));

            System.out.println((System.currentTimeMillis() - before) / 1000);
            exception = assertThrows(ExecutionException.class, promise::get);
            assertInstanceOf(RequestException.class, exception.getCause());
            assertTrue(exception.getMessage().contains("Script killed by user"));
        }

        // TODO replace with FUNCTION DELETE
        assertEquals(
                OK, regularClient.customCommand(new String[] {"FUNCTION", "DELETE", libName}).get());
    }

    @Test
    @SneakyThrows
    public void functionStats() {
        assumeTrue(REDIS_VERSION.isGreaterThanOrEqualTo("7.0.0"), "This feature added in redis 7");

        // TODO use FUNCTION FLUSH
        assertEquals(OK, regularClient.customCommand(new String[] {"FUNCTION", "FLUSH", "SYNC"}).get());

        String code =
                "#!lua name=stats1 \n"
                        + "redis.register_function('myfunc1', function(keys, args) return args[1] end)";
        assertEquals("stats1", regularClient.functionLoad(code).get());

        var response = regularClient.functionStats().get();
        checkFunctionStatsResponse(response, new String[0], 1, 1);

        code =
                "#!lua name=stats2 \n"
                        + "redis.register_function('myfunc2', function(keys, args) return 'OK' end)\n"
                        + "redis.register_function('myfunc3', function(keys, args) return 42 end)";
        assertEquals("stats2", regularClient.functionLoad(code).get());

        response = regularClient.functionStats().get();
        checkFunctionStatsResponse(response, new String[0], 2, 3);

        // TODO use FUNCTION FLUSH
        assertEquals(OK, regularClient.customCommand(new String[] {"FUNCTION", "FLUSH", "SYNC"}).get());

        response = regularClient.functionStats().get();
        checkFunctionStatsResponse(response, new String[0], 0, 0);
    }
}
