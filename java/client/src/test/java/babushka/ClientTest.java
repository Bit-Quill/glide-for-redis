package babushka;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import babushka.connection.SocketManager;
import connection_request.ConnectionRequestOuterClass.ConnectionRequest;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import response.ResponseOuterClass;

public class ClientTest {

  Client testClient;

  SocketManager socketManager;

  private static String HOST = "host";
  private static int PORT = 9999;

  @BeforeEach
  public void setUp() {
    socketManager = mock(SocketManager.class);
    testClient = new Client(socketManager);
  }

  @Test
  public void test_asyncConnectToRedis_success() {
    // setup
    boolean useSsl = false;
    boolean clusterMode = false;
    ConnectionRequest connectionRequest =
        Client.getConnectionRequest(HOST, PORT, useSsl, clusterMode);

    // exercise
    CompletableFuture<ResponseOuterClass.Response> connectionResponse =
        testClient.asyncConnectToRedis(HOST, PORT, useSsl, clusterMode);

    // verify
    //    assertTrue(connectionResponse instanceof CompletableFuture);
    verify(socketManager).connect(eq(connectionRequest));

    // teardown
  }
}
