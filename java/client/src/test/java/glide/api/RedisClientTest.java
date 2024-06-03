/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api;

import static glide.api.BaseClient.OK;
import static glide.api.commands.HashBaseCommands.WITH_VALUES_REDIS_API;
import static glide.api.commands.ListBaseCommands.COUNT_FOR_LIST_REDIS_API;
import static glide.api.commands.ServerManagementCommands.VERSION_REDIS_API;
import static glide.api.commands.SortedSetBaseCommands.LIMIT_REDIS_API;
import static glide.api.commands.SortedSetBaseCommands.WITH_SCORES_REDIS_API;
import static glide.api.commands.SortedSetBaseCommands.WITH_SCORE_REDIS_API;
import static glide.api.models.commands.FlushMode.SYNC;
import static glide.api.models.commands.LInsertOptions.InsertPosition.BEFORE;
import static glide.api.models.commands.ScoreFilter.MAX;
import static glide.api.models.commands.SetOptions.ConditionalSet.ONLY_IF_DOES_NOT_EXIST;
import static glide.api.models.commands.SetOptions.ConditionalSet.ONLY_IF_EXISTS;
import static glide.api.models.commands.SetOptions.RETURN_OLD_VALUE;
import static glide.api.models.commands.geospatial.GeoAddOptions.CHANGED_REDIS_API;
import static glide.api.models.commands.stream.StreamAddOptions.NO_MAKE_STREAM_REDIS_API;
import static glide.api.models.commands.stream.StreamTrimOptions.TRIM_EXACT_REDIS_API;
import static glide.api.models.commands.stream.StreamTrimOptions.TRIM_LIMIT_REDIS_API;
import static glide.api.models.commands.stream.StreamTrimOptions.TRIM_MAXLEN_REDIS_API;
import static glide.api.models.commands.stream.StreamTrimOptions.TRIM_MINID_REDIS_API;
import static glide.api.models.commands.stream.StreamTrimOptions.TRIM_NOT_EXACT_REDIS_API;
import static glide.utils.ArrayTransformUtils.concatenateArrays;
import static glide.utils.ArrayTransformUtils.convertMapToKeyValueStringArray;
import static glide.utils.ArrayTransformUtils.convertMapToValueKeyStringArray;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static redis_request.RedisRequestOuterClass.RequestType.Append;
import static redis_request.RedisRequestOuterClass.RequestType.BLMPop;
import static redis_request.RedisRequestOuterClass.RequestType.BLPop;
import static redis_request.RedisRequestOuterClass.RequestType.BRPop;
import static redis_request.RedisRequestOuterClass.RequestType.BZMPop;
import static redis_request.RedisRequestOuterClass.RequestType.BZPopMax;
import static redis_request.RedisRequestOuterClass.RequestType.BZPopMin;
import static redis_request.RedisRequestOuterClass.RequestType.BitCount;
import static redis_request.RedisRequestOuterClass.RequestType.BitOp;
import static redis_request.RedisRequestOuterClass.RequestType.BitPos;
import static redis_request.RedisRequestOuterClass.RequestType.ClientGetName;
import static redis_request.RedisRequestOuterClass.RequestType.ClientId;
import static redis_request.RedisRequestOuterClass.RequestType.ConfigGet;
import static redis_request.RedisRequestOuterClass.RequestType.ConfigResetStat;
import static redis_request.RedisRequestOuterClass.RequestType.ConfigRewrite;
import static redis_request.RedisRequestOuterClass.RequestType.ConfigSet;
import static redis_request.RedisRequestOuterClass.RequestType.CustomCommand;
import static redis_request.RedisRequestOuterClass.RequestType.Decr;
import static redis_request.RedisRequestOuterClass.RequestType.DecrBy;
import static redis_request.RedisRequestOuterClass.RequestType.Del;
import static redis_request.RedisRequestOuterClass.RequestType.Echo;
import static redis_request.RedisRequestOuterClass.RequestType.Exists;
import static redis_request.RedisRequestOuterClass.RequestType.Expire;
import static redis_request.RedisRequestOuterClass.RequestType.ExpireAt;
import static redis_request.RedisRequestOuterClass.RequestType.ExpireTime;
import static redis_request.RedisRequestOuterClass.RequestType.FlushAll;
import static redis_request.RedisRequestOuterClass.RequestType.FunctionLoad;
import static redis_request.RedisRequestOuterClass.RequestType.GeoAdd;
import static redis_request.RedisRequestOuterClass.RequestType.GeoDist;
import static redis_request.RedisRequestOuterClass.RequestType.GeoHash;
import static redis_request.RedisRequestOuterClass.RequestType.GeoPos;
import static redis_request.RedisRequestOuterClass.RequestType.Get;
import static redis_request.RedisRequestOuterClass.RequestType.GetBit;
import static redis_request.RedisRequestOuterClass.RequestType.GetRange;
import static redis_request.RedisRequestOuterClass.RequestType.HDel;
import static redis_request.RedisRequestOuterClass.RequestType.HExists;
import static redis_request.RedisRequestOuterClass.RequestType.HGet;
import static redis_request.RedisRequestOuterClass.RequestType.HGetAll;
import static redis_request.RedisRequestOuterClass.RequestType.HIncrBy;
import static redis_request.RedisRequestOuterClass.RequestType.HIncrByFloat;
import static redis_request.RedisRequestOuterClass.RequestType.HKeys;
import static redis_request.RedisRequestOuterClass.RequestType.HLen;
import static redis_request.RedisRequestOuterClass.RequestType.HMGet;
import static redis_request.RedisRequestOuterClass.RequestType.HRandField;
import static redis_request.RedisRequestOuterClass.RequestType.HSet;
import static redis_request.RedisRequestOuterClass.RequestType.HSetNX;
import static redis_request.RedisRequestOuterClass.RequestType.HStrlen;
import static redis_request.RedisRequestOuterClass.RequestType.HVals;
import static redis_request.RedisRequestOuterClass.RequestType.Incr;
import static redis_request.RedisRequestOuterClass.RequestType.IncrBy;
import static redis_request.RedisRequestOuterClass.RequestType.IncrByFloat;
import static redis_request.RedisRequestOuterClass.RequestType.Info;
import static redis_request.RedisRequestOuterClass.RequestType.LIndex;
import static redis_request.RedisRequestOuterClass.RequestType.LInsert;
import static redis_request.RedisRequestOuterClass.RequestType.LLen;
import static redis_request.RedisRequestOuterClass.RequestType.LMPop;
import static redis_request.RedisRequestOuterClass.RequestType.LMove;
import static redis_request.RedisRequestOuterClass.RequestType.LPop;
import static redis_request.RedisRequestOuterClass.RequestType.LPush;
import static redis_request.RedisRequestOuterClass.RequestType.LPushX;
import static redis_request.RedisRequestOuterClass.RequestType.LRange;
import static redis_request.RedisRequestOuterClass.RequestType.LRem;
import static redis_request.RedisRequestOuterClass.RequestType.LSet;
import static redis_request.RedisRequestOuterClass.RequestType.LTrim;
import static redis_request.RedisRequestOuterClass.RequestType.LastSave;
import static redis_request.RedisRequestOuterClass.RequestType.Lolwut;
import static redis_request.RedisRequestOuterClass.RequestType.MGet;
import static redis_request.RedisRequestOuterClass.RequestType.MSet;
import static redis_request.RedisRequestOuterClass.RequestType.ObjectEncoding;
import static redis_request.RedisRequestOuterClass.RequestType.ObjectFreq;
import static redis_request.RedisRequestOuterClass.RequestType.ObjectIdleTime;
import static redis_request.RedisRequestOuterClass.RequestType.ObjectRefCount;
import static redis_request.RedisRequestOuterClass.RequestType.PExpire;
import static redis_request.RedisRequestOuterClass.RequestType.PExpireAt;
import static redis_request.RedisRequestOuterClass.RequestType.PExpireTime;
import static redis_request.RedisRequestOuterClass.RequestType.PTTL;
import static redis_request.RedisRequestOuterClass.RequestType.Persist;
import static redis_request.RedisRequestOuterClass.RequestType.PfAdd;
import static redis_request.RedisRequestOuterClass.RequestType.PfCount;
import static redis_request.RedisRequestOuterClass.RequestType.PfMerge;
import static redis_request.RedisRequestOuterClass.RequestType.Ping;
import static redis_request.RedisRequestOuterClass.RequestType.RPop;
import static redis_request.RedisRequestOuterClass.RequestType.RPush;
import static redis_request.RedisRequestOuterClass.RequestType.RPushX;
import static redis_request.RedisRequestOuterClass.RequestType.RandomKey;
import static redis_request.RedisRequestOuterClass.RequestType.Rename;
import static redis_request.RedisRequestOuterClass.RequestType.RenameNX;
import static redis_request.RedisRequestOuterClass.RequestType.SAdd;
import static redis_request.RedisRequestOuterClass.RequestType.SCard;
import static redis_request.RedisRequestOuterClass.RequestType.SDiff;
import static redis_request.RedisRequestOuterClass.RequestType.SDiffStore;
import static redis_request.RedisRequestOuterClass.RequestType.SInter;
import static redis_request.RedisRequestOuterClass.RequestType.SInterStore;
import static redis_request.RedisRequestOuterClass.RequestType.SIsMember;
import static redis_request.RedisRequestOuterClass.RequestType.SMIsMember;
import static redis_request.RedisRequestOuterClass.RequestType.SMembers;
import static redis_request.RedisRequestOuterClass.RequestType.SMove;
import static redis_request.RedisRequestOuterClass.RequestType.SRem;
import static redis_request.RedisRequestOuterClass.RequestType.SUnionStore;
import static redis_request.RedisRequestOuterClass.RequestType.Select;
import static redis_request.RedisRequestOuterClass.RequestType.SetBit;
import static redis_request.RedisRequestOuterClass.RequestType.SetRange;
import static redis_request.RedisRequestOuterClass.RequestType.Strlen;
import static redis_request.RedisRequestOuterClass.RequestType.TTL;
import static redis_request.RedisRequestOuterClass.RequestType.Time;
import static redis_request.RedisRequestOuterClass.RequestType.Touch;
import static redis_request.RedisRequestOuterClass.RequestType.Type;
import static redis_request.RedisRequestOuterClass.RequestType.Unlink;
import static redis_request.RedisRequestOuterClass.RequestType.XAdd;
import static redis_request.RedisRequestOuterClass.RequestType.XDel;
import static redis_request.RedisRequestOuterClass.RequestType.XLen;
import static redis_request.RedisRequestOuterClass.RequestType.XTrim;
import static redis_request.RedisRequestOuterClass.RequestType.ZAdd;
import static redis_request.RedisRequestOuterClass.RequestType.ZCard;
import static redis_request.RedisRequestOuterClass.RequestType.ZCount;
import static redis_request.RedisRequestOuterClass.RequestType.ZDiff;
import static redis_request.RedisRequestOuterClass.RequestType.ZDiffStore;
import static redis_request.RedisRequestOuterClass.RequestType.ZIncrBy;
import static redis_request.RedisRequestOuterClass.RequestType.ZInter;
import static redis_request.RedisRequestOuterClass.RequestType.ZInterCard;
import static redis_request.RedisRequestOuterClass.RequestType.ZInterStore;
import static redis_request.RedisRequestOuterClass.RequestType.ZLexCount;
import static redis_request.RedisRequestOuterClass.RequestType.ZMPop;
import static redis_request.RedisRequestOuterClass.RequestType.ZMScore;
import static redis_request.RedisRequestOuterClass.RequestType.ZPopMax;
import static redis_request.RedisRequestOuterClass.RequestType.ZPopMin;
import static redis_request.RedisRequestOuterClass.RequestType.ZRandMember;
import static redis_request.RedisRequestOuterClass.RequestType.ZRange;
import static redis_request.RedisRequestOuterClass.RequestType.ZRangeStore;
import static redis_request.RedisRequestOuterClass.RequestType.ZRank;
import static redis_request.RedisRequestOuterClass.RequestType.ZRem;
import static redis_request.RedisRequestOuterClass.RequestType.ZRemRangeByLex;
import static redis_request.RedisRequestOuterClass.RequestType.ZRemRangeByRank;
import static redis_request.RedisRequestOuterClass.RequestType.ZRemRangeByScore;
import static redis_request.RedisRequestOuterClass.RequestType.ZRevRank;
import static redis_request.RedisRequestOuterClass.RequestType.ZScore;
import static redis_request.RedisRequestOuterClass.RequestType.ZUnion;
import static redis_request.RedisRequestOuterClass.RequestType.ZUnionStore;

import glide.api.models.Script;
import glide.api.models.Transaction;
import glide.api.models.commands.ConditionalChange;
import glide.api.models.commands.ExpireOptions;
import glide.api.models.commands.InfoOptions;
import glide.api.models.commands.ListDirection;
import glide.api.models.commands.RangeOptions;
import glide.api.models.commands.RangeOptions.InfLexBound;
import glide.api.models.commands.RangeOptions.InfScoreBound;
import glide.api.models.commands.RangeOptions.LexBoundary;
import glide.api.models.commands.RangeOptions.RangeByIndex;
import glide.api.models.commands.RangeOptions.RangeByLex;
import glide.api.models.commands.RangeOptions.RangeByScore;
import glide.api.models.commands.RangeOptions.ScoreBoundary;
import glide.api.models.commands.ScoreFilter;
import glide.api.models.commands.ScriptOptions;
import glide.api.models.commands.SetOptions;
import glide.api.models.commands.SetOptions.Expiry;
import glide.api.models.commands.WeightAggregateOptions.Aggregate;
import glide.api.models.commands.WeightAggregateOptions.KeyArray;
import glide.api.models.commands.WeightAggregateOptions.WeightedKeys;
import glide.api.models.commands.ZAddOptions;
import glide.api.models.commands.bitmap.BitmapIndexType;
import glide.api.models.commands.bitmap.BitwiseOperation;
import glide.api.models.commands.function.FunctionLoadOptions;
import glide.api.models.commands.geospatial.GeoAddOptions;
import glide.api.models.commands.geospatial.GeoUnit;
import glide.api.models.commands.geospatial.GeospatialData;
import glide.api.models.commands.stream.StreamAddOptions;
import glide.api.models.commands.stream.StreamTrimOptions;
import glide.api.models.commands.stream.StreamTrimOptions.MaxLen;
import glide.api.models.commands.stream.StreamTrimOptions.MinId;
import glide.managers.CommandManager;
import glide.managers.ConnectionManager;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.SneakyThrows;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import redis_request.RedisRequestOuterClass.RequestType;

public class RedisClientTest {

    // bypass import conflict between Set (collection) and Set (enum variant)
    private static final RequestType pSet = RequestType.Set;

    RedisClient service;

    ConnectionManager connectionManager;

    CommandManager commandManager;

    @BeforeEach
    public void setUp() {
        connectionManager = mock(ConnectionManager.class);
        commandManager = mock(CommandManager.class);
        service = new RedisClient(connectionManager, commandManager);
    }

    @SneakyThrows
    @Test
    public void customCommand_returns_success() {
        // setup
        String key = "testKey";
        Object value = "testValue";
        String cmd = "GETSTRING";
        String[] arguments = new String[] {cmd, key};
        CompletableFuture<Object> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.submitNewCommand(eq(CustomCommand), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Object> response = service.customCommand(arguments);
        String payload = (String) response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void exec() {
        // setup
        Object[] value = new Object[] {"PONG", "PONG"};
        Transaction transaction = new Transaction().ping().ping();

        CompletableFuture<Object[]> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Object[]>submitNewTransaction(eq(transaction), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Object[]> response = service.exec(transaction);
        Object[] payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertArrayEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void echo_returns_success() {
        // setup
        String message = "GLIDE FOR REDIS";
        String[] arguments = new String[] {message};
        CompletableFuture<String> testResponse = new CompletableFuture<>();
        testResponse.complete(message);

        // match on protobuf request
        when(commandManager.<String>submitNewCommand(eq(Echo), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String> response = service.echo(message);
        String echo = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(message, echo);
    }

    @SneakyThrows
    @Test
    public void ping_returns_success() {
        // setup
        CompletableFuture<String> testResponse = new CompletableFuture<>();
        testResponse.complete("PONG");

        // match on protobuf request
        when(commandManager.<String>submitNewCommand(eq(Ping), eq(new String[0]), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String> response = service.ping();
        String payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals("PONG", payload);
    }

    @SneakyThrows
    @Test
    public void ping_with_message_returns_success() {
        // setup
        String message = "RETURN OF THE PONG";
        String[] arguments = new String[] {message};
        CompletableFuture<String> testResponse = new CompletableFuture<>();
        testResponse.complete(message);

        // match on protobuf request
        when(commandManager.<String>submitNewCommand(eq(Ping), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String> response = service.ping(message);
        String pong = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(message, pong);
    }

    @SneakyThrows
    @Test
    public void select_returns_success() {
        // setup
        CompletableFuture<String> testResponse = new CompletableFuture<>();
        long index = 5L;
        testResponse.complete(OK);

        // match on protobuf request
        when(commandManager.<String>submitNewCommand(
                        eq(Select), eq(new String[] {Long.toString(index)}), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String> response = service.select(index);
        String payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(OK, payload);
    }

    @SneakyThrows
    @Test
    public void del_returns_long_success() {
        // setup
        String[] keys = new String[] {"testKey1", "testKey2"};
        Long numberDeleted = 1L;
        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(numberDeleted);
        when(commandManager.<Long>submitNewCommand(eq(Del), eq(keys), any())).thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.del(keys);
        Long result = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(numberDeleted, result);
    }

    @SneakyThrows
    @Test
    public void unlink_returns_long_success() {
        // setup
        String[] keys = new String[] {"testKey1", "testKey2"};
        Long numberUnlinked = 1L;
        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(numberUnlinked);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(Unlink), eq(keys), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.unlink(keys);
        Long result = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(numberUnlinked, result);
    }

    @SneakyThrows
    @Test
    public void get_returns_success() {
        // setup
        String key = "testKey";
        String value = "testValue";
        CompletableFuture<String> testResponse = new CompletableFuture<>();
        testResponse.complete(value);
        when(commandManager.<String>submitNewCommand(eq(Get), eq(new String[] {key}), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String> response = service.get(key);
        String payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void set_returns_success() {
        // setup
        String key = "testKey";
        String value = "testValue";
        CompletableFuture<String> testResponse = new CompletableFuture<>();
        testResponse.complete(null);
        when(commandManager.<String>submitNewCommand(eq(pSet), eq(new String[] {key, value}), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String> response = service.set(key, value);
        Object okResponse = response.get();

        // verify
        assertEquals(testResponse, response);
        assertNull(okResponse);
    }

    @SneakyThrows
    @Test
    public void set_with_SetOptions_OnlyIfExists_returns_success() {
        // setup
        String key = "testKey";
        String value = "testValue";
        SetOptions setOptions =
                SetOptions.builder()
                        .conditionalSet(ONLY_IF_EXISTS)
                        .returnOldValue(false)
                        .expiry(Expiry.KeepExisting())
                        .build();
        String[] arguments = new String[] {key, value, ONLY_IF_EXISTS.getRedisApi(), "KEEPTTL"};

        CompletableFuture<String> testResponse = new CompletableFuture<>();
        testResponse.complete(null);
        when(commandManager.<String>submitNewCommand(eq(pSet), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String> response = service.set(key, value, setOptions);

        // verify
        assertEquals(testResponse, response);
        assertNull(response.get());
    }

    @SneakyThrows
    @Test
    public void set_with_SetOptions_OnlyIfDoesNotExist_returns_success() {
        // setup
        String key = "testKey";
        String value = "testValue";
        SetOptions setOptions =
                SetOptions.builder()
                        .conditionalSet(ONLY_IF_DOES_NOT_EXIST)
                        .returnOldValue(true)
                        .expiry(Expiry.UnixSeconds(60L))
                        .build();
        String[] arguments =
                new String[] {
                    key, value, ONLY_IF_DOES_NOT_EXIST.getRedisApi(), RETURN_OLD_VALUE, "EXAT", "60"
                };
        CompletableFuture<String> testResponse = new CompletableFuture<>();
        testResponse.complete(value);
        when(commandManager.<String>submitNewCommand(eq(pSet), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String> response = service.set(key, value, setOptions);

        // verify
        assertNotNull(response);
        assertEquals(value, response.get());
    }

    @SneakyThrows
    @Test
    public void exists_returns_long_success() {
        // setup
        String[] keys = new String[] {"testKey1", "testKey2"};
        Long numberExisting = 1L;
        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(numberExisting);
        when(commandManager.<Long>submitNewCommand(eq(Exists), eq(keys), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.exists(keys);
        Long result = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(numberExisting, result);
    }

    @SneakyThrows
    @Test
    public void expire_returns_success() {
        // setup
        String key = "testKey";
        long seconds = 10L;
        String[] arguments = new String[] {key, Long.toString(seconds)};

        CompletableFuture<Boolean> testResponse = new CompletableFuture<>();
        testResponse.complete(Boolean.TRUE);

        // match on protobuf request
        when(commandManager.<Boolean>submitNewCommand(eq(Expire), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Boolean> response = service.expire(key, seconds);

        // verify
        assertEquals(testResponse, response);
        assertEquals(true, response.get());
    }

    @SneakyThrows
    @Test
    public void expire_with_expireOptions_returns_success() {
        // setup
        String key = "testKey";
        long seconds = 10L;
        String[] arguments = new String[] {key, Long.toString(seconds), "NX"};

        CompletableFuture<Boolean> testResponse = new CompletableFuture<>();
        testResponse.complete(Boolean.FALSE);

        // match on protobuf request
        when(commandManager.<Boolean>submitNewCommand(eq(Expire), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Boolean> response = service.expire(key, seconds, ExpireOptions.HAS_NO_EXPIRY);

        // verify
        assertEquals(testResponse, response);
        assertEquals(false, response.get());
    }

    @SneakyThrows
    @Test
    public void expireAt_returns_success() {
        // setup
        String key = "testKey";
        long unixSeconds = 100000L;
        String[] arguments = new String[] {key, Long.toString(unixSeconds)};

        CompletableFuture<Boolean> testResponse = new CompletableFuture<>();
        testResponse.complete(Boolean.TRUE);

        // match on protobuf request
        when(commandManager.<Boolean>submitNewCommand(eq(ExpireAt), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Boolean> response = service.expireAt(key, unixSeconds);

        // verify
        assertEquals(testResponse, response);
        assertEquals(true, response.get());
    }

    @SneakyThrows
    @Test
    public void expireAt_with_expireOptions_returns_success() {
        // setup
        String key = "testKey";
        long unixSeconds = 100000L;
        String[] arguments = new String[] {key, Long.toString(unixSeconds), "XX"};

        CompletableFuture<Boolean> testResponse = new CompletableFuture<>();
        testResponse.complete(Boolean.FALSE);

        // match on protobuf request
        when(commandManager.<Boolean>submitNewCommand(eq(ExpireAt), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Boolean> response =
                service.expireAt(key, unixSeconds, ExpireOptions.HAS_EXISTING_EXPIRY);

        // verify
        assertEquals(testResponse, response);
        assertEquals(false, response.get());
    }

    @SneakyThrows
    @Test
    public void pexpire_returns_success() {
        // setup
        String key = "testKey";
        long milliseconds = 50000L;
        String[] arguments = new String[] {key, Long.toString(milliseconds)};

        CompletableFuture<Boolean> testResponse = new CompletableFuture<>();
        testResponse.complete(Boolean.TRUE);

        // match on protobuf request
        when(commandManager.<Boolean>submitNewCommand(eq(PExpire), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Boolean> response = service.pexpire(key, milliseconds);

        // verify
        assertEquals(testResponse, response);
        assertEquals(true, response.get());
    }

    @SneakyThrows
    @Test
    public void pexpire_with_expireOptions_returns_success() {
        // setup
        String key = "testKey";
        long milliseconds = 50000L;
        String[] arguments = new String[] {key, Long.toString(milliseconds), "LT"};

        CompletableFuture<Boolean> testResponse = new CompletableFuture<>();
        testResponse.complete(Boolean.FALSE);

        // match on protobuf request
        when(commandManager.<Boolean>submitNewCommand(eq(PExpire), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Boolean> response =
                service.pexpire(key, milliseconds, ExpireOptions.NEW_EXPIRY_LESS_THAN_CURRENT);

        // verify
        assertEquals(testResponse, response);
        assertEquals(false, response.get());
    }

    @SneakyThrows
    @Test
    public void pexpireAt_returns_success() {
        // setup
        String key = "testKey";
        long unixMilliseconds = 999999L;
        String[] arguments = new String[] {key, Long.toString(unixMilliseconds)};

        CompletableFuture<Boolean> testResponse = new CompletableFuture<>();
        testResponse.complete(Boolean.TRUE);

        // match on protobuf request
        when(commandManager.<Boolean>submitNewCommand(eq(PExpireAt), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Boolean> response = service.pexpireAt(key, unixMilliseconds);

        // verify
        assertEquals(testResponse, response);
        assertEquals(true, response.get());
    }

    @SneakyThrows
    @Test
    public void pexpireAt_with_expireOptions_returns_success() {
        // setup
        String key = "testKey";
        long unixMilliseconds = 999999L;
        String[] arguments = new String[] {key, Long.toString(unixMilliseconds), "GT"};

        CompletableFuture<Boolean> testResponse = new CompletableFuture<>();
        testResponse.complete(Boolean.FALSE);

        // match on protobuf request
        when(commandManager.<Boolean>submitNewCommand(eq(PExpireAt), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Boolean> response =
                service.pexpireAt(key, unixMilliseconds, ExpireOptions.NEW_EXPIRY_GREATER_THAN_CURRENT);

        // verify
        assertEquals(testResponse, response);
        assertEquals(false, response.get());
    }

    @SneakyThrows
    @Test
    public void ttl_returns_success() {
        // setup
        String key = "testKey";
        long ttl = 999L;
        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(ttl);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(TTL), eq(new String[] {key}), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.ttl(key);

        // verify
        assertEquals(testResponse, response);
        assertEquals(ttl, response.get());
    }

    @SneakyThrows
    @Test
    public void expiretime_returns_success() {
        // setup
        String key = "testKey";
        long value = 999L;
        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(ExpireTime), eq(new String[] {key}), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.expiretime(key);

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, response.get());
    }

    @SneakyThrows
    @Test
    public void pexpiretime_returns_success() {
        // setup
        String key = "testKey";
        long value = 999L;
        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(PExpireTime), eq(new String[] {key}), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.pexpiretime(key);

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, response.get());
    }

    @SneakyThrows
    @Test
    public void invokeScript_returns_success() {
        // setup
        Script script = mock(Script.class);
        String hash = UUID.randomUUID().toString();
        when(script.getHash()).thenReturn(hash);
        String payload = "hello";

        CompletableFuture<Object> testResponse = new CompletableFuture<>();
        testResponse.complete(payload);

        // match on protobuf request
        when(commandManager.submitScript(eq(script), eq(List.of()), eq(List.of()), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Object> response = service.invokeScript(script);

        // verify
        assertEquals(testResponse, response);
        assertEquals(payload, response.get());
    }

    @SneakyThrows
    @Test
    public void invokeScript_with_ScriptOptions_returns_success() {
        // setup
        Script script = mock(Script.class);
        String hash = UUID.randomUUID().toString();
        when(script.getHash()).thenReturn(hash);
        String payload = "hello";

        ScriptOptions options =
                ScriptOptions.builder().key("key1").key("key2").arg("arg1").arg("arg2").build();

        CompletableFuture<Object> testResponse = new CompletableFuture<>();
        testResponse.complete(payload);

        // match on protobuf request
        when(commandManager.submitScript(
                        eq(script), eq(List.of("key1", "key2")), eq(List.of("arg1", "arg2")), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Object> response = service.invokeScript(script, options);

        // verify
        assertEquals(testResponse, response);
        assertEquals(payload, response.get());
    }

    @SneakyThrows
    @Test
    public void pttl_returns_success() {
        // setup
        String key = "testKey";
        long pttl = 999000L;

        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(pttl);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(PTTL), eq(new String[] {key}), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.pttl(key);

        // verify
        assertEquals(testResponse, response);
        assertEquals(pttl, response.get());
    }

    @SneakyThrows
    @Test
    public void persist_returns_success() {
        // setup
        String key = "testKey";
        Boolean isTimeoutRemoved = true;

        CompletableFuture<Boolean> testResponse = new CompletableFuture<>();
        testResponse.complete(isTimeoutRemoved);

        // match on protobuf request
        when(commandManager.<Boolean>submitNewCommand(eq(Persist), eq(new String[] {key}), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Boolean> response = service.persist(key);

        // verify
        assertEquals(testResponse, response);
        assertEquals(isTimeoutRemoved, response.get());
    }

    @SneakyThrows
    @Test
    public void info_returns_success() {
        // setup
        String testPayload = "Key: Value";
        CompletableFuture<String> testResponse = new CompletableFuture<>();
        testResponse.complete(testPayload);
        when(commandManager.<String>submitNewCommand(eq(Info), eq(new String[0]), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String> response = service.info();
        String payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(testPayload, payload);
    }

    @SneakyThrows
    @Test
    public void info_with_multiple_InfoOptions_returns_success() {
        // setup
        String[] arguments =
                new String[] {InfoOptions.Section.ALL.toString(), InfoOptions.Section.DEFAULT.toString()};
        String testPayload = "Key: Value";
        CompletableFuture<String> testResponse = new CompletableFuture<>();
        testResponse.complete(testPayload);
        when(commandManager.<String>submitNewCommand(eq(Info), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        InfoOptions options =
                InfoOptions.builder()
                        .section(InfoOptions.Section.ALL)
                        .section(InfoOptions.Section.DEFAULT)
                        .build();
        CompletableFuture<String> response = service.info(options);
        String payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(testPayload, payload);
    }

    @SneakyThrows
    @Test
    public void info_with_empty_InfoOptions_returns_success() {
        // setup
        String testPayload = "Key: Value";
        CompletableFuture<String> testResponse = new CompletableFuture<>();
        testResponse.complete(testPayload);
        when(commandManager.<String>submitNewCommand(eq(Info), eq(new String[0]), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String> response = service.info(InfoOptions.builder().build());
        String payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(testPayload, payload);
    }

    @SneakyThrows
    @Test
    public void mget_returns_success() {
        // setup
        String[] keys = {"key1", null, "key2"};
        String[] values = {"value1", null, "value2"};

        CompletableFuture<String[]> testResponse = new CompletableFuture<>();
        testResponse.complete(values);

        // match on protobuf request
        when(commandManager.<String[]>submitNewCommand(eq(MGet), eq(keys), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String[]> response = service.mget(keys);
        String[] payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(values, payload);
    }

    @SneakyThrows
    @Test
    public void mset_returns_success() {
        // setup
        Map<String, String> keyValueMap = new LinkedHashMap<>();
        keyValueMap.put("key1", "value1");
        keyValueMap.put("key2", "value2");
        String[] args = {"key1", "value1", "key2", "value2"};

        CompletableFuture<String> testResponse = new CompletableFuture<>();
        testResponse.complete(OK);

        // match on protobuf request
        when(commandManager.<String>submitNewCommand(eq(MSet), eq(args), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String> response = service.mset(keyValueMap);
        String payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(OK, payload);
    }

    @SneakyThrows
    @Test
    public void incr_returns_success() {
        // setup
        String key = "testKey";
        Long value = 10L;

        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(Incr), eq(new String[] {key}), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.incr(key);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void incrBy_returns_success() {
        // setup
        String key = "testKey";
        long amount = 1L;
        Long value = 10L;

        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(
                        eq(IncrBy), eq(new String[] {key, Long.toString(amount)}), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.incrBy(key, amount);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void incrByFloat_returns_success() {
        // setup
        String key = "testKey";
        double amount = 1.1;
        Double value = 10.1;

        CompletableFuture<Double> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Double>submitNewCommand(
                        eq(IncrByFloat), eq(new String[] {key, Double.toString(amount)}), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Double> response = service.incrByFloat(key, amount);
        Double payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void decr_returns_success() {
        // setup
        String key = "testKey";
        Long value = 10L;

        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(Decr), eq(new String[] {key}), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.decr(key);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void decrBy_returns_success() {
        // setup
        String key = "testKey";
        long amount = 1L;
        Long value = 10L;

        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(
                        eq(DecrBy), eq(new String[] {key, Long.toString(amount)}), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.decrBy(key, amount);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void strlen_returns_success() {
        // setup
        String key = "testKey";
        Long value = 10L;

        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(Strlen), eq(new String[] {key}), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.strlen(key);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void setrange_returns_success() {
        // setup
        String key = "testKey";
        int offset = 42;
        String str = "pewpew";
        String[] arguments = new String[] {key, Integer.toString(offset), str};
        Long value = 10L;

        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(SetRange), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.setrange(key, offset, str);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void getrange_returns_success() {
        // setup
        String key = "testKey";
        int start = 42;
        int end = 54;
        String[] arguments = new String[] {key, Integer.toString(start), Integer.toString(end)};
        String value = "pewpew";

        CompletableFuture<String> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<String>submitNewCommand(eq(GetRange), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String> response = service.getrange(key, start, end);
        String payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void hget_success() {
        // setup
        String key = "testKey";
        String field = "field";
        String[] args = new String[] {key, field};
        String value = "value";

        CompletableFuture<String> testResponse = new CompletableFuture<>();
        testResponse.complete(value);
        when(commandManager.<String>submitNewCommand(eq(HGet), eq(args), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String> response = service.hget(key, field);
        String payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void hset_success() {
        // setup
        String key = "testKey";
        Map<String, String> fieldValueMap = new LinkedHashMap<>();
        fieldValueMap.put("field1", "value1");
        fieldValueMap.put("field2", "value2");
        String[] args = new String[] {key, "field1", "value1", "field2", "value2"};
        Long value = 2L;

        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(value);
        when(commandManager.<Long>submitNewCommand(eq(HSet), eq(args), any())).thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.hset(key, fieldValueMap);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void hsetnx_success() {
        // setup
        String key = "testKey";
        String field = "testField";
        String value = "testValue";
        String[] args = new String[] {key, field, value};

        CompletableFuture<Boolean> testResponse = new CompletableFuture<>();
        testResponse.complete(Boolean.TRUE);

        // match on protobuf request
        when(commandManager.<Boolean>submitNewCommand(eq(HSetNX), eq(args), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Boolean> response = service.hsetnx(key, field, value);
        Boolean payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertTrue(payload);
    }

    @SneakyThrows
    @Test
    public void hdel_success() {
        // setup
        String key = "testKey";
        String[] fields = {"testField1", "testField2"};
        String[] args = {key, "testField1", "testField2"};
        Long value = 2L;

        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(HDel), eq(args), any())).thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.hdel(key, fields);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void hlen_success() {
        // setup
        String key = "testKey";
        String[] args = {key};
        Long value = 2L;

        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(HLen), eq(args), any())).thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.hlen(key);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void hvals_success() {
        // setup
        String key = "testKey";
        String[] args = {key};
        String[] values = new String[] {"value1", "value2"};

        CompletableFuture<String[]> testResponse = new CompletableFuture<>();
        testResponse.complete(values);

        // match on protobuf request
        when(commandManager.<String[]>submitNewCommand(eq(HVals), eq(args), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String[]> response = service.hvals(key);
        String[] payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(values, payload);
    }

    @SneakyThrows
    @Test
    public void hmget_success() {
        // setup
        String key = "testKey";
        String[] fields = {"testField1", "testField2"};
        String[] args = {"testKey", "testField1", "testField2"};
        String[] value = {"testValue1", "testValue2"};

        CompletableFuture<String[]> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<String[]>submitNewCommand(eq(HMGet), eq(args), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String[]> response = service.hmget(key, fields);
        String[] payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void hexists_success() {
        // setup
        String key = "testKey";
        String field = "testField";
        String[] args = new String[] {key, field};
        Boolean value = true;

        CompletableFuture<Boolean> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Boolean>submitNewCommand(eq(HExists), eq(args), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Boolean> response = service.hexists(key, field);
        Boolean payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void hgetall_success() {
        // setup
        String key = "testKey";
        String[] args = new String[] {key};
        Map<String, String> value = new LinkedHashMap<>();
        value.put("key1", "field1");
        value.put("key2", "field2");

        CompletableFuture<Map<String, String>> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Map<String, String>>submitNewCommand(eq(HGetAll), eq(args), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Map<String, String>> response = service.hgetall(key);
        Map<String, String> payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void hincrBy_returns_success() {
        // setup
        String key = "testKey";
        String field = "field";
        long amount = 1L;
        Long value = 10L;

        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(
                        eq(HIncrBy), eq(new String[] {key, field, Long.toString(amount)}), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.hincrBy(key, field, amount);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void hincrByFloat_returns_success() {
        // setup
        String key = "testKey";
        String field = "field";
        double amount = 1.0;
        Double value = 10.0;

        CompletableFuture<Double> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Double>submitNewCommand(
                        eq(HIncrByFloat), eq(new String[] {key, field, Double.toString(amount)}), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Double> response = service.hincrByFloat(key, field, amount);
        Double payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void hkeys_returns_success() {
        // setup
        String key = "testKey";
        String[] args = {key};
        String[] values = new String[] {"field_1", "field_2"};

        CompletableFuture<String[]> testResponse = new CompletableFuture<>();
        testResponse.complete(values);

        // match on protobuf request
        when(commandManager.<String[]>submitNewCommand(eq(HKeys), eq(args), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String[]> response = service.hkeys(key);
        String[] payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(values, payload);
    }

    @SneakyThrows
    @Test
    public void hstrlen_returns_success() {
        // setup
        String key = "testKey";
        String field = "field";
        String[] args = {key, field};
        Long value = 42L;

        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(HStrlen), eq(args), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.hstrlen(key, field);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void hrandfield_returns_success() {
        // setup
        String key = "testKey";
        String[] args = {key};
        String field = "field";

        CompletableFuture<String> testResponse = new CompletableFuture<>();
        testResponse.complete(field);

        // match on protobuf request
        when(commandManager.<String>submitNewCommand(eq(HRandField), eq(args), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String> response = service.hrandfield(key);
        String payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(field, payload);
    }

    @SneakyThrows
    @Test
    public void hrandfieldWithCount_returns_success() {
        // setup
        String key = "testKey";
        String[] args = {key, "2"};
        String[] fields = new String[] {"field_1", "field_2"};

        CompletableFuture<String[]> testResponse = new CompletableFuture<>();
        testResponse.complete(fields);

        // match on protobuf request
        when(commandManager.<String[]>submitNewCommand(eq(HRandField), eq(args), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String[]> response = service.hrandfieldWithCount(key, 2);
        String[] payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(fields, payload);
    }

    @SneakyThrows
    @Test
    public void hrandfieldWithCountWithValues_returns_success() {
        // setup
        String key = "testKey";
        String[] args = {key, "2", WITH_VALUES_REDIS_API};
        String[][] fields = new String[][] {{"field_1", "value_1"}, {"field_2", "value_2"}};

        CompletableFuture<String[][]> testResponse = new CompletableFuture<>();
        testResponse.complete(fields);

        // match on protobuf request
        when(commandManager.<String[][]>submitNewCommand(eq(HRandField), eq(args), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String[][]> response = service.hrandfieldWithCountWithValues(key, 2);
        String[][] payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(fields, payload);
    }

    @SneakyThrows
    @Test
    public void lpush_returns_success() {
        // setup
        String key = "testKey";
        String[] elements = new String[] {"value1", "value2"};
        String[] args = new String[] {key, "value1", "value2"};
        Long value = 2L;

        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(LPush), eq(args), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.lpush(key, elements);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void lpop_returns_success() {
        // setup
        String key = "testKey";
        String[] args = new String[] {key};
        String value = "value";

        CompletableFuture<String> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<String>submitNewCommand(eq(LPop), eq(args), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String> response = service.lpop(key);
        String payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void lpopCount_returns_success() {
        // setup
        String key = "testKey";
        long count = 2L;
        String[] args = new String[] {key, Long.toString(count)};
        String[] value = new String[] {"value1", "value2"};

        CompletableFuture<String[]> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<String[]>submitNewCommand(eq(LPop), eq(args), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String[]> response = service.lpopCount(key, count);
        String[] payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void lrange_returns_success() {
        // setup
        String key = "testKey";
        long start = 2L;
        long end = 4L;
        String[] args = new String[] {key, Long.toString(start), Long.toString(end)};
        String[] value = new String[] {"value1", "value2"};

        CompletableFuture<String[]> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<String[]>submitNewCommand(eq(LRange), eq(args), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String[]> response = service.lrange(key, start, end);
        String[] payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void lindex_returns_success() {
        // setup
        String key = "testKey";
        long index = 2;
        String[] args = new String[] {key, Long.toString(index)};
        String value = "value";

        CompletableFuture<String> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<String>submitNewCommand(eq(LIndex), eq(args), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String> response = service.lindex(key, index);
        String payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void ltrim_returns_success() {
        // setup
        String key = "testKey";
        long start = 2L;
        long end = 2L;
        String[] args = new String[] {key, Long.toString(end), Long.toString(start)};

        CompletableFuture<String> testResponse = new CompletableFuture<>();
        testResponse.complete(OK);

        // match on protobuf request
        when(commandManager.<String>submitNewCommand(eq(LTrim), eq(args), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String> response = service.ltrim(key, start, end);
        String payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(OK, payload);
    }

    @SneakyThrows
    @Test
    public void llen_returns_success() {
        // setup
        String key = "testKey";
        String[] args = new String[] {key};
        long value = 2L;

        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(LLen), eq(args), any())).thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.llen(key);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void lrem_returns_success() {
        // setup
        String key = "testKey";
        long count = 2L;
        String element = "value";
        String[] args = new String[] {key, Long.toString(count), element};
        long value = 2L;

        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(LRem), eq(args), any())).thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.lrem(key, count, element);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void rpush_returns_success() {
        // setup
        String key = "testKey";
        String[] elements = new String[] {"value1", "value2"};
        String[] args = new String[] {key, "value1", "value2"};
        Long value = 2L;

        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(RPush), eq(args), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.rpush(key, elements);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void rpop_returns_success() {
        // setup
        String key = "testKey";
        String value = "value";
        String[] args = new String[] {key};

        CompletableFuture<String> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<String>submitNewCommand(eq(RPop), eq(args), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String> response = service.rpop(key);
        String payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void rpopCount_returns_success() {
        // setup
        String key = "testKey";
        long count = 2L;
        String[] args = new String[] {key, Long.toString(count)};
        String[] value = new String[] {"value1", "value2"};

        CompletableFuture<String[]> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<String[]>submitNewCommand(eq(RPop), eq(args), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String[]> response = service.rpopCount(key, count);
        String[] payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void sadd_returns_success() {
        // setup
        String key = "testKey";
        String[] members = new String[] {"testMember1", "testMember2"};
        String[] arguments = ArrayUtils.addFirst(members, key);
        Long value = 2L;

        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(SAdd), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.sadd(key, members);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void sismember_returns_success() {
        // setup
        String key = "testKey";
        String member = "testMember";
        String[] arguments = new String[] {key, member};

        CompletableFuture<Boolean> testResponse = new CompletableFuture<>();
        testResponse.complete(true);

        // match on protobuf request
        when(commandManager.<Boolean>submitNewCommand(eq(SIsMember), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Boolean> response = service.sismember(key, member);
        Boolean payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertTrue(payload);
    }

    @SneakyThrows
    @Test
    public void srem_returns_success() {
        // setup
        String key = "testKey";
        String[] members = new String[] {"testMember1", "testMember2"};
        String[] arguments = ArrayUtils.addFirst(members, key);
        Long value = 2L;

        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(SRem), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.srem(key, members);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void smembers_returns_success() {
        // setup
        String key = "testKey";
        Set<String> value = Set.of("testMember");

        CompletableFuture<Set<String>> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Set<String>>submitNewCommand(eq(SMembers), eq(new String[] {key}), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Set<String>> response = service.smembers(key);
        Set<String> payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void scard_returns_success() {
        // setup
        String key = "testKey";
        Long value = 2L;

        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(SCard), eq(new String[] {key}), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.scard(key);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void sdiff_returns_success() {
        // setup
        String[] keys = new String[] {"key1", "key2"};
        Set<String> value = Set.of("1", "2");

        CompletableFuture<Set<String>> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Set<String>>submitNewCommand(eq(SDiff), eq(keys), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Set<String>> response = service.sdiff(keys);
        Set<String> payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void smismember_returns_success() {
        // setup
        String key = "testKey";
        String[] members = {"1", "2"};
        String[] arguments = {"testKey", "1", "2"};
        Boolean[] value = {true, false};

        CompletableFuture<Boolean[]> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Boolean[]>submitNewCommand(eq(SMIsMember), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Boolean[]> response = service.smismember(key, members);
        Boolean[] payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void sdiffstore_returns_success() {
        // setup
        String destination = "dest";
        String[] keys = new String[] {"set1", "set2"};
        String[] arguments = {"dest", "set1", "set2"};

        Long value = 2L;

        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(SDiffStore), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.sdiffstore(destination, keys);

        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void smove_returns_success() {
        // setup
        String source = "src";
        String destination = "dst";
        String member = "elem";
        String[] arguments = {source, destination, member};

        CompletableFuture<Boolean> testResponse = new CompletableFuture<>();
        testResponse.complete(true);

        // match on protobuf request
        when(commandManager.<Boolean>submitNewCommand(eq(SMove), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Boolean> response = service.smove(source, destination, member);

        // verify
        assertEquals(testResponse, response);
        assertTrue(response.get());
    }

    @SneakyThrows
    @Test
    public void sinter_returns_success() {
        // setup
        String[] keys = new String[] {"key1", "key2"};
        Set<String> value = Set.of("1", "2");

        CompletableFuture<Set<String>> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Set<String>>submitNewCommand(eq(SInter), eq(keys), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Set<String>> response = service.sinter(keys);
        Set<String> payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void sinterstore_returns_success() {
        // setup
        String destination = "key";
        String[] keys = new String[] {"set1", "set2"};
        String[] args = new String[] {"key", "set1", "set2"};
        Long value = 2L;

        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(SInterStore), eq(args), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.sinterstore(destination, keys);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void sunionstore_returns_success() {
        // setup
        String destination = "key";
        String[] keys = new String[] {"set1", "set2"};
        String[] args = new String[] {"key", "set1", "set2"};
        Long value = 2L;

        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(SUnionStore), eq(args), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.sunionstore(destination, keys);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void zadd_noOptions_returns_success() {
        // setup
        String key = "testKey";
        Map<String, Double> membersScores = new LinkedHashMap<>();
        membersScores.put("testMember1", 1.0);
        membersScores.put("testMember2", 2.0);
        String[] membersScoresArgs = convertMapToValueKeyStringArray(membersScores);
        String[] arguments = ArrayUtils.addFirst(membersScoresArgs, key);
        Long value = 2L;

        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(ZAdd), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response =
                service.zadd(key, membersScores, ZAddOptions.builder().build(), false);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void zadd_withOptions_returns_success() {
        // setup
        String key = "testKey";
        ZAddOptions options =
                ZAddOptions.builder()
                        .conditionalChange(ZAddOptions.ConditionalChange.ONLY_IF_EXISTS)
                        .updateOptions(ZAddOptions.UpdateOptions.SCORE_GREATER_THAN_CURRENT)
                        .build();
        Map<String, Double> membersScores = new LinkedHashMap<>();
        membersScores.put("testMember1", 1.0);
        membersScores.put("testMember2", 2.0);
        String[] membersScoresArgs = convertMapToValueKeyStringArray(membersScores);
        String[] arguments = ArrayUtils.addAll(new String[] {key}, options.toArgs());
        arguments = ArrayUtils.addAll(arguments, membersScoresArgs);
        Long value = 2L;

        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(ZAdd), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.zadd(key, membersScores, options, false);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void zadd_withIllegalArgument_throws_exception() {
        // setup
        String key = "testKey";
        ZAddOptions options =
                ZAddOptions.builder()
                        .conditionalChange(ZAddOptions.ConditionalChange.ONLY_IF_DOES_NOT_EXIST)
                        .updateOptions(ZAddOptions.UpdateOptions.SCORE_GREATER_THAN_CURRENT)
                        .build();
        Map<String, Double> membersScores = new LinkedHashMap<>();
        membersScores.put("testMember1", 1.0);
        membersScores.put("testMember2", 2.0);

        assertThrows(
                IllegalArgumentException.class, () -> service.zadd(key, membersScores, options, false));
    }

    @SneakyThrows
    @Test
    public void zaddIncr_noOptions_returns_success() {
        // setup
        String key = "testKey";
        String member = "member";
        double increment = 3.0;
        String[] arguments = new String[] {key, "INCR", Double.toString(increment), member};
        Double value = 3.0;

        CompletableFuture<Double> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Double>submitNewCommand(eq(ZAdd), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Double> response =
                service.zaddIncr(key, member, increment, ZAddOptions.builder().build());
        Double payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void zaddIncr_withOptions_returns_success() {
        // setup
        String key = "testKey";
        ZAddOptions options =
                ZAddOptions.builder()
                        .updateOptions(ZAddOptions.UpdateOptions.SCORE_GREATER_THAN_CURRENT)
                        .build();
        String member = "member";
        double increment = 3.0;
        String[] arguments =
                concatenateArrays(
                        new String[] {key},
                        options.toArgs(),
                        new String[] {"INCR", Double.toString(increment), member});
        Double value = 3.0;

        CompletableFuture<Double> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Double>submitNewCommand(eq(ZAdd), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Double> response = service.zaddIncr(key, member, increment, options);
        Double payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void zmpop_returns_success() {
        // setup
        String[] keys = new String[] {"key1", "key2"};
        ScoreFilter modifier = MAX;
        String[] arguments = {"2", "key1", "key2", "MAX"};
        Object[] value = new Object[] {"key1", "elem"};

        CompletableFuture<Object[]> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Object[]>submitNewCommand(eq(ZMPop), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Object[]> response = service.zmpop(keys, modifier);
        Object[] payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertArrayEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void zmpop_with_count_returns_success() {
        // setup
        String[] keys = new String[] {"key1", "key2"};
        ScoreFilter modifier = MAX;
        long count = 42;
        String[] arguments = {"2", "key1", "key2", "MAX", "COUNT", "42"};
        Object[] value = new Object[] {"key1", "elem"};

        CompletableFuture<Object[]> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Object[]>submitNewCommand(eq(ZMPop), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Object[]> response = service.zmpop(keys, modifier, count);
        Object[] payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertArrayEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void bzmpop_returns_success() {
        // setup
        double timeout = .5;
        String[] keys = new String[] {"key1", "key2"};
        ScoreFilter modifier = MAX;
        String[] arguments = {"0.5", "2", "key1", "key2", "MAX"};
        Object[] value = new Object[] {"key1", "elem"};

        CompletableFuture<Object[]> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Object[]>submitNewCommand(eq(BZMPop), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Object[]> response = service.bzmpop(keys, modifier, timeout);
        Object[] payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertArrayEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void bzmpop_with_count_returns_success() {
        // setup
        double timeout = .5;
        String[] keys = new String[] {"key1", "key2"};
        ScoreFilter modifier = MAX;
        long count = 42;
        String[] arguments = {"0.5", "2", "key1", "key2", "MAX", "COUNT", "42"};
        Object[] value = new Object[] {"key1", "elem"};

        CompletableFuture<Object[]> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Object[]>submitNewCommand(eq(BZMPop), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Object[]> response = service.bzmpop(keys, modifier, timeout, count);
        Object[] payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertArrayEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void clientId_returns_success() {
        // setup
        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(42L);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(ClientId), eq(new String[0]), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.clientId();

        // verify
        assertEquals(testResponse, response);
        assertEquals(42L, response.get());
    }

    @SneakyThrows
    @Test
    public void clientGetName_returns_success() {
        // setup
        CompletableFuture<String> testResponse = new CompletableFuture<>();
        testResponse.complete("TEST");

        // match on protobuf request
        when(commandManager.<String>submitNewCommand(eq(ClientGetName), eq(new String[0]), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String> response = service.clientGetName();

        // verify
        assertEquals(testResponse, response);
        assertEquals("TEST", response.get());
    }

    @SneakyThrows
    @Test
    public void configRewrite_returns_success() {
        // setup
        CompletableFuture<String> testResponse = new CompletableFuture<>();
        testResponse.complete(OK);

        // match on protobuf request
        when(commandManager.<String>submitNewCommand(eq(ConfigRewrite), eq(new String[0]), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String> response = service.configRewrite();
        String payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(OK, payload);
    }

    @SneakyThrows
    @Test
    public void configResetStat_returns_success() {
        // setup
        CompletableFuture<String> testResponse = new CompletableFuture<>();
        testResponse.complete(OK);

        // match on protobuf request
        when(commandManager.<String>submitNewCommand(eq(ConfigResetStat), eq(new String[0]), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String> response = service.configResetStat();
        String payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(OK, payload);
    }

    @SneakyThrows
    @Test
    public void configGet_returns_success() {
        // setup
        Map<String, String> testPayload = Map.of("timeout", "1000");
        CompletableFuture<Map<String, String>> testResponse = new CompletableFuture<>();
        testResponse.complete(testPayload);

        // match on protobuf request
        when(commandManager.<Map<String, String>>submitNewCommand(
                        eq(ConfigGet), eq(new String[] {"timeout"}), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Map<String, String>> response = service.configGet(new String[] {"timeout"});
        Map<String, String> payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(testPayload, payload);
    }

    @SneakyThrows
    @Test
    public void configSet_returns_success() {
        // setup
        CompletableFuture<String> testResponse = new CompletableFuture<>();
        testResponse.complete(OK);

        // match on protobuf request
        when(commandManager.<String>submitNewCommand(
                        eq(ConfigSet), eq(new String[] {"timeout", "1000"}), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String> response = service.configSet(Map.of("timeout", "1000"));

        // verify
        assertEquals(testResponse, response);
        assertEquals(OK, response.get());
    }

    @SneakyThrows
    @Test
    public void zrem_returns_success() {
        // setup
        String key = "testKey";
        String[] members = new String[] {"member1", "member2"};
        String[] arguments = ArrayUtils.addFirst(members, key);
        Long value = 2L;

        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(ZRem), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.zrem(key, members);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void zcard_returns_success() {
        // setup
        String key = "testKey";
        String[] arguments = new String[] {key};
        Long value = 3L;

        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(ZCard), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.zcard(key);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void zpopmin_returns_success() {
        // setup
        String key = "testKey";
        String[] arguments = new String[] {key};
        Map<String, Double> value = Map.of("member1", 2.5);

        CompletableFuture<Map<String, Double>> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Map<String, Double>>submitNewCommand(eq(ZPopMin), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Map<String, Double>> response = service.zpopmin(key);
        Map<String, Double> payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void zpopmin_with_count_returns_success() {
        // setup
        String key = "testKey";
        long count = 2L;
        String[] arguments = new String[] {key, Long.toString(count)};
        Map<String, Double> value = Map.of("member1", 2.0, "member2", 3.0);

        CompletableFuture<Map<String, Double>> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Map<String, Double>>submitNewCommand(eq(ZPopMin), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Map<String, Double>> response = service.zpopmin(key, count);
        Map<String, Double> payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void bzpopmin_returns_success() {
        // setup
        String[] keys = new String[] {"key1", "key2"};
        double timeout = .5;
        String[] arguments = new String[] {"key1", "key2", "0.5"};
        Object[] value = new Object[] {"key1", "elem", 42.};

        CompletableFuture<Object[]> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Object[]>submitNewCommand(eq(BZPopMin), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Object[]> response = service.bzpopmin(keys, timeout);
        Object[] payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void zpopmax_returns_success() {
        // setup
        String key = "testKey";
        String[] arguments = new String[] {key};
        Map<String, Double> value = Map.of("member1", 2.5);

        CompletableFuture<Map<String, Double>> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Map<String, Double>>submitNewCommand(eq(ZPopMax), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Map<String, Double>> response = service.zpopmax(key);
        Map<String, Double> payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void bzpopmax_returns_success() {
        // setup
        String[] keys = new String[] {"key1", "key2"};
        double timeout = .5;
        String[] arguments = new String[] {"key1", "key2", "0.5"};
        Object[] value = new Object[] {"key1", "elem", 42.};

        CompletableFuture<Object[]> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Object[]>submitNewCommand(eq(BZPopMax), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Object[]> response = service.bzpopmax(keys, timeout);
        Object[] payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void zpopmax_with_count_returns_success() {
        // setup
        String key = "testKey";
        long count = 2L;
        String[] arguments = new String[] {key, Long.toString(count)};
        Map<String, Double> value = Map.of("member1", 3.0, "member2", 1.0);

        CompletableFuture<Map<String, Double>> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Map<String, Double>>submitNewCommand(eq(ZPopMax), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Map<String, Double>> response = service.zpopmax(key, count);
        Map<String, Double> payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void zscore_returns_success() {
        // setup
        String key = "testKey";
        String member = "testMember";
        String[] arguments = new String[] {key, member};
        Double value = 3.5;

        CompletableFuture<Double> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Double>submitNewCommand(eq(ZScore), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Double> response = service.zscore(key, member);
        Double payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void zrange_by_index_returns_success() {
        // setup
        String key = "testKey";
        RangeByIndex rangeByIndex = new RangeByIndex(0, 1);
        String[] arguments = new String[] {key, rangeByIndex.getStart(), rangeByIndex.getEnd()};
        String[] value = new String[] {"one", "two"};

        CompletableFuture<String[]> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<String[]>submitNewCommand(eq(ZRange), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String[]> response = service.zrange(key, rangeByIndex);
        String[] payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void zrange_by_score_with_reverse_returns_success() {
        // setup
        String key = "testKey";
        RangeByScore rangeByScore =
                new RangeByScore(new ScoreBoundary(3, false), InfScoreBound.NEGATIVE_INFINITY);
        String[] arguments =
                new String[] {key, rangeByScore.getStart(), rangeByScore.getEnd(), "BYSCORE", "REV"};
        String[] value = new String[] {"two", "one"};

        CompletableFuture<String[]> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<String[]>submitNewCommand(eq(ZRange), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String[]> response = service.zrange(key, rangeByScore, true);
        String[] payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void zrange_by_lex_returns_success() {
        // setup
        String key = "testKey";
        RangeByLex rangeByLex =
                new RangeByLex(InfLexBound.NEGATIVE_INFINITY, new LexBoundary("c", false));
        String[] arguments = new String[] {key, rangeByLex.getStart(), rangeByLex.getEnd(), "BYLEX"};
        String[] value = new String[] {"a", "b"};

        CompletableFuture<String[]> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<String[]>submitNewCommand(eq(ZRange), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String[]> response = service.zrange(key, rangeByLex);
        String[] payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void zrangeWithScores_by_index_returns_success() {
        // setup
        String key = "testKey";
        RangeByIndex rangeByIndex = new RangeByIndex(0, 4);
        String[] arguments =
                new String[] {key, rangeByIndex.getStart(), rangeByIndex.getEnd(), WITH_SCORES_REDIS_API};
        Map<String, Double> value = Map.of("one", 1.0, "two", 2.0, "three", 3.0);

        CompletableFuture<Map<String, Double>> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Map<String, Double>>submitNewCommand(eq(ZRange), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Map<String, Double>> response = service.zrangeWithScores(key, rangeByIndex);
        Map<String, Double> payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void zrangeWithScores_by_score_returns_success() {
        // setup
        String key = "testKey";
        RangeByScore rangeByScore =
                new RangeByScore(
                        InfScoreBound.NEGATIVE_INFINITY,
                        InfScoreBound.POSITIVE_INFINITY,
                        new RangeOptions.Limit(1, 2));
        String[] arguments =
                new String[] {
                    key,
                    rangeByScore.getStart(),
                    rangeByScore.getEnd(),
                    "BYSCORE",
                    "LIMIT",
                    "1",
                    "2",
                    WITH_SCORES_REDIS_API
                };
        Map<String, Double> value = Map.of("two", 2.0, "three", 3.0);

        CompletableFuture<Map<String, Double>> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Map<String, Double>>submitNewCommand(eq(ZRange), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Map<String, Double>> response =
                service.zrangeWithScores(key, rangeByScore, false);
        Map<String, Double> payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void zrank_returns_success() {
        // setup
        String key = "testKey";
        String member = "testMember";
        String[] arguments = new String[] {key, member};
        Long value = 3L;

        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(ZRank), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.zrank(key, member);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void zrankWithScore_returns_success() {
        // setup
        String key = "testKey";
        String member = "testMember";
        String[] arguments = new String[] {key, member, WITH_SCORE_REDIS_API};
        Object[] value = new Object[] {1, 6.0};

        CompletableFuture<Object[]> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Object[]>submitNewCommand(eq(ZRank), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Object[]> response = service.zrankWithScore(key, member);
        Object[] payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void zrevrank_returns_success() {
        // setup
        String key = "testKey";
        String member = "testMember";
        String[] arguments = new String[] {key, member};
        Long value = 3L;

        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(ZRevRank), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.zrevrank(key, member);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void zrevrankWithScore_returns_success() {
        // setup
        String key = "testKey";
        String member = "testMember";
        String[] arguments = new String[] {key, member, WITH_SCORE_REDIS_API};
        Object[] value = new Object[] {1, 6.0};

        CompletableFuture<Object[]> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Object[]>submitNewCommand(eq(ZRevRank), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Object[]> response = service.zrevrankWithScore(key, member);
        Object[] payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void zmscore_returns_success() {
        // setup
        String key = "testKey";
        String[] members = new String[] {"member1", "member2"};
        String[] arguments = new String[] {key, "member1", "member2"};
        Double[] value = new Double[] {2.5, 8.2};

        CompletableFuture<Double[]> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Double[]>submitNewCommand(eq(ZMScore), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Double[]> response = service.zmscore(key, members);
        Double[] payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void zdiffstore_returns_success() {
        // setup
        String destKey = "testDestKey";
        String[] keys = new String[] {"testKey1", "testKey2"};
        String[] arguments = new String[] {destKey, Long.toString(keys.length), "testKey1", "testKey2"};
        Long value = 3L;

        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(ZDiffStore), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.zdiffstore(destKey, keys);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void zdiff_returns_success() {
        // setup
        String key1 = "testKey1";
        String key2 = "testKey2";
        String[] arguments = new String[] {"2", key1, key2};
        String[] value = new String[] {"element1"};

        CompletableFuture<String[]> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<String[]>submitNewCommand(eq(ZDiff), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String[]> response = service.zdiff(new String[] {key1, key2});
        String[] payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void zdiffWithScores_returns_success() {
        // setup
        String key1 = "testKey1";
        String key2 = "testKey2";
        String[] arguments = new String[] {"2", key1, key2, WITH_SCORES_REDIS_API};
        Map<String, Double> value = Map.of("element1", 2.0);

        CompletableFuture<Map<String, Double>> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Map<String, Double>>submitNewCommand(eq(ZDiff), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Map<String, Double>> response =
                service.zdiffWithScores(new String[] {key1, key2});
        Map<String, Double> payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void zcount_returns_success() {
        // setup
        String key = "testKey";
        String[] arguments = new String[] {key, "-inf", "10.0"};
        Long value = 3L;

        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(ZCount), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response =
                service.zcount(key, InfScoreBound.NEGATIVE_INFINITY, new ScoreBoundary(10, true));
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void zremrangebyrank_returns_success() {
        // setup
        String key = "testKey";
        long start = 0;
        long end = -1;
        String[] arguments = new String[] {key, Long.toString(start), Long.toString(end)};
        Long value = 5L;

        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(ZRemRangeByRank), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.zremrangebyrank(key, start, end);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void zremrangebylex_returns_success() {
        // setup
        String key = "testKey";
        String[] arguments = new String[] {key, "-", "[z"};
        Long value = 3L;

        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(ZRemRangeByLex), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response =
                service.zremrangebylex(key, InfLexBound.NEGATIVE_INFINITY, new LexBoundary("z", true));
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void zremrangebyscore_returns_success() {
        // setup
        String key = "testKey";
        String[] arguments = new String[] {key, "-inf", "10.0"};
        Long value = 3L;

        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(ZRemRangeByScore), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response =
                service.zremrangebyscore(key, InfScoreBound.NEGATIVE_INFINITY, new ScoreBoundary(10, true));
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void zlexcount_returns_success() {
        // setup
        String key = "testKey";
        String[] arguments = new String[] {key, "-", "[c"};
        Long value = 3L;

        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(ZLexCount), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response =
                service.zlexcount(key, InfLexBound.NEGATIVE_INFINITY, new LexBoundary("c", true));
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void zrangestore_by_lex_returns_success() {
        // setup
        String source = "testSourceKey";
        String destination = "testDestinationKey";
        RangeByLex rangeByLex =
                new RangeByLex(InfLexBound.NEGATIVE_INFINITY, new LexBoundary("c", false));
        String[] arguments =
                new String[] {source, destination, rangeByLex.getStart(), rangeByLex.getEnd(), "BYLEX"};
        Long value = 2L;

        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(ZRangeStore), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.zrangestore(source, destination, rangeByLex);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void zrangestore_by_index_returns_success() {
        // setup
        String source = "testSourceKey";
        String destination = "testDestinationKey";
        RangeByIndex rangeByIndex = new RangeByIndex(0, 1);
        String[] arguments =
                new String[] {source, destination, rangeByIndex.getStart(), rangeByIndex.getEnd()};
        Long value = 2L;

        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(ZRangeStore), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.zrangestore(source, destination, rangeByIndex);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void zrangestore_by_score_with_reverse_returns_success() {
        // setup
        String source = "testSourceKey";
        String destination = "testDestinationKey";
        RangeByScore rangeByScore =
                new RangeByScore(new ScoreBoundary(3, false), InfScoreBound.NEGATIVE_INFINITY);
        boolean reversed = true;
        String[] arguments =
                new String[] {
                    source, destination, rangeByScore.getStart(), rangeByScore.getEnd(), "BYSCORE", "REV"
                };
        Long value = 2L;

        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(ZRangeStore), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response =
                service.zrangestore(source, destination, rangeByScore, reversed);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void zunion_returns_success() {
        // setup
        String[] keys = new String[] {"key1", "key2"};
        KeyArray keyArray = new KeyArray(keys);
        String[] arguments = keyArray.toArgs();
        String[] value = new String[] {"elem1", "elem2"};

        CompletableFuture<String[]> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<String[]>submitNewCommand(eq(ZUnion), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String[]> response = service.zunion(keyArray);
        String[] payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void zunionstore_returns_success() {
        // setup
        String destination = "destinationKey";
        String[] keys = new String[] {"key1", "key2"};
        KeyArray keyArray = new KeyArray(keys);
        String[] arguments = concatenateArrays(new String[] {destination}, keyArray.toArgs());
        Long value = 5L;

        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(ZUnionStore), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.zunionstore(destination, keyArray);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void zunionstore_with_options_returns_success() {
        // setup
        String destination = "destinationKey";
        String[] keys = new String[] {"key1", "key2"};
        List<Pair<String, Double>> keysWeights = new ArrayList<>();
        keysWeights.add(Pair.of("key1", 10.0));
        keysWeights.add(Pair.of("key2", 20.0));
        WeightedKeys weightedKeys = new WeightedKeys(keysWeights);
        Aggregate aggregate = Aggregate.MIN;
        String[] arguments =
                concatenateArrays(new String[] {destination}, weightedKeys.toArgs(), aggregate.toArgs());
        Long value = 5L;

        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(ZUnionStore), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.zunionstore(destination, weightedKeys, aggregate);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void zunion_with_options_returns_success() {
        // setup
        List<Pair<String, Double>> keysWeights = new ArrayList<>();
        keysWeights.add(Pair.of("key1", 10.0));
        keysWeights.add(Pair.of("key2", 20.0));
        WeightedKeys weightedKeys = new WeightedKeys(keysWeights);
        Aggregate aggregate = Aggregate.MIN;
        String[] arguments = concatenateArrays(weightedKeys.toArgs(), aggregate.toArgs());
        String[] value = new String[] {"elem1", "elem2"};

        CompletableFuture<String[]> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<String[]>submitNewCommand(eq(ZUnion), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String[]> response = service.zunion(weightedKeys, aggregate);
        String[] payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void zunionWithScores_returns_success() {
        // setup
        String[] keys = new String[] {"key1", "key2"};
        KeyArray keyArray = new KeyArray(keys);
        String[] arguments = concatenateArrays(keyArray.toArgs(), new String[] {WITH_SCORES_REDIS_API});
        Map<String, Double> value = Map.of("elem1", 1.0, "elem2", 2.0);

        CompletableFuture<Map<String, Double>> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Map<String, Double>>submitNewCommand(eq(ZUnion), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Map<String, Double>> response = service.zunionWithScores(keyArray);
        Map<String, Double> payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void zunionWithScores_with_options_returns_success() {
        // setup
        List<Pair<String, Double>> keysWeights = new ArrayList<>();
        keysWeights.add(Pair.of("key1", 10.0));
        keysWeights.add(Pair.of("key2", 20.0));
        WeightedKeys weightedKeys = new WeightedKeys(keysWeights);
        Aggregate aggregate = Aggregate.MIN;
        String[] arguments =
                concatenateArrays(
                        weightedKeys.toArgs(), aggregate.toArgs(), new String[] {WITH_SCORES_REDIS_API});
        Map<String, Double> value = Map.of("elem1", 1.0, "elem2", 2.0);

        CompletableFuture<Map<String, Double>> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Map<String, Double>>submitNewCommand(eq(ZUnion), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Map<String, Double>> response =
                service.zunionWithScores(weightedKeys, aggregate);
        Map<String, Double> payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void zinter_returns_success() {
        // setup
        String[] keys = new String[] {"key1", "key2"};
        KeyArray keyArray = new KeyArray(keys);
        String[] arguments = keyArray.toArgs();
        String[] value = new String[] {"elem1", "elem2"};

        CompletableFuture<String[]> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<String[]>submitNewCommand(eq(ZInter), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String[]> response = service.zinter(keyArray);
        String[] payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void zinter_with_aggregation_returns_success() {
        // setup
        List<Pair<String, Double>> keysWeights = new ArrayList<>();
        keysWeights.add(Pair.of("key1", 10.0));
        keysWeights.add(Pair.of("key2", 20.0));
        WeightedKeys weightedKeys = new WeightedKeys(keysWeights);
        Aggregate aggregate = Aggregate.MIN;
        String[] arguments = concatenateArrays(weightedKeys.toArgs(), aggregate.toArgs());
        String[] value = new String[] {"elem1", "elem2"};

        CompletableFuture<String[]> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<String[]>submitNewCommand(eq(ZInter), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String[]> response = service.zinter(weightedKeys, aggregate);
        String[] payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void zinterWithScores_returns_success() {
        // setup
        String[] keys = new String[] {"key1", "key2"};
        KeyArray keyArray = new KeyArray(keys);
        String[] arguments = concatenateArrays(keyArray.toArgs(), new String[] {WITH_SCORES_REDIS_API});
        Map<String, Double> value = Map.of("elem1", 1.0, "elem2", 2.0);

        CompletableFuture<Map<String, Double>> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Map<String, Double>>submitNewCommand(eq(ZInter), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Map<String, Double>> response = service.zinterWithScores(keyArray);
        Map<String, Double> payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void zinterWithScores_with_aggregation_returns_success() {
        // setup
        List<Pair<String, Double>> keysWeights = new ArrayList<>();
        keysWeights.add(Pair.of("key1", 10.0));
        keysWeights.add(Pair.of("key2", 20.0));
        WeightedKeys weightedKeys = new WeightedKeys(keysWeights);
        Aggregate aggregate = Aggregate.MIN;
        String[] arguments =
                concatenateArrays(
                        weightedKeys.toArgs(), aggregate.toArgs(), new String[] {WITH_SCORES_REDIS_API});
        Map<String, Double> value = Map.of("elem1", 1.0, "elem2", 2.0);

        CompletableFuture<Map<String, Double>> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Map<String, Double>>submitNewCommand(eq(ZInter), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Map<String, Double>> response =
                service.zinterWithScores(weightedKeys, aggregate);
        Map<String, Double> payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void zinterstore_returns_success() {
        // setup
        String destination = "destinationKey";
        String[] keys = new String[] {"key1", "key2"};
        KeyArray keyArray = new KeyArray(keys);
        String[] arguments = concatenateArrays(new String[] {destination}, keyArray.toArgs());
        Long value = 3L;

        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(ZInterStore), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.zinterstore(destination, keyArray);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void zinterstore_with_options_returns_success() {
        // setup
        String destination = "destinationKey";
        List<Pair<String, Double>> keysWeights = new ArrayList<>();
        keysWeights.add(Pair.of("key1", 10.0));
        keysWeights.add(Pair.of("key2", 20.0));
        WeightedKeys weightedKeys = new WeightedKeys(keysWeights);
        Aggregate aggregate = Aggregate.MIN;
        String[] arguments =
                concatenateArrays(new String[] {destination}, weightedKeys.toArgs(), aggregate.toArgs());
        Long value = 3L;

        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(ZInterStore), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.zinterstore(destination, weightedKeys, aggregate);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void zintercard_with_limit_returns_success() {
        // setup
        String[] keys = new String[] {"key1", "key2"};
        long limit = 3L;
        String[] arguments = new String[] {"2", "key1", "key2", LIMIT_REDIS_API, "3"};
        Long value = 3L;

        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(ZInterCard), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.zintercard(keys, limit);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void zintercard_returns_success() {
        // setup
        String[] keys = new String[] {"key1", "key2"};
        String[] arguments = new String[] {"2", "key1", "key2"};
        Long value = 3L;

        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(ZInterCard), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.zintercard(keys);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void xadd_returns_success() {
        // setup
        String key = "testKey";
        Map<String, String> fieldValues = new LinkedHashMap<>();
        fieldValues.put("testField1", "testValue1");
        fieldValues.put("testField2", "testValue2");
        String[] fieldValuesArgs = convertMapToKeyValueStringArray(fieldValues);
        String[] arguments = new String[] {key, "*"};
        arguments = ArrayUtils.addAll(arguments, fieldValuesArgs);
        String returnId = "testId";

        CompletableFuture<String> testResponse = new CompletableFuture<>();
        testResponse.complete(returnId);

        // match on protobuf request
        when(commandManager.<String>submitNewCommand(eq(XAdd), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String> response = service.xadd(key, fieldValues);

        // verify
        assertEquals(testResponse, response);
        assertEquals(returnId, response.get());
    }

    @SneakyThrows
    @Test
    public void zrandmember_returns_success() {
        // setup
        String key = "testKey";
        String[] arguments = new String[] {key};
        String value = "testValue";

        CompletableFuture<String> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<String>submitNewCommand(eq(ZRandMember), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String> response = service.zrandmember(key);
        String payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void zrandmemberWithCount_returns_success() {
        // setup
        String key = "testKey";
        long count = 2L;
        String[] arguments = new String[] {key, Long.toString(count)};
        String[] value = new String[] {"member1", "member2"};

        CompletableFuture<String[]> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<String[]>submitNewCommand(eq(ZRandMember), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String[]> response = service.zrandmemberWithCount(key, count);
        String[] payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void zrandmemberWithCountWithScores_returns_success() {
        // setup
        String key = "testKey";
        long count = 2L;
        String[] arguments = new String[] {key, Long.toString(count), WITH_SCORES_REDIS_API};
        Object[][] value = new Object[][] {{"member1", 2.0}, {"member2", 3.0}};

        CompletableFuture<Object[][]> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Object[][]>submitNewCommand(eq(ZRandMember), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Object[][]> response = service.zrandmemberWithCountWithScores(key, count);
        Object[] payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void zincrby_returns_success() {
        // setup
        String key = "testKey";
        double increment = 4.2;
        String member = "member";
        String[] arguments = new String[] {key, "4.2", member};
        Double value = 3.14;

        CompletableFuture<Double> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Double>submitNewCommand(eq(ZIncrBy), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Double> response = service.zincrby(key, increment, member);
        Double payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    private static List<Arguments> getStreamAddOptions() {
        return List.of(
                Arguments.of(
                        // no TRIM option
                        "test_xadd_no_trim",
                        StreamAddOptions.builder().id("id").makeStream(Boolean.FALSE).build(),
                        new String[] {NO_MAKE_STREAM_REDIS_API, "id"},
                        Arguments.of(
                                // MAXLEN with LIMIT
                                "test_xadd_maxlen_with_limit",
                                StreamAddOptions.builder()
                                        .id("id")
                                        .makeStream(Boolean.TRUE)
                                        .trim(new MaxLen(5L, 10L))
                                        .build(),
                                new String[] {
                                    TRIM_MAXLEN_REDIS_API,
                                    TRIM_EXACT_REDIS_API,
                                    Long.toString(5L),
                                    TRIM_LIMIT_REDIS_API,
                                    Long.toString(10L),
                                    "id"
                                }),
                        Arguments.of(
                                // MAXLEN with non exact match
                                "test_xadd_maxlen_with_non_exact_match",
                                StreamAddOptions.builder()
                                        .makeStream(Boolean.FALSE)
                                        .trim(new MaxLen(false, 2L))
                                        .build(),
                                new String[] {
                                    NO_MAKE_STREAM_REDIS_API,
                                    TRIM_MAXLEN_REDIS_API,
                                    TRIM_NOT_EXACT_REDIS_API,
                                    Long.toString(2L),
                                    "*"
                                }),
                        Arguments.of(
                                // MIN ID with LIMIT
                                "test_xadd_minid_with_limit",
                                StreamAddOptions.builder()
                                        .id("id")
                                        .makeStream(Boolean.TRUE)
                                        .trim(new MinId("testKey", 10L))
                                        .build(),
                                new String[] {
                                    TRIM_MINID_REDIS_API,
                                    TRIM_EXACT_REDIS_API,
                                    Long.toString(5L),
                                    TRIM_LIMIT_REDIS_API,
                                    Long.toString(10L),
                                    "id"
                                }),
                        Arguments.of(
                                // MIN ID with non exact match
                                "test_xadd_minid_with_non_exact_match",
                                StreamAddOptions.builder()
                                        .makeStream(Boolean.FALSE)
                                        .trim(new MinId(false, "testKey"))
                                        .build(),
                                new String[] {
                                    NO_MAKE_STREAM_REDIS_API,
                                    TRIM_MINID_REDIS_API,
                                    TRIM_NOT_EXACT_REDIS_API,
                                    Long.toString(5L),
                                    "*"
                                })));
    }

    @SneakyThrows
    @ParameterizedTest(name = "{0}")
    @MethodSource("getStreamAddOptions")
    public void xadd_with_options_to_arguments(
            String testName, StreamAddOptions options, String[] expectedArgs) {
        assertArrayEquals(expectedArgs, options.toArgs());
    }

    @SneakyThrows
    @Test
    public void xadd_with_nomakestream_maxlen_options_returns_success() {
        // setup
        String key = "testKey";
        Map<String, String> fieldValues = new LinkedHashMap<>();
        fieldValues.put("testField1", "testValue1");
        fieldValues.put("testField2", "testValue2");
        StreamAddOptions options =
                StreamAddOptions.builder().id("id").makeStream(false).trim(new MaxLen(true, 5L)).build();

        String[] arguments =
                new String[] {
                    key,
                    NO_MAKE_STREAM_REDIS_API,
                    TRIM_MAXLEN_REDIS_API,
                    TRIM_EXACT_REDIS_API,
                    Long.toString(5L),
                    "id"
                };
        arguments = ArrayUtils.addAll(arguments, convertMapToKeyValueStringArray(fieldValues));

        String returnId = "testId";
        CompletableFuture<String> testResponse = new CompletableFuture<>();
        testResponse.complete(returnId);

        // match on protobuf request
        when(commandManager.<String>submitNewCommand(eq(XAdd), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String> response = service.xadd(key, fieldValues, options);
        String payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(returnId, payload);
    }

    @Test
    @SneakyThrows
    public void xtrim_with_exact_MinId() {
        // setup
        String key = "testKey";
        StreamTrimOptions limit = new MinId(true, "id");
        String[] arguments = new String[] {key, TRIM_MINID_REDIS_API, TRIM_EXACT_REDIS_API, "id"};
        Long completedResult = 1L;

        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(completedResult);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(XTrim), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.xtrim(key, limit);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(completedResult, payload);
    }

    private static List<Arguments> getStreamTrimOptions() {
        return List.of(
                Arguments.of(
                        // MAXLEN just THRESHOLD
                        "test_xtrim_maxlen", new MaxLen(5L), new String[] {TRIM_MAXLEN_REDIS_API, "5"}),
                Arguments.of(
                        // MAXLEN with LIMIT
                        "test_xtrim_maxlen_with_limit",
                        new MaxLen(5L, 10L),
                        new String[] {
                            TRIM_MAXLEN_REDIS_API, TRIM_NOT_EXACT_REDIS_API, "5", TRIM_LIMIT_REDIS_API, "10"
                        }),
                Arguments.of(
                        // MAXLEN with exact
                        "test_xtrim_exact_maxlen",
                        new MaxLen(true, 10L),
                        new String[] {TRIM_MAXLEN_REDIS_API, TRIM_EXACT_REDIS_API, "10"}),
                Arguments.of(
                        // MINID just THRESHOLD
                        "test_xtrim_minid", new MinId("0-1"), new String[] {TRIM_MINID_REDIS_API, "0-1"}),
                Arguments.of(
                        // MINID with exact
                        "test_xtrim_exact_minid",
                        new MinId(true, "0-2"),
                        new String[] {TRIM_MINID_REDIS_API, TRIM_EXACT_REDIS_API, "0-2"}),
                Arguments.of(
                        // MINID with LIMIT
                        "test_xtrim_minid_with_limit",
                        new MinId("0-3", 10L),
                        new String[] {
                            TRIM_MINID_REDIS_API, TRIM_NOT_EXACT_REDIS_API, "0-3", TRIM_LIMIT_REDIS_API, "10"
                        }));
    }

    @SneakyThrows
    @ParameterizedTest(name = "{0}")
    @MethodSource("getStreamTrimOptions")
    public void xtrim_with_options_to_arguments(
            String testName, StreamTrimOptions options, String[] expectedArgs) {
        assertArrayEquals(expectedArgs, options.toArgs());
    }

    @Test
    @SneakyThrows
    public void xlen_returns_success() {
        // setup
        String key = "testKey";
        Long completedResult = 99L;

        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(completedResult);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(XLen), eq(new String[] {key}), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.xlen(key);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(completedResult, payload);
    }

    @Test
    @SneakyThrows
    public void xdel_returns_success() {
        // setup
        String key = "testKey";
        String[] ids = {"one-1", "two-2", "three-3"};
        Long completedResult = 69L;

        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(completedResult);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(
                        eq(XDel), eq(new String[] {key, "one-1", "two-2", "three-3"}), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.xdel(key, ids);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(completedResult, payload);
    }

    @SneakyThrows
    @Test
    public void type_returns_success() {
        // setup
        String key = "testKey";
        String[] arguments = new String[] {key};
        String value = "none";

        CompletableFuture<String> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<String>submitNewCommand(eq(Type), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String> response = service.type(key);
        String payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void randomKey() {
        // setup
        String key1 = "key1";
        String key2 = "key2";
        String[] arguments = new String[] {};
        CompletableFuture<String> testResponse = new CompletableFuture<>();
        testResponse.complete(key1);

        // match on protobuf request
        when(commandManager.<String>submitNewCommand(eq(RandomKey), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        service.set(key1, "a");
        CompletableFuture<String> response = service.randomKey();

        // verify
        assertEquals(testResponse, response);
    }

    @SneakyThrows
    @Test
    public void rename() {
        // setup
        String key = "key1";
        String newKey = "key2";
        String[] arguments = new String[] {key, newKey};
        CompletableFuture<String> testResponse = new CompletableFuture<>();
        testResponse.complete(OK);

        // match on protobuf request
        when(commandManager.<String>submitNewCommand(eq(Rename), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String> response = service.rename(key, newKey);

        // verify
        assertEquals(testResponse, response);
        assertEquals(OK, response.get());
    }

    @SneakyThrows
    @Test
    public void renamenx_returns_success() {
        // setup
        String key = "key1";
        String newKey = "key2";
        String[] arguments = new String[] {key, newKey};

        CompletableFuture<Boolean> testResponse = new CompletableFuture<>();
        testResponse.complete(true);

        // match on protobuf request
        when(commandManager.<Boolean>submitNewCommand(eq(RenameNX), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Boolean> response = service.renamenx(key, newKey);

        // verify
        assertEquals(testResponse, response);
        assertTrue(response.get());
    }

    @SneakyThrows
    @Test
    public void time_returns_success() {
        // setup
        CompletableFuture<String[]> testResponse = new CompletableFuture<>();
        String[] payload = new String[] {"UnixTime", "ms"};
        testResponse.complete(payload);
        // match on protobuf request
        when(commandManager.<String[]>submitNewCommand(eq(Time), eq(new String[0]), any()))
                .thenReturn(testResponse);
        // exercise
        CompletableFuture<String[]> response = service.time();

        // verify
        assertEquals(testResponse, response);
        assertEquals(payload, response.get());
    }

    @SneakyThrows
    @Test
    public void lastsave_returns_success() {
        // setup
        Long value = 42L;
        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(LastSave), eq(new String[0]), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.lastsave();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, response.get());
    }

    @SneakyThrows
    @Test
    public void flushall_returns_success() {
        // setup
        CompletableFuture<String> testResponse = new CompletableFuture<>();
        testResponse.complete(OK);

        // match on protobuf request
        when(commandManager.<String>submitNewCommand(eq(FlushAll), eq(new String[0]), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String> response = service.flushall();
        String payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(OK, payload);
    }

    @SneakyThrows
    @Test
    public void flushall_with_mode_returns_success() {
        // setup
        CompletableFuture<String> testResponse = new CompletableFuture<>();
        testResponse.complete(OK);

        // match on protobuf request
        when(commandManager.<String>submitNewCommand(
                        eq(FlushAll), eq(new String[] {SYNC.toString()}), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String> response = service.flushall(SYNC);
        String payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(OK, payload);
    }

    @SneakyThrows
    @Test
    public void lolwut_returns_success() {
        // setup
        String value = "pewpew";
        CompletableFuture<String> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<String>submitNewCommand(eq(Lolwut), eq(new String[0]), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String> response = service.lolwut();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, response.get());
    }

    @SneakyThrows
    @Test
    public void lolwut_with_params_returns_success() {
        // setup
        String value = "pewpew";
        String[] arguments = new String[] {"1", "2"};
        int[] params = new int[] {1, 2};
        CompletableFuture<String> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<String>submitNewCommand(eq(Lolwut), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String> response = service.lolwut(params);

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, response.get());
    }

    @SneakyThrows
    @Test
    public void lolwut_with_version_returns_success() {
        // setup
        String value = "pewpew";
        CompletableFuture<String> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<String>submitNewCommand(
                        eq(Lolwut), eq(new String[] {VERSION_REDIS_API, "42"}), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String> response = service.lolwut(42);

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, response.get());
    }

    @SneakyThrows
    @Test
    public void lolwut_with_version_and_params_returns_success() {
        // setup
        String value = "pewpew";
        String[] arguments = new String[] {VERSION_REDIS_API, "42", "1", "2"};
        int[] params = new int[] {1, 2};
        CompletableFuture<String> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<String>submitNewCommand(eq(Lolwut), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String> response = service.lolwut(42, params);

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, response.get());
    }

    @SneakyThrows
    @Test
    public void linsert_returns_success() {
        // setup
        String key = "testKey";
        var position = BEFORE;
        String pivot = "pivot";
        String elem = "elem";
        String[] arguments = new String[] {key, position.toString(), pivot, elem};
        long value = 42;

        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(LInsert), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.linsert(key, position, pivot, elem);
        long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void blpop_returns_success() {
        // setup
        String key = "key";
        double timeout = 0.5;
        String[] arguments = new String[] {key, "0.5"};
        String[] value = new String[] {"key", "value"};

        CompletableFuture<String[]> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<String[]>submitNewCommand(eq(BLPop), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String[]> response = service.blpop(new String[] {key}, timeout);
        String[] payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void rpushx_returns_success() {
        // setup
        String key = "testKey";
        String[] elements = new String[] {"value1", "value2"};
        String[] args = new String[] {key, "value1", "value2"};
        Long value = 2L;

        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(RPushX), eq(args), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.rpushx(key, elements);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void lpushx_returns_success() {
        // setup
        String key = "testKey";
        String[] elements = new String[] {"value1", "value2"};
        String[] args = new String[] {key, "value1", "value2"};
        Long value = 2L;

        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(LPushX), eq(args), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.lpushx(key, elements);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void brpop_returns_success() {
        // setup
        String key = "key";
        double timeout = 0.5;
        String[] arguments = new String[] {key, "0.5"};
        String[] value = new String[] {"key", "value"};

        CompletableFuture<String[]> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<String[]>submitNewCommand(eq(BRPop), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String[]> response = service.brpop(new String[] {key}, timeout);
        String[] payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void pfadd_returns_success() {
        // setup
        String key = "testKey";
        String[] elements = new String[] {"a", "b", "c"};
        String[] arguments = new String[] {key, "a", "b", "c"};
        Long value = 1L;

        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(PfAdd), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.pfadd(key, elements);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void pfcount_returns_success() {
        // setup
        String[] keys = new String[] {"a", "b", "c"};
        Long value = 1L;

        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(PfCount), eq(keys), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.pfcount(keys);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
        assertEquals(payload, response.get());
    }

    @SneakyThrows
    @Test
    public void pfmerge_returns_success() {
        // setup
        String destKey = "testKey";
        String[] sourceKeys = new String[] {"a", "b", "c"};
        String[] arguments = new String[] {destKey, "a", "b", "c"};

        CompletableFuture<String> testResponse = new CompletableFuture<>();
        testResponse.complete(OK);

        // match on protobuf request
        when(commandManager.<String>submitNewCommand(eq(PfMerge), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String> response = service.pfmerge(destKey, sourceKeys);

        // verify
        assertEquals(testResponse, response);
        assertEquals(OK, response.get());
    }

    @SneakyThrows
    @Test
    public void objectEncoding_returns_success() {
        // setup
        String key = "testKey";
        String encoding = "testEncoding";
        CompletableFuture<String> testResponse = new CompletableFuture<>();
        testResponse.complete(encoding);

        // match on protobuf request
        when(commandManager.<String>submitNewCommand(eq(ObjectEncoding), eq(new String[] {key}), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String> response = service.objectEncoding(key);
        String payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(encoding, payload);
    }

    @SneakyThrows
    @Test
    public void objectFreq_returns_success() {
        // setup
        String key = "testKey";
        Long frequency = 0L;
        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(frequency);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(ObjectFreq), eq(new String[] {key}), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.objectFreq(key);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(frequency, payload);
    }

    @SneakyThrows
    @Test
    public void objectIdletime_returns_success() {
        // setup
        String key = "testKey";
        Long idletime = 0L;
        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(idletime);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(ObjectIdleTime), eq(new String[] {key}), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.objectIdletime(key);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(idletime, payload);
    }

    @SneakyThrows
    @Test
    public void objectRefcount_returns_success() {
        // setup
        String key = "testKey";
        Long refcount = 0L;
        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(refcount);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(ObjectRefCount), eq(new String[] {key}), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.objectRefcount(key);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(refcount, payload);
    }

    @SneakyThrows
    @Test
    public void touch_returns_success() {
        // setup
        String[] keys = new String[] {"testKey1", "testKey2"};
        Long value = 2L;
        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(Touch), eq(keys), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.touch(keys);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void geoadd_returns_success() {
        // setup
        String key = "testKey";
        Map<String, GeospatialData> membersToGeoSpatialData = new LinkedHashMap<>();
        membersToGeoSpatialData.put("Catania", new GeospatialData(15.087269, 40));
        membersToGeoSpatialData.put("Palermo", new GeospatialData(13.361389, 38.115556));
        String[] arguments =
                new String[] {key, "15.087269", "40.0", "Catania", "13.361389", "38.115556", "Palermo"};
        Long value = 1L;

        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(GeoAdd), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.geoadd(key, membersToGeoSpatialData);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void geoadd_with_options_returns_success() {
        // setup
        String key = "testKey";
        Map<String, GeospatialData> membersToGeoSpatialData = new LinkedHashMap<>();
        membersToGeoSpatialData.put("Catania", new GeospatialData(15.087269, 40));
        membersToGeoSpatialData.put("Palermo", new GeospatialData(13.361389, 38.115556));
        GeoAddOptions options = new GeoAddOptions(ConditionalChange.ONLY_IF_EXISTS, true);
        String[] arguments =
                new String[] {
                    key,
                    ConditionalChange.ONLY_IF_EXISTS.getRedisApi(),
                    CHANGED_REDIS_API,
                    "15.087269",
                    "40.0",
                    "Catania",
                    "13.361389",
                    "38.115556",
                    "Palermo"
                };
        Long value = 1L;

        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(GeoAdd), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.geoadd(key, membersToGeoSpatialData, options);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void geopos_returns_success() {
        // setup
        String key = "testKey";
        String[] members = {"Catania", "Palermo"};
        String[] arguments = new String[] {key, "Catania", "Palermo"};
        Double[][] value = {{15.087269, 40.0}, {13.361389, 38.115556}};

        CompletableFuture<Double[][]> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Double[][]>submitNewCommand(eq(GeoPos), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Double[][]> response = service.geopos(key, members);
        Object[] payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void append() {
        // setup
        String key = "testKey";
        String value = "testValue";
        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(1L);
        when(commandManager.<Long>submitNewCommand(eq(Append), eq(new String[] {key, value}), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.append(key, value);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(1L, payload);
    }

    @SneakyThrows
    @Test
    public void geohash_returns_success() {
        // setup
        String key = "testKey";
        String[] members = {"Catania", "Palermo", "NonExisting"};
        String[] arguments = new String[] {key, "Catania", "Palermo", "NonExisting"};
        String[] value = {"sqc8b49rny0", "sqdtr74hyu0", null};

        CompletableFuture<String[]> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<String[]>submitNewCommand(eq(GeoHash), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String[]> response = service.geohash(key, members);
        Object[] payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void geodist_returns_success() {
        // setup
        String key = "testKey";
        String member1 = "Catania";
        String member2 = "Palermo";
        String[] arguments = new String[] {key, member1, member2};
        Double value = 166274.1516;

        CompletableFuture<Double> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Double>submitNewCommand(eq(GeoDist), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Double> response = service.geodist(key, member1, member2);
        Double payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void geodist_with_metrics_returns_success() {
        // setup
        String key = "testKey";
        String member1 = "Catania";
        String member2 = "Palermo";
        GeoUnit geoUnit = GeoUnit.KILOMETERS;
        String[] arguments = new String[] {key, member1, member2, GeoUnit.KILOMETERS.getRedisApi()};
        Double value = 166.2742;

        CompletableFuture<Double> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Double>submitNewCommand(eq(GeoDist), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Double> response = service.geodist(key, member1, member2, geoUnit);
        Double payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void functionLoad_returns_success() {
        // setup
        String code = "The best code ever";
        String[] args = new String[] {code};
        String value = "42";
        CompletableFuture<String> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<String>submitNewCommand(eq(FunctionLoad), eq(args), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String> response = service.functionLoad(code);
        String payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void functionLoadReplace_returns_success() {
        // setup
        String code = "The best code ever";
        String[] args = new String[] {FunctionLoadOptions.REPLACE.toString(), code};
        String value = "42";
        CompletableFuture<String> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<String>submitNewCommand(eq(FunctionLoad), eq(args), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String> response = service.functionLoadReplace(code);
        String payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void bitcount_returns_success() {
        // setup
        String key = "testKey";
        Long bitcount = 1L;
        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(bitcount);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(BitCount), eq(new String[] {key}), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.bitcount(key);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(1L, payload);
        assertEquals(bitcount, payload);
    }

    @SneakyThrows
    @Test
    public void bitcount_indices_returns_success() {
        // setup
        String key = "testKey";
        Long bitcount = 1L;
        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(bitcount);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(
                        eq(BitCount), eq(new String[] {key, "1", "2"}), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.bitcount(key, 1, 2);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(bitcount, payload);
    }

    @SneakyThrows
    @Test
    public void bitcount_indices_with_option_returns_success() {
        // setup
        String key = "testKey";
        Long bitcount = 1L;
        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(bitcount);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(
                        eq(BitCount), eq(new String[] {key, "1", "2", "BIT"}), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.bitcount(key, 1, 2, BitmapIndexType.BIT);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(bitcount, payload);
    }

    @SneakyThrows
    @Test
    public void setbit_returns_success() {
        // setup
        String key = "testKey";
        Long value = 1L;
        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(SetBit), eq(new String[] {key, "8", "1"}), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.setbit(key, 8, 1);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void blmpop_returns_success() {
        // setup
        String key = "testKey";
        String key2 = "testKey2";
        String[] keys = {key, key2};
        ListDirection listDirection = ListDirection.LEFT;
        double timeout = 0.1;
        String[] arguments =
                new String[] {Double.toString(timeout), "2", key, key2, listDirection.toString()};
        Map<String, String[]> value = Map.of(key, new String[] {"five"});

        CompletableFuture<Map<String, String[]>> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Map<String, String[]>>submitNewCommand(eq(BLMPop), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Map<String, String[]>> response =
                service.blmpop(keys, listDirection, timeout);
        Map<String, String[]> payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void blmpop_with_count_returns_success() {
        // setup
        String key = "testKey";
        String key2 = "testKey2";
        String[] keys = {key, key2};
        ListDirection listDirection = ListDirection.LEFT;
        long count = 1L;
        double timeout = 0.1;
        String[] arguments =
                new String[] {
                    Double.toString(timeout),
                    "2",
                    key,
                    key2,
                    listDirection.toString(),
                    COUNT_FOR_LIST_REDIS_API,
                    Long.toString(count)
                };
        Map<String, String[]> value = Map.of(key, new String[] {"five"});

        CompletableFuture<Map<String, String[]>> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Map<String, String[]>>submitNewCommand(eq(BLMPop), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Map<String, String[]>> response =
                service.blmpop(keys, listDirection, count, timeout);
        Map<String, String[]> payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void getbit_returns_success() {
        // setup
        String key = "testKey";
        Long bit = 1L;
        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(bit);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(GetBit), eq(new String[] {key, "8"}), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.getbit(key, 8);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(bit, payload);
    }

    @SneakyThrows
    @Test
    public void bitpos_returns_success() {
        // setup
        String key = "testKey";
        Long bit = 0L;
        Long bitPosition = 10L;
        String[] arguments = new String[] {key, Long.toString(bit)};
        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(bitPosition);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(BitPos), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.bitpos(key, bit);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(bitPosition, payload);
    }

    @SneakyThrows
    @Test
    public void bitpos_with_start_returns_success() {
        // setup
        String key = "testKey";
        Long bit = 0L;
        Long start = 5L;
        Long bitPosition = 10L;
        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(bitPosition);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(
                        eq(BitPos), eq(new String[] {key, Long.toString(bit), Long.toString(start)}), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.bitpos(key, bit, start);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(bitPosition, payload);
    }

    @SneakyThrows
    @Test
    public void bitpos_with_start_and_end_returns_success() {
        // setup
        String key = "testKey";
        Long bit = 0L;
        Long start = 5L;
        Long end = 10L;
        Long bitPosition = 10L;
        String[] arguments =
                new String[] {key, Long.toString(bit), Long.toString(start), Long.toString(end)};
        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(bitPosition);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(BitPos), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.bitpos(key, bit, start, end);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(bitPosition, payload);
    }

    @SneakyThrows
    @Test
    public void bitpos_with_start_and_end_and_type_returns_success() {
        // setup
        String key = "testKey";
        Long bit = 0L;
        Long start = 5L;
        Long end = 10L;
        Long bitPosition = 10L;
        String[] arguments =
                new String[] {
                    key,
                    Long.toString(bit),
                    Long.toString(start),
                    Long.toString(end),
                    BitmapIndexType.BIT.toString()
                };
        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(bitPosition);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(BitPos), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.bitpos(key, bit, start, end, BitmapIndexType.BIT);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(bitPosition, payload);
    }

    @SneakyThrows
    @Test
    public void bitop_returns_success() {
        // setup
        String destination = "destination";
        String[] keys = new String[] {"key1", "key2"};
        Long result = 6L;
        BitwiseOperation bitwiseAnd = BitwiseOperation.AND;
        String[] arguments = concatenateArrays(new String[] {bitwiseAnd.toString(), destination}, keys);
        CompletableFuture<Long> testResponse = new CompletableFuture<>();
        testResponse.complete(result);

        // match on protobuf request
        when(commandManager.<Long>submitNewCommand(eq(BitOp), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Long> response = service.bitop(bitwiseAnd, destination, keys);
        Long payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(result, payload);
    }

    @SneakyThrows
    @Test
    public void lmpop_returns_success() {
        // setup
        String key = "testKey";
        String key2 = "testKey2";
        String[] keys = {key, key2};
        ListDirection listDirection = ListDirection.LEFT;
        String[] arguments = new String[] {"2", key, key2, listDirection.toString()};
        Map<String, String[]> value = Map.of(key, new String[] {"five"});

        CompletableFuture<Map<String, String[]>> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Map<String, String[]>>submitNewCommand(eq(LMPop), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Map<String, String[]>> response = service.lmpop(keys, listDirection);
        Map<String, String[]> payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void lmpop_with_count_returns_success() {
        // setup
        String key = "testKey";
        String key2 = "testKey2";
        String[] keys = {key, key2};
        ListDirection listDirection = ListDirection.LEFT;
        long count = 1L;
        String[] arguments =
                new String[] {
                    "2", key, key2, listDirection.toString(), COUNT_FOR_LIST_REDIS_API, Long.toString(count)
                };
        Map<String, String[]> value = Map.of(key, new String[] {"five"});

        CompletableFuture<Map<String, String[]>> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<Map<String, String[]>>submitNewCommand(eq(LMPop), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<Map<String, String[]>> response = service.lmpop(keys, listDirection, count);
        Map<String, String[]> payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void lmove_returns_success() {
        // setup
        String key1 = "testKey";
        String key2 = "testKey2";
        ListDirection wherefrom = ListDirection.LEFT;
        ListDirection whereto = ListDirection.RIGHT;
        String[] arguments = new String[] {key1, key2, wherefrom.toString(), whereto.toString()};
        String value = "one";

        CompletableFuture<String> testResponse = new CompletableFuture<>();
        testResponse.complete(value);

        // match on protobuf request
        when(commandManager.<String>submitNewCommand(eq(LMove), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String> response = service.lmove(key1, key2, wherefrom, whereto);
        String payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(value, payload);
    }

    @SneakyThrows
    @Test
    public void lset_returns_success() {
        // setup
        String key = "testKey";
        long index = 0;
        String element = "two";
        String[] arguments = new String[] {key, "0", element};

        CompletableFuture<String> testResponse = new CompletableFuture<>();
        testResponse.complete(OK);

        // match on protobuf request
        when(commandManager.<String>submitNewCommand(eq(LSet), eq(arguments), any()))
                .thenReturn(testResponse);

        // exercise
        CompletableFuture<String> response = service.lset(key, index, element);
        String payload = response.get();

        // verify
        assertEquals(testResponse, response);
        assertEquals(OK, payload);
    }
}
