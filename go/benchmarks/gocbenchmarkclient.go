package benchmarks

import (
	"fmt"
	babushkaclient_c "github.com/aws/babushka/go/babushkaclient-c"
	c_client "github.com/aws/babushka/go/babushkaclient-c/rustlib"
)

type GoCBenchmarkClient struct {
	coreClient c_client.BaseClient
}

func (goCBenchmarkClient *GoCBenchmarkClient) ConnectToRedis(connectionSettings *ConnectionSettings) error {
	clientConfig := babushkaclient_c.ClientConfiguration{}
	addressInfo := []babushkaclient_c.AddressInfo{
		{Host: connectionSettings.Host, Port: uint32(connectionSettings.Port)},
	}
	clientConfig.Addresses = addressInfo
	clientConfig.UseTLS = connectionSettings.UseSsl

	goCBenchmarkClient.coreClient.Config = clientConfig

	err := goCBenchmarkClient.coreClient.ConnectToRedis(connectionSettings.ClusterModeEnabled)
	if err != nil {
		return err
	}
	return nil
}

func (goCBenchmarkClient *GoCBenchmarkClient) Get(key string) (string, error) {
	return goCBenchmarkClient.coreClient.Get(key)
}

func (goCBenchmarkClient *GoCBenchmarkClient) Set(key string, value interface{}) error {
	str := fmt.Sprintf("%v", value)
	return goCBenchmarkClient.coreClient.Set(key, str)
}

func (goCBenchmarkClient *GoCBenchmarkClient) CloseConnection() error {
	goCBenchmarkClient.coreClient.CloseConnection()
	return nil
}

func (goCBenchmarkClient *GoCBenchmarkClient) GetName() string {
	return "babushka"
}
