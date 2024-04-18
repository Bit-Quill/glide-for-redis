/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.standalone;

import static glide.TestConfiguration.REDIS_VERSION;
import static glide.TestUtilities.createDefaultStandaloneClient;
import static glide.TestUtilities.tryCommandWithExpectedError;
import static glide.TransactionTestUtilities.transactionTest;
import static glide.TransactionTestUtilities.transactionTestResult;
import static glide.api.BaseClient.OK;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import glide.api.RedisClient;
import glide.api.models.Transaction;
import glide.api.models.commands.InfoOptions;
import glide.api.models.exceptions.RequestException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import lombok.SneakyThrows;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(10) // seconds
public class TransactionTests {

    private static RedisClient client = null;

    @BeforeAll
    public static void init() {
        client = createDefaultStandaloneClient();
    }

    @AfterAll
    @SneakyThrows
    public static void teardown() {
        client.close();
    }

    @Test
    @SneakyThrows
    public void custom_command_info() {
        Transaction transaction = new Transaction().customCommand(new String[] {"info"});
        Object[] result = client.exec(transaction).get();
        assertTrue(((String) result[0]).contains("# Stats"));
    }

    @Test
    @SneakyThrows
    public void info_test() {
        Transaction transaction =
                new Transaction()
                        .info()
                        .info(InfoOptions.builder().section(InfoOptions.Section.CLUSTER).build());
        Object[] result = client.exec(transaction).get();

        // sanity check
        assertTrue(((String) result[0]).contains("# Stats"));
        assertFalse(((String) result[1]).contains("# Stats"));
    }

    @Test
    @SneakyThrows
    public void ping_tests() {
        Transaction transaction = new Transaction();
        int numberOfPings = 100;
        for (int idx = 0; idx < numberOfPings; idx++) {
            if ((idx % 2) == 0) {
                transaction.ping();
            } else {
                transaction.ping(Integer.toString(idx));
            }
        }
        Object[] result = client.exec(transaction).get();
        for (int idx = 0; idx < numberOfPings; idx++) {
            if ((idx % 2) == 0) {
                assertEquals("PONG", result[idx]);
            } else {
                assertEquals(Integer.toString(idx), result[idx]);
            }
        }
    }

    @SneakyThrows
    @Test
    public void test_standalone_transactions() {
        Transaction transaction = (Transaction) transactionTest(new Transaction());
        Object[] expectedResult = transactionTestResult();

        String key = UUID.randomUUID().toString();
        String value = UUID.randomUUID().toString();

        transaction.select(1);
        transaction.set(key, value);
        transaction.get(key);
        transaction.select(0);
        transaction.get(key);

        expectedResult = ArrayUtils.addAll(expectedResult, OK, OK, value, OK, null);

        Object[] result = client.exec(transaction).get();
        assertArrayEquals(expectedResult, result);
    }

    @Test
    @SneakyThrows
    public void save() {
        String error = "Background save already in progress";
        // use another client, because it could be blocked
        try (var testClient = createDefaultStandaloneClient()) {

            if (REDIS_VERSION.isGreaterThanOrEqualTo("7.0.0")) {
                Exception ex =
                        assertThrows(
                                ExecutionException.class, () -> testClient.exec(new Transaction().save()).get());
                assertInstanceOf(RequestException.class, ex.getCause());
                assertTrue(ex.getCause().getMessage().contains("Command not allowed inside a transaction"));
            } else {
                var transactionResponse =
                        tryCommandWithExpectedError(() -> testClient.exec(new Transaction().save()), error);
                assertTrue(
                        transactionResponse.getValue() != null || transactionResponse.getKey()[0].equals(OK));
            }
        }
    }

    @Test
    @SneakyThrows
    public void lastsave() {
        var yesterday = Instant.now().minus(1, ChronoUnit.DAYS);

        var response = client.exec(new Transaction().lastsave()).get();
        assertTrue(Instant.ofEpochSecond((long) response[0]).isAfter(yesterday));
    }
}
