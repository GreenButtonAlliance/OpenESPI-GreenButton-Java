# DataCustodian Integration

## Overview

The DataCustodian Integration module provides seamless communication between the OpenESPI Authorization Server and OpenESPI DataCustodian applications. This integration enables:

- User authentication verification
- Retail customer management
- Usage point authorization
- Data access scope validation
- Real-time synchronization of authorization grants

## Architecture

```
┌─────────────────────┐    ┌─────────────────────┐    ┌─────────────────────┐
│   Third Party       │    │  Authorization      │    │   DataCustodian     │
│   Application       │    │     Server          │    │                     │
├─────────────────────┤    ├─────────────────────┤    ├─────────────────────┤
│ 1. Request Auth     │───▶│ 2. Verify User      │───▶│ 3. Authenticate     │
│ 4. Receive Token    │◀───│ 5. Create Token     │◀───│ 6. Authorize        │
│ 7. Access Data      │───▶│ 8. Validate Token   │───▶│ 9. Serve Data       │
└─────────────────────┘    └─────────────────────┘    └─────────────────────┘
```

## Configuration

### Application Properties

```yaml
espi:
  datacustodian:
    base-url: "http://localhost:8080/DataCustodian"
    admin:
      client-id: "data_custodian_admin"
      client-secret: "${DATACUSTODIAN_ADMIN_SECRET}"
    connection-timeout: 5000
    read-timeout: 10000
    max-retries: 3
    retry-delay: 2000
    health-check-interval: 300
    health-log-retention-days: 30
    api-log-retention-days: 7
    enable-health-checks: true
    enable-api-logging: true
    enable-retries: true
    validate-ssl-certificates: true
```

### Environment Variables

```bash
# DataCustodian connection settings
DATACUSTODIAN_BASE_URL=https://datacustodian.example.com
DATACUSTODIAN_ADMIN_CLIENT_ID=data_custodian_admin
DATACUSTODIAN_ADMIN_SECRET=your-admin-secret

# SSL/TLS settings
DATACUSTODIAN_VALIDATE_SSL=true
DATACUSTODIAN_CONNECTION_TIMEOUT=5000
DATACUSTODIAN_READ_TIMEOUT=10000
```

## API Endpoints

### Base URL
```
/api/v1/datacustodian
```

### 1. User Verification

Verify user credentials with the DataCustodian.

**Endpoint:** `POST /api/v1/datacustodian/verify-user`

**Request:**
```json
{
  "username": "customer@example.com",
  "password": "user-password"
}
```

**Response:**
```json
{
  "valid": true,
  "user_id": "customer-123",
  "username": "customer@example.com",
  "email": "customer@example.com",
  "roles": ["ROLE_CUSTOMER"],
  "customer_type": "RESIDENTIAL"
}
```

### 2. Customer Information

Get retail customer details.

**Endpoint:** `GET /api/v1/datacustodian/customers/{customerId}`

**Response:**
```json
{
  "customer_id": "customer-123",
  "username": "customer@example.com",
  "first_name": "John",
  "last_name": "Doe",
  "email": "customer@example.com",
  "customer_type": "RESIDENTIAL",
  "account_number": "ACC-789456",
  "service_territory": "Northern California"
}
```

### 3. Usage Points

Get usage points for a customer.

**Endpoint:** `GET /api/v1/datacustodian/customers/{customerId}/usage-points`

**Response:**
```json
{
  "customer_id": "customer-123",
  "total_count": 2,
  "usage_points": [
    {
      "usage_point_id": "up-001",
      "usage_point_uuid": "550e8400-e29b-41d4-a716-446655440000",
      "service_category": "ELECTRICITY",
      "service_kind": "ENERGY",
      "customer_id": "customer-123",
      "meter_number": "MTR-001",
      "service_address": "123 Main St, Anytown, CA 94000",
      "status": "ACTIVE"
    },
    {
      "usage_point_id": "up-002",
      "usage_point_uuid": "550e8400-e29b-41d4-a716-446655440001",
      "service_category": "GAS",
      "service_kind": "ENERGY",
      "customer_id": "customer-123",
      "meter_number": "MTR-002",
      "service_address": "123 Main St, Anytown, CA 94000",
      "status": "ACTIVE"
    }
  ]
}
```

### 4. Scope Validation

Validate access scope for specific usage points.

**Endpoint:** `POST /api/v1/datacustodian/validate-scope`

**Request:**
```json
{
  "customer_id": "customer-123",
  "scope": "FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13",
  "usage_point_ids": ["up-001", "up-002"]
}
```

**Response:**
```json
{
  "valid": true,
  "granted_scope": "FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13",
  "authorized_usage_points": ["up-001", "up-002"],
  "denied_usage_points": [],
  "reasons": []
}
```

### 5. Authorization Grants

Create authorization grants in the DataCustodian.

**Endpoint:** `POST /api/v1/datacustodian/grants`

**Request:**
```json
{
  "customer_id": "customer-123",
  "client_id": "third_party_app",
  "scope": "FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13",
  "usage_point_ids": ["up-001", "up-002"]
}
```

**Response:**
```json
{
  "grant_id": "grant-456",
  "customer_id": "customer-123",
  "client_id": "third_party_app",
  "scope": "FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13",
  "usage_point_ids": ["up-001", "up-002"],
  "granted_at": "2024-01-15T10:30:00Z",
  "expires_at": "2025-01-15T10:30:00Z"
}
```

### 6. Health Status

Check DataCustodian health and connectivity.

**Endpoint:** `GET /api/v1/datacustodian/health`

**Response:**
```json
{
  "status": "UP",
  "version": "1.4.0",
  "uptime": 86400000,
  "database_connected": true,
  "active_connections": 15,
  "total_customers": 10000,
  "total_usage_points": 15000
}
```

## Database Schema

### Integration Mapping Table

```sql
CREATE TABLE datacustodian_integration_mapping (
    id BIGINT PRIMARY KEY,
    authorization_id VARCHAR(100) UNIQUE NOT NULL,
    grant_id VARCHAR(100) NOT NULL,
    customer_id VARCHAR(100) NOT NULL,
    usage_point_ids JSON,
    data_custodian_url VARCHAR(500),
    integration_status VARCHAR(20) DEFAULT 'active',
    last_sync_at TIMESTAMP,
    sync_error_count INTEGER DEFAULT 0,
    last_sync_error TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Health Monitoring Table

```sql
CREATE TABLE datacustodian_health_log (
    id BIGINT PRIMARY KEY,
    data_custodian_url VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL,
    version VARCHAR(50),
    uptime BIGINT,
    database_connected BOOLEAN,
    active_connections INTEGER,
    total_customers INTEGER,
    total_usage_points INTEGER,
    response_time_ms INTEGER,
    error_message TEXT,
    checked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### API Call Logging Table

```sql
CREATE TABLE datacustodian_api_log (
    id BIGINT PRIMARY KEY,
    integration_mapping_id BIGINT,
    api_endpoint VARCHAR(200) NOT NULL,
    http_method VARCHAR(10) NOT NULL,
    request_data JSON,
    response_status INTEGER,
    response_data JSON,
    response_time_ms INTEGER,
    error_message TEXT,
    called_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## Service Classes

### DataCustodianIntegrationService

Main service class for DataCustodian communication:

```java
@Service
public class DataCustodianIntegrationService {
    
    // User verification
    public UserVerificationResult verifyUser(String username, String password);
    
    // Customer management
    public RetailCustomerInfo getRetailCustomer(String customerId);
    public List<UsagePointInfo> getUsagePoints(String customerId);
    
    // Authorization management
    public ScopeValidationResult validateScopeAccess(String customerId, String scope, List<String> usagePointIds);
    public AuthorizationGrantResult createAuthorizationGrant(String customerId, String clientId, String scope, List<String> usagePointIds);
    public boolean revokeAuthorizationGrant(String grantId, String reason);
    
    // Health monitoring
    public DataCustodianHealth getHealthStatus();
    
    // Integration mapping
    public void storeIntegrationMapping(String authorizationId, String grantId, String customerId, List<String> usagePointIds);
    public IntegrationMapping getIntegrationMapping(String authorizationId);
}
```

## Security Considerations

### Authentication

- Admin API calls use client credentials authentication
- All communications should use HTTPS in production
- Client secrets must be stored securely

### Authorization

- Only authenticated admin users can access integration endpoints
- Role-based access control (RBAC) is enforced
- API calls are logged for audit purposes

### Data Protection

- Customer data is encrypted in transit
- Sensitive information is not logged
- Integration mappings are stored securely

## Error Handling

### Retry Logic

The integration service implements exponential backoff retry logic:

```yaml
espi:
  datacustodian:
    max-retries: 3
    retry-delay: 2000  # Initial delay in milliseconds
    enable-retries: true
```

### Circuit Breaker

For production deployments, consider implementing a circuit breaker pattern to handle DataCustodian failures gracefully.

### Error Responses

Standard error format:

```json
{
  "status": "error",
  "error": "connection_failed",
  "message": "Unable to connect to DataCustodian"
}
```

Common error codes:
- `connection_failed`: Network connectivity issues
- `authentication_failed`: Admin credentials invalid
- `customer_not_found`: Customer ID not found in DataCustodian
- `scope_validation_failed`: Requested scope not valid
- `grant_creation_failed`: Unable to create authorization grant

## Monitoring and Metrics

### Health Checks

Automated health checks run every 5 minutes (configurable):

- DataCustodian connectivity
- Database connection status
- Response time monitoring
- Error rate tracking

### Metrics Collection

The service collects the following metrics:

- API call success/failure rates
- Response times
- Customer verification rates
- Grant creation/revocation counts
- Health check results

### Alerting

Set up alerts for:

- DataCustodian unavailability
- High error rates
- Slow response times
- Authentication failures

## Development and Testing

### Local Development

For local development, use the test DataCustodian instance:

```yaml
espi:
  datacustodian:
    base-url: "http://localhost:8080/DataCustodian"
    admin:
      client-id: "data_custodian_admin"
      client-secret: "dev-secret"
```

### Testing

Integration tests should cover:

- User verification scenarios
- Customer data retrieval
- Usage point authorization
- Error handling and retries
- Health check functionality

### Mock DataCustodian

For unit testing, implement a mock DataCustodian service that simulates the API responses.

## Deployment

### Production Configuration

```yaml
espi:
  datacustodian:
    base-url: "https://datacustodian.production.com"
    admin:
      client-id: "prod_admin_client"
      client-secret: "${DATACUSTODIAN_ADMIN_SECRET}"
    connection-timeout: 10000
    read-timeout: 30000
    validate-ssl-certificates: true
    enable-health-checks: true
    enable-api-logging: true
```

### Environment Variables

Required environment variables for production:

```bash
DATACUSTODIAN_BASE_URL=https://datacustodian.production.com
DATACUSTODIAN_ADMIN_CLIENT_ID=prod_admin_client
DATACUSTODIAN_ADMIN_SECRET=secure-admin-secret
```

### Load Balancing

For high availability, deploy multiple DataCustodian instances behind a load balancer and configure the base URL to point to the load balancer.

## Troubleshooting

### Common Issues

1. **Connection Timeouts**
   - Check network connectivity
   - Verify DataCustodian is running
   - Increase timeout values if needed

2. **Authentication Failures**
   - Verify admin client credentials
   - Check DataCustodian admin user configuration

3. **Customer Not Found**
   - Ensure customer exists in DataCustodian
   - Verify customer ID format

4. **Scope Validation Errors**
   - Check ESPI scope format
   - Verify usage point ownership

### Diagnostic Tools

Use the health endpoint to diagnose issues:

```bash
curl -X GET "http://localhost:8080/api/v1/datacustodian/health"
```

Check integration mapping status:

```bash
curl -X GET "http://localhost:8080/api/v1/datacustodian/mappings/{authorizationId}"
```

### Logging

Enable debug logging for detailed troubleshooting:

```yaml
logging:
  level:
    org.greenbuttonalliance.espi.authserver.service.DataCustodianIntegrationService: DEBUG
```

## Support

For issues with DataCustodian integration:

1. Check the health endpoint first
2. Review application logs for errors
3. Verify DataCustodian connectivity
4. Contact the DataCustodian administrator
5. Refer to the NAESB ESPI specification for scope formats