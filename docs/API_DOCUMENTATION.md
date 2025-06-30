# OpenESPI Authorization Server API Documentation

## Overview

The OpenESPI Authorization Server provides OAuth2/OIDC authorization services for Green Button Alliance ESPI 4.0 compliant applications. This server implements Spring Authorization Server 1.3+ with NAESB ESPI 4.0 extensions.

## Table of Contents

1. [Authentication & Authorization](#authentication--authorization)
2. [OAuth2 Client Management API](#oauth2-client-management-api)
3. [DataCustodian Integration API](#datacustodian-integration-api)
4. [OIDC UserInfo Endpoint](#oidc-userinfo-endpoint)
5. [OAuth2 Standard Endpoints](#oauth2-standard-endpoints)
6. [Error Handling](#error-handling)
7. [Security Requirements](#security-requirements)
8. [ESPI Compliance](#espi-compliance)

## Base URL

```
https://authorization.greenbuttonalliance.org
```

## Authentication & Authorization

### API Authentication Methods

1. **Bearer Token**: For API endpoints
2. **Basic Authentication**: For client credentials
3. **Certificate-based**: For ESPI 4.0 compliance (TLS client certificates)

### Required Headers

```http
Authorization: Bearer <access_token>
Content-Type: application/json
Accept: application/json
User-Agent: YourApp/1.0
```

## OAuth2 Client Management API

### Base Path: `/api/v1/oauth2/clients`

#### 1. List OAuth2 Clients

**Endpoint**: `GET /api/v1/oauth2/clients`

**Description**: Retrieve a paginated list of registered OAuth2 clients

**Parameters**:
- `page` (optional): Page number (default: 0)
- `size` (optional): Page size (default: 20, max: 100)
- `sort` (optional): Sort field (clientName, clientId, createdAt)
- `direction` (optional): Sort direction (ASC, DESC)
- `search` (optional): Search term for client name or ID
- `espiCompliant` (optional): Filter by ESPI compliance (true/false)

**Request Example**:
```http
GET /api/v1/oauth2/clients?page=0&size=10&sort=clientName&direction=ASC&espiCompliant=true
Authorization: Bearer eyJhbGciOiJSUzI1NiIs...
Accept: application/json
```

**Response Example**:
```json
{
  "content": [
    {
      "clientId": "espi-third-party-001",
      "clientName": "Green Button Data Analytics",
      "clientSecret": "[PROTECTED]",
      "redirectUris": [
        "https://analytics.example.com/callback"
      ],
      "scopes": [
        "openid",
        "profile",
        "FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13"
      ],
      "authorizationGrantTypes": [
        "authorization_code",
        "refresh_token"
      ],
      "clientAuthenticationMethods": [
        "client_secret_basic",
        "tls_client_auth"
      ],
      "espiCompliant": true,
      "securityLevel": "HIGH",
      "certificationStatus": "CERTIFIED",
      "createdAt": "2024-01-15T10:00:00Z",
      "updatedAt": "2024-01-15T10:00:00Z"
    }
  ],
  "pageable": {
    "sort": {
      "sorted": true,
      "orderBy": "clientName"
    },
    "pageNumber": 0,
    "pageSize": 10
  },
  "totalElements": 25,
  "totalPages": 3,
  "first": true,
  "last": false
}
```

#### 2. Get Client by ID

**Endpoint**: `GET /api/v1/oauth2/clients/{clientId}`

**Description**: Retrieve a specific OAuth2 client by ID

**Path Parameters**:
- `clientId`: The client identifier

**Request Example**:
```http
GET /api/v1/oauth2/clients/espi-third-party-001
Authorization: Bearer eyJhbGciOiJSUzI1NiIs...
Accept: application/json
```

**Response Example**:
```json
{
  "clientId": "espi-third-party-001",
  "clientName": "Green Button Data Analytics",
  "clientSecret": "[PROTECTED]",
  "redirectUris": [
    "https://analytics.example.com/callback"
  ],
  "scopes": [
    "openid",
    "profile",
    "FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13"
  ],
  "authorizationGrantTypes": [
    "authorization_code",
    "refresh_token"
  ],
  "clientAuthenticationMethods": [
    "client_secret_basic",
    "tls_client_auth"
  ],
  "espiCompliant": true,
  "securityLevel": "HIGH",
  "certificationStatus": "CERTIFIED",
  "createdAt": "2024-01-15T10:00:00Z",
  "updatedAt": "2024-01-15T10:00:00Z",
  "lastUsed": "2024-01-16T14:30:00Z",
  "usageMetrics": {
    "totalTokensIssued": 1250,
    "totalAuthorizationsGranted": 45,
    "averageTokenLifetime": 3600
  }
}
```

#### 3. Create New Client

**Endpoint**: `POST /api/v1/oauth2/clients`

**Description**: Register a new OAuth2 client

**Request Body**:
```json
{
  "clientName": "Energy Management App",
  "redirectUris": [
    "https://energyapp.example.com/oauth/callback"
  ],
  "scopes": [
    "openid",
    "profile",
    "email",
    "FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13"
  ],
  "authorizationGrantTypes": [
    "authorization_code",
    "refresh_token"
  ],
  "clientAuthenticationMethods": [
    "client_secret_basic"
  ],
  "espiCompliant": true,
  "securityLevel": "HIGH",
  "clientSettings": {
    "requireAuthorizationConsent": true,
    "requireProofKey": true
  },
  "tokenSettings": {
    "accessTokenTimeToLive": "PT1H",
    "refreshTokenTimeToLive": "P30D",
    "reuseRefreshTokens": false
  }
}
```

**Response Example**:
```json
{
  "clientId": "espi-energy-app-002",
  "clientSecret": "ZGVmYXVsdC1zZWNyZXQtdmFsdWU",
  "clientName": "Energy Management App",
  "redirectUris": [
    "https://energyapp.example.com/oauth/callback"
  ],
  "scopes": [
    "openid",
    "profile",
    "email",
    "FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13"
  ],
  "authorizationGrantTypes": [
    "authorization_code",
    "refresh_token"
  ],
  "clientAuthenticationMethods": [
    "client_secret_basic"
  ],
  "espiCompliant": true,
  "securityLevel": "HIGH",
  "certificationStatus": "PENDING",
  "createdAt": "2024-01-16T15:30:00Z",
  "updatedAt": "2024-01-16T15:30:00Z"
}
```

#### 4. Update Client

**Endpoint**: `PUT /api/v1/oauth2/clients/{clientId}`

**Description**: Update an existing OAuth2 client

**Path Parameters**:
- `clientId`: The client identifier

**Request Body**: Same as create, but all fields are optional

**Response**: Updated client object (same format as GET)

#### 5. Delete Client

**Endpoint**: `DELETE /api/v1/oauth2/clients/{clientId}`

**Description**: Remove an OAuth2 client (soft delete)

**Path Parameters**:
- `clientId`: The client identifier

**Response**:
```json
{
  "message": "Client successfully deleted",
  "clientId": "espi-energy-app-002",
  "deletedAt": "2024-01-16T16:00:00Z"
}
```

#### 6. Client Metrics

**Endpoint**: `GET /api/v1/oauth2/clients/{clientId}/metrics`

**Description**: Get usage metrics for a specific client

**Response Example**:
```json
{
  "clientId": "espi-third-party-001",
  "metrics": {
    "totalTokensIssued": 1250,
    "totalAuthorizationsGranted": 45,
    "totalRefreshTokensUsed": 234,
    "averageTokenLifetime": 3600,
    "lastTokenIssuedAt": "2024-01-16T14:30:00Z",
    "peakUsageHour": 14,
    "weeklyStats": {
      "currentWeek": {
        "authorizationsGranted": 8,
        "tokensIssued": 45
      },
      "previousWeek": {
        "authorizationsGranted": 12,
        "tokensIssued": 67
      }
    }
  }
}
```

#### 7. Bulk Operations

**Endpoint**: `POST /api/v1/oauth2/clients/bulk`

**Description**: Perform bulk operations on multiple clients

**Request Body**:
```json
{
  "operation": "UPDATE_SETTINGS",
  "clientIds": [
    "client-001",
    "client-002",
    "client-003"
  ],
  "settings": {
    "requireAuthorizationConsent": true,
    "accessTokenTimeToLive": "PT2H"
  }
}
```

**Response Example**:
```json
{
  "operation": "UPDATE_SETTINGS",
  "totalRequested": 3,
  "successCount": 3,
  "failureCount": 0,
  "results": [
    {
      "clientId": "client-001",
      "status": "SUCCESS"
    },
    {
      "clientId": "client-002",
      "status": "SUCCESS"
    },
    {
      "clientId": "client-003",
      "status": "SUCCESS"
    }
  ]
}
```

## DataCustodian Integration API

### Base Path: `/api/v1/datacustodian`

#### 1. Verify User

**Endpoint**: `POST /api/v1/datacustodian/verify-user`

**Description**: Verify user credentials with DataCustodian

**Request Body**:
```json
{
  "username": "customer@example.com",
  "password": "userPassword123"
}
```

**Response Example**:
```json
{
  "verified": true,
  "customerId": "customer-123",
  "customerType": "RESIDENTIAL",
  "message": "User verified successfully"
}
```

#### 2. Get Retail Customer Info

**Endpoint**: `GET /api/v1/datacustodian/customers/{customerId}`

**Description**: Retrieve retail customer information

**Path Parameters**:
- `customerId`: DataCustodian customer identifier

**Response Example**:
```json
{
  "customerId": "customer-123",
  "username": "customer@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "email": "customer@example.com",
  "customerType": "RESIDENTIAL",
  "accountNumber": "ACC-789456",
  "serviceTerritory": "Northern California",
  "enrollmentDate": "2023-05-15T00:00:00Z",
  "status": "ACTIVE"
}
```

#### 3. Get Usage Points

**Endpoint**: `GET /api/v1/datacustodian/customers/{customerId}/usage-points`

**Description**: Retrieve usage points for a customer

**Response Example**:
```json
{
  "customerId": "customer-123",
  "usagePoints": [
    {
      "usagePointId": "up-001",
      "usagePointUUID": "550e8400-e29b-41d4-a716-446655440000",
      "serviceCategory": "ELECTRICITY",
      "serviceKind": "ENERGY",
      "meterNumber": "MTR-001",
      "serviceAddress": "123 Main St, Anytown, CA 94000",
      "status": "ACTIVE",
      "installationDate": "2023-01-15T00:00:00Z"
    }
  ]
}
```

#### 4. Validate Scope Access

**Endpoint**: `POST /api/v1/datacustodian/validate-scope`

**Description**: Validate if a customer has access to specific ESPI scopes

**Request Body**:
```json
{
  "customerId": "customer-123",
  "scopes": [
    "FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13"
  ]
}
```

**Response Example**:
```json
{
  "customerId": "customer-123",
  "scopeValidation": [
    {
      "scope": "FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13",
      "valid": true,
      "accessLevel": "READ",
      "restrictions": [],
      "usagePointsAuthorized": ["up-001", "up-002"]
    }
  ]
}
```

#### 5. Create Authorization Grant

**Endpoint**: `POST /api/v1/datacustodian/authorization-grants`

**Description**: Create an authorization grant in DataCustodian

**Request Body**:
```json
{
  "customerId": "customer-123",
  "thirdPartyId": "espi-third-party-001",
  "scopes": [
    "FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13"
  ],
  "usagePointIds": ["up-001"],
  "expirationDate": "2025-01-16T00:00:00Z"
}
```

**Response Example**:
```json
{
  "grantId": "grant-456",
  "customerId": "customer-123",
  "thirdPartyId": "espi-third-party-001",
  "scopes": [
    "FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13"
  ],
  "usagePointIds": ["up-001"],
  "status": "ACTIVE",
  "createdAt": "2024-01-16T15:30:00Z",
  "expirationDate": "2025-01-16T00:00:00Z"
}
```

#### 6. Integration Health Check

**Endpoint**: `GET /api/v1/datacustodian/health`

**Description**: Check DataCustodian integration health

**Response Example**:
```json
{
  "status": "HEALTHY",
  "datacustodianVersion": "3.5.0",
  "connectionStatus": "CONNECTED",
  "responseTime": 45,
  "lastSuccessfulCall": "2024-01-16T15:29:30Z",
  "metrics": {
    "totalRequests": 1250,
    "successfulRequests": 1247,
    "failedRequests": 3,
    "averageResponseTime": 42
  }
}
```

## OIDC UserInfo Endpoint

### Endpoint: `/userinfo`

**Description**: OpenID Connect UserInfo endpoint with ESPI-specific claims

**Methods**: `GET`, `POST`

**Authentication**: Bearer token with `openid` scope

#### Request Examples

**GET Request**:
```http
GET /userinfo
Authorization: Bearer eyJhbGciOiJSUzI1NiIs...
Accept: application/json
```

**POST Request**:
```http
POST /userinfo
Authorization: Bearer eyJhbGciOiJSUzI1NiIs...
Content-Type: application/x-www-form-urlencoded

access_token=eyJhbGciOiJSUzI1NiIs...
```

#### Response Examples

**Standard OIDC Claims** (profile scope):
```json
{
  "sub": "customer@example.com",
  "name": "John Doe",
  "given_name": "John",
  "family_name": "Doe",
  "preferred_username": "customer@example.com",
  "locale": "en-US",
  "zoneinfo": "America/Los_Angeles",
  "updated_at": "2024-01-15T10:30:00Z"
}
```

**ESPI Claims** (ESPI scopes):
```json
{
  "sub": "customer@example.com",
  "name": "John Doe",
  "email": "customer@example.com",
  "email_verified": true,
  "customer_id": "customer-123",
  "customer_type": "RESIDENTIAL",
  "account_number": "ACC-789456",
  "service_territory": "Northern California",
  "espi_scopes": [
    "FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13"
  ],
  "datacustodian_grant_id": "grant-456",
  "authorized_usage_points": ["up-001", "up-002"],
  "usage_point_details": [
    {
      "usage_point_id": "up-001",
      "usage_point_uuid": "550e8400-e29b-41d4-a716-446655440000",
      "service_category": "ELECTRICITY",
      "service_kind": "ENERGY",
      "meter_number": "MTR-001",
      "service_address": "123 Main St, Anytown, CA 94000",
      "status": "ACTIVE"
    }
  ],
  "gba_version": "2024.1",
  "espi_version": "4.0",
  "data_rights": [
    "ENERGY_USAGE_DATA",
    "HOURLY_INTERVALS",
    "MONTHLY_BILLING_DATA"
  ],
  "aud": "third_party_client",
  "iss": "https://authorization.greenbuttonalliance.org",
  "iat": "2024-01-15T10:00:00Z",
  "exp": "2024-01-15T16:00:00Z",
  "auth_time": "2024-01-15T10:00:00Z",
  "scope": "openid profile email FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13"
}
```

## OAuth2 Standard Endpoints

### Authorization Endpoint

**Endpoint**: `/oauth2/authorize`

**Description**: OAuth2 authorization endpoint

**Parameters**:
- `response_type`: `code` (authorization code flow)
- `client_id`: Client identifier
- `redirect_uri`: Callback URI
- `scope`: Requested scopes (space-separated)
- `state`: CSRF protection parameter
- `code_challenge`: PKCE code challenge (optional but recommended)
- `code_challenge_method`: `S256` for PKCE

**Example**:
```http
GET /oauth2/authorize?response_type=code&client_id=espi-third-party-001&redirect_uri=https://app.example.com/callback&scope=openid%20profile%20FB%3D4_5_15%3BIntervalDuration%3D3600%3BBlockDuration%3Dmonthly%3BHistoryLength%3D13&state=xyz123&code_challenge=dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk&code_challenge_method=S256
```

### Token Endpoint

**Endpoint**: `/oauth2/token`

**Description**: OAuth2 token endpoint

**Method**: `POST`

**Content-Type**: `application/x-www-form-urlencoded`

**Authorization Code Grant**:
```http
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded
Authorization: Basic ZXNwaS10aGlyZC1wYXJ0eS0wMDE6c2VjcmV0

grant_type=authorization_code&code=ABC123&redirect_uri=https://app.example.com/callback&code_verifier=dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk
```

**Refresh Token Grant**:
```http
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded
Authorization: Basic ZXNwaS10aGlyZC1wYXJ0eS0wMDE6c2VjcmV0

grant_type=refresh_token&refresh_token=def456
```

**Response**:
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIs...",
  "token_type": "Bearer",
  "expires_in": 3600,
  "refresh_token": "def456",
  "scope": "openid profile FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13",
  "id_token": "eyJhbGciOiJSUzI1NiIs..."
}
```

### Token Introspection

**Endpoint**: `/oauth2/introspect`

**Description**: OAuth2 token introspection endpoint

**Method**: `POST`

**Request**:
```http
POST /oauth2/introspect
Content-Type: application/x-www-form-urlencoded
Authorization: Basic ZXNwaS10aGlyZC1wYXJ0eS0wMDE6c2VjcmV0

token=eyJhbGciOiJSUzI1NiIs...
```

**Response**:
```json
{
  "active": true,
  "sub": "customer@example.com",
  "aud": "espi-third-party-001",
  "iss": "https://authorization.greenbuttonalliance.org",
  "exp": 1642262400,
  "iat": 1642258800,
  "scope": "openid profile FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13",
  "client_id": "espi-third-party-001",
  "token_type": "Bearer"
}
```

### Token Revocation

**Endpoint**: `/oauth2/revoke`

**Description**: OAuth2 token revocation endpoint

**Method**: `POST`

**Request**:
```http
POST /oauth2/revoke
Content-Type: application/x-www-form-urlencoded
Authorization: Basic ZXNwaS10aGlyZC1wYXJ0eS0wMDE6c2VjcmV0

token=eyJhbGciOiJSUzI1NiIs...&token_type_hint=access_token
```

**Response**: `200 OK` (empty body)

## Error Handling

### Standard Error Response Format

```json
{
  "error": "error_code",
  "error_description": "Human-readable error description",
  "error_uri": "https://docs.greenbuttonalliance.org/errors/error_code",
  "timestamp": "2024-01-16T15:30:00Z",
  "path": "/api/v1/oauth2/clients",
  "correlationId": "abc-123-def-456"
}
```

### Common Error Codes

#### OAuth2 Errors

| Error Code | HTTP Status | Description |
|------------|-------------|-------------|
| `invalid_request` | 400 | Malformed request |
| `invalid_client` | 401 | Invalid client credentials |
| `invalid_grant` | 400 | Invalid authorization grant |
| `unauthorized_client` | 401 | Client not authorized for grant type |
| `unsupported_grant_type` | 400 | Grant type not supported |
| `invalid_scope` | 400 | Invalid scope value |
| `access_denied` | 403 | User denied authorization |
| `server_error` | 500 | Internal server error |

#### API Errors

| Error Code | HTTP Status | Description |
|------------|-------------|-------------|
| `client_not_found` | 404 | OAuth2 client not found |
| `client_already_exists` | 409 | Client ID already in use |
| `validation_error` | 422 | Request validation failed |
| `datacustodian_error` | 502 | DataCustodian integration error |
| `rate_limit_exceeded` | 429 | Too many requests |
| `insufficient_scope` | 403 | Token lacks required scope |

### Error Response Examples

**Invalid Client**:
```json
{
  "error": "invalid_client",
  "error_description": "Client authentication failed",
  "timestamp": "2024-01-16T15:30:00Z",
  "path": "/oauth2/token",
  "correlationId": "abc-123-def-456"
}
```

**Validation Error**:
```json
{
  "error": "validation_error",
  "error_description": "Request validation failed",
  "timestamp": "2024-01-16T15:30:00Z",
  "path": "/api/v1/oauth2/clients",
  "correlationId": "abc-123-def-456",
  "details": [
    {
      "field": "redirectUris",
      "message": "At least one redirect URI is required"
    },
    {
      "field": "clientName",
      "message": "Client name must be between 1 and 100 characters"
    }
  ]
}
```

## Security Requirements

### TLS Requirements

- **TLS 1.3 ONLY**: NAESB ESPI 4.0 compliance requirement
- **Perfect Forward Secrecy (PFS)**: Required cipher suites
- **Certificate Validation**: Strict certificate chain validation

### Authentication Requirements

#### API Access
- **Bearer Token**: Required for all API endpoints
- **Scope Validation**: Endpoints validate required scopes
- **Rate Limiting**: Applied per client/user

#### Client Authentication
- **client_secret_basic**: HTTP Basic authentication
- **client_secret_post**: Form parameters
- **tls_client_auth**: Certificate-based (ESPI 4.0)

### ESPI 4.0 Security Features

- **Certificate-based Client Authentication**: X.509 client certificates
- **Scope Validation**: ESPI-specific scope format validation
- **Audit Logging**: Comprehensive security event logging
- **Data Minimization**: Only authorized data access

## ESPI Compliance

### ESPI Scope Format

Green Button scopes follow the format:
```
FB=x_y_z;IntervalDuration=nnn;BlockDuration=xxx;HistoryLength=nn
```

**Examples**:
- `FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13`
- `FB=4_5_15;IntervalDuration=900;BlockDuration=daily;HistoryLength=30`

### Scope Parameters

| Parameter | Description | Values |
|-----------|-------------|---------|
| `FB` | Function Block version | `4_5_15`, `4_5_16`, etc. |
| `IntervalDuration` | Data interval in seconds | `900` (15min), `3600` (1hr) |
| `BlockDuration` | Billing period | `monthly`, `daily`, `hourly` |
| `HistoryLength` | Historical data periods | Number of periods |

### Data Rights

Based on granted scopes, the following data rights are available:

| Data Right | Description | Required Scope |
|------------|-------------|----------------|
| `ENERGY_USAGE_DATA` | Basic energy usage | Any FB scope |
| `15_MINUTE_INTERVALS` | 15-minute interval data | `IntervalDuration=900` |
| `HOURLY_INTERVALS` | Hourly interval data | `IntervalDuration=3600` |
| `DAILY_USAGE_DATA` | Daily usage summaries | `BlockDuration=daily` |
| `MONTHLY_BILLING_DATA` | Monthly billing | `BlockDuration=monthly` |
| `ADMIN_ACCESS` | Administrative access | `DataCustodian_Admin_Access` |
| `UPLOAD_ACCESS` | Data upload | `Upload_Admin_Access` |

### Compliance Validation

The Authorization Server validates:
- ESPI scope format syntax
- Client certificate compliance
- TLS 1.3 enforcement
- Data access authorization
- Audit trail completeness

---

## Rate Limiting

All API endpoints are subject to rate limiting:

| Endpoint Category | Rate Limit | Window |
|------------------|------------|---------|
| OAuth2 Token | 100 requests | 1 hour |
| API Management | 1000 requests | 1 hour |
| UserInfo | 500 requests | 1 hour |
| DataCustodian Integration | 200 requests | 1 hour |

Rate limit headers are included in responses:
```http
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 999
X-RateLimit-Reset: 1642262400
```

## SDK and Libraries

### Official SDKs

- **Java**: [OpenESPI-Java-SDK](https://github.com/greenbuttonalliance/openespi-java-sdk)
- **JavaScript/Node.js**: [OpenESPI-JS-SDK](https://github.com/greenbuttonalliance/openespi-js-sdk)
- **Python**: [OpenESPI-Python-SDK](https://github.com/greenbuttonalliance/openespi-python-sdk)

### Example Usage

**Java**:
```java
OpenESPIClient client = new OpenESPIClient("https://authorization.greenbuttonalliance.org");
client.setCredentials("client-id", "client-secret");

ClientResponse response = client.oauth2()
    .clients()
    .list(ClientListRequest.builder()
        .page(0)
        .size(10)
        .espiCompliant(true)
        .build());
```

**JavaScript**:
```javascript
import { OpenESPIClient } from '@greenbuttonalliance/openespi-sdk';

const client = new OpenESPIClient({
  baseURL: 'https://authorization.greenbuttonalliance.org',
  clientId: 'client-id',
  clientSecret: 'client-secret'
});

const clients = await client.oauth2.clients.list({
  page: 0,
  size: 10,
  espiCompliant: true
});
```

---

*This documentation is for OpenESPI Authorization Server v1.0.0 with Spring Boot 3.5 and NAESB ESPI 4.0 compliance.*