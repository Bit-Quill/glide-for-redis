package glide.api;

import static glide.api.models.configuration.RequestRoutingConfiguration.SimpleRoute.ALL_NODES;
import static glide.api.models.configuration.RequestRoutingConfiguration.SimpleRoute.RANDOM;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import glide.api.models.ClusterValue;
import glide.api.models.commands.InfoOptions;
import glide.api.models.configuration.RequestRoutingConfiguration;
import glide.managers.CommandManager;
import glide.managers.ConnectionManager;
import glide.managers.RedisExceptionCheckedFunction;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import redis_request.RedisRequestOuterClass;
import response.ResponseOuterClass.Response;

public class RedisClusterClientTest {

    RedisClusterClient service;

    ConnectionManager connectionManager;

    CommandManager commandManager;

    @BeforeEach
    public void setUp() {
        connectionManager = mock(ConnectionManager.class);
        commandManager = mock(CommandManager.class);
        service = new RedisClusterClient(connectionManager, commandManager);
    }

    @Test
    @SneakyThrows
    public void customCommand_returns_single_value() {
        var commandManager = new TestCommandManager(null);

        var client = new TestClient(commandManager, "TEST");

        var value = client.customCommand(new String[0]).get();
        assertAll(
                () -> assertTrue(value.hasSingleData()),
                () -> assertEquals("TEST", value.getSingleValue()));
    }

    @Test
    @SneakyThrows
    public void customCommand_returns_multi_value() {
        var commandManager = new TestCommandManager(null);

        var data = Map.of("key1", "value1", "key2", "value2");
        var client = new TestClient(commandManager, data);

        var value = client.customCommand(new String[0]).get();
        assertAll(
                () -> assertTrue(value.hasMultiData()), () -> assertEquals(data, value.getMultiValue()));
    }

    @Test
    @SneakyThrows
    // test checks that even a map returned as a single value when single node route is used
    public void customCommand_with_single_node_route_returns_single_value() {
        var commandManager = new TestCommandManager(null);

        var data = Map.of("key1", "value1", "key2", "value2");
        var client = new TestClient(commandManager, data);

        var value = client.customCommand(new String[0], RANDOM).get();
        assertAll(
                () -> assertTrue(value.hasSingleData()), () -> assertEquals(data, value.getSingleValue()));
    }

    @Test
    @SneakyThrows
    public void customCommand_with_multi_node_route_returns_multi_value() {
        var commandManager = new TestCommandManager(null);

        var data = Map.of("key1", "value1", "key2", "value2");
        var client = new TestClient(commandManager, data);

        var value = client.customCommand(new String[0], ALL_NODES).get();
        assertAll(
                () -> assertTrue(value.hasMultiData()), () -> assertEquals(data, value.getMultiValue()));
    }

    private static class TestClient extends RedisClusterClient {

        private final Object object;

        public TestClient(CommandManager commandManager, Object objectToReturn) {
            super(null, commandManager);
            object = objectToReturn;
        }

        @Override
        protected Object handleObjectResponse(Response response) {
            return object;
        }
    }

    private static class TestCommandManager extends CommandManager {

        private final Response response;

        public TestCommandManager(Response responseToReturn) {
            super(null);
            response = responseToReturn;
        }

        @Override
        public <T> CompletableFuture<T> submitNewCommand(
                RedisRequestOuterClass.RedisRequest.Builder command,
                RedisExceptionCheckedFunction<Response, T> responseHandler) {
            return CompletableFuture.supplyAsync(() -> responseHandler.apply(response));
        }
    }

    @SneakyThrows
    @Test
    public void customCommand_success() {
        // setup
        String key = "testKey";
        String value = "testValue";
        String cmd = "GETSTRING";
        String[] arguments = new String[] {cmd, key};
        CompletableFuture<ClusterValue<Object>> testResponse = mock(CompletableFuture.class);
        when(testResponse.get()).thenReturn(ClusterValue.of(value));
        when(commandManager.<ClusterValue<Object>>submitNewCommand(any(), any(), any(), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<ClusterValue<Object>> response = service.customCommand(arguments);

        // verify
        assertEquals(testResponse, response);
        ClusterValue clusterValue = response.get();
        assertTrue(clusterValue.hasSingleData());
        String payload = (String) clusterValue.getSingleValue();
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void customCommand_interruptedException() {
        // setup
        String key = "testKey";
        String cmd = "GETSTRING";
        String[] arguments = new String[] {cmd, key};
        CompletableFuture<ClusterValue<Object>> testResponse = mock(CompletableFuture.class);
        InterruptedException interruptedException = new InterruptedException();
        when(testResponse.get()).thenThrow(interruptedException);
        when(commandManager.<ClusterValue<Object>>submitNewCommand(any(), any(), any(), any()))
                .thenReturn(testResponse);

        // exercise
        InterruptedException exception =
                assertThrows(
                        InterruptedException.class,
                        () -> {
                            CompletableFuture<ClusterValue<Object>> response = service.customCommand(arguments);
                            response.get();
                        });

        // verify
        assertEquals(interruptedException, exception);
    }

    @SneakyThrows
    @Test
    public void info_returns_string() {
        // setup
        CompletableFuture<ClusterValue<Map>> testResponse = mock(CompletableFuture.class);
        Map testPayload = new HashMap<String, String>();
        testPayload.put("key1", "value1");
        testPayload.put("key2", "value2");
        testPayload.put("key3", "value3");
        when(testResponse.get()).thenReturn(ClusterValue.of(testPayload));
        when(commandManager.<ClusterValue<Map>>submitNewCommand(any(), any(), any(), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<ClusterValue<Map>> response = service.info();

        // verify
        assertEquals(testResponse, response);
        ClusterValue<Map> clusterValue = response.get();
        assertTrue(clusterValue.hasMultiData());
        Map payload = clusterValue.getMultiValue();
        assertEquals(testPayload, payload);
    }

    @SneakyThrows
    @Test
    public void info_with_route_returns_string() {
        // setup
        CompletableFuture<ClusterValue<Map>> testResponse = mock(CompletableFuture.class);
        Map testPayloadAddr1 = Map.of("key1", "value1", "key2", "value2");
        Map testPayloadAddr2 = Map.of("key3", "value3", "key4", "value4");
        Map<String, Map> testClusterValue =
                Map.of("addr1", testPayloadAddr1, "addr2", testPayloadAddr2);
        RequestRoutingConfiguration.Route route = RequestRoutingConfiguration.SimpleRoute.ALL_NODES;
        when(testResponse.get()).thenReturn(ClusterValue.of(testClusterValue));
        when(commandManager.<ClusterValue<Map>>submitNewCommand(any(), any(), any(), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<ClusterValue<Map>> response = service.info(route);

        // verify
        assertEquals(testResponse, response);
        ClusterValue<Map> clusterValue = response.get();
        assertTrue(clusterValue.hasMultiData());
        Map<String, Map> clusterMap = clusterValue.getMultiValue();
        assertEquals(testPayloadAddr1, clusterMap.get("addr1"));
        assertEquals(testPayloadAddr2, clusterMap.get("addr2"));
    }

    @SneakyThrows
    @Test
    public void info_with_route_with_infoOptions_returns_string() {
        // setup
        String[] infoArguments = new String[] {"ALL", "DEFAULT"};
        CompletableFuture<ClusterValue<Map>> testResponse = mock(CompletableFuture.class);
        Map testPayloadAddr1 = Map.of("key1", "value1", "key2", "value2");
        Map testPayloadAddr2 = Map.of("key3", "value3", "key4", "value4");
        Map<String, Map> testClusterValue =
                Map.of("addr1", testPayloadAddr1, "addr2", testPayloadAddr2);
        when(testResponse.get()).thenReturn(ClusterValue.of(testClusterValue));
        RequestRoutingConfiguration.Route route = RequestRoutingConfiguration.SimpleRoute.ALL_PRIMARIES;
        when(commandManager.<ClusterValue<Map>>submitNewCommand(any(), any(), any(), any()))
                .thenReturn(testResponse);

        // exercise
        InfoOptions options =
                InfoOptions.builder()
                        .section(InfoOptions.Section.ALL)
                        .section(InfoOptions.Section.DEFAULT)
                        .build();
        CompletableFuture<ClusterValue<Map>> response = service.info(options, route);

        // verify
        assertEquals(testResponse, response);
        ClusterValue<Map> clusterValue = response.get();
        assertTrue(clusterValue.hasMultiData());
        Map<String, Map> clusterMap = clusterValue.getMultiValue();
        assertEquals(testPayloadAddr1, clusterMap.get("addr1"));
        assertEquals(testPayloadAddr2, clusterMap.get("addr2"));
    }
}
