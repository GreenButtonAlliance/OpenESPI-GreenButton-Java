# ESPI 4.0 Compliance Guide

## Overview

This guide provides detailed information about NAESB ESPI 4.0 compliance features implemented in the OpenESPI Authorization Server.

## Table of Contents

1. [ESPI 4.0 Requirements](#espi-40-requirements)
2. [Scope Format and Validation](#scope-format-and-validation)
3. [Certificate-based Authentication](#certificate-based-authentication)
4. [Security Enhancements](#security-enhancements)
5. [Compliance Validation](#compliance-validation)
6. [Testing and Certification](#testing-and-certification)

## ESPI 4.0 Requirements

### Mandatory Features

#### 1. TLS 1.3 ONLY
- **Requirement**: All communications must use TLS 1.3
- **Implementation**: Server configured to reject TLS 1.2 and below
- **Verification**: Connection attempts with older TLS versions will fail

#### 2. Perfect Forward Secrecy (PFS)
- **Requirement**: All cipher suites must support PFS
- **Implementation**: Only ECDHE and DHE cipher suites enabled
- **Supported Ciphers**:
  - `TLS_AES_256_GCM_SHA384`
  - `TLS_CHACHA20_POLY1305_SHA256`
  - `TLS_AES_128_GCM_SHA256`

#### 3. Certificate-based Client Authentication
- **Requirement**: Support for X.509 client certificate authentication
- **Implementation**: `tls_client_auth` method available
- **Certificate Requirements**:
  - Valid X.509 certificate
  - Proper certificate chain validation
  - Certificate must be issued by trusted CA

### Green Button Connect My Data (CMD) Compliance

#### 1. Function Block Support
- **FB=4_5_15**: Full support for ESPI 4.0 Function Block 15
- **FB=4_5_16**: Extended support for future versions
- **Backward Compatibility**: Support for earlier FB versions

#### 2. Scope Format Validation
- **Syntax Checking**: Validates ESPI scope syntax
- **Parameter Validation**: Ensures valid parameter values
- **Range Checking**: Validates data ranges and intervals

#### 3. Data Rights Management
- **Granular Permissions**: Fine-grained access control
- **Scope-based Authorization**: Access based on granted scopes
- **Usage Point Authorization**: Per-meter access control

## Scope Format and Validation

### ESPI Scope Syntax

```
FB=x_y_z;IntervalDuration=nnn;BlockDuration=xxx;HistoryLength=nn
```

### Detailed Parameters

#### Function Block (FB)
- **Format**: `x_y_z` where x, y, z are version numbers
- **Current**: `4_5_15` (ESPI 4.0, version 5, release 15)
- **Examples**: `4_5_15`, `4_5_16`, `4_6_0`

#### Interval Duration
- **Purpose**: Specifies data granularity
- **Units**: Seconds
- **Common Values**:
  - `900` - 15-minute intervals
  - `3600` - 1-hour intervals
  - `86400` - Daily intervals

#### Block Duration
- **Purpose**: Billing cycle granularity
- **Valid Values**:
  - `monthly` - Monthly billing cycles
  - `daily` - Daily summaries
  - `hourly` - Hourly blocks

#### History Length
- **Purpose**: Amount of historical data
- **Units**: Number of periods (based on BlockDuration)
- **Range**: 1-999
- **Examples**:
  - `13` - 13 months of history
  - `30` - 30 days of history

### Validation Rules

#### 1. Syntax Validation
```java
// Regex pattern for ESPI scope validation
String ESPI_SCOPE_PATTERN = 
    "^FB=[0-9]+_[0-9]+_[0-9]+;IntervalDuration=[0-9]+;BlockDuration=(monthly|daily|hourly);HistoryLength=[0-9]+$";
```

#### 2. Parameter Range Validation
- **IntervalDuration**: Must be between 60 and 86400 seconds
- **HistoryLength**: Must be between 1 and 999 periods
- **FB Version**: Must match supported versions

#### 3. Logical Consistency
- **Interval vs Block**: IntervalDuration must be compatible with BlockDuration
- **History Limits**: HistoryLength must not exceed data availability

### Example Valid Scopes

```javascript
// Standard hourly data with monthly billing, 13 months history
"FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13"

// 15-minute intervals with daily billing, 30 days history
"FB=4_5_15;IntervalDuration=900;BlockDuration=daily;HistoryLength=30"

// Daily data with monthly billing, 24 months history
"FB=4_5_15;IntervalDuration=86400;BlockDuration=monthly;HistoryLength=24"
```

### Scope Parsing Implementation

```java
public class ESPIScopeParser {
    public ESPIScopeParams parseScope(String scope) {
        if (!validateESPIScope(scope)) {
            throw new InvalidScopeException("Invalid ESPI scope format");
        }
        
        Map<String, String> params = parseParameters(scope);
        
        return ESPIScopeParams.builder()
            .fbVersion(params.get("FB"))
            .intervalDuration(Integer.parseInt(params.get("IntervalDuration")))
            .blockDuration(params.get("BlockDuration"))
            .historyLength(Integer.parseInt(params.get("HistoryLength")))
            .build();
    }
    
    private boolean validateESPIScope(String scope) {
        return scope.matches(ESPI_SCOPE_PATTERN);
    }
}
```

## Certificate-based Authentication

### TLS Client Authentication (`tls_client_auth`)

#### Configuration
```yaml
server:
  ssl:
    client-auth: want
    trust-store: classpath:truststore.p12
    trust-store-password: ${TRUSTSTORE_PASSWORD}
    trust-store-type: PKCS12
```

#### Certificate Requirements

1. **Certificate Authority (CA)**
   - Must be issued by a trusted CA
   - CA certificate must be in server truststore
   - Support for intermediate certificates

2. **Certificate Fields**
   - **Subject**: Must contain client identifier
   - **Key Usage**: Digital Signature, Key Encipherment
   - **Extended Key Usage**: Client Authentication

3. **Validation Process**
   - Certificate chain validation
   - Expiration date checking
   - Revocation status checking (CRL/OCSP)

#### Client Registration for Certificate Auth

```json
{
  "clientId": "espi-cert-client-001",
  "clientName": "Certificate-based Client",
  "clientAuthenticationMethods": ["tls_client_auth"],
  "certificateSubjectDN": "CN=client-001,OU=ESPI,O=Green Button Alliance",
  "certificateSerialNumber": "123456789ABCDEF",
  "certificateIssuerDN": "CN=ESPI CA,O=Green Button Alliance"
}
```

#### Authentication Flow

1. **TLS Handshake**: Client presents certificate
2. **Certificate Validation**: Server validates certificate chain
3. **Client Identification**: Map certificate to registered client
4. **Token Issuance**: Issue access token on successful auth

### Certificate Management

#### Certificate Storage
```sql
-- Client certificate mapping table
CREATE TABLE client_certificates (
    id BIGSERIAL PRIMARY KEY,
    client_id VARCHAR(255) NOT NULL,
    subject_dn VARCHAR(500) NOT NULL,
    issuer_dn VARCHAR(500) NOT NULL,
    serial_number VARCHAR(100) NOT NULL,
    thumbprint_sha256 VARCHAR(64) NOT NULL,
    not_before TIMESTAMP NOT NULL,
    not_after TIMESTAMP NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(client_id, serial_number)
);
```

#### Certificate Validation Service
```java
@Service
public class CertificateValidationService {
    
    public ValidationResult validateClientCertificate(X509Certificate certificate) {
        return ValidationResult.builder()
            .valid(isValid(certificate))
            .clientId(extractClientId(certificate))
            .errors(getValidationErrors(certificate))
            .build();
    }
    
    private boolean isValid(X509Certificate certificate) {
        return isNotExpired(certificate) 
            && isNotRevoked(certificate)
            && hasValidChain(certificate)
            && hasValidKeyUsage(certificate);
    }
}
```

## Security Enhancements

### 1. Enhanced Audit Logging

#### Audit Event Types
- `CERTIFICATE_AUTH_SUCCESS`
- `CERTIFICATE_AUTH_FAILURE`
- `ESPI_SCOPE_ACCESS`
- `DATA_ACCESS_GRANTED`
- `DATA_ACCESS_DENIED`

#### Audit Log Format
```json
{
  "eventType": "CERTIFICATE_AUTH_SUCCESS",
  "timestamp": "2024-01-16T15:30:00Z",
  "clientId": "espi-cert-client-001",
  "principalName": "customer@example.com",
  "sourceIP": "192.168.1.100",
  "userAgent": "ESPIClient/1.0",
  "certificateInfo": {
    "subjectDN": "CN=client-001,OU=ESPI,O=Green Button Alliance",
    "issuerDN": "CN=ESPI CA,O=Green Button Alliance",
    "serialNumber": "123456789ABCDEF",
    "thumbprint": "A1B2C3D4E5F6..."
  },
  "espiSpecific": {
    "scopes": ["FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13"],
    "usagePoints": ["up-001", "up-002"],
    "dataRights": ["ENERGY_USAGE_DATA", "HOURLY_INTERVALS"]
  }
}
```

### 2. Data Minimization

#### Scope-based Data Filtering
- Only return data explicitly authorized by scopes
- Filter usage points based on granted access
- Time-bound data access based on HistoryLength

#### Implementation Example
```java
@Service
public class DataMinimizationService {
    
    public UserInfoResponse filterUserInfo(UserInfoResponse userInfo, Set<String> scopes) {
        UserInfoResponse filtered = new UserInfoResponse();
        
        // Standard OIDC claims
        if (scopes.contains("openid")) {
            filtered.setSub(userInfo.getSub());
        }
        
        if (scopes.contains("profile")) {
            filtered.setName(userInfo.getName());
            filtered.setGivenName(userInfo.getGivenName());
            filtered.setFamilyName(userInfo.getFamilyName());
        }
        
        // ESPI-specific filtering
        scopes.stream()
            .filter(scope -> scope.startsWith("FB="))
            .forEach(scope -> {
                ESPIScopeParams params = parseScope(scope);
                filterESPIData(filtered, userInfo, params);
            });
        
        return filtered;
    }
}
```

### 3. Rate Limiting and DDoS Protection

#### Rate Limiting Configuration
```yaml
security:
  rate-limiting:
    oauth2-token:
      limit: 100
      window: 3600  # 1 hour
    api-requests:
      limit: 1000
      window: 3600
    certificate-auth:
      limit: 50
      window: 3600
```

#### DDoS Protection
- Request size limits
- Connection rate limiting
- Suspicious pattern detection
- Automatic IP blocking for abuse

## Compliance Validation

### 1. Automated Compliance Checks

#### Daily Compliance Report
```java
@Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
public void generateComplianceReport() {
    ComplianceReport report = ComplianceReport.builder()
        .reportDate(LocalDate.now())
        .tlsCompliance(validateTLSCompliance())
        .certificateCompliance(validateCertificateCompliance())
        .scopeCompliance(validateScopeCompliance())
        .auditCompliance(validateAuditCompliance())
        .build();
    
    complianceReportService.save(report);
    
    if (!report.isFullyCompliant()) {
        alertingService.sendComplianceAlert(report);
    }
}
```

#### Compliance Metrics
- TLS 1.3 usage percentage
- Certificate authentication success rate
- ESPI scope validation pass rate
- Audit log completeness percentage

### 2. Real-time Monitoring

#### Health Check Endpoint
```http
GET /actuator/health/espi-compliance
```

Response:
```json
{
  "status": "UP",
  "components": {
    "tls-compliance": {
      "status": "UP",
      "details": {
        "tlsVersion": "1.3",
        "cipherSuite": "TLS_AES_256_GCM_SHA384",
        "perfectForwardSecrecy": true
      }
    },
    "certificate-validation": {
      "status": "UP",
      "details": {
        "truststoreLoaded": true,
        "crlCheckEnabled": true,
        "ocspCheckEnabled": true
      }
    },
    "espi-scope-validation": {
      "status": "UP",
      "details": {
        "validationEnabled": true,
        "supportedFunctionBlocks": ["4_5_15", "4_5_16"]
      }
    }
  }
}
```

## Testing and Certification

### 1. ESPI Compliance Test Suite

#### Test Categories
1. **TLS Compliance Tests**
   - TLS version enforcement
   - Cipher suite validation
   - Certificate chain validation

2. **Scope Validation Tests**
   - Valid scope format acceptance
   - Invalid scope rejection
   - Parameter range validation

3. **Certificate Authentication Tests**
   - Valid certificate acceptance
   - Invalid certificate rejection
   - Certificate revocation checking

4. **Data Access Tests**
   - Scope-based data filtering
   - Usage point authorization
   - Time-bound access validation

#### Sample Test
```java
@Test
public void testESPIScopeValidation() {
    // Valid ESPI scope
    String validScope = "FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13";
    assertTrue(scopeValidator.validate(validScope));
    
    // Invalid ESPI scope - bad format
    String invalidScope = "FB=4_5_15;IntervalDuration=invalid;BlockDuration=monthly;HistoryLength=13";
    assertFalse(scopeValidator.validate(invalidScope));
    
    // Invalid ESPI scope - out of range
    String outOfRangeScope = "FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=1000";
    assertFalse(scopeValidator.validate(outOfRangeScope));
}
```

### 2. Green Button Alliance Certification

#### Certification Process
1. **Self-Assessment**: Internal compliance validation
2. **Test Suite Execution**: Run certified test suite
3. **Documentation Review**: Compliance documentation audit
4. **Security Assessment**: Security configuration review
5. **Certification Submission**: Submit to Green Button Alliance

#### Required Documentation
- ESPI compliance statement
- Security configuration details
- Test execution results
- Audit trail samples
- Certificate management procedures

#### Certification Maintenance
- Annual compliance reviews
- Quarterly security assessments
- Continuous monitoring reports
- Incident response documentation

---

## Appendix

### A. Supported ESPI Function Blocks

| Function Block | Version | Status | Features |
|---------------|---------|---------|-----------|
| FB=4_5_15 | 4.0.5.15 | Certified | Full ESPI 4.0 support |
| FB=4_5_16 | 4.0.5.16 | Beta | Enhanced security features |
| FB=4_6_0 | 4.0.6.0 | Planned | Future enhancements |

### B. Certificate Authority Requirements

| Requirement | Details |
|-------------|---------|
| Root CA | Must be in trusted CA list |
| Intermediate CA | Support for certificate chains |
| Key Length | Minimum 2048-bit RSA or 256-bit ECC |
| Hash Algorithm | SHA-256 or stronger |
| Validity Period | Maximum 2 years |

### C. Compliance Checklist

- [ ] TLS 1.3 ONLY configuration
- [ ] Perfect Forward Secrecy enabled
- [ ] Certificate-based authentication implemented
- [ ] ESPI scope validation active
- [ ] Audit logging comprehensive
- [ ] Data minimization enforced
- [ ] Rate limiting configured
- [ ] Compliance monitoring active
- [ ] Test suite passing
- [ ] Documentation complete

---

*This compliance guide is for OpenESPI Authorization Server v1.0.0 with NAESB ESPI 4.0 certification.*