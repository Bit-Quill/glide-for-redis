package asyncClientRawFFI

/*
#cgo LDFLAGS: -L./target/release -lgorustffi
#include "lib.h"

void successCallback(uintptr_t id , char *cstr1, uintptr_t channelPtr, long long rustOpStart, long long rustOpEnd);
void failureCallback(uintptr_t id);
*/
import "C"

import (
	"fmt"
	"github.com/aws/babushka/go/benchmarks"
	"sync"
	"time"
	"unsafe"
)

// TODO proper error handling for all functions
type AsyncRedisClient struct {
	coreClient   unsafe.Pointer
	timingValues SafeSlice
}

type Timing struct {
	t_go_rust_get      int64
	t_rustop           int64
	t_rust_go_callback int64
	t_go_callback_wait int64
	message            string
}

type SafeSlice struct {
	slice []Timing
	mu    sync.Mutex
}

func (s *SafeSlice) Append(value Timing) {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.slice = append(s.slice, value)
}

//export successCallback
func successCallback(connectionID C.uintptr_t, message *C.char, channelPtr C.uintptr_t, rustOpStart C.longlong, rustOpEnd C.longlong) {
	startCallback := time.Now().UnixNano()
	res := Timing{}
	res.message = C.GoString(message)
	res.t_rustop = int64(rustOpEnd - rustOpStart)
	res.t_rust_go_callback = startCallback - int64(rustOpEnd)
	res.t_go_callback_wait = startCallback
	returnedAddress := uintptr(channelPtr)
	channel := *(*chan Timing)(unsafe.Pointer(returnedAddress))
	channel <- res
}

//export failureCallback
func failureCallback(connectionID C.uintptr_t) {
	panic("In failure callback get or set")
}

func (asyncRedisClient *AsyncRedisClient) ConnectToRedis(connectionSettings *benchmarks.ConnectionSettings) error {
	caddress := C.CString(connectionSettings.Host)
	defer C.free(unsafe.Pointer(caddress))

	asyncRedisClient.coreClient = C.create_connection(caddress, C.uint32_t(connectionSettings.Port), C._Bool(connectionSettings.UseSsl), C._Bool(connectionSettings.ClusterModeEnabled), (C.success_callback)(unsafe.Pointer(C.successCallback)), (C.failure_callback)(unsafe.Pointer(C.failureCallback)))
	if asyncRedisClient.coreClient == nil {
		return fmt.Errorf("error connecting to asyncRedisClient")
	}
	return nil
}

func (asyncRedisClient *AsyncRedisClient) Set(key string, value interface{}) error {
	strValue := fmt.Sprintf("%v", value)
	ckey := C.CString(key)
	cval := C.CString(strValue)
	defer C.free(unsafe.Pointer(ckey))
	defer C.free(unsafe.Pointer(cval))

	result := make(chan string)
	chAddress := uintptr(unsafe.Pointer(&result))

	C.set(asyncRedisClient.coreClient, C.uintptr_t(1), ckey, cval, C.uintptr_t(chAddress))

	<-result

	return nil
}

func (asyncRedisClient *AsyncRedisClient) Get(key string) (string, error) {
	goTimeNanos := time.Now().UnixNano()
	ckey := C.CString(key)
	defer C.free(unsafe.Pointer(ckey))

	result := make(chan Timing)
	chAddress := uintptr(unsafe.Pointer(&result)) //Gives you the raw memory address of the result variable as an integer.

	rustStartGet := C.get(asyncRedisClient.coreClient, C.uintptr_t(1), ckey, C.uintptr_t(chAddress))
	value := <-result
	value.t_go_callback_wait = time.Now().UnixNano() - value.t_go_callback_wait
	value.t_go_rust_get = int64(rustStartGet) - goTimeNanos
	asyncRedisClient.timingValues.Append(value)
	return value.message, nil
}

func (asyncRedisClient *AsyncRedisClient) CloseConnection() error {
	C.close_connection(asyncRedisClient.coreClient)
	asyncRedisClient.calculateAndPrintAverages()
	return nil
}

func (asyncRedisClient *AsyncRedisClient) GetName() string {
	return "babushka"
}

func (asyncRedisClient *AsyncRedisClient) calculateAndPrintAverages() {
	var sumGoRustGet, sumRustOp, sumRustGoCallback, sumGoCallbackWait int64
	count := len(asyncRedisClient.timingValues.slice)

	for _, t := range asyncRedisClient.timingValues.slice {
		sumGoRustGet += t.t_go_rust_get
		sumRustOp += t.t_rustop
		sumRustGoCallback += t.t_rust_go_callback
		sumGoCallbackWait += t.t_go_callback_wait
	}

	if count > 0 {
		// Calculate averages
		avgGoRustGet := float64(sumGoRustGet) / float64(count)
		avgRustOp := float64(sumRustOp) / float64(count)
		avgRustGoCallback := float64(sumRustGoCallback) / float64(count)
		avgGoCallbackWait := float64(sumGoCallbackWait) / float64(count)

		// Print averages
		fmt.Printf("Average time between the start of go get call and rust get call: %v\n", avgGoRustGet/1e6)
		fmt.Printf("Average time for the rust operation (start of get call and right before callback): %v\n", avgRustOp/1e6)
		fmt.Printf("Average time between rust calling the callback and the start of the go callback: %v\n", avgRustGoCallback/1e6)
		fmt.Printf("Average time between the start of the gocallback and the get call receiving the value from the channel: %v\n", avgGoCallbackWait/1e6)
	} else {
		fmt.Println("No timings data available to calculate averages.")
	}
}
