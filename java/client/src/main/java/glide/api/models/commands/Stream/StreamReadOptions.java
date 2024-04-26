/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.models.commands.Stream;

import glide.api.commands.StreamBaseCommands;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Builder;

/**
 * Optional arguments for {@link StreamBaseCommands#xread(Map, StreamReadOptions)}
 *
 * @see <a href="https://redis.io/commands/xread/">redis.io</a>
 */
@Builder
public final class StreamReadOptions {

    public static final String READ_COUNT_REDIS_API = "COUNT";
    public static final String READ_BLOCK_REDIS_API = "BLOCK";
    public static final String READ_STREAMS_REDIS_API = "STREAMS";

    /**
     * If set, the read request will block for the set amount of milliseconds or until the server has
     * the required number of entries. Equivalent to <code>BLOCK</code> in the Redis API.
     */
    Long block;

    /**
     * The maximal number of elements requested. Equivalent to <code>COUNT</code> in the Redis API.
     */
    Long count;

    /**
     * Converts options for {@link StreamBaseCommands#xread(Map, StreamReadOptions)} into a String[].
     *
     * @return String[]
     */
    public String[] toArgs(Map<String, String> streams) {
        List<String> optionArgs = new ArrayList<>();

        if (this.block != null) {
            optionArgs.add(READ_BLOCK_REDIS_API);
            optionArgs.add(block.toString());
        }

        if (this.count != null) {
            optionArgs.add(READ_COUNT_REDIS_API);
            optionArgs.add(count.toString());
        }

        optionArgs.add(READ_STREAMS_REDIS_API);
        Set<Map.Entry<String, String>> entrySet = streams.entrySet();
        optionArgs.addAll(entrySet.stream().map(Map.Entry::getKey).collect(Collectors.toList()));
        optionArgs.addAll(entrySet.stream().map(Map.Entry::getValue).collect(Collectors.toList()));

        return optionArgs.toArray(new String[0]);
    }
}
