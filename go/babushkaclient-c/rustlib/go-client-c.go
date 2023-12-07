package c_client

/*
#cgo LDFLAGS: -L./target/release -lclientC
#include "lib.h"
#include "crequests.h"
#include <stdlib.h>

void successCallbackC(char *message, uintptr_t channelPtr);
void failureCallbackC(char *errMessage, uintptr_t channelPtr);
*/
import "C"
import (
	"fmt"
	"github.com/aws/babushka/go/babushkaclient-c"
	"unsafe"
)

type BaseClient struct {
	Config            babushkaclient_c.ClientConfiguration
	connectionPointer unsafe.Pointer
}

type payload struct {
	value      string
	errMessage error
}

//export successCallbackC
func successCallbackC(message *C.char, channelPtr C.uintptr_t) {
	goMessage := C.GoString(message)
	goChannelPointer := uintptr(channelPtr)
	resultChannel := *(*chan payload)(unsafe.Pointer(goChannelPointer))
	resultChannel <- payload{value: goMessage, errMessage: nil}
}

//export failureCallbackC
func failureCallbackC(errMessage *C.char, channelPtr C.uintptr_t) {
	goMessage := C.GoString(errMessage)
	goChannelPointer := uintptr(channelPtr)
	resultChannel := *(*chan payload)(unsafe.Pointer(goChannelPointer))
	resultChannel <- payload{value: "", errMessage: fmt.Errorf("error at redis operation: %s", goMessage)}
}

func (baseClient *BaseClient) ConnectToRedis(clusterMode bool) error {
	connectionRequest := baseClient.CreateConnectionStruct(clusterMode)
	defer C.freeConnectionRequest(connectionRequest)

	baseClient.connectionPointer = C.create_connection_c(connectionRequest, (C.SuccessCallback)(unsafe.Pointer(C.successCallbackC)), (C.FailureCallback)(unsafe.Pointer(C.failureCallbackC)))
	if baseClient.connectionPointer == nil {
		return fmt.Errorf("error connecting to babushkaRedisClient")
	}
	return nil
}

func (baseClient *BaseClient) CloseConnection() {
	C.close_connection_c(baseClient.connectionPointer)
}

func (baseClient *BaseClient) Set(key string, value string) error {
	var args []string
	args = append(args, key, value)

	_, err := baseClient.ExecuteCommand("SET", args)
	return err
}
func (baseClient *BaseClient) Get(key string) (string, error) {
	var args []string
	args = append(args, key)
	return baseClient.ExecuteCommand("GET", args)
}

func (baseClient *BaseClient) Ping(message ...string) (string, error) {
	var args []string
	if len(message) == 1 {
		args = append(args, message[0])
	}
	return baseClient.ExecuteCommand("PING", args)
}

func (baseClient *BaseClient) Info() (string, error) {
	return baseClient.ExecuteCommand("INFO", nil)
}

func (baseClient *BaseClient) ExecuteCommand(commandName string, argumentArray []string) (string, error) {
	redisRequestC := C.createRedisRequestC()
	redisRequestC.command_name = C.CString(commandName)
	var numArgs int = 0
	for _, arg := range argumentArray {
		C.appendArgumentsToRedisRequestC(redisRequestC, C.CString(arg), C.int(numArgs))
		numArgs++
	}
	resultChannel := make(chan payload)
	resultChannelPtr := uintptr(unsafe.Pointer(&resultChannel))
	C.execute_command_rust(baseClient.connectionPointer, redisRequestC, C.uintptr_t(resultChannelPtr))
	resultPayload := <-resultChannel
	return resultPayload.value, resultPayload.errMessage
}

// TODO: Needs to be updated based on optional values
func (baseClient *BaseClient) CreateConnectionStruct(clusterMode bool) *C.ConnectionRequest {
	// Create an ExampleStruct in Go
	connectionRequest := C.createConnectionRequest()

	var sizeCounter int = 0
	for _, address := range baseClient.Config.Addresses {
		cAddressInfo := C.createAddressInfoC()
		cAddressInfo.host = C.CString(address.Host)
		cAddressInfo.port = C.uint(address.Port)

		C.appendAddress(connectionRequest, cAddressInfo, C.int(sizeCounter))
		sizeCounter++
	}

	if baseClient.Config.UseTLS {
		connectionRequest.tls_mode = C.SecureTls
	} else {
		connectionRequest.tls_mode = C.NoTls
	}

	connectionRequest.read_from_replica_strategy = C.AlwaysFromPrimary

	//TODO will need to be changed as optional values
	if baseClient.Config.ResponseTimout != 0 {
		connectionRequest.response_timeout = C.uint(baseClient.Config.ResponseTimout)
	}

	//TODO will need to be changed as optional values
	if baseClient.Config.ClientCreationTimeOut != 0 {
		connectionRequest.client_creation_timeout = C.uint(baseClient.Config.ClientCreationTimeOut)
	}

	if clusterMode {
		connectionRequest.cluster_mode_enabled = C.bool(true)
	} else {
		connectionRequest.cluster_mode_enabled = C.bool(false)
	}

	connectionRequest.authentication_info = C.createAuthenticationInfoC()
	connectionRequest.authentication_info.username = C.CString(baseClient.Config.Credentials.Username)
	connectionRequest.authentication_info.password = C.CString(baseClient.Config.Credentials.Password)

	return connectionRequest
}
