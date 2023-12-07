#ifndef CREQUESTS_H
#define CREQUESTS_H

#include <stdint.h>
#include <stdio.h>
#include <stdbool.h>
#include <stdlib.h>
#include "crequests.h"

typedef enum
{
    NoTls,
    SecureTls,
    InsecureTls
} TlsMode;

typedef enum
{
    AlwaysFromPrimary,
    RoundRobin,
    LowestLatency,
    AZAffinity
} ReadFromReplicaStrategy;

typedef struct AuthenticationInfoC AuthenticationInfoC;
struct AuthenticationInfoC
{
    char *password;
    char *username;
};

typedef struct AddressInfoC AddressInfoC;
struct AddressInfoC
{
    char *host;
    uint32_t port;
};

typedef struct ConnectionRequest ConnectionRequest;
struct ConnectionRequest
{
    AddressInfoC **addresses;
    TlsMode tls_mode;
    bool cluster_mode_enabled;
    uint32_t response_timeout;
    uint32_t client_creation_timeout;
    ReadFromReplicaStrategy read_from_replica_strategy;
    AuthenticationInfoC *authentication_info;
};

typedef struct RedisRequestC RedisRequestC;
struct RedisRequestC
{
    char *command_name;
    char **argument_array;
};

ConnectionRequest* createConnectionRequest();
AddressInfoC* createAddressInfoC();
AuthenticationInfoC* createAuthenticationInfoC();
RedisRequestC* createRedisRequestC();
void appendAddress(ConnectionRequest *request, AddressInfoC *newAddress, int currentSize);
void appendArgumentsToRedisRequestC(RedisRequestC *request, char *argument, int currentSize);
void printAddresses(const AddressInfoC **addresses);
void freeConnectionRequest(ConnectionRequest* request);
void freeAddressInfoC(AddressInfoC* address);
void freeAuthenticationInfoC(AuthenticationInfoC* authentication);
#endif // CREQUESTS_H
