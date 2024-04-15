// Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0

using System.Buffers;
using System.Runtime.InteropServices;

namespace Glide;

public class AsyncClient : IDisposable
{
    #region public methods
    public AsyncClient(string host, uint port, bool useTLS)
    {
        _successCallbackDelegate = SuccessCallback;
        nint successCallbackPointer = Marshal.GetFunctionPointerForDelegate(_successCallbackDelegate);
        _failureCallbackDelegate = FailureCallback;
        nint failureCallbackPointer = Marshal.GetFunctionPointerForDelegate(_failureCallbackDelegate);
        _clientPointer = CreateClientFfi(host, port, useTLS, successCallbackPointer, failureCallbackPointer);
        if (_clientPointer == IntPtr.Zero)
        {
            throw new Exception("Failed creating a client");
        }
    }

    private async Task<string?> Command(IntPtr[] args, int argsCount, RequestType requestType)
    {
        // We need to pin the array in place, in order to ensure that the GC doesn't move it while the operation is running.
        GCHandle pinnedArray = GCHandle.Alloc(args, GCHandleType.Pinned);
        IntPtr pointer = pinnedArray.AddrOfPinnedObject();
        Message<string> message = _messageContainer.GetMessageForCall(args, argsCount);
        CommandFfi(_clientPointer, (ulong)message.Index, (int)requestType, pointer, (uint)argsCount);
        string? result = await message;
        pinnedArray.Free();
        return result;
    }

    public async Task<string?> SetAsync(string key, string value)
    {
        IntPtr[] args = _arrayPool.Rent(2);
        args[0] = Marshal.StringToHGlobalAnsi(key);
        args[1] = Marshal.StringToHGlobalAnsi(value);
        string? result = await Command(args, 2, RequestType.SetString);
        _arrayPool.Return(args);
        return result;
    }

    public async Task<string?> GetAsync(string key)
    {
        IntPtr[] args = _arrayPool.Rent(1);
        args[0] = Marshal.StringToHGlobalAnsi(key);
        string? result = await Command(args, 1, RequestType.GetString);
        _arrayPool.Return(args);
        return result;
    }

    public void Dispose()
    {
        if (_clientPointer == IntPtr.Zero)
        {
            return;
        }
        _messageContainer.DisposeWithError(null);
        CloseClientFfi(_clientPointer);
        _clientPointer = IntPtr.Zero;
    }

    #endregion public methods

    #region private methods

    private void SuccessCallback(ulong index, IntPtr str)
    {
        string? result = str == IntPtr.Zero ? null : Marshal.PtrToStringAnsi(str);
        // Work needs to be offloaded from the calling thread, because otherwise we might starve the client's thread pool.
        _ = Task.Run(() =>
        {
            Message<string> message = _messageContainer.GetMessage((int)index);
            message.SetResult(result);
        });
    }

    private void FailureCallback(ulong index) =>
        // Work needs to be offloaded from the calling thread, because otherwise we might starve the client's thread pool.
        Task.Run(() =>
        {
            Message<string> message = _messageContainer.GetMessage((int)index);
            message.SetException(new Exception("Operation failed"));
        });

    ~AsyncClient() => Dispose();
    #endregion private methods

    #region private fields

    /// Held as a measure to prevent the delegate being garbage collected. These are delegated once
    /// and held in order to prevent the cost of marshalling on each function call.
    private readonly FailureAction _failureCallbackDelegate;

    /// Held as a measure to prevent the delegate being garbage collected. These are delegated once
    /// and held in order to prevent the cost of marshalling on each function call.
    private readonly StringAction _successCallbackDelegate;

    /// Raw pointer to the underlying native client.
    private IntPtr _clientPointer;
    private readonly MessageContainer<string> _messageContainer = new();
    private readonly ArrayPool<IntPtr> _arrayPool = ArrayPool<IntPtr>.Shared;

    #endregion private fields

    #region FFI function declarations

    private delegate void StringAction(ulong index, IntPtr str);
    private delegate void FailureAction(ulong index);
    [DllImport("libglide_rs", CallingConvention = CallingConvention.Cdecl, EntryPoint = "command")]
    private static extern void CommandFfi(IntPtr client, ulong index, int requestType, IntPtr args, uint argCount);

    private delegate void IntAction(IntPtr arg);
    [DllImport("libglide_rs", CallingConvention = CallingConvention.Cdecl, EntryPoint = "create_client")]
    private static extern IntPtr CreateClientFfi(string host, uint port, bool useTLS, IntPtr successCallback, IntPtr failureCallback);

    [DllImport("libglide_rs", CallingConvention = CallingConvention.Cdecl, EntryPoint = "close_client")]
    private static extern void CloseClientFfi(IntPtr client);

    #endregion

    #region RequestType

    // TODO: generate this with a bindings generator
    private enum RequestType
    {
        /// Invalid request type
        InvalidRequest = 0,
        /// An unknown command, where all arguments are defined by the user.
        CustomCommand = 1,

        //// Bitmap commands: https://redis.io/docs/latest/commands/?group=bitmap
        BitCount = 101,
        BitField = 102,
        BitFieldRo = 103,
        BitOp = 104,
        BitPos = 105,
        GetBit = 106,
        SetBit = 107,

        //// Cluster commands : https://redis.io/commands/?group=cluster
        Asking = 201,
        ClusterAddSlots = 202,
        ClusterAddSlotsRange = 203,
        ClusterBumpEpoch = 204,
        ClusterCountFailureReports = 205,
        ClusterCountKeysInSlot = 206,
        ClusterDelSlots = 207,
        ClusterDelSlotsRange = 208,
        ClusterFailover = 209,
        ClusterFlushSlots = 210,
        ClusterForget = 211,
        ClusterGetKeysInSlot = 212,
        ClusterInfo = 213,
        ClusterKeySlot = 214,
        ClusterLinks = 215,
        ClusterMeet = 216,
        ClusterMyId = 217,
        ClusterMyShardId = 218,
        ClusterNodes = 219,
        ClusterReplicas = 220,
        ClusterReplicate = 221,
        ClusterReset = 222,
        ClusterSaveConfig = 223,
        ClusterSetConfigEpoch = 224,
        ClusterSetslot = 225,
        ClusterShards = 226,
        ClusterSlaves = 227,
        ClusterSlots = 228,
        ReadOnly = 229,
        ReadWrite = 230,

        //// Connection Management commands : https://redis.io/commands/?group=connection
        Auth = 301,
        ClientCaching = 302,
        ClientGetName = 303,
        ClientGetRedir = 304,
        ClientId = 305,
        ClientInfo = 306,
        ClientKillSimple = 307,
        ClientKill = 308,
        ClientList = 309,
        ClientNoEvict = 310,
        ClientNoTouch = 311,
        ClientPause = 312,
        ClientReply = 313,
        ClientSetInfo = 314,
        ClientSetName = 315,
        ClientTracking = 316,
        ClientTrackingInfo = 317,
        ClientUnblock = 318,
        ClientUnpause = 319,
        Echo = 320,
        Hello = 321,
        Ping = 322,
        Quit = 323, // deprecated in 7.2.0
        Reset = 324,
        Select = 325,

        //// Generic commands : https://redis.io/commands/?group=generic
        Copy = 401,
        Del = 402,
        Dump = 403,
        Exists = 404,
        Expire = 405,
        ExpireAt = 406,
        ExpireTime = 407,
        Keys = 408,
        Migrate = 409,
        Move = 410,
        ObjectEncoding = 411,
        ObjectFreq = 412,
        ObjectIdleTime = 413,
        ObjectRefCount = 414,
        Persist = 415,
        PExpire = 416,
        PExpireAt = 417,
        PExpireTime = 418,
        PTtl = 419,
        RandomKey = 420,
        Rename = 421,
        RenameNx = 422,
        Restore = 423,
        Scan = 424,
        Sort = 425,
        SortRo = 426,
        Touch = 427,
        Ttl = 428,
        Type = 429,
        Unlink = 430,
        Wait = 431,
        WaitAof = 432,

        //// Geospatial indices commands: https://redis.io/docs/latest/commands/geoadd/
        GeoAdd = 501,
        GeoDist = 502,
        GeoHash = 503,
        GeoPos = 504,
        GeoRadius = 505,
        GeoRadiusRo = 506, // deprecated in 6.2.0
        GeoRadiusByMember = 507,
        GeoRadiusByMemberRo = 508, // deprecated in 6.2.0
        GeoSearch = 509,
        GeoSearchStore = 510,

        //// Hash commands: https://redis.io/docs/latest/commands/?group=hash
        HDel = 601,
        HExists = 602,
        HGet = 603,
        HGetAll = 604,
        HIncrBy = 605,
        HIncrByFloat = 606,
        HKeys = 607,
        HLen = 608,
        HMGet = 609,
        HMSet = 610,
        HRandField = 611,
        HScan = 612,
        HSet = 613,
        HSetNx = 614,
        HStrlen = 615,
        HVals = 616,

        //// HyperLogLog commands: https://redis.io/docs/latest/commands/?group=hyperloglog
        PfAdd = 701,
        PfCount = 702,
        PfMerge = 703,

        //// List commands: https://redis.io/docs/latest/commands/?group=list
        BLMove = 801,
        BLMPop = 802,
        BLPop = 803,
        BRPop = 804,
        BRPopLPush = 805, // deprecated in 6.2.0
        LIndex = 806,
        LInsert = 807,
        LLen = 808,
        LMove = 809,
        LMPop = 810,
        LPop = 811,
        LPos = 812,
        LPush = 813,
        LPushX = 814,
        LRange = 815,
        LRem = 816,
        LSet = 817,
        LTrim = 818,
        RPop = 819,
        RPopLPush = 820, // deprecated in 6.2.0
        RPush = 821,
        RPushX = 822,

        //// Pub/Sub commands: https://redis.io/docs/latest/commands/?group=pubsub
        PSubscribe = 901,
        Publish = 902,
        PubSubChannels = 903,
        PubSubNumPat = 904,
        PubSubNumSub = 905,
        PubSubShardChannels = 906,
        PubSubShardNumSub = 907,
        PUnsubscribe = 908,
        SPublish = 909,
        SSubscribe = 910,
        Subscribe = 911,
        SUnsubscribe = 912,
        Unsubscribe = 913,

        //// Scripting and Functions commands: https://redis.io/docs/latest/commands/?group=scripting
        Eval = 1001,
        EvalRo = 1002,
        EvalSha = 1003,
        EvalShaRo = 1004,
        FCall = 1005,
        FCallRo = 1006,
        FunctionDelete = 1007,
        FunctionDump = 1008,
        FunctionFlush = 1009,
        FunctionKill = 1010,
        FunctionList = 1011,
        FunctionLoad = 1012,
        FunctionRestore = 1013,
        FunctionStats = 1014,
        ScriptDebug = 1015,
        ScriptExists = 1016,
        ScriptFlush = 1017,
        ScriptKill = 1018,
        ScriptLoad = 1019,

        //// Server management commands: https://redis.io/docs/latest/commands/?group=server
        AclCat = 1101,
        AclDelUser = 1102,
        AclDryRun = 1103,
        AclGenPass = 1104,
        AclGetUser = 1105,
        AclList = 1106,
        AclLoad = 1107,
        AclLog = 1108,
        AclSave = 1109,
        AclSetSser = 1110,
        AclUsers = 1111,
        AclWhoami = 1112,
        BgRewriteAof = 1113,
        BgSave = 1114,
        Command = 1115,
        CommandCount = 1116,
        CommandDocs = 1117,
        CommandGetKeys = 1118,
        CommandGetKeysAndFlags = 1119,
        CommandInfo = 1120,
        CommandList = 1121,
        ConfigGet = 1122,
        ConfigResetStat = 1123,
        ConfigRewrite = 1124,
        ConfigSet = 1125,
        DbSize = 1126,
        FailOver = 1127,
        FlushAll = 1128,
        FlushDb = 1129,
        Info = 1130,
        LastSave = 1131,
        LatencyDoctor = 1132,
        LatencyGraph = 1133,
        LatencyHistogram = 1134,
        LatencyHistory = 1135,
        LatencyLatest = 1136,
        LatencyReset = 1137,
        Lolwut = 1138,
        MemoryDoctor = 1139,
        MemoryMallocStats = 1140,
        MemoryPurge = 1141,
        MemoryStats = 1142,
        MemoryUsage = 1143,
        ModuleList = 1144,
        ModuleLoad = 1145,
        ModuleLoadEx = 1146,
        ModuleUnload = 1147,
        Monitor = 1148,
        PSync = 1149,
        ReplConf = 1150,
        ReplicaOf = 1151,
        RestoreAsking = 1152,
        Role = 1153,
        Save = 1154,
        ShutDown = 1155,
        SlaveOf = 1156,
        SlowLogGet = 1157,
        SlowLogLen = 1158,
        SlowLogReset = 1159,
        SwapDb = 1160,
        Sync = 1161,
        Time = 1162,

        //// Set commands: https://redis.io/docs/latest/commands/?group=set
        SAdd = 1201,
        SCard = 1202,
        SDiff = 1203,
        SDiffStore = 1204,
        SInter = 1205,
        SInterCard = 1206,
        SInterStore = 1207,
        SIsMember = 1208,
        SMembers = 1209,
        SMIsMember = 1210,
        SMove = 1211,
        SPop = 1212,
        SRandMember = 1213,
        SRem = 1214,
        SScan = 1215,
        SUnion = 1216,
        SUnionStore = 1217,

        //// Sorted set commands: https://redis.io/docs/latest/commands/?group=sorted-set
        BZMPop = 1301,
        BZPopMax = 1302,
        BZPopMin = 1303,
        ZAdd = 1304,
        ZCard = 1305,
        ZCount = 1306,
        ZDiff = 1307,
        ZDiffStore = 1308,
        ZIncrBy = 1309,
        ZInter = 1310,
        ZInterCard = 1311,
        ZInterStore = 1312,
        ZLexCount = 1313,
        ZMpop = 1314,
        ZMScore = 1315,
        ZPopMax = 1316,
        ZPopMin = 1317,
        ZRandMember = 1318,
        ZRange = 1319,
        ZRangeByLex = 1320,
        ZRangeByScore = 1321,
        ZRangeStore = 1322,
        ZRank = 1323,
        ZRem = 1324,
        ZRemRangeByLex = 1325,
        ZRemRangeByRank = 1326,
        ZRemRangeByScore = 1327,
        ZRevRange = 1328,
        ZRevRangeByLex = 1329,
        ZRevRangeByScore = 1330,
        ZRevRank = 1331,
        ZScan = 1332,
        ZScore = 1333,
        ZUnion = 1334,
        ZUnionStore = 1335,

        //// Stream commands: https://redis.io/docs/latest/commands/?group=stream
        XAck = 1401,
        XAdd = 1402,
        XAutoClaim = 1403,
        XClaim = 1404,
        XDel = 1405,
        XGroupCreate = 1406,
        XGroupCreateConsumer = 1407,
        XGroupDelConsumer = 1408,
        XGroupDestroy = 1409,
        XGroupSetId = 1410,
        XInfoConsumers = 1411,
        XInfoGroups = 1412,
        XInfoStream = 1413,
        XLen = 1414,
        XPending = 1415,
        XRange = 1416,
        XRead = 1417,
        XReadGroup = 1418,
        XRevRange = 1419,
        XSetId = 1420,
        XTrim = 1421,

        //// String commands : https://redis.io/commands/?group=string
        Append = 1501,
        Decr = 1502,
        DecrBy = 1503,
        /// Type of a get string request.
        GetString = 1504, // Get
        GetDel = 1505,
        GetEx = 1506,
        GetRange = 1507,
        GetSet = 1508, // deprecated in 6.2.0
        Incr = 1509,
        IncrBy = 1510,
        IncrByFloat = 1511,
        Lcs = 1512,
        LcsLen = 1513,
        LcsIdx = 1514,
        MGet = 1515,
        MSet = 1516,
        MSetNx = 1517,
        PSetEx = 1518,    // deprecated in 2.6.12
        SetString = 1519, // Set
        SetEx = 1520,     // deprecated in 2.6.12
        SetNx = 1521,     // deprecated in 2.6.12
        SetRange = 1522,
        Strlen = 1523,
        Substr = 1524,

        //// Transaction commands: https://redis.io/docs/latest/commands/?group=transactions
        Discard = 1601,
        Exec = 1602,
        Multi = 1603,
        Unwatch = 1604,
        Watch = 1605,

        //// Bloom filter commands: https://redis.io/docs/latest/commands/?group=bf
        BfAdd = 1701,
        BfCard = 1702,
        BfExists = 1703,
        BfInfo = 1704,
        BfInsert = 1705,
        BfLoadChunk = 1706,
        BfMAdd = 1707,
        BfMExists = 1708,
        BfReserve = 1709,
        BfScanDump = 1710,

        //// Cuckoo filter commands: https://redis.io/docs/latest/commands/?group=cf
        CfAdd = 1801,
        CfAddNx = 1802,
        CfCount = 1803,
        CfDel = 1804,
        CfExists = 1805,
        CfInfo = 1806,
        CfInsert = 1807,
        CfInsertNx = 1808,
        CfLoadChunk = 1809,
        CfMExists = 1810,
        CfReserve = 1811,
        CfScanDump = 1812,

        //// Count-min sketch commands: https://redis.io/docs/latest/commands/?group=cms
        CmsIncrBy = 1901,
        CmsInfo = 1902,
        CmsInitByDim = 1903,
        CmsInitByProb = 1904,
        CmsMerge = 1905,
        CmsQuery = 1906,

        //// JSON commands: https://redis.io/docs/latest/commands/?group=json
        JsonArrAppend = 2001,
        JsonArrIndex = 2002,
        JsonArrInsert = 2003,
        JsonArrLen = 2004,
        JsonArrPop = 2005,
        JsonArrTrim = 2006,
        JsonClear = 2007,
        JsonDebug = 2008,
        JsonDebugMemory = 2009,
        JsonDel = 2010,
        JsonForget = 2011,
        JsonGet = 2012,
        JsonMerge = 2013,
        JsonMGet = 2014,
        JsonMSet = 2015,
        JsonNumIncrBy = 2016,
        JsonNumMultBy = 2017,
        JsonObjKeys = 2018,
        JsonObjLen = 2019,
        JsonResp = 2020,
        JsonSet = 2021,
        JsonStrAppend = 2022,
        JsonStrLen = 2023,
        JsonToggle = 2024,
        JsonType = 2025,

        //// Search and query commands: https://redis.io/docs/latest/commands/?group=search
        FtList = 2101,
        FtAggregate = 2102,
        FtAliasAdd = 2103,
        FtAliasDel = 2104,
        FtAliasUpdate = 2105,
        FtAlter = 2106,
        FtConfigGet = 2107,
        FtConfigSet = 2108,
        FtCreate = 2109,
        FtCursorDel = 2110,
        FtCursorRead = 2111,
        FtDictAdd = 2112,
        FtDictDel = 2113,
        FtDictDump = 2114,
        FtDropIndex = 2115,
        FtExplain = 2116,
        FtExplainCli = 2117,
        FtInfo = 2118,
        FtProfile = 2119,
        FtSearch = 2120,
        FtSpellCheck = 2121,
        FtSynDump = 2122,
        FtSynUpdate = 2123,
        FtTagVals = 2124,

        //// Triggers and functions commands: https://redis.io/docs/latest/commands/?group=triggers_and_functions
        TFCall = 2201,
        TFCallAsync = 2202,
        TFunctionDelete = 2203,
        TFunctionList = 2204,
        TFunctionLoad = 2205,

        //// Auto-suggest commands: https://redis.io/docs/latest/commands/?group=suggestion
        FtSugAdd = 2301,
        FtSugDel = 2302,
        FtSugGet = 2303,
        FtSugLen = 2304,

        //// T-digest commands: https://redis.io/docs/latest/commands/?group=tdigest
        TDigestAdd = 2401,
        TDigestByRank = 2402,
        TDigestByRevRank = 2403,
        TDigestCdf = 2404,
        TDigestCreate = 2405,
        TDigestInfo = 2406,
        TDigestMax = 2407,
        TDigestMerge = 2408,
        TDigestMin = 2409,
        TDigestQuantile = 2410,
        TDigestRank = 2411,
        TDigestReset = 2412,
        TDigestRevRank = 2413,
        TDigestTrimmedMean = 2414,

        //// Time series commands: https://redis.io/docs/latest/commands/?group=timeseries
        TsAdd = 2501,
        TsAlter = 2502,
        TsCreate = 2503,
        TsCreateRule = 2504,
        TsDecrBy = 2505,
        TsDel = 2506,
        TsDeleteRule = 2507,
        TsGet = 2508,
        TsIncrBy = 2509,
        TsInfo = 2510,
        TsMAdd = 2511,
        TsMGet = 2512,
        TsMRange = 2513,
        TsMRevRange = 2514,
        TsQueryIndex = 2515,
        TsRange = 2516,
        TsRevRange = 2517,

        //// Top-K commands: https://redis.io/docs/latest/commands/?group=topk
        TopKAdd = 2601,
        TopKCount = 2602,
        TopKIncrBy = 2603,
        TopKInfo = 2604,
        TopKList = 2605,
        TopKQuery = 2606,
        TopKReserve = 2607,
    }

    #endregion
}
