/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.standalone;

import static glide.TransactionTestUtilities.HashCommandTransactionBuilder;
import static glide.TransactionTestUtilities.ListCommandTransactionBuilder;
import static glide.TransactionTestUtilities.ServerManagementCommandTransactionBuilder;
import static glide.TransactionTestUtilities.SetCommandTransactionBuilder;
import static glide.TransactionTestUtilities.SortedSetCommandTransactionBuilder;
import static glide.TransactionTestUtilities.StringCommandTransactionBuilder;
import static glide.api.BaseClient.OK;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import glide.TestConfiguration;
import glide.TransactionTestUtilities;
import glide.api.RedisClient;
import glide.api.models.Transaction;
import glide.api.models.commands.InfoOptions;
import glide.api.models.configuration.NodeAddress;
import glide.api.models.configuration.RedisClientConfiguration;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@Timeout(10) // seconds
public class TransactionTests {

    private static RedisClient client = null;

    @BeforeAll
    @SneakyThrows
    public static void init() {
        client =
                RedisClient.CreateClient(
                                RedisClientConfiguration.builder()
                                        .address(
                                                NodeAddress.builder().port(TestConfiguration.STANDALONE_PORTS[0]).build())
                                        .build())
                        .get();
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

    public static Stream<Arguments> getTransactionBuilders() {
        return Stream.of(
                Arguments.of("String Commands", StringCommandTransactionBuilder),
                Arguments.of("Hash Commands", HashCommandTransactionBuilder),
                Arguments.of("List Commands", ListCommandTransactionBuilder),
                Arguments.of("Set Commands", SetCommandTransactionBuilder),
                Arguments.of("Sorted Set Commands", SortedSetCommandTransactionBuilder),
                Arguments.of("Server Management Commands", ServerManagementCommandTransactionBuilder));
    }

    @SneakyThrows
    @ParameterizedTest(name = "{0}")
    @MethodSource("getTransactionBuilders")
    public void transactions_with_group_of_command(
            String testName, TransactionTestUtilities.TransactionBuilder builder) {
        Transaction transaction = new Transaction();
        Object[] expectedResult = builder.apply(transaction);

        Object[] results = client.exec(transaction).get();
        assertArrayEquals(expectedResult, results);
    }

    @SneakyThrows
    @Test
    public void test_standalone_transaction() {
        String key = UUID.randomUUID().toString();
        String value = UUID.randomUUID().toString();

        Transaction transaction =
                new Transaction().select(1).set(key, value).get(key).select(0).get(key);

        Object[] expectedResult = new Object[] {OK, OK, value, OK, null};

        Object[] result = client.exec(transaction).get();
        assertArrayEquals(expectedResult, result);
    }
}
