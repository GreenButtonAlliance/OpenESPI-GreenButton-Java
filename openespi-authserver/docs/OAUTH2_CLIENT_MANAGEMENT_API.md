# OAuth2 Client Management API

## Overview

The OAuth2 Client Management API provides comprehensive CRUD operations for managing OAuth2 clients in the ESPI 4.0 Authorization Server. This API is designed for enterprise-grade deployments with support for bulk operations, metrics analytics, and NAESB ESPI 4.0 compliance.

## Authentication

All endpoints require `ROLE_ADMIN` or `ROLE_DC_ADMIN` authority.

## Base URL

```
/api/v1/oauth2/clients
```

## Endpoints

### 1. List Clients

Retrieve a paginated list of OAuth2 clients with filtering and sorting capabilities.

**Endpoint:** `GET /api/v1/oauth2/clients`

**Parameters:**
- `page` (optional): Page number (default: 0)
- `size` (optional): Page size (default: 20, max: 100)
- `sortBy` (optional): Sort field (default: client_id)
- `sortDirection` (optional): Sort direction - asc/desc (default: asc)
- `status` (optional): Filter by client status - active/suspended/revoked
- `securityLevel` (optional): Filter by security level - standard/high/maximum
- `espiVersion` (optional): Filter by ESPI version
- `search` (optional): Search in client name and client ID

**Example Request:**
```http
GET /api/v1/oauth2/clients?page=0&size=10&status=active&search=utility
Authorization: Bearer <access_token>
```

**Example Response:**
```json
{
  "clients": [
    {
      "id": "client-uuid-123",
      "client_id": "utility_admin_portal",
      "client_name": "Utility Admin Portal",
      "authentication_methods": ["client_secret_basic"],
      "grant_types": ["client_credentials"],
      "redirect_uris": [],
      "scopes": ["DataCustodian_Admin_Access", "Upload_Admin_Access"],
      "espi_version": "4.0",
      "security_level": "maximum",
      "status": "active",
      "rate_limit_per_minute": 1000,
      "max_concurrent_sessions": 50,
      "created_by": "migration_v3_0_0",
      "last_used_at": "2024-01-15T10:30:00Z",
      "failure_count": 0,
      "business_category": "Electric Utility",
      "certification_status": "self_certified",
      "service_territory": "State of California"
    }
  ],
  "page": 0,
  "size": 10,
  "total_elements": 5,
  "total_pages": 1
}
```

### 2. Create Client

Create a new OAuth2 client with ESPI 4.0 compliance features.

**Endpoint:** `POST /api/v1/oauth2/clients`

**Request Body:**
```json
{
  "client_name": "Green Button Connect My Data",
  "client_name_prefix": "gbconnect",
  "client_description": "Official Green Button Connect My Data application for secure energy data sharing",
  "authentication_methods": ["client_secret_basic"],
  "grant_types": ["authorization_code", "refresh_token"],
  "redirect_uris": [
    "https://gbconnect.example.com/oauth/callback",
    "https://gbconnect.example.com/oauth/callback2"
  ],
  "post_logout_redirect_uris": [
    "https://gbconnect.example.com/logout"
  ],
  "scopes": [
    "openid",
    "profile",
    "FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13",
    "FB=4_5_16;IntervalDuration=900;BlockDuration=daily;HistoryLength=7"
  ],
  "require_proof_key": true,
  "require_authorization_consent": true,
  "application_type": "WEB",
  "security_level": "high",
  "security_classification": "internal",
  "certification_status": "gba_certified",
  "business_category": "Energy Data Platform",
  "service_territory": "North America",
  "rate_limit_per_minute": 200,
  "max_concurrent_sessions": 10,
  "created_by": "admin_user"
}
```

**Response:**
```json
{
  "client_id": "gbconnect_1703505000_a1b2c3d4",
  "client_secret": "aB3fG9kL2mN5qR8tV1wX4yZ7",
  "client_name": "Green Button Connect My Data",
  "created_at": "2024-01-15T12:00:00Z",
  "espi_version": "4.0"
}
```

### 3. Get Client Details

Retrieve detailed information about a specific client.

**Endpoint:** `GET /api/v1/oauth2/clients/{clientId}`

**Example Response:**
```json
{
  "id": "client-uuid-123",
  "client_id": "gbconnect_1703505000_a1b2c3d4",
  "client_name": "Green Button Connect My Data",
  "authentication_methods": ["client_secret_basic"],
  "grant_types": ["authorization_code", "refresh_token"],
  "redirect_uris": [
    "https://gbconnect.example.com/oauth/callback",
    "https://gbconnect.example.com/oauth/callback2"
  ],
  "scopes": [
    "openid",
    "profile",
    "FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13"
  ],
  "espi_version": "4.0",
  "security_level": "high",
  "status": "active",
  "rate_limit_per_minute": 200,
  "max_concurrent_sessions": 10,
  "created_by": "admin_user",
  "last_used_at": "2024-01-15T11:45:00Z",
  "failure_count": 0,
  "business_category": "Energy Data Platform",
  "certification_status": "gba_certified",
  "service_territory": "North America"
}
```

### 4. Update Client

Update an existing client's configuration.

**Endpoint:** `PUT /api/v1/oauth2/clients/{clientId}`

**Request Body:**
```json
{
  "client_name": "Updated Client Name",
  "redirect_uris": [
    "https://new-redirect.example.com/callback"
  ],
  "scopes": [
    "openid",
    "profile",
    "FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13"
  ],
  "security_level": "maximum",
  "rate_limit_per_minute": 500,
  "max_concurrent_sessions": 20,
  "updated_by": "admin_user"
}
```

**Response:**
```json
{
  "status": "success",
  "message": "Client updated successfully"
}
```

### 5. Update Client Status

Update a client's status (activate, suspend, revoke).

**Endpoint:** `PUT /api/v1/oauth2/clients/{clientId}/status`

**Request Body:**
```json
{
  "status": "suspended",
  "reason": "Security investigation in progress",
  "lock_duration_minutes": 1440,
  "updated_by": "security_admin"
}
```

**Response:**
```json
{
  "status": "success",
  "message": "Client status updated successfully"
}
```

### 6. Get Client Metrics

Retrieve usage analytics and metrics for a specific client.

**Endpoint:** `GET /api/v1/oauth2/clients/{clientId}/metrics`

**Parameters:**
- `days` (optional): Number of days to retrieve metrics for (default: 30, max: 365)

**Example Response:**
```json
{
  "client_id": "gbconnect_1703505000_a1b2c3d4",
  "period_days": 30,
  "summary": {
    "total_requests": 15420,
    "total_successful_requests": 15275,
    "total_failed_requests": 145,
    "total_tokens_issued": 1850,
    "success_rate": 99.06,
    "average_response_time": 85.5,
    "total_unique_users": 342
  },
  "daily_metrics": [
    {
      "date": "2024-01-15",
      "total_requests": 150,
      "successful_requests": 145,
      "failed_requests": 5,
      "total_tokens_issued": 25,
      "avg_response_time_ms": 85.5,
      "unique_users_served": 12,
      "espi_data_requests": 120,
      "consent_grants": 3,
      "consent_withdrawals": 0
    }
  ]
}
```

### 7. Bulk Operations

Perform operations on multiple clients simultaneously.

**Endpoint:** `POST /api/v1/oauth2/clients/bulk`

**Request Body:**
```json
{
  "client_ids": [
    "client_1",
    "client_2",
    "client_3"
  ],
  "operation": "suspend",
  "reason": "Maintenance window"
}
```

**Supported Operations:**
- `suspend`: Suspend clients
- `activate`: Activate clients
- `delete`: Delete clients (use with caution)

**Response:**
```json
{
  "operation": "suspend",
  "total_clients": 3,
  "success_count": 3,
  "failure_count": 0,
  "results": [
    {
      "client_id": "client_1",
      "success": true,
      "message": "Client suspended"
    },
    {
      "client_id": "client_2",
      "success": true,
      "message": "Client suspended"
    },
    {
      "client_id": "client_3",
      "success": true,
      "message": "Client suspended"
    }
  ]
}
```

## ESPI 4.0 Compliance Features

### Security Levels

- **standard**: Basic security requirements
- **high**: Enhanced security with additional monitoring
- **maximum**: Highest security level with strict controls

### Security Classifications

- **public**: Public information
- **internal**: Internal organizational use
- **confidential**: Sensitive business information
- **restricted**: Highly sensitive, restricted access

### Certification Status

- **self_certified**: Self-certified by the organization
- **third_party_certified**: Certified by an independent third party
- **gba_certified**: Certified by the Green Button Alliance

### Rate Limiting

All clients have configurable rate limits:
- Minimum: 1 request per minute
- Maximum: 10,000 requests per minute
- Default: 100 requests per minute

### Session Management

Concurrent session limits:
- Minimum: 1 session
- Maximum: 1,000 sessions
- Default: 5 sessions

## Error Responses

### Validation Errors (400 Bad Request)
```json
{
  "status": "error",
  "error": "invalid_request",
  "message": "Client name is required"
}
```

### Not Found (404)
```json
{
  "status": "error",
  "error": "not_found",
  "message": "Client not found"
}
```

### Server Error (500)
```json
{
  "status": "error",
  "error": "server_error",
  "message": "Internal server error"
}
```

## Best Practices

### 1. Client Secret Security
- Store client secrets securely
- Rotate secrets regularly
- Never log or expose secrets in responses

### 2. Rate Limiting
- Set appropriate rate limits based on expected usage
- Monitor rate limit violations
- Implement exponential backoff for retries

### 3. Monitoring
- Use the metrics endpoints for monitoring
- Set up alerts for security violations
- Track success rates and response times

### 4. Bulk Operations
- Use bulk operations for efficiency
- Test operations on a small subset first
- Monitor results carefully

### 5. ESPI Compliance
- Use ESPI-compliant scopes
- Set appropriate security levels
- Maintain proper certification status

## Configuration

### Application Properties

```yaml
espi:
  oauth2:
    client-management:
      default-rate-limit-per-minute: 100
      default-max-concurrent-sessions: 5
      max-failures-before-lock: 10
      default-lock-duration-minutes: 60
      metrics-retention-days: 365
      enable-automatic-metrics-calculation: true
      enable-automatic-cleanup: true
      enable-security-monitoring: true
```

### Database Requirements

The API requires the following database tables:
- `oauth2_registered_client` (enhanced with ESPI 4.0 columns)
- `oauth2_audit_log`
- `oauth2_client_metrics`
- `oauth2_consent_details`
- `oauth2_token_usage`
- `espi_application_info`

Refer to the Flyway migration scripts for complete schema definitions.

## Security Considerations

1. **Authentication**: All endpoints require admin-level authentication
2. **Authorization**: Role-based access control (RBAC)
3. **Audit Logging**: All operations are logged for compliance
4. **Rate Limiting**: Built-in protection against abuse
5. **Input Validation**: Comprehensive validation of all inputs
6. **TLS 1.3**: All communications must use TLS 1.3 as per ESPI 4.0

## Support

For questions about this API, please contact the Green Button Alliance development team or refer to the NAESB ESPI 4.0 specification.