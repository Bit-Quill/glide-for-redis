/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.models;

import static glide.api.models.commands.SortOptions.STORE_COMMAND_STRING;
import static redis_request.RedisRequestOuterClass.RequestType.Move;
import static redis_request.RedisRequestOuterClass.RequestType.Select;
import static redis_request.RedisRequestOuterClass.RequestType.Sort;
import static redis_request.RedisRequestOuterClass.RequestType.SortReadOnly;

import glide.api.models.commands.SortStandaloneOptions;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.apache.commons.lang3.ArrayUtils;
import redis_request.RedisRequestOuterClass.Command.ArgsArray;

/**
 * Extends BaseTransaction class for Redis standalone commands. Transactions allow the execution of
 * a group of commands in a single step.
 *
 * <p>Command Response: An array of command responses is returned by the client <code>exec</code>
 * command, in the order they were given. Each element in the array represents a command given to
 * the <code>Transaction</code>. The response for each command depends on the executed Redis
 * command. Specific response types are documented alongside each method.
 *
 * @example
 *     <pre>{@code
 * Transaction transaction = new Transaction()
 *     .set("key", "value")
 *     .get("key");
 * Object[] result = client.exec(transaction).get();
 * // result contains: OK and "value"
 * assert result[0].equals("OK");
 * assert result[1].equals("value");
 * }</pre>
 */
@AllArgsConstructor
public class Transaction extends BaseTransaction<Transaction> {
    @Override
    protected Transaction getThis() {
        return this;
    }

    /**
     * Changes the currently selected Redis database.
     *
     * @see <a href="https://redis.io/commands/select/">redis.io</a> for details.
     * @param index The index of the database to select.
     * @return Command Response - A simple <code>OK</code> response.
     */
    public Transaction select(long index) {
        ArgsArray commandArgs = buildArgs(Long.toString(index));

        protobufTransaction.addCommands(buildCommand(Select, commandArgs));
        return this;
    }

    /**
     * Move <code>key</code> from the currently selected database to the database specified by <code>
     * dbIndex</code>.
     *
     * @see <a href="https://redis.io/commands/move/">redis.io</a> for more details.
     * @param key The key to move.
     * @param dbIndex The index of the database to move <code>key</code> to.
     * @return Command Response - <code>true</code> if <code>key</code> was moved, or <code>false
     *     </code> if the <code>key</code> already exists in the destination database or does not
     *     exist in the source database.
     */
    public Transaction move(String key, long dbIndex) {
        ArgsArray commandArgs = buildArgs(key, Long.toString(dbIndex));
        protobufTransaction.addCommands(buildCommand(Move, commandArgs));
        return this;
    }

    /**
     * Sorts the elements in the list, set, or sorted set at <code>key</code> and returns the result.
     * The <code>sort</code> command can be used to sort elements based on different criteria and
     * apply transformations on sorted elements. To store the result into a new key, see <code>
     * sort_store</code>.
     *
     * @param key The key of the list, set, or sorted set to be sorted.
     * @param sortStandaloneOptions The {@link SortStandaloneOptions}.
     * @return Command Response - A list of sorted elements.
     */
    public Transaction sort(
            @NonNull String key, @NonNull SortStandaloneOptions sortStandaloneOptions) {
        ArgsArray commandArgs = buildArgs(ArrayUtils.addFirst(sortStandaloneOptions.toArgs(), key));
        protobufTransaction.addCommands(buildCommand(Sort, commandArgs));
        return this;
    }

    /**
     * Sorts the elements in the list, set, or sorted set at <code>key</code> and returns the result.
     * This command is routed depending on the client's <code>ReadFrom</code> strategy. The <code>
     * sortReadOnly</code> command can be used to sort elements based on different criteria and apply
     * transformations on sorted elements.
     *
     * @param key The key of the list, set, or sorted set to be sorted.
     * @param sortStandaloneOptions The {@link SortStandaloneOptions}.
     * @return Command Response - A list of sorted elements.
     */
    public Transaction sortReadOnly(
            @NonNull String key, @NonNull SortStandaloneOptions sortStandaloneOptions) {
        ArgsArray commandArgs = buildArgs(ArrayUtils.addFirst(sortStandaloneOptions.toArgs(), key));
        protobufTransaction.addCommands(buildCommand(SortReadOnly, commandArgs));
        return this;
    }

    /**
     * Sorts the elements in the list, set, or sorted set at <code>key</code> and stores the result in
     * <code>destination</code>. The <code>sort</code> command can be used to sort elements based on
     * different criteria, apply transformations on sorted elements, and store the result in a new
     * key. To get the sort result without storing it into a key, see <code>sort</code>.
     *
     * @param key The key of the list, set, or sorted set to be sorted.
     * @param sortStandaloneOptions The {@link SortStandaloneOptions}.
     * @param destination The key where the sorted result will be stored.
     * @return Command Response - The number of elements in the sorted key stored at <code>destination
     *     </code>.
     */
    public Transaction sortStore(
            @NonNull String key,
            @NonNull String destination,
            @NonNull SortStandaloneOptions sortStandaloneOptions) {
        String[] storeArguments = new String[] {STORE_COMMAND_STRING, destination};
        ArgsArray arguments =
                buildArgs(
                        ArrayUtils.addFirst(
                                ArrayUtils.addAll(storeArguments, sortStandaloneOptions.toArgs()), key));
        protobufTransaction.addCommands(buildCommand(Sort, arguments));
        return this;
    }
}
