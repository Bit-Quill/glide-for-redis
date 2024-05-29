/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.commands;

import glide.api.models.commands.stream.StreamAddOptions;
import glide.api.models.commands.stream.StreamAddOptions.StreamAddOptionsBuilder;
import glide.api.models.commands.stream.StreamReadOptions;
import glide.api.models.commands.stream.StreamTrimOptions;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Supports commands and transactions for the "Stream Commands" group for standalone and cluster
 * clients.
 *
 * @see <a href="https://redis.io/commands/?group=stream">Stream Commands</a>
 */
public interface StreamBaseCommands {

    /**
     * Adds an entry to the specified stream stored at <code>key</code>.<br>
     * If the <code>key</code> doesn't exist, the stream is created.
     *
     * @see <a href="https://redis.io/commands/xadd/">redis.io</a> for details.
     * @param key The key of the stream.
     * @param values Field-value pairs to be added to the entry.
     * @return The id of the added entry.
     * @example
     *     <pre>{@code
     * String streamId = client.xadd("key", Map.of("name", "Sara", "surname", "OConnor").get();
     * System.out.println("Stream: " + streamId);
     * }</pre>
     */
    CompletableFuture<String> xadd(String key, Map<String, String> values);

    /**
     * Adds an entry to the specified stream stored at <code>key</code>.<br>
     * If the <code>key</code> doesn't exist, the stream is created.
     *
     * @see <a href="https://redis.io/commands/xadd/">redis.io</a> for details.
     * @param key The key of the stream.
     * @param values Field-value pairs to be added to the entry.
     * @param options Stream add options {@link StreamAddOptions}.
     * @return The id of the added entry, or <code>null</code> if {@link
     *     StreamAddOptionsBuilder#makeStream(Boolean)} is set to <code>false</code> and no stream
     *     with the matching <code>key</code> exists.
     * @example
     *     <pre>{@code
     * // Option to use the existing stream, or return null if the stream doesn't already exist at "key"
     * StreamAddOptions options = StreamAddOptions.builder().id("sid").makeStream(Boolean.FALSE).build();
     * String streamId = client.xadd("key", Map.of("name", "Sara", "surname", "OConnor"), options).get();
     * if (streamId != null) {
     *     assert streamId.equals("sid");
     * }
     * }</pre>
     */
    CompletableFuture<String> xadd(String key, Map<String, String> values, StreamAddOptions options);

    /**
     * Trims the stream by evicting older entries.
     *
     * @see <a href="https://redis.io/commands/xtrim/">redis.io</a> for details.
     * @param key The key of the stream.
     * @param options Stream trim options {@link StreamTrimOptions}.
     * @return The number of entries deleted from the stream.
     * @example
     *     <pre>{@code
     * // A nearly exact trimming of the stream to at least a length of 10
     * Long trimmed = client.xtrim("key", new MaxLen(false, 10L)).get();
     * System.out.println("Number of trimmed entries from stream: " + trimmed);
     *
     * // An exact trimming of the stream by minimum id of "0-3", limit of 10 entries
     * Long trimmed = client.xtrim("key", new MinId(true, "0-3", 10L)).get();
     * System.out.println("Number of trimmed entries from stream: " + trimmed);
     * }</pre>
     */
    CompletableFuture<Long> xtrim(String key, StreamTrimOptions options);

    /**
     * Returns the number of entries in the stream stored at <code>key</code>.
     *
     * @see <a href="https://valkey.io/commands/xlen/">valkey.io</a> for details.
     * @param key The key of the stream.
     * @return The number of entries in the stream. If <code>key</code> does not exist, return <code>0
     *     </code>.
     * @example
     *     <pre>{@code
     * Long num = client.xlen("key").get();
     * assert num == 2L; // Stream has 2 entries
     * }</pre>
     */
    CompletableFuture<Long> xlen(String key);

    /**
     * Removes the specified entries by id from a stream, and returns the number of entries deleted.
     *
     * @see <a href="https://valkey.io/commands/xdel/">valkey.io</a> for details.
     * @param key The key of the stream.
     * @param ids An array of entry ids.
     * @return The number of entries removed from the stream. This number may be less than the number
     *     of entries in <code>ids</code>, if the specified <code>ids</code> don't exist in the
     *     stream.
     * @example
     *     <pre>{@code
     * Long num = client.xdel("key", new String[] {"1538561698944-0", "1538561698944-1"}).get();
     * assert num == 2L; // Stream marked 2 entries as deleted
     * }</pre>
     */
    CompletableFuture<Long> xdel(String key, String[] ids);

    /**
     * Reads entries from the given streams.
     *
     * @apiNote When in cluster mode, all keys in <code>keysAndIds</code> must map to the same hash
     *     slot.
     * @see <a href="https://redis.io/commands/xread/">redis.io</a> for details.
     * @param keysAndIds A <code>Map</code> of keys and entry ids to read from. The <code>
     *     Map</code> is composed of a stream's key and the id of the entry after which the stream
     *     will be read.
     * @return A <code>{@literal Map<String, Map<String, Map<String, String>>>}</code> with stream
     *     keys, to <code>Map</code> of stream-ids, to a <code>Map</code> of field-entries.
     * @example
     *     <pre>{@code
     * Map<String, String> xreadKeys = Map.of("streamKey", "readId");
     * Map<String, Map<String, Map<String, String>>> streamReadResponse = client.xread(xreadKeys).get();
     * for (var keyEntry : streamReadResponse.entrySet()) {
     *     for (var streamEntry : keyEntry.getValue().entrySet()) {
     *         for (var fieldEntry : streamEntry.getValue().entrySet()) {
     *             System.out.printf("Key: %s; stream id: %s; field: %s; value: %s\n", keyentry.getKey(), streamEntry.getKey(), fieldEntry.getKey(), fieldEntry.getValue());
     *         }
     *     }
     * }
     * }</pre>
     */
    CompletableFuture<Map<String, Map<String, Map<String, String>>>> xread(
            Map<String, String> keysAndIds);

    /**
     * Reads entries from the given streams.
     *
     * @apiNote When in cluster mode, all keys in <code>keysAndIds</code> must map to the same hash
     *     slot.
     * @see <a href="https://redis.io/commands/xread/">redis.io</a> for details.
     * @param keysAndIds A <code>Map</code> of keys and entry ids to read from. The <code>
     *     Map</code> is composed of a stream's key and the id of the entry after which the stream
     *     will be read.
     * @param options Options detailing how to read the stream {@link StreamReadOptions}.
     * @return A <code>{@literal Map<String, Map<String, Map<String, String>>>}</code> with stream
     *     keys, to <code>Map</code> of stream-ids, to a <code>Map</code> of field-entries.
     * @example
     *     <pre>{@code
     * Map<String, String> xreadKeys = Map.of("streamKey", "readId");
     * StreamReadOptions options = StreamReadOptions.builder().block(1L).build();
     * Map<String, Map<String, Map<String, String>>> streamReadResponse = client.xread(xreadKeys, options).get();
     * for (var keyEntry : streamReadResponse.entrySet()) {
     *     for (var streamEntry : keyEntry.getValue().entrySet()) {
     *         for (var fieldEntry : streamEntry.getValue().entrySet()) {
     *             System.out.printf("Key: %s; stream id: %s; field: %s; value: %s\n", keyentry.getKey(), streamEntry.getKey(), fieldEntry.getKey(), fieldEntry.getValue());
     *         }
     *     }
     * }
     * }</pre>
     */
    CompletableFuture<Map<String, Map<String, Map<String, String>>>> xread(
            Map<String, String> keysAndIds, StreamReadOptions options);
}
