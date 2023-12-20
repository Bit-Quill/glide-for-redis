package babushka.api.config;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

/** Represents the address and port of a node in the cluster. */
@Getter
@Builder
public class NodeAddress {
  public static String DEFAULT_HOST = "localhost";
  public static Integer PORT = 6379;

  @NonNull @Builder.Default private final String host = DEFAULT_HOST;
  @NonNull @Builder.Default private final Integer port = 6379;
}
