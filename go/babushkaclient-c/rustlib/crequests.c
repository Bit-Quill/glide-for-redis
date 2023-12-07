#include "crequests.h"

ConnectionRequest* createConnectionRequest() {
    ConnectionRequest* request = malloc(sizeof(ConnectionRequest));
    if (request == NULL) {
        return NULL; // Allocation failed
    }
    return request;
}

//TODO error handling
void appendAddress(ConnectionRequest *request, AddressInfoC *newAddress, int currentSize) {
    if (!request || !newAddress) {
        return; 
    }

    int newSize = currentSize + 1;

    AddressInfoC **newAddresses = realloc(request->addresses, (newSize + 1) * sizeof(AddressInfoC *));
    if (!newAddresses) {
        return;
    }

    // Append new address and null-terminate the array
    newAddresses[currentSize] = newAddress;
    newAddresses[newSize] = NULL;  // Null-termination

    request->addresses = newAddresses;
}

void printAddresses(const AddressInfoC **addresses) {
    if (addresses == NULL) {
        printf("No addresses.\n");
        return;
    }

    int index = 0;
    while (addresses[index] != NULL) {
        printf("Address %d: Host = %s, Port = %u\n", index, addresses[index]->host, addresses[index]->port);
        index++;
    }
}


AddressInfoC* createAddressInfoC() {
    AddressInfoC* addressInfo = malloc(sizeof(AddressInfoC));
    if (addressInfo == NULL) {
        return NULL; // Allocation failed
    }
    return addressInfo;
}

AuthenticationInfoC* createAuthenticationInfoC() {
    AuthenticationInfoC* authenticationInfo = malloc(sizeof(AuthenticationInfoC));
    if (authenticationInfo == NULL) {
        return NULL; // Allocation failed
    }
    return authenticationInfo;
}

RedisRequestC* createRedisRequestC() {
    RedisRequestC* redisRequestC = malloc(sizeof(RedisRequestC));
    if (redisRequestC == NULL) {
        return NULL; // Allocation failed
    }
    return redisRequestC;
}

void appendArgumentsToRedisRequestC(RedisRequestC *request, char *argument, int currentSize) {
    if (!request || !argument) {
        return; 
    }

    int newSize = currentSize + 1;

    char **newArgumentArray = realloc(request->argument_array, (newSize + 1) * sizeof(char *));
    if (!newArgumentArray) {
        return;
    }

    // Append new address and null-terminate the array
    newArgumentArray[currentSize] = argument;
    newArgumentArray[newSize] = NULL;  // Null-termination

    request->argument_array = newArgumentArray;
}

void freeConnectionRequest(ConnectionRequest* request) {
    if (request != NULL) {
        freeAuthenticationInfoC(request->authentication_info);

        if (request->addresses != NULL) {
            for (int i = 0; request->addresses[i] != NULL; i++) {
                freeAddressInfoC(request->addresses[i]);
            }
        }
        free(request);
    }
}

void freeRedisRequest(RedisRequestC* request) {
    if (request != NULL) {
        if (request->argument_array != NULL) {
            for (int i = 0; request->argument_array[i] != NULL; i++) {
                free(request->argument_array[i]);
            }
        }
        free(request->command_name);
        free(request);
    }
}

void freeAddressInfoC(AddressInfoC* address) {
    if (address) {
        free(address->host);
        free(address);
    }
}

void freeAuthenticationInfoC(AuthenticationInfoC* authentication) {
    if (authentication) {
        free(authentication->username);
        free(authentication->password);
        free(authentication);
    }
}
