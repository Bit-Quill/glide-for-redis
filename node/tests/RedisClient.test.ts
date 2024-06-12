/**
 * Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0
 */

import {
    afterAll,
    afterEach,
    beforeAll,
    describe,
    expect,
    it,
} from "@jest/globals";
import { BufferReader, BufferWriter } from "protobufjs";
import { v4 as uuidv4 } from "uuid";
import { ProtocolVersion, RedisClient, Transaction } from "..";
import { RedisCluster } from "../../utils/TestUtils.js";
import { redis_request } from "../src/ProtobufMessage";
import { runBaseTests } from "./SharedTests";
import {
    convertStringArrayToBuffer,
    flushAndCloseClient,
    getClientConfigurationOption,
    parseCommandLineArgs,
    parseEndpoints,
    transactionTest,
} from "./TestUtilities";

/* eslint-disable @typescript-eslint/no-var-requires */

type Context = {
    client: RedisClient;
};

const TIMEOUT = 50000;

describe("RedisClient", () => {
    let testsFailed = 0;
    let cluster: RedisCluster;
    let client: RedisClient;
    beforeAll(async () => {
        const standaloneAddresses =
            parseCommandLineArgs()["standalone-endpoints"];
        // Connect to cluster or create a new one based on the parsed addresses
        cluster = standaloneAddresses
            ? RedisCluster.initFromExistingCluster(
                  parseEndpoints(standaloneAddresses),
              )
            : await RedisCluster.createCluster(false, 1, 1);
    }, 20000);

    afterEach(async () => {
        await flushAndCloseClient(false, cluster.getAddresses(), client);
    });

    afterAll(async () => {
        if (testsFailed === 0) {
            await cluster.close();
        }
    }, TIMEOUT);

    it("test protobuf encode/decode delimited", () => {
        // This test is required in order to verify that the autogenerated protobuf
        // files has been corrected and the encoding/decoding works as expected.
        // See "Manually compile protobuf files" in node/README.md to get more info about the fix.
        const writer = new BufferWriter();
        const request = {
            callbackIdx: 1,
            singleCommand: {
                requestType: 2,
                argsArray: redis_request.Command.ArgsArray.create({
                    args: convertStringArrayToBuffer(["bar1", "bar2"]),
                }),
            },
        };
        const request2 = {
            callbackIdx: 3,
            singleCommand: {
                requestType: 4,
                argsArray: redis_request.Command.ArgsArray.create({
                    args: convertStringArrayToBuffer(["bar3", "bar4"]),
                }),
            },
        };
        redis_request.RedisRequest.encodeDelimited(request, writer);
        redis_request.RedisRequest.encodeDelimited(request2, writer);
        const buffer = writer.finish();
        const reader = new BufferReader(buffer);

        const dec_msg1 = redis_request.RedisRequest.decodeDelimited(reader);
        expect(dec_msg1.callbackIdx).toEqual(1);
        expect(dec_msg1.singleCommand?.requestType).toEqual(2);
        expect(dec_msg1.singleCommand?.argsArray?.args).toEqual(
            convertStringArrayToBuffer(["bar1", "bar2"]),
        );

        const dec_msg2 = redis_request.RedisRequest.decodeDelimited(reader);
        expect(dec_msg2.callbackIdx).toEqual(3);
        expect(dec_msg2.singleCommand?.requestType).toEqual(4);
        expect(dec_msg2.singleCommand?.argsArray?.args).toEqual(
            convertStringArrayToBuffer(["bar3", "bar4"]),
        );
    });

    it.each([ProtocolVersion.RESP2, ProtocolVersion.RESP3])(
        "info without parameters",
        async (protocol) => {
            client = await RedisClient.createClient(
                getClientConfigurationOption(cluster.getAddresses(), protocol),
            );
            const result = await client.info();
            expect(result).toEqual(expect.stringContaining("# Server"));
            expect(result).toEqual(expect.stringContaining("# Replication"));
            expect(result).toEqual(
                expect.not.stringContaining("# Latencystats"),
            );
        },
    );

    it.each([ProtocolVersion.RESP2, ProtocolVersion.RESP3])(
        "simple select test",
        async (protocol) => {
            client = await RedisClient.createClient(
                getClientConfigurationOption(cluster.getAddresses(), protocol),
            );
            let selectResult = await client.select(0);
            expect(selectResult).toEqual("OK");

            const key = uuidv4();
            const value = uuidv4();
            const result = await client.set(key, value);
            expect(result).toEqual("OK");

            selectResult = await client.select(1);
            expect(selectResult).toEqual("OK");
            expect(await client.get(key)).toEqual(null);

            selectResult = await client.select(0);
            expect(selectResult).toEqual("OK");
            expect(await client.get(key)).toEqual(value);
        },
    );

    it.each([ProtocolVersion.RESP2, ProtocolVersion.RESP3])(
        `can send transactions_%p`,
        async (protocol) => {
            client = await RedisClient.createClient(
                getClientConfigurationOption(cluster.getAddresses(), protocol),
            );
            const transaction = new Transaction();
            const expectedRes = await transactionTest(transaction);
            transaction.select(0);
            const result = await client.exec(transaction);
            expectedRes.push("OK");
            expect(result).toEqual(expectedRes);
        },
    );

    it.each([ProtocolVersion.RESP2, ProtocolVersion.RESP3])(
        "can return null on WATCH transaction failures",
        async (protocol) => {
            const client1 = await RedisClient.createClient(
                getClientConfigurationOption(cluster.getAddresses(), protocol),
            );
            const client2 = await RedisClient.createClient(
                getClientConfigurationOption(cluster.getAddresses(), protocol),
            );
            const transaction = new Transaction();
            transaction.get("key");
            const result1 = await client1.customCommand(["WATCH", "key"]);
            expect(result1).toEqual("OK");

            const result2 = await client2.set("key", "foo");
            expect(result2).toEqual("OK");

            const result3 = await client1.exec(transaction);
            expect(result3).toBeNull();

            client1.close();
            client2.close();
        },
    );

    it.each([ProtocolVersion.RESP2, ProtocolVersion.RESP3])(
        "object freq transaction test_%p",
        async (protocol) => {
            const client = await RedisClient.createClient(
                getClientConfigurationOption(cluster.getAddresses(), protocol),
            );

            const key = uuidv4();
            const maxmemoryPolicyKey = "maxmemory-policy";
            const config = await client.configGet([maxmemoryPolicyKey]);
            const maxmemoryPolicy = String(config[maxmemoryPolicyKey]);

            try {
                const transaction = new Transaction();
                transaction.configSet({
                    [maxmemoryPolicyKey]: "allkeys-lfu",
                });
                transaction.set(key, "foo");
                transaction.object_freq(key);

                const response = await client.exec(transaction);
                expect(response).not.toBeNull();

                if (response != null) {
                    expect(response.length).toEqual(3);
                    expect(response[0]).toEqual("OK");
                    expect(response[1]).toEqual("OK");
                    expect(response[2]).toBeGreaterThanOrEqual(0);
                }
            } finally {
                expect(
                    await client.configSet({
                        [maxmemoryPolicyKey]: maxmemoryPolicy,
                    }),
                ).toEqual("OK");
            }

            client.close();
        },
    );

    it.each([ProtocolVersion.RESP2, ProtocolVersion.RESP3])(
        "object idletime transaction test_%p",
        async (protocol) => {
            const client = await RedisClient.createClient(
                getClientConfigurationOption(cluster.getAddresses(), protocol),
            );

            const key = uuidv4();
            const maxmemoryPolicyKey = "maxmemory-policy";
            const config = await client.configGet([maxmemoryPolicyKey]);
            const maxmemoryPolicy = String(config[maxmemoryPolicyKey]);

            try {
                const transaction = new Transaction();
                transaction.configSet({
                    // OBJECT IDLETIME requires a non-LFU maxmemory-policy
                    [maxmemoryPolicyKey]: "allkeys-random",
                });
                transaction.set(key, "foo");
                transaction.objectIdletime(key);

                const response = await client.exec(transaction);
                expect(response).not.toBeNull();

                if (response != null) {
                    expect(response.length).toEqual(3);
                    // transaction.configSet({[maxmemoryPolicyKey]: "allkeys-random"});
                    expect(response[0]).toEqual("OK");
                    // transaction.set(key, "foo");
                    expect(response[1]).toEqual("OK");
                    // transaction.objectIdletime(key);
                    expect(response[2]).toBeGreaterThanOrEqual(0);
                }
            } finally {
                expect(
                    await client.configSet({
                        [maxmemoryPolicyKey]: maxmemoryPolicy,
                    }),
                ).toEqual("OK");
            }

            client.close();
        },
    );

    runBaseTests<Context>({
        init: async (protocol, clientName?) => {
            const options = getClientConfigurationOption(
                cluster.getAddresses(),
                protocol,
            );
            options.protocol = protocol;
            options.clientName = clientName;
            testsFailed += 1;
            client = await RedisClient.createClient(options);
            return { client, context: { client } };
        },
        close: (context: Context, testSucceeded: boolean) => {
            if (testSucceeded) {
                testsFailed -= 1;
            }
        },
        timeout: TIMEOUT,
    });
});
