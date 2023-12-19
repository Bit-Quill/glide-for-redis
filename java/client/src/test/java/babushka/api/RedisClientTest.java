package babushka.api;

import static babushka.api.models.commands.SetOptions.CONDITIONAL_SET_ONLY_IF_DOES_NOT_EXIST;
import static babushka.api.models.commands.SetOptions.CONDITIONAL_SET_ONLY_IF_EXISTS;
import static babushka.api.models.commands.SetOptions.RETURN_OLD_VALUE;
import static babushka.api.models.commands.SetOptions.TIME_TO_LIVE_KEEP_EXISTING;
import static babushka.api.models.commands.SetOptions.TIME_TO_LIVE_UNIX_SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import babushka.api.commands.Command;
import babushka.api.models.commands.SetOptions;
import babushka.api.models.configuration.RedisClientConfiguration;
import babushka.managers.CommandManager;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RedisClientTest {

  RedisClient service;

  CommandManager commandManager;

  private static String HOST = "host";
  private static int PORT = 9999;

  @BeforeEach
  public void setUp() {
    RedisClientConfiguration configuration =
        RedisClientConfiguration.builder()
            .host(HOST)
            .port(PORT)
            .isTls(false)
            .clusterMode(false)
            .build();
    commandManager = mock(CommandManager.class);
    service = new RedisClient(commandManager);
  }

  @Test
  public void customCommand_success() throws ExecutionException, InterruptedException {
    // setup
    String key = "testKey";
    Object value = "testValue";
    String cmd = "GETSTRING";
    CompletableFuture<Object> testResponse = mock(CompletableFuture.class);
    when(testResponse.get()).thenReturn(value);
    when(commandManager.submitNewCommand(any(), any())).thenReturn(testResponse);

    // exercise
    CompletableFuture<Object> response = service.customCommand(cmd, new String[] {key});
    String payload = (String) response.get();

    // verify
    assertEquals(testResponse, response);
    assertEquals(value, payload);

    // teardown
  }

  @Test
  public void get_success() throws ExecutionException, InterruptedException {
    // setup
    // TODO: randomize keys
    String key = "testKey";
    String value = "testValue";
    Command cmd =
        Command.builder()
            .requestType(Command.RequestType.GETSTRING)
            .arguments(new String[] {key})
            .build();
    CompletableFuture<String> testResponse = mock(CompletableFuture.class);
    when(testResponse.get()).thenReturn(value);
    when(commandManager.<String>submitNewCommand(any(), any())).thenReturn(testResponse);

    // exercise
    CompletableFuture<String> response = service.get(key);
    String payload = response.get();

    // verify
    assertEquals(testResponse, response);
    assertEquals(value, payload);

    // teardown
  }

  // TODO: test_get_InterruptedException and ExecutionException

  @Test
  public void set_success() throws ExecutionException, InterruptedException {
    // setup
    // TODO: randomize keys
    String key = "testKey";
    String value = "testValue";
    Command cmd =
        Command.builder()
            .requestType(Command.RequestType.SETSTRING)
            .arguments(new String[] {key, value})
            .build();
    CompletableFuture<Void> testResponse = mock(CompletableFuture.class);
    when(testResponse.get()).thenReturn(null);
    when(commandManager.<Void>submitNewCommand(any(), any())).thenReturn(testResponse);

    // exercise
    CompletableFuture<Void> response = service.set(key, value);
    Object nullResponse = response.get();

    // verify
    assertEquals(testResponse, response);
    assertNull(nullResponse);

    // teardown
  }

  @Test
  public void set_withOptionsOnlyIfExists_success() throws ExecutionException, InterruptedException {
    // setup
    String key = "testKey";
    String value = "testValue";
    SetOptions setOptions = SetOptions.builder()
        .conditionalSet(SetOptions.ConditionalSet.ONLY_IF_EXISTS)
        .returnOldValue(false)
        .expiry(SetOptions.TimeToLive.builder()
            .type(SetOptions.TimeToLiveType.KEEP_EXISTING)
            .build())
        .build();
    Command cmd =
        Command.builder()
            .requestType(Command.RequestType.SETSTRING)
            .arguments(new String[] {
                key,
                value,
                CONDITIONAL_SET_ONLY_IF_EXISTS,
                TIME_TO_LIVE_KEEP_EXISTING
            })
            .build();
    CompletableFuture<String> testResponse = mock(CompletableFuture.class);
    when(testResponse.get()).thenReturn(null);
    when(commandManager.<String>submitNewCommand(eq(cmd), any())).thenReturn(testResponse);

    // exercise
    CompletableFuture<String> response = service.set(key, value, setOptions);

    // verify
    assertNotNull(response);
    assertNull(response.get());

    // teardown
  }

  @Test
  public void set_withOptionsOnlyIfDoesNotExist_success() throws ExecutionException, InterruptedException {
    // setup
    String key = "testKey";
    String value = "testValue";
    SetOptions setOptions = SetOptions.builder()
        .conditionalSet(SetOptions.ConditionalSet.ONLY_IF_DOES_NOT_EXIST)
        .returnOldValue(true)
        .expiry(SetOptions.TimeToLive.builder()
            .type(SetOptions.TimeToLiveType.UNIX_SECONDS)
            .count(60)
            .build())
        .build();
    Command cmd =
        Command.builder()
            .requestType(Command.RequestType.SETSTRING)
            .arguments(new String[] {
                key,
                value,
                CONDITIONAL_SET_ONLY_IF_DOES_NOT_EXIST,
                RETURN_OLD_VALUE,
                TIME_TO_LIVE_UNIX_SECONDS + " 60"
            })
            .build();
    CompletableFuture<String> testResponse = mock(CompletableFuture.class);
    when(testResponse.get()).thenReturn(value);
    when(commandManager.<String>submitNewCommand(eq(cmd), any())).thenReturn(testResponse);

    // exercise
    CompletableFuture<String> response = service.set(key, value, setOptions);

    // verify
    assertNotNull(response);
    assertEquals(value, response.get());

    // teardown
  }

  @Test
  public void test_ping_success() {
    // setup

    // exercise

    // verify

    // teardown
  }

  @Test
  public void test_info_success() {
    // setup

    // exercise

    // verify

    // teardown
  }

  @Test
  public void asyncConnectToRedis_success() {
    // setup
    //    boolean useSsl = false;
    //    boolean clusterMode = false;
    //    CompletableFuture<String> testResponse = mock(CompletableFuture.class);
    //    when(connectionManager.connectToRedis(anyString(), anyInt(), anyBoolean(), anyBoolean()))
    //        .thenReturn(testResponse);
    //
    //    // exercise
    //    CompletableFuture<String> connectionResponse =
    //        service.connectToRedis(HOST, PORT, useSsl, clusterMode);
    //
    //    // verify
    //    Mockito.verify(connectionManager, times(1))
    //        .connectToRedis(eq(HOST), eq(PORT), eq(useSsl), eq(clusterMode));
    //    assertEquals(testResponse, connectionResponse);

    // teardown
  }
}
