/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.commands;

import glide.api.models.commands.FlushMode;
import glide.api.models.configuration.RequestRoutingConfiguration.Route;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Supports commands and transactions for the "Scripting and Function" group for a cluster client.
 *
 * @see <a href="https://redis.io/docs/latest/commands/?group=scripting">Scripting and Function
 *     Commands</a>
 */
public interface ScriptingAndFunctionsClusterCommands {

    /**
     * Loads a library to Redis.<br>
     * The command will be routed to all primary nodes.
     *
     * @see <a href="https://redis.io/docs/latest/commands/function-load/">redis.io</a> for details.
     * @param libraryCode The source code that implements the library.
     * @return The library name that was loaded.
     * @example
     *     <pre>{@code
     * String code = "#!lua name=mylib \n redis.register_function('myfunc', function(keys, args) return args[1] end)";
     * String response = client.functionLoad(code).get();
     * assert response.equals("mylib");
     * }</pre>
     */
    CompletableFuture<String> functionLoad(String libraryCode);

    /**
     * Loads a library to Redis and overwrites the existing library with the new contents.<br>
     * The command will be routed to all primary nodes.
     *
     * @see <a href="https://redis.io/docs/latest/commands/function-load/">redis.io</a> for details.
     * @param libraryCode The source code that implements the library.
     * @return The library name that was loaded.
     * @example
     *     <pre>{@code
     * String code = "#!lua name=mylib \n redis.register_function('myfunc', function(keys, args) return args[1] end)";
     * String response = client.functionLoadWithReplace(code).get();
     * assert response.equals("mylib");
     * }</pre>
     */
    CompletableFuture<String> functionLoadWithReplace(String libraryCode);

    /**
     * Loads a library to Redis.
     *
     * @see <a href="https://redis.io/docs/latest/commands/function-load/">redis.io</a> for details.
     * @param libraryCode The source code that implements the library.
     * @param route Specifies the routing configuration for the command. The client will route the
     *     command to the nodes defined by <code>route</code>.
     * @return The library name that was loaded.
     * @example
     *     <pre>{@code
     * String code = "#!lua name=mylib \n redis.register_function('myfunc', function(keys, args) return args[1] end)";
     * Route route = new SlotKeyRoute("key", PRIMARY);
     * String response = client.functionLoad(code, route).get();
     * assert response.equals("mylib");
     * }</pre>
     */
    CompletableFuture<String> functionLoad(String libraryCode, Route route);

    /**
     * Loads a library to Redis and overwrites the existing library with the new contents.
     *
     * @see <a href="https://redis.io/docs/latest/commands/function-load/">redis.io</a> for details.
     * @param libraryCode The source code that implements the library.
     * @param route Specifies the routing configuration for the command. The client will route the
     *     command to the nodes defined by <code>route</code>.
     * @return The library name that was loaded.
     * @example
     *     <pre>{@code
     * String code = "#!lua name=mylib \n redis.register_function('myfunc', function(keys, args) return args[1] end)";
     * String response = client.functionLoadWithReplace(code, ALL_NODES).get();
     * assert response.equals("mylib");
     * }</pre>
     */
    CompletableFuture<String> functionLoadWithReplace(String libraryCode, Route route);

    /**
     * Returns information about the functions and libraries.<br>
     * The command will be routed to all primary nodes.
     *
     * @since Redis 7.0 and above
     * @see <a href="https://redis.io/docs/latest/commands/function-list/">redis.io</a> for details.
     * @return Info about all libraries and their functions.
     * @example
     *     <pre>{@code
     * Map<String, Object>[] response = client.functionList().get();
     * for (Map<String, Object> libraryInfo : response) {
     *   System.out.printf("Server has library '%s' which runs on %s engine%n",
     *       libraryInfo.get("library_name"), libraryInfo.get("engine"));
     *   Object[] functions = (Object[]) libraryInfo.get("functions");
     *   for (int i = 0; i < functions.length; i++) {
     *     Map<String, Object> function = (Map<String, Object>) functions[i];
     *     Set<String> flags = (Set<String>) function.get("flags");
     *     System.out.printf("Library has function '%s' with flags '%s' described as %s%n",
     *         function.get("name"), String.join(", ", flags), function.get("description"));
     *   }
     * }
     * }</pre>
     */
    CompletableFuture<Map<String, Object>[]> functionList();

    /**
     * Returns information about the functions and libraries.<br>
     * The command will be routed to all primary nodes.
     *
     * @since Redis 7.0 and above
     * @see <a href="https://redis.io/docs/latest/commands/function-list/">redis.io</a> for details.
     * @return Info about all libraries, their functions, and their code.
     * @example
     *     <pre>{@code
     * Map<String, Object>[] response = client.functionList().get();
     * for (Map<String, Object> libraryInfo : response) {
     *   System.out.printf("Server has library '%s' which runs on %s engine%n",
     *       libraryInfo.get("library_name"), libraryInfo.get("engine"));
     *   Object[] functions = (Object[]) libraryInfo.get("functions");
     *   for (int i = 0; i < functions.length; i++) {
     *     Map<String, Object> function = (Map<String, Object>) functions[i];
     *     Set<String> flags = (Set<String>) function.get("flags");
     *     System.out.printf("Library has function '%s' with flags '%s' described as %s%n",
     *         function.get("name"), String.join(", ", flags), function.get("description"));
     *     System.out.printf("Library code:%n%s%n", function.get("library_code"));
     *   }
     * }
     * }</pre>
     */
    CompletableFuture<Map<String, Object>[]> functionListWithCode();

    /**
     * Returns information about the functions and libraries.<br>
     * The command will be routed to all primary nodes.
     *
     * @since Redis 7.0 and above
     * @see <a href="https://redis.io/docs/latest/commands/function-list/">redis.io</a> for details.
     * @param libNamePattern A wildcard pattern for matching library names.
     * @return Info about queried libraries and their functions.
     * @example
     *     <pre>{@code
     * Map<String, Object>[] response = client.functionList("myLib?_backup").get();
     * for (Map<String, Object> libraryInfo : response) {
     *   System.out.printf("Server has library '%s' which runs on %s engine%n",
     *       libraryInfo.get("library_name"), libraryInfo.get("engine"));
     *   Object[] functions = (Object[]) libraryInfo.get("functions");
     *   for (int i = 0; i < functions.length; i++) {
     *     Map<String, Object> function = (Map<String, Object>) functions[i];
     *     Set<String> flags = (Set<String>) function.get("flags");
     *     System.out.printf("Library has function '%s' with flags '%s' described as %s%n",
     *         function.get("name"), String.join(", ", flags), function.get("description"));
     *   }
     * }
     * }</pre>
     */
    CompletableFuture<Map<String, Object>[]> functionList(String libNamePattern);

    /**
     * Returns information about the functions and libraries.<br>
     * The command will be routed to all primary nodes.
     *
     * @since Redis 7.0 and above
     * @see <a href="https://redis.io/docs/latest/commands/function-list/">redis.io</a> for details.
     * @param libNamePattern A wildcard pattern for matching library names.
     * @return Info about queried libraries, their functions, and their code.
     * @example
     *     <pre>{@code
     * Map<String, Object>[] response = client.functionList("GLIDE*").get();
     * for (Map<String, Object> libraryInfo : response) {
     *   System.out.printf("Server has library '%s' which runs on %s engine%n",
     *       libraryInfo.get("library_name"), libraryInfo.get("engine"));
     *   Object[] functions = (Object[]) libraryInfo.get("functions");
     *   for (int i = 0; i < functions.length; i++) {
     *     Map<String, Object> function = (Map<String, Object>) functions[i];
     *     Set<String> flags = (Set<String>) function.get("flags");
     *     System.out.printf("Library has function '%s' with flags '%s' described as %s%n",
     *         function.get("name"), String.join(", ", flags), function.get("description"));
     *     System.out.printf("Library code:%n%s%n", function.get("library_code"));
     *   }
     * }
     * }</pre>
     */
    CompletableFuture<Map<String, Object>[]> functionListWithCode(String libNamePattern);

    /**
     * Returns information about the functions and libraries.
     *
     * @since Redis 7.0 and above
     * @see <a href="https://redis.io/docs/latest/commands/function-list/">redis.io</a> for details.
     * @param route Specifies the routing configuration for the command. The client will route the
     *     command to the nodes defined by <code>route</code>.
     * @return Info about all libraries and their functions.
     * @example
     *     <pre>{@code
     * Map<String, Object>[] response = client.functionList(RANDOM).get();
     * for (Map<String, Object> libraryInfo : response) {
     *   System.out.printf("Server has library '%s' which runs on %s engine%n",
     *       libraryInfo.get("library_name"), libraryInfo.get("engine"));
     *   Object[] functions = (Object[]) libraryInfo.get("functions");
     *   for (int i = 0; i < functions.length; i++) {
     *     Map<String, Object> function = (Map<String, Object>) functions[i];
     *     Set<String> flags = (Set<String>) function.get("flags");
     *     System.out.printf("Library has function '%s' with flags '%s' described as %s%n",
     *         function.get("name"), String.join(", ", flags), function.get("description"));
     *   }
     * }
     * }</pre>
     */
    CompletableFuture<Map<String, Object>[]> functionList(Route route);

    /**
     * Returns information about the functions and libraries.
     *
     * @since Redis 7.0 and above
     * @see <a href="https://redis.io/docs/latest/commands/function-list/">redis.io</a> for details.
     * @param route Specifies the routing configuration for the command. The client will route the
     *     command to the nodes defined by <code>route</code>.
     * @return Info about all libraries, their functions, and their code.
     * @example
     *     <pre>{@code
     * Map<String, Object>[] response = client.functionList(RANDOM).get();
     * for (Map<String, Object> libraryInfo : response) {
     *   System.out.printf("Server has library '%s' which runs on %s engine%n",
     *       libraryInfo.get("library_name"), libraryInfo.get("engine"));
     *   Object[] functions = (Object[]) libraryInfo.get("functions");
     *   for (int i = 0; i < functions.length; i++) {
     *     Map<String, Object> function = (Map<String, Object>) functions[i];
     *     Set<String> flags = (Set<String>) function.get("flags");
     *     System.out.printf("Library has function '%s' with flags '%s' described as %s%n",
     *         function.get("name"), String.join(", ", flags), function.get("description"));
     *     System.out.printf("Library code:%n%s%n", function.get("library_code"));
     *   }
     * }
     * }</pre>
     */
    CompletableFuture<Map<String, Object>[]> functionListWithCode(Route route);

    /**
     * Returns information about the functions and libraries.
     *
     * @since Redis 7.0 and above
     * @see <a href="https://redis.io/docs/latest/commands/function-list/">redis.io</a> for details.
     * @param route Specifies the routing configuration for the command. The client will route the
     *     command to the nodes defined by <code>route</code>.
     * @param libNamePattern A wildcard pattern for matching library names.
     * @return Info about queried libraries and their functions.
     * @example
     *     <pre>{@code
     * Map<String, Object>[] response = client.functionList("myLib?_backup", RANDOM).get();
     * for (Map<String, Object> libraryInfo : response) {
     *   System.out.printf("Server has library '%s' which runs on %s engine%n",
     *       libraryInfo.get("library_name"), libraryInfo.get("engine"));
     *   Object[] functions = (Object[]) libraryInfo.get("functions");
     *   for (int i = 0; i < functions.length; i++) {
     *     Map<String, Object> function = (Map<String, Object>) functions[i];
     *     Set<String> flags = (Set<String>) function.get("flags");
     *     System.out.printf("Library has function '%s' with flags '%s' described as %s%n",
     *         function.get("name"), String.join(", ", flags), function.get("description"));
     *   }
     * }
     * }</pre>
     */
    CompletableFuture<Map<String, Object>[]> functionList(String libNamePattern, Route route);

    /**
     * Returns information about the functions and libraries.
     *
     * @since Redis 7.0 and above
     * @see <a href="https://redis.io/docs/latest/commands/function-list/">redis.io</a> for details.
     * @param route Specifies the routing configuration for the command. The client will route the
     *     command to the nodes defined by <code>route</code>.
     * @param libNamePattern A wildcard pattern for matching library names.
     * @return Info about queried libraries, their functions, and their code.
     * @example
     *     <pre>{@code
     * Map<String, Object>[] response = client.functionList("GLIDE*", RANDOM).get();
     * for (Map<String, Object> libraryInfo : response) {
     *   System.out.printf("Server has library '%s' which runs on %s engine%n",
     *       libraryInfo.get("library_name"), libraryInfo.get("engine"));
     *   Object[] functions = (Object[]) libraryInfo.get("functions");
     *   for (int i = 0; i < functions.length; i++) {
     *     Map<String, Object> function = (Map<String, Object>) functions[i];
     *     Set<String> flags = (Set<String>) function.get("flags");
     *     System.out.printf("Library has function '%s' with flags '%s' described as %s%n",
     *         function.get("name"), String.join(", ", flags), function.get("description"));
     *     System.out.printf("Library code:%n%s%n", function.get("library_code"));
     *   }
     * }
     * }</pre>
     */
    CompletableFuture<Map<String, Object>[]> functionListWithCode(String libNamePattern, Route route);

    /**
     * Deletes all the libraries.<br>
     * The command will be routed to all primary nodes.
     *
     * @since Redis 7.0 and above
     * @see <a href="https://redis.io/docs/latest/commands/function-flush/">redis.io</a> for details.
     * @return <code>OK</code>.
     * @example
     *     <pre>{@code
     * String response = client.functionFlush().get();
     * assert response.equals("OK");
     * }</pre>
     */
    CompletableFuture<String> functionFlush();

    /**
     * Deletes all the libraries.<br>
     * The command will be routed to all primary nodes.
     *
     * @since Redis 7.0 and above
     * @see <a href="https://redis.io/docs/latest/commands/function-flush/">redis.io</a> for details.
     * @param mode The flushing mode, could be either {@link FlushMode#SYNC} or {@link
     *     FlushMode#ASYNC}.
     * @return <code>OK</code>.
     * @example
     *     <pre>{@code
     * String response = client.functionFlush(SYNC).get();
     * assert response.equals("OK");
     * }</pre>
     */
    CompletableFuture<String> functionFlush(FlushMode mode);

    /**
     * Deletes all the libraries.
     *
     * @since Redis 7.0 and above
     * @see <a href="https://redis.io/docs/latest/commands/function-flush/">redis.io</a> for details.
     * @param route Specifies the routing configuration for the command. The client will route the
     *     command to the nodes defined by <code>route</code>.
     * @return <code>OK</code>.
     * @example
     *     <pre>{@code
     * String response = client.functionFlush().get();
     * assert response.equals("OK");
     * }</pre>
     */
    CompletableFuture<String> functionFlush(Route route);

    /**
     * Deletes all the libraries.
     *
     * @since Redis 7.0 and above
     * @see <a href="https://redis.io/docs/latest/commands/function-flush/">redis.io</a> for details.
     * @param route Specifies the routing configuration for the command. The client will route the
     *     command to the nodes defined by <code>route</code>.
     * @param mode The flushing mode, could be either {@link FlushMode#SYNC} or {@link
     *     FlushMode#ASYNC}.
     * @return <code>OK</code>.
     * @example
     *     <pre>{@code
     * String response = client.functionFlush(SYNC).get();
     * assert response.equals("OK");
     * }</pre>
     */
    CompletableFuture<String> functionFlush(FlushMode mode, Route route);
}
