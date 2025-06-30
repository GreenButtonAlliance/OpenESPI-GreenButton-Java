# OpenESPI Authorization Server Integration Guide

## Overview

This guide provides comprehensive instructions for integrating with the OpenESPI Authorization Server, including OAuth2 flows, ESPI compliance requirements, and practical implementation examples.

## Table of Contents

1. [Quick Start](#quick-start)
2. [Authentication Flows](#authentication-flows)
3. [Client Registration](#client-registration)
4. [ESPI Scope Implementation](#espi-scope-implementation)
5. [Integration Examples](#integration-examples)
6. [Testing and Validation](#testing-and-validation)
7. [Troubleshooting](#troubleshooting)

## Quick Start

### Prerequisites

- TLS 1.3 support in your application
- Valid X.509 client certificate (for ESPI compliance)
- Understanding of OAuth2 Authorization Code flow
- Green Button Alliance membership (for production)

### Basic Integration Steps

1. **Register your client application**
2. **Implement OAuth2 Authorization Code flow**
3. **Handle ESPI-specific scopes**
4. **Integrate with UserInfo endpoint**
5. **Test compliance requirements**

## Authentication Flows

### 1. Authorization Code Flow (Standard)

#### Step 1: Authorization Request
```http
GET /oauth2/authorize?
  response_type=code&
  client_id=your-client-id&
  redirect_uri=https://yourapp.com/callback&
  scope=openid%20profile%20FB%3D4_5_15%3BIntervalDuration%3D3600%3BBlockDuration%3Dmonthly%3BHistoryLength%3D13&
  state=random-state-value&
  code_challenge=dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk&
  code_challenge_method=S256
```

#### Step 2: User Authentication and Consent
The user will be redirected to the authorization server where they:
1. Authenticate (if not already authenticated)
2. Review requested permissions
3. Grant or deny consent

#### Step 3: Authorization Response
```http
HTTP/1.1 302 Found
Location: https://yourapp.com/callback?
  code=authorization-code-value&
  state=random-state-value
```

#### Step 4: Token Exchange
```http
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded
Authorization: Basic base64(client_id:client_secret)

grant_type=authorization_code&
code=authorization-code-value&
redirect_uri=https://yourapp.com/callback&
code_verifier=dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk
```

#### Step 5: Token Response
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

### 2. Certificate-based Authentication (ESPI 4.0)

For ESPI 4.0 compliance, client authentication can use X.509 certificates:

#### Client Configuration
```json
{
  "clientId": "espi-cert-client-001",
  "clientAuthenticationMethods": ["tls_client_auth"],
  "certificateSubjectDN": "CN=your-client,OU=ESPI,O=Your Organization",
  "espiCompliant": true
}
```

#### Token Request with Certificate
```http
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded
# TLS client certificate used for authentication

grant_type=authorization_code&
code=authorization-code-value&
redirect_uri=https://yourapp.com/callback
```

## Client Registration

### 1. Manual Registration (Development)

Submit a client registration request with the following information:

```json
{
  "clientName": "My Energy App",
  "redirectUris": [
    "https://myapp.example.com/oauth/callback"
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

### 2. API-based Registration (Production)

Use the Client Management API for programmatic registration:

```http
POST /api/v1/oauth2/clients
Authorization: Bearer your-admin-token
Content-Type: application/json

{
  "clientName": "Production Energy App",
  "redirectUris": ["https://app.example.com/callback"],
  "scopes": ["openid", "profile", "FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13"],
  "espiCompliant": true,
  "securityLevel": "HIGH"
}
```

### 3. Certificate-based Client Registration

For ESPI 4.0 compliance with certificate authentication:

```json
{
  "clientName": "ESPI Certified App",
  "clientAuthenticationMethods": ["tls_client_auth"],
  "certificateSubjectDN": "CN=espi-app,OU=Energy,O=Utility Corp",
  "certificateSerialNumber": "123456789ABCDEF",
  "certificateIssuerDN": "CN=ESPI CA,O=Green Button Alliance",
  "espiCompliant": true,
  "securityLevel": "HIGH"
}
```

## ESPI Scope Implementation

### Understanding ESPI Scopes

ESPI scopes follow the format:
```
FB=x_y_z;IntervalDuration=nnn;BlockDuration=xxx;HistoryLength=nn
```

### Common ESPI Scope Examples

#### 1. Hourly Data with Monthly Billing
```
FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13
```
- **FB=4_5_15**: ESPI 4.0, version 5, release 15
- **IntervalDuration=3600**: 1-hour intervals
- **BlockDuration=monthly**: Monthly billing periods
- **HistoryLength=13**: 13 months of history

#### 2. 15-minute Data with Daily Billing
```
FB=4_5_15;IntervalDuration=900;BlockDuration=daily;HistoryLength=30
```
- **IntervalDuration=900**: 15-minute intervals
- **BlockDuration=daily**: Daily summaries
- **HistoryLength=30**: 30 days of history

#### 3. Daily Data with Monthly Billing
```
FB=4_5_15;IntervalDuration=86400;BlockDuration=monthly;HistoryLength=24
```
- **IntervalDuration=86400**: Daily intervals (24 hours)
- **HistoryLength=24**: 24 months of history

### Scope Validation

Before requesting a scope, validate its format:

```javascript
function validateESPIScope(scope) {
  const pattern = /^FB=[0-9]+_[0-9]+_[0-9]+;IntervalDuration=[0-9]+;BlockDuration=(monthly|daily|hourly);HistoryLength=[0-9]+$/;
  return pattern.test(scope);
}

// Example usage
const scope = "FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13";
if (validateESPIScope(scope)) {
  console.log("Valid ESPI scope");
} else {
  console.log("Invalid ESPI scope format");
}
```

### Requesting Multiple Scopes

You can request multiple scopes in a single authorization request:

```javascript
const scopes = [
  "openid",
  "profile", 
  "email",
  "FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13",
  "FB=4_5_15;IntervalDuration=900;BlockDuration=daily;HistoryLength=7"
];

const scopeString = scopes.join(" ");
// Result: "openid profile email FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13 FB=4_5_15;IntervalDuration=900;BlockDuration=daily;HistoryLength=7"
```

## Integration Examples

### 1. JavaScript/Node.js Integration

#### OAuth2 Client Setup
```javascript
const { AuthorizationCode } = require('simple-oauth2');

const config = {
  client: {
    id: 'your-client-id',
    secret: 'your-client-secret'
  },
  auth: {
    tokenHost: 'https://authorization.greenbuttonalliance.org',
    tokenPath: '/oauth2/token',
    authorizePath: '/oauth2/authorize'
  }
};

const client = new AuthorizationCode(config);
```

#### Authorization URL Generation
```javascript
const authorizationUri = client.authorizeURL({
  redirect_uri: 'https://yourapp.com/callback',
  scope: 'openid profile FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13',
  state: generateRandomState()
});

console.log('Redirect to:', authorizationUri);
```

#### Token Exchange
```javascript
async function handleCallback(req, res) {
  const { code, state } = req.query;
  
  try {
    const result = await client.getToken({
      code: code,
      redirect_uri: 'https://yourapp.com/callback'
    });
    
    const accessToken = result.token.access_token;
    
    // Get user info
    const userInfo = await getUserInfo(accessToken);
    console.log('User info:', userInfo);
    
  } catch (error) {
    console.error('Token exchange failed:', error);
  }
}
```

#### UserInfo Request
```javascript
async function getUserInfo(accessToken) {
  const response = await fetch('https://authorization.greenbuttonalliance.org/userinfo', {
    headers: {
      'Authorization': `Bearer ${accessToken}`,
      'Accept': 'application/json'
    }
  });
  
  if (!response.ok) {
    throw new Error(`UserInfo request failed: ${response.status}`);
  }
  
  return await response.json();
}
```

### 2. Java Integration

#### Spring Boot OAuth2 Client
```java
@Configuration
@EnableWebSecurity
public class OAuth2Config {
    
    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        return new InMemoryClientRegistrationRepository(
            espiClientRegistration()
        );
    }
    
    private ClientRegistration espiClientRegistration() {
        return ClientRegistration.withRegistrationId("espi")
            .clientId("your-client-id")
            .clientSecret("your-client-secret")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
            .scope("openid", "profile", "FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13")
            .authorizationUri("https://authorization.greenbuttonalliance.org/oauth2/authorize")
            .tokenUri("https://authorization.greenbuttonalliance.org/oauth2/token")
            .userInfoUri("https://authorization.greenbuttonalliance.org/userinfo")
            .userNameAttributeName("sub")
            .clientName("ESPI Client")
            .build();
    }
}
```

#### OAuth2 Controller
```java
@RestController
public class OAuth2Controller {
    
    @Autowired
    private OAuth2AuthorizedClientService authorizedClientService;
    
    @GetMapping("/user")
    public Map<String, Object> user(OAuth2AuthenticationToken authentication) {
        OAuth2AuthorizedClient authorizedClient = authorizedClientService
            .loadAuthorizedClient("espi", authentication.getName());
        
        return getUserInfo(authorizedClient.getAccessToken().getTokenValue());
    }
    
    private Map<String, Object> getUserInfo(String accessToken) {
        WebClient webClient = WebClient.builder()
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .build();
        
        return webClient.get()
            .uri("https://authorization.greenbuttonalliance.org/userinfo")
            .retrieve()
            .bodyToMono(Map.class)
            .block();
    }
}
```

### 3. Python Integration

#### OAuth2 Client with requests-oauthlib
```python
from requests_oauthlib import OAuth2Session
import requests

class ESPIOAuth2Client:
    def __init__(self, client_id, client_secret, redirect_uri):
        self.client_id = client_id
        self.client_secret = client_secret
        self.redirect_uri = redirect_uri
        self.authorization_base_url = 'https://authorization.greenbuttonalliance.org/oauth2/authorize'
        self.token_url = 'https://authorization.greenbuttonalliance.org/oauth2/token'
        self.userinfo_url = 'https://authorization.greenbuttonalliance.org/userinfo'
    
    def get_authorization_url(self, scopes):
        oauth = OAuth2Session(self.client_id, scope=scopes, redirect_uri=self.redirect_uri)
        authorization_url, state = oauth.authorization_url(self.authorization_base_url)
        return authorization_url, state
    
    def get_token(self, authorization_response, state):
        oauth = OAuth2Session(self.client_id, state=state, redirect_uri=self.redirect_uri)
        token = oauth.fetch_token(
            self.token_url,
            authorization_response=authorization_response,
            client_secret=self.client_secret
        )
        return token
    
    def get_user_info(self, token):
        headers = {
            'Authorization': f'Bearer {token["access_token"]}',
            'Accept': 'application/json'
        }
        response = requests.get(self.userinfo_url, headers=headers)
        response.raise_for_status()
        return response.json()
```

#### Usage Example
```python
# Initialize client
client = ESPIOAuth2Client(
    client_id='your-client-id',
    client_secret='your-client-secret',
    redirect_uri='https://yourapp.com/callback'
)

# Get authorization URL
scopes = [
    'openid',
    'profile',
    'FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13'
]
auth_url, state = client.get_authorization_url(scopes)
print(f'Please go to {auth_url} and authorize access.')

# After user authorization, exchange code for token
authorization_response = input('Paste the full redirect URL here:')
token = client.get_token(authorization_response, state)

# Get user information
user_info = client.get_user_info(token)
print('User information:', user_info)
```

### 4. cURL Examples

#### Authorization Code Exchange
```bash
curl -X POST https://authorization.greenbuttonalliance.org/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -H "Authorization: Basic $(echo -n 'client_id:client_secret' | base64)" \
  -d "grant_type=authorization_code" \
  -d "code=authorization_code_value" \
  -d "redirect_uri=https://yourapp.com/callback"
```

#### UserInfo Request
```bash
curl -X GET https://authorization.greenbuttonalliance.org/userinfo \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIs..." \
  -H "Accept: application/json"
```

#### Token Introspection
```bash
curl -X POST https://authorization.greenbuttonalliance.org/oauth2/introspect \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -H "Authorization: Basic $(echo -n 'client_id:client_secret' | base64)" \
  -d "token=eyJhbGciOiJSUzI1NiIs..."
```

## Testing and Validation

### 1. ESPI Compliance Testing

#### Scope Validation Test
```javascript
const testScopes = [
  "FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13", // Valid
  "FB=4_5_15;IntervalDuration=900;BlockDuration=daily;HistoryLength=30",    // Valid
  "FB=4_5_15;IntervalDuration=invalid;BlockDuration=monthly;HistoryLength=13", // Invalid
  "FB=4_5_15;IntervalDuration=3600;BlockDuration=weekly;HistoryLength=13"     // Invalid (weekly not supported)
];

testScopes.forEach(scope => {
  console.log(`${scope}: ${validateESPIScope(scope) ? 'VALID' : 'INVALID'}`);
});
```

#### Certificate Authentication Test
```bash
# Test with client certificate
curl -X POST https://authorization.greenbuttonalliance.org/oauth2/token \
  --cert client.crt \
  --key client.key \
  --cacert ca.crt \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=authorization_code" \
  -d "code=authorization_code_value" \
  -d "redirect_uri=https://yourapp.com/callback"
```

### 2. Integration Testing

#### Test Authorization Flow
```javascript
describe('OAuth2 Integration', () => {
  test('should complete authorization code flow', async () => {
    // 1. Get authorization URL
    const authUrl = getAuthorizationUrl();
    expect(authUrl).toContain('https://authorization.greenbuttonalliance.org/oauth2/authorize');
    
    // 2. Simulate user authorization (in real test, use browser automation)
    const authCode = 'test-authorization-code';
    
    // 3. Exchange code for token
    const token = await exchangeCodeForToken(authCode);
    expect(token.access_token).toBeDefined();
    expect(token.token_type).toBe('Bearer');
    
    // 4. Get user info
    const userInfo = await getUserInfo(token.access_token);
    expect(userInfo.sub).toBeDefined();
    expect(userInfo.customer_id).toBeDefined();
  });
});
```

### 3. Performance Testing

#### Token Request Load Test
```javascript
const loadTest = async () => {
  const requests = [];
  const numRequests = 100;
  
  for (let i = 0; i < numRequests; i++) {
    requests.push(
      fetch('/oauth2/token', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: 'grant_type=client_credentials&scope=openid'
      })
    );
  }
  
  const start = Date.now();
  const responses = await Promise.all(requests);
  const duration = Date.now() - start;
  
  console.log(`${numRequests} requests completed in ${duration}ms`);
  console.log(`Average response time: ${duration / numRequests}ms`);
};
```

## Troubleshooting

### Common Issues and Solutions

#### 1. Invalid Scope Format
**Error**: `invalid_scope: ESPI scope format is invalid`

**Solution**: Verify scope syntax matches the pattern:
```
FB=x_y_z;IntervalDuration=nnn;BlockDuration=xxx;HistoryLength=nn
```

**Example Fix**:
```javascript
// Wrong
const scope = "FB=4.5.15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13";

// Correct
const scope = "FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13";
```

#### 2. TLS Version Issues
**Error**: `SSL handshake failed`

**Solution**: Ensure your client supports TLS 1.3:
```javascript
// Node.js example
const https = require('https');
const options = {
  secureProtocol: 'TLSv1_3_method',
  // other options
};
```

#### 3. Certificate Authentication Failures
**Error**: `invalid_client: Certificate authentication failed`

**Checklist**:
- Certificate is valid and not expired
- Certificate chain is complete
- Client certificate is registered with the authorization server
- Certificate Subject DN matches registered value

#### 4. Token Expiration
**Error**: `invalid_token: Token has expired`

**Solution**: Implement token refresh:
```javascript
async function refreshToken(refreshToken) {
  const response = await fetch('/oauth2/token', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: `grant_type=refresh_token&refresh_token=${refreshToken}`
  });
  
  if (!response.ok) {
    throw new Error('Token refresh failed');
  }
  
  return await response.json();
}
```

#### 5. CORS Issues (Browser Integration)
**Error**: `CORS policy: No 'Access-Control-Allow-Origin' header`

**Solution**: Use proper OAuth2 flow with redirects instead of direct AJAX calls for authorization.

### Debug Mode

Enable debug logging by setting:
```yaml
logging:
  level:
    org.greenbuttonalliance.espi.authserver: DEBUG
    org.springframework.security.oauth2: DEBUG
```

### Support and Resources

- **Documentation**: https://docs.greenbuttonalliance.org
- **GitHub Issues**: https://github.com/greenbuttonalliance/openespi-authorization-server/issues
- **Community Forum**: https://community.greenbuttonalliance.org
- **ESPI Specification**: https://www.naesb.org/espi

---

*This integration guide is for OpenESPI Authorization Server v1.0.0 with NAESB ESPI 4.0 compliance.*