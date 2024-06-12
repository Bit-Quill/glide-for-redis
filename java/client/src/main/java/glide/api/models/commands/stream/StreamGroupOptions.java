/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.models.commands.stream;

import glide.api.commands.StreamBaseCommands;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;

/**
 * Optional arguments for {@link StreamBaseCommands#xgroupCreate(String, String, String,
 * StreamGroupOptions)}
 *
 * @see <a href="https://valkey.io/commands/xgroup-create/">valkey.io</a>
 */
@Builder
public final class StreamGroupOptions {

    public static final String MAKE_STREAM_REDIS_API = "MKSTREAM";
    public static final String ENTRIES_READ_REDIS_API = "ENTRIESREAD";

    /** If the stream doesn't exist, creates a new stream with a length of 0. */
    Boolean makeStream;

    /**
     * An arbitrary ID (that isn't the first ID, last ID, or the zero <code>"0-0"</code>. Use it to
     * find out how many entries are between the arbitrary ID (excluding it) and the stream's last
     * entry.
     *
     * @since Redis 7.0.0
     */
    String entriesRead;

    /**
     * Converts options and the key-to-id input for {@link StreamBaseCommands#xgroupCreate(String,
     * String, String, StreamGroupOptions)} into a String[].
     *
     * @return String[]
     */
    public String[] toArgs() {
        List<String> optionArgs = new ArrayList<>();

        if (this.makeStream != null && this.makeStream) {
            optionArgs.add(MAKE_STREAM_REDIS_API);
        }

        if (this.entriesRead != null) {
            optionArgs.add(ENTRIES_READ_REDIS_API);
            optionArgs.add(this.entriesRead);
        }

        return optionArgs.toArray(new String[0]);
    }
}