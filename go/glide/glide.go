/**
 * Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0
 */
package glide

/*
#cgo LDFLAGS: -L../target/release -lglide_rs
#include "lib.h"

void successCallback(uintptr_t channelPtr, char *message);
void failureCallback(uintptr_t channelPtr, char *errMessage);
*/
import "C"

import (
    "fmt"
    "unsafe"
    "github.com/aws/glide-for-redis/go/glide/protobuf"
    "github.com/golang/protobuf/proto"
)

type GlideRedisClient struct {
    coreClient unsafe.Pointer
}

type payload struct {
    value      string
    errMessage error
}

type RequestType uint32

const (
    _ = iota
    CustomCommand RequestType = iota
    GetString
    SetString
    Ping
    Info
    Del
    Select
    ConfigGet
    ConfigSet
    ConfigResetStat
    ConfigRewrite
    ClientGetName
    ClientGetRedir
    ClientId
    ClientInfo
    ClientKill
    ClientList
    ClientNoEvict
    ClientNoTouch
    ClientPause
    ClientReply
    ClientSetInfo
    ClientSetName
    ClientUnblock
    ClientUnpause
    Expire
    HashSet
    HashGet
    HashDel
    HashExists
    MGet
    MSet
    Incr
    IncrBy
    Decr
    IncrByFloat
    DecrBy
    HashGetAll
    HashMSet
    HashMGet
    HashIncrBy
    HashIncrByFloat
    LPush
    LPop
    RPush
    RPop
    LLen
    LRem
    LRange
    LTrim
    SAdd
    SRem
    SMembers
    SCard
    PExpireAt
    PExpire
    ExpireAt
    Exists
    Unlink
    TTL
    Zadd
    Zrem
    Zrange
    Zcard
    Zcount
    ZIncrBy
    ZScore
    Type
    HLen
    Echo
    ZPopMin
    Strlen
    Lindex
    ZPopMax
    XRead
    XAdd
    XReadGroup
    XAck
    XTrim
    XGroupCreate
    XGroupDestroy
)

type ErrorType uint32

const (
    ClosingError = iota
    RequestError
    TimeoutError
    ExecAbortError
    ConnectionError
)

//export successCallback
func successCallback(channelPtr C.uintptr_t, message *C.char) {
    goMessage := C.GoString(message)
    goChannelPointer := uintptr(channelPtr)
    resultChannel := *(*chan payload)(unsafe.Pointer(goChannelPointer))
    resultChannel <- payload{value: goMessage, errMessage: nil}
}

//export failureCallback
func failureCallback(channelPtr C.uintptr_t, errMessage *C.char) {
    goMessage := C.GoString(errMessage)
    goChannelPointer := uintptr(channelPtr)
    resultChannel := *(*chan payload)(unsafe.Pointer(goChannelPointer))
    resultChannel <- payload{value: "", errMessage: fmt.Errorf("error at redis operation: %s", goMessage)}
}

func stringsToCStrings(args []string) []*C.char {
    cArgs := make([]*C.char, len(args))
    for index, string := range args {
        cString := C.CString(string)
        cArgs[index] = cString
    }
    return cArgs
}

func freeCStrings(cArgs []*C.char) {
    for _, arg := range cArgs {
        C.free(unsafe.Pointer(arg))
    }
}

func (glideRedisClient *GlideRedisClient) ConnectToRedis(request *protobuf.ConnectionRequest) error {
    marshalledRequest, err := proto.Marshal(request)
    if err != nil {
        return fmt.Errorf("Failed to encode connection request:", err)
    }
    byteCount := len(marshalledRequest)
    requestBytes := C.CBytes(marshalledRequest)
    response := (*C.struct_ConnectionResponse)(C.create_client((*C.uchar)(requestBytes), C.uintptr_t(byteCount), (C.SuccessCallback)(unsafe.Pointer(C.successCallback)), (C.FailureCallback)(unsafe.Pointer(C.failureCallback))))
    defer C.free(unsafe.Pointer(response))
    if response.error != nil {
        return fmt.Errorf(C.GoString(response.error.message))
    }
    glideRedisClient.coreClient = response.conn_ptr
    return nil
}

func (glideRedisClient *GlideRedisClient) Set(key string, value interface{}) error {
    strValue := fmt.Sprintf("%v", value)
    ckey := C.CString(key)
    cval := C.CString(strValue)
    defer C.free(unsafe.Pointer(ckey))
    defer C.free(unsafe.Pointer(cval))

    resultChannel := make(chan payload)
    resultChannelPtr := uintptr(unsafe.Pointer(&resultChannel))

    args := []string{key, strValue}
    cArgs := stringsToCStrings(args)
    defer freeCStrings(cArgs)
    requestType := C.uint32_t(SetString)
    C.command(glideRedisClient.coreClient, C.uintptr_t(resultChannelPtr), requestType, C.uintptr_t(2), &cArgs[0])

    resultPayload := <-resultChannel

    return resultPayload.errMessage
}

func (glideRedisClient *GlideRedisClient) Get(key string) (string, error) {
    ckey := C.CString(key)
    defer C.free(unsafe.Pointer(ckey))

    resultChannel := make(chan payload)
    resultChannelPtr := uintptr(unsafe.Pointer(&resultChannel))

    args := []string{key}
    cArgs := stringsToCStrings(args)
    defer freeCStrings(cArgs)
    requestType := C.uint32_t(GetString)
    C.command(glideRedisClient.coreClient, C.uintptr_t(resultChannelPtr), requestType, C.uintptr_t(1), &cArgs[0])
    resultPayload := <-resultChannel

    return resultPayload.value, nil
}

func (glideRedisClient *GlideRedisClient) Ping() (string, error) {
    resultChannel := make(chan payload)
    resultChannelPtr := uintptr(unsafe.Pointer(&resultChannel))

    cArgs := []*C.char{}
    requestType := C.uint32_t(Ping)
    C.command(glideRedisClient.coreClient, C.uintptr_t(resultChannelPtr), requestType, C.uintptr_t(0), &cArgs[0])
    resultPayload := <-resultChannel

    return resultPayload.value, resultPayload.errMessage
}

func (glideRedisClient *GlideRedisClient) Info() (string, error) {
    resultChannel := make(chan payload)
    resultChannelPtr := uintptr(unsafe.Pointer(&resultChannel))

    cArgs := []*C.char{}
    requestType := C.uint32_t(Info)
    C.command(glideRedisClient.coreClient, C.uintptr_t(resultChannelPtr), requestType, C.uintptr_t(0), &cArgs[0])
    resultPayload := <-resultChannel

    return resultPayload.value, resultPayload.errMessage
}

func (glideRedisClient *GlideRedisClient) CloseClient() {
    C.close_client(glideRedisClient.coreClient)
}
