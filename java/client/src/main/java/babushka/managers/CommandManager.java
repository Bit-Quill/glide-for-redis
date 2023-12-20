package babushka.managers;

import babushka.connectors.ClientState;
import babushka.connectors.handlers.ChannelHandler;
import babushka.ffi.resolvers.RedisValueResolver;
import babushka.models.RequestBuilder;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import redis_request.RedisRequestOuterClass.RequestType;
import response.ResponseOuterClass.Response;

@RequiredArgsConstructor
public class CommandManager {

  /** UDS connection representation. */
  private final ChannelHandler channel;

  /**
   * Client state, which {@link CommandManager} can flick to closed if corresponding error received.
   */
  private final ClientState.ClosableClientState clientState;

  /**
   * Async (non-blocking) get.<br>
   * See <a href="https://redis.io/commands/get/">REDIS docs for GET</a>.
   *
   * @param key The key name
   */
  public CompletableFuture<String> get(String key) {
    return submitNewRequest(RequestType.GetString, List.of(key));
  }

  /**
   * Async (non-blocking) set.<br>
   * See <a href="https://redis.io/commands/set/">REDIS docs for SET</a>.
   *
   * @param key The key name
   * @param value The value to set
   */
  public CompletableFuture<String> set(String key, String value) {
    return submitNewRequest(RequestType.SetString, List.of(key, value));
  }

  /**
   * Build a command and submit it Netty to send.
   *
   * @param command Command type
   * @param args Command arguments
   * @return A result promise
   */
  private CompletableFuture<String> submitNewRequest(RequestType command, List<String> args) {
    if (!clientState.isConnected()) {
      throw new IllegalStateException("Connection is not open");
    }

    // TODO this explicitly uses ForkJoin thread pool. May be we should use another one.
    return CompletableFuture.supplyAsync(
            () -> channel.write(RequestBuilder.prepareRedisRequest(command, args), true))
        // TODO: is there a better way to execute this?
        .thenComposeAsync(f -> f)
        .thenApplyAsync(this::extractValueFromResponse);
  }

  /**
   * Check response and extract data from it.
   *
   * @param response A response received from Babushka
   * @return A String from the Redis RESP2 response, or Ok. Otherwise, returns null
   */
  private String extractValueFromResponse(Response response) {
    if (response.hasRequestError()) {
      // TODO do we need to support different types of exceptions and distinguish them by type?
      throw new RuntimeException(
          String.format(
              "%s: %s",
              response.getRequestError().getType(), response.getRequestError().getMessage()));
    } else if (response.hasClosingError()) {
      CompletableFuture.runAsync(channel::close);
      clientState.disconnect();
      throw new RuntimeException("Connection closed: " + response.getClosingError());
    } else if (response.hasConstantResponse()) {
      return response.getConstantResponse().toString();
    } else if (response.hasRespPointer()) {
      return RedisValueResolver.valueFromPointer(response.getRespPointer()).toString();
    }
    return null;
  }
}
