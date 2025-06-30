# OIDC UserInfo Endpoint

## Overview

The OpenID Connect UserInfo Endpoint provides user information to authorized clients. It implements the OpenID Connect Core 1.0 specification with Green Button Alliance and NAESB ESPI 4.0 extensions.

## Specification Compliance

- **OpenID Connect Core 1.0**: Section 5.3 (UserInfo Endpoint)
- **NAESB ESPI 4.0**: Green Button Connect My Data compliance
- **Green Button Alliance**: Extensions for energy data access

## Endpoint Information

### URL
```
GET /userinfo
POST /userinfo
```

### Authentication
- **Bearer Token**: OAuth2 access token with `openid` scope
- **Content-Type**: `application/json`

### Supported Scopes
- `openid` (required)
- `profile`
- `email`
- `phone`
- `address`
- ESPI scopes (e.g., `FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13`)

## Request Examples

### GET Request
```http
GET /userinfo HTTP/1.1
Host: auth.greenbuttonalliance.org
Authorization: Bearer eyJhbGciOiJSUzI1NiIs...
Accept: application/json
```

### POST Request
```http
POST /userinfo HTTP/1.1
Host: auth.greenbuttonalliance.org
Authorization: Bearer eyJhbGciOiJSUzI1NiIs...
Content-Type: application/x-www-form-urlencoded

access_token=eyJhbGciOiJSUzI1NiIs...
```

## Response Format

### Standard Claims (profile scope)
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

### Email Claims (email scope)
```json
{
  "sub": "customer@example.com",
  "email": "customer@example.com",
  "email_verified": true
}
```

### ESPI Claims (ESPI scopes)
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

## Claims Reference

### Standard OIDC Claims

| Claim | Scope | Type | Description |
|-------|-------|------|-------------|
| `sub` | openid | string | Subject identifier (required) |
| `name` | profile | string | Full name |
| `given_name` | profile | string | Given name (first name) |
| `family_name` | profile | string | Family name (last name) |
| `preferred_username` | profile | string | Preferred username |
| `email` | email | string | Email address |
| `email_verified` | email | boolean | Email verification status |
| `phone_number` | phone | string | Phone number |
| `phone_number_verified` | phone | boolean | Phone verification status |
| `address` | address | object | Physical mailing address |
| `locale` | profile | string | Locale preference (e.g., en-US) |
| `zoneinfo` | profile | string | Time zone (e.g., America/Los_Angeles) |
| `updated_at` | profile | timestamp | Last profile update time |

### Authorization Claims

| Claim | Type | Description |
|-------|------|-------------|
| `aud` | string | Audience (client ID) |
| `iss` | string | Issuer |
| `iat` | timestamp | Issued at time |
| `exp` | timestamp | Expiration time |
| `auth_time` | timestamp | Authentication time |
| `scope` | string | Granted scopes |

### ESPI-Specific Claims

| Claim | Scope | Type | Description |
|-------|-------|------|-------------|
| `customer_id` | ESPI | string | DataCustodian customer identifier |
| `customer_type` | ESPI | string | Customer type (RESIDENTIAL, COMMERCIAL, etc.) |
| `account_number` | ESPI | string | Utility account number |
| `service_territory` | ESPI | string | Geographic service territory |
| `espi_scopes` | ESPI | array | ESPI-specific scopes granted |
| `datacustodian_grant_id` | ESPI | string | DataCustodian grant identifier |
| `authorized_usage_points` | ESPI | array | Authorized usage point IDs |
| `usage_point_details` | ESPI | array | Detailed usage point information |

### Green Button Alliance Extensions

| Claim | Type | Description |
|-------|------|-------------|
| `gba_version` | string | Green Button Alliance version |
| `espi_version` | string | NAESB ESPI version |
| `data_rights` | array | Granted data access rights |

## Data Rights Values

The `data_rights` claim contains an array of strings indicating the types of data access granted:

- `ENERGY_USAGE_DATA`: Access to energy usage data
- `15_MINUTE_INTERVALS`: 15-minute interval data
- `HOURLY_INTERVALS`: Hourly interval data
- `DAILY_USAGE_DATA`: Daily usage summaries
- `MONTHLY_BILLING_DATA`: Monthly billing information
- `ADMIN_ACCESS`: Administrative access
- `UPLOAD_ACCESS`: Data upload permissions

## Error Responses

### Invalid Token (401 Unauthorized)
```json
{
  "error": "invalid_token",
  "error_description": "The access token is invalid or expired"
}
```

### Insufficient Scope (403 Forbidden)
```json
{
  "error": "insufficient_scope",
  "error_description": "Token missing required openid scope"
}
```

### Server Error (500 Internal Server Error)
```json
{
  "error": "server_error",
  "error_description": "Internal server error"
}
```

## Usage Examples

### JavaScript/Node.js
```javascript
const axios = require('axios');

async function getUserInfo(accessToken) {
  try {
    const response = await axios.get('https://auth.greenbuttonalliance.org/userinfo', {
      headers: {
        'Authorization': `Bearer ${accessToken}`,
        'Accept': 'application/json'
      }
    });
    
    return response.data;
  } catch (error) {
    console.error('UserInfo request failed:', error.response.data);
    throw error;
  }
}

// Usage
const userInfo = await getUserInfo('eyJhbGciOiJSUzI1NiIs...');
console.log('User:', userInfo.name);
console.log('Customer ID:', userInfo.customer_id);
console.log('Usage Points:', userInfo.authorized_usage_points);
```

### Python
```python
import requests

def get_user_info(access_token):
    headers = {
        'Authorization': f'Bearer {access_token}',
        'Accept': 'application/json'
    }
    
    response = requests.get(
        'https://auth.greenbuttonalliance.org/userinfo',
        headers=headers
    )
    
    if response.status_code == 200:
        return response.json()
    else:
        raise Exception(f'UserInfo request failed: {response.text}')

# Usage
user_info = get_user_info('eyJhbGciOiJSUzI1NiIs...')
print(f"User: {user_info['name']}")
print(f"Customer ID: {user_info.get('customer_id')}")
print(f"ESPI Scopes: {user_info.get('espi_scopes', [])}")
```

### Java
```java
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

public class UserInfoClient {
    private final RestTemplate restTemplate = new RestTemplate();
    
    public Map<String, Object> getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("Accept", "application/json");
        
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        
        ResponseEntity<Map> response = restTemplate.exchange(
            "https://auth.greenbuttonalliance.org/userinfo",
            HttpMethod.GET,
            entity,
            Map.class
        );
        
        return response.getBody();
    }
}

// Usage
UserInfoClient client = new UserInfoClient();
Map<String, Object> userInfo = client.getUserInfo("eyJhbGciOiJSUzI1NiIs...");
System.out.println("User: " + userInfo.get("name"));
System.out.println("Customer ID: " + userInfo.get("customer_id"));
```

## Security Considerations

### Token Validation
- Access token must be valid and not expired
- Token must include the `openid` scope
- Token must be associated with an active authorization

### Data Privacy
- Only returns claims for authorized scopes
- Customer data is only returned for ESPI scopes
- Usage point details require appropriate authorization

### Rate Limiting
- UserInfo endpoint is subject to rate limiting
- Clients should cache user information appropriately
- Avoid excessive requests for the same user

## Implementation Details

### Data Sources
The UserInfo endpoint aggregates data from multiple sources:

1. **Local Database**: User profile information
2. **DataCustodian**: Customer and usage point data
3. **Authorization Context**: Token and scope information
4. **Static Configuration**: GBA and ESPI version information

### Caching Strategy
- User information is cached for 5 minutes
- Cache is invalidated when user data is updated
- DataCustodian integration provides real-time updates

### Performance Optimization
- Efficient database queries with proper indexing
- Lazy loading of usage point details
- Concurrent data fetching from multiple sources

## Configuration

### Application Properties
```yaml
oidc:
  userinfo:
    cache-expiry-minutes: 5
    include-usage-point-details: true
    datacustodian-integration: true
    gba-extensions: true
```

### Custom Claims
Additional claims can be configured through the `oidc_claims_mapping` table:

```sql
INSERT INTO oidc_claims_mapping (scope, claim_name, claim_description, essential, data_source, espi_specific)
VALUES ('custom_scope', 'custom_claim', 'Custom claim description', false, 'user_info', false);
```

## Testing

### Unit Tests
```java
@Test
public void testUserInfoWithESPIScopes() {
    // Test UserInfo response with ESPI scopes
    String accessToken = createTokenWithScopes("openid", "profile", "FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13");
    
    ResponseEntity<Map> response = restTemplate.exchange(
        "/userinfo",
        HttpMethod.GET,
        createAuthenticatedRequest(accessToken),
        Map.class
    );
    
    assertEquals(HttpStatus.OK, response.getStatusCode());
    Map<String, Object> userInfo = response.getBody();
    
    assertNotNull(userInfo.get("sub"));
    assertNotNull(userInfo.get("customer_id"));
    assertNotNull(userInfo.get("espi_scopes"));
    assertNotNull(userInfo.get("data_rights"));
}
```

### Integration Tests
```java
@Test
public void testUserInfoWithDataCustodianIntegration() {
    // Test integration with DataCustodian
    String customerId = "customer-123";
    mockDataCustodianService.setupCustomer(customerId);
    
    String accessToken = createTokenForCustomer(customerId);
    
    ResponseEntity<Map> response = getUserInfo(accessToken);
    
    assertEquals(HttpStatus.OK, response.getStatusCode());
    Map<String, Object> userInfo = response.getBody();
    
    assertEquals(customerId, userInfo.get("customer_id"));
    assertNotNull(userInfo.get("usage_point_details"));
}
```

## Monitoring and Metrics

### Key Metrics
- UserInfo request count and latency
- Error rates by error type
- Cache hit/miss ratios
- DataCustodian integration success rates

### Logging
- Request/response logging (excluding sensitive data)
- Performance metrics logging
- Error and exception logging
- Audit trail for data access

## Troubleshooting

### Common Issues

1. **Missing openid scope**
   - Ensure access token includes `openid` scope
   - Check client configuration for scope grants

2. **No customer data returned**
   - Verify ESPI scopes are included in token
   - Check DataCustodian integration status
   - Confirm customer exists in DataCustodian

3. **Performance issues**
   - Check database query performance
   - Monitor DataCustodian response times
   - Verify cache configuration

### Debug Logging
```yaml
logging:
  level:
    org.greenbuttonalliance.espi.authserver.controller.UserInfoController: DEBUG
    org.greenbuttonalliance.espi.authserver.service.UserInfoService: DEBUG
```

## Future Enhancements

- Support for additional OIDC features (aggregated claims, distributed claims)
- Enhanced ESPI claim mappings
- Real-time user data synchronization
- Advanced caching strategies
- Support for custom claim providers