package glide.api.commands;

import glide.api.models.ClusterValue;
import glide.api.models.commands.InfoOptions;
import glide.api.models.configuration.RequestRoutingConfiguration.Route;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Server Management Commands interface.
 *
 * @see: <a href="https://redis.io/commands/?group=server">Server Management Commands</a>
 */
public interface ClusterServerCommands {

    /**
     * Get information and statistics about the Redis server. DEFAULT option is assumed
     *
     * @see <a href="https://redis.io/commands/info/">redis.io</a> for details.
     * @return CompletableFuture with the response
     */
    CompletableFuture<ClusterValue<Map>> info();

    /**
     * Get information and statistics about the Redis server. DEFAULT option is assumed
     *
     * @see <a href="https://redis.io/commands/info/">redis.io</a> for details.
     * @param route Routing configuration for the command
     * @return CompletableFuture with the response
     */
    CompletableFuture<ClusterValue<Map>> info(Route route);

    /**
     * Get information and statistics about the Redis server.
     *
     * @see <a href="https://redis.io/commands/info/">redis.io</a> for details.
     * @param options - A list of InfoSection values specifying which sections of information to
     *     retrieve. When no parameter is provided, the default option is assumed.
     * @return CompletableFuture with the response
     */
    CompletableFuture<ClusterValue<Map>> info(InfoOptions options);

    /**
     * Get information and statistics about the Redis server.
     *
     * @see <a href="https://redis.io/commands/info/">redis.io</a> for details.
     * @param options - A list of InfoSection values specifying which sections of information to
     *     retrieve. When no parameter is provided, the default option is assumed.
     * @param route Routing configuration for the command
     * @return CompletableFuture with the response
     */
    CompletableFuture<ClusterValue<Map>> info(InfoOptions options, Route route);
}
