package glide.api.commands;

import glide.api.models.ClusterTransaction;
import glide.api.models.ClusterValue;
import glide.api.models.Transaction;
import glide.api.models.configuration.RequestRoutingConfiguration.Route;
import java.util.concurrent.CompletableFuture;

/**
 * Base Commands interface to handle generic command and transaction requests with routing options.
 */
public interface ClusterBaseCommands {

    /**
     * Executes a single command, without checking inputs. Every part of the command, including
     * subcommands, should be added as a separate value in {@code args}.
     *
     * @remarks This function should only be used for single-response commands. Commands that don't
     *     return response (such as <em>SUBSCRIBE</em>), or that return potentially more than a single
     *     response (such as <em>XREAD</em>), or that change the client's behavior (such as entering
     *     <em>pub</em>/<em>sub</em> mode on <em>RESP2</em> connections) shouldn't be called using
     *     this function.
     * @example Returns a list of all <em>pub</em>/<em>sub</em> clients:
     *     <p><code>
     * Object result = client.customCommand(new String[]{ "CLIENT", "LIST", "TYPE", "PUBSUB" }).get();
     * </code>
     * @param args Arguments for the custom command including the command name
     * @return A <em>CompletableFuture</em> with response result from Redis
     */
    CompletableFuture<ClusterValue<Object>> customCommand(String[] args);

    /**
     * Executes a single command, without checking inputs. Every part of the command, including
     * subcommands, should be added as a separate value in {@code args}.
     *
     * @remarks This function should only be used for single-response commands. Commands that don't
     *     return response (such as <em>SUBSCRIBE</em>), or that return potentially more than a single
     *     response (such as <em>XREAD</em>), or that change the client's behavior (such as entering
     *     <em>pub</em>/<em>sub</em> mode on <em>RESP2</em> connections) shouldn't be called using
     *     this function.
     * @example Returns a list of all <em>pub</em>/<em>sub</em> clients:
     *     <p><code>
     * Object result = client.customCommand(new String[]{ "CLIENT", "LIST", "TYPE", "PUBSUB" }).get();
     * </code>
     * @param args Arguments for the custom command including the command name
     * @param route Routing configuration for the command
     * @return A <em>CompletableFuture</em> with response result from Redis
     */
    CompletableFuture<ClusterValue<Object>> customCommand(String[] args, Route route);

    /**
     * Execute a transaction by processing the queued commands.
     *
     * @see <a href="https://redis.io/topics/Transactions/">redis.io</a> for details on Redis
     *     Transactions.
     * @param transaction - A {@link Transaction} object containing a list of commands to be executed.
     * @return A list of results corresponding to the execution of each command in the transaction.
     *     <ul>
     *       <li>If a command returns a value, it will be included in the list. If a command doesn't
     *           return a value, the list entry will be null.
     *       <li>If the transaction failed due to a WATCH command, `exec` will return `null`.
     *     </ul>
     */
    CompletableFuture<ClusterValue<Object[]>> exec(ClusterTransaction transaction);

    /**
     * Execute a transaction by processing the queued commands.
     *
     * @see <a href="https://redis.io/topics/Transactions/">redis.io</a> for details on Redis
     *     Transactions.
     * @param transaction - A {@link Transaction} object containing a list of commands to be executed.
     * @param route Routing configuration for the command
     * @return A list of results corresponding to the execution of each command in the transaction.
     *     <ul>
     *       <li>If a command returns a value, it will be included in the list. If a command doesn't
     *           return a value, the list entry will be null.
     *       <li>If the transaction failed due to a WATCH command, `exec` will return `null`.
     *     </ul>
     */
    CompletableFuture<ClusterValue<Object[]>> exec(ClusterTransaction transaction, Route route);
}
