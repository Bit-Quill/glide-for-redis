package babushkaclient_c

type AddressInfo struct {
	Host string
	Port uint32
}

// TODO: Username is optional param
type AuthenticationOptions struct {
	Password string
	Username string
}

// TODO
func NewAuthenticationOptions(password string, username string) *AuthenticationOptions {
	return &AuthenticationOptions{Password: password, Username: username}
}

// TODO: Optional Params
type ClientConfiguration struct {
	Addresses             []AddressInfo
	UseTLS                bool
	Credentials           AuthenticationOptions
	ClientCreationTimeOut uint32
	ResponseTimout        uint32
}

func NewClientConfiguration(addresses []AddressInfo, useTLS bool, credentials AuthenticationOptions, clientCreationTimeOut uint32, responseTimout uint32) *ClientConfiguration {
	return &ClientConfiguration{Addresses: addresses, UseTLS: useTLS, Credentials: credentials, ClientCreationTimeOut: clientCreationTimeOut, ResponseTimout: responseTimout}
}
