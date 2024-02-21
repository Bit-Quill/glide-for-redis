/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide;

import glide.api.models.BaseTransaction;
import glide.api.models.commands.SetOptions;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TransactionTestUtilities {
    private static final String key1 = "{key}" + UUID.randomUUID();
    private static final String key2 = "{key}" + UUID.randomUUID();
    private static final String key3 = "{key}" + UUID.randomUUID();
    private static final String key4 = "{key}" + UUID.randomUUID();
    private static final String key5 = "{key}" + UUID.randomUUID();
    private static final String key6 = "{key}" + UUID.randomUUID();
    private static final String value1 = UUID.randomUUID().toString();
    private static final String value2 = UUID.randomUUID().toString();
    private static final String field1 = UUID.randomUUID().toString();
    private static final String field2 = UUID.randomUUID().toString();

    public static BaseTransaction<?> transactionTest(BaseTransaction<?> baseTransaction) {

        baseTransaction.set(key1, value1);
        baseTransaction.get(key1);

        baseTransaction.set(key2, value2, SetOptions.builder().returnOldValue(true).build());
        baseTransaction.customCommand(new String[] {"MGET", key1, key2});

        baseTransaction.exists(new String[] {key1});

        baseTransaction.del(new String[] {key1});
        baseTransaction.get(key1);

        baseTransaction.mset(Map.of(key1, value2, key2, value1));
        baseTransaction.mget(new String[] {key1, key2});

        baseTransaction.incr(key3);
        baseTransaction.incrBy(key3, 2);

        baseTransaction.decr(key3);
        baseTransaction.decrBy(key3, 2);

        baseTransaction.incrByFloat(key3, 0.5);

        baseTransaction.hset(key4, Map.of(field1, value1, field2, value2));
        baseTransaction.hget(key4, field1);
        baseTransaction.hdel(key4, new String[] {field1});

        baseTransaction.rpush(key5, new String[] {value1, value2, value2});
        baseTransaction.rpop(key5);
        baseTransaction.rpopCount(key5, 2);

        baseTransaction.sadd(key6, new String[] {"baz", "foo"});
        baseTransaction.srem(key6, new String[] {"foo"});
        baseTransaction.scard(key6);
        baseTransaction.smembers(key6);

        return baseTransaction;
    }

    public static Object[] transactionTestResult() {
        return new Object[] {
            "OK",
            value1,
            null,
            new String[] {value1, value2},
            1L,
            1L,
            null,
            "OK",
            new String[] {value2, value1},
            1L,
            3L,
            2L,
            0L,
            0.5,
            2L,
            value1,
            1L,
            3L,
            value2,
            new String[] {value2, value1},
            2L,
            1L,
            1L,
            Set.of("baz"),
        };
    }
}
