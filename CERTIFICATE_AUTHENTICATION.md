# Certificate-Based Client Authentication

This document describes the certificate-based client authentication implementation for the OpenESPI Authorization Server, providing NAESB ESPI 4.0 compliant mutual TLS (mTLS) authentication.

## Overview

The certificate authentication system enables clients to authenticate using X.509 certificates instead of or in addition to client secrets. This provides enhanced security and is particularly important for enterprise deployments and regulatory compliance.

## Features

### Core Features
- **X.509 Certificate Validation**: Full certificate chain validation with configurable trust stores
- **Certificate Revocation Checking**: Support for CRL (Certificate Revocation List) and OCSP (Online Certificate Status Protocol)
- **Flexible Authentication**: Certificate-only or certificate + secret authentication modes
- **Certificate Management**: Upload, storage, validation, and revocation of client certificates
- **ESPI 4.0 Compliance**: Meets NAESB ESPI 4.0 certificate authentication requirements

### Security Features
- **Trust Store Management**: Configurable trusted certificate authorities
- **Certificate Validation Levels**: Basic, Standard, and Strict validation modes
- **Expiration Monitoring**: Automatic detection of expiring certificates
- **Audit Logging**: Comprehensive logging of all certificate operations
- **Rate Limiting**: Protection against certificate-based attacks

## Configuration

### Basic Configuration

Add to `application.yml`:

```yaml
espi:
  security:
    certificate:
      # Enable certificate authentication
      enable-certificate-authentication: true
      
      # Require client certificates for authentication
      require-client-certificate: false
      
      # Certificate validation settings
      enable-certificate-revocation-check: true
      enable-ocsp-check: true
      enable-crl-check: true
      
      # Trust store configuration
      trust-store-path: "classpath:certificates/truststore.jks"
      trust-store-password: "${TRUST_STORE_PASSWORD:}"
      trust-store-type: "JKS"
      
      # Certificate validity settings
      certificate-validity-days: 30
      certificate-renewal-warning-days: 7
      certificate-cache-expiration: 3600
      
      # Trusted certificate authorities
      trusted-certificate-authorities:
        - "CN=ESPI Root CA, O=Green Button Alliance, C=US"
        - "CN=ESPI Intermediate CA, O=Green Button Alliance, C=US"
      
      # Certificate extensions
      required-certificate-extensions:
        - "2.5.29.15" # Key Usage
        - "2.5.29.37" # Extended Key Usage
      
      allowed-certificate-extensions:
        - "2.5.29.17" # Subject Alternative Name
        - "2.5.29.32" # Certificate Policies
```

### Production Configuration

For production deployments:

```yaml
espi:
  security:
    certificate:
      enable-certificate-authentication: true
      require-client-certificate: true
      enable-certificate-revocation-check: true
      enable-ocsp-check: true
      enable-crl-check: true
      trust-store-path: "file:/etc/ssl/certs/espi-truststore.jks"
      trust-store-password: "${TRUST_STORE_PASSWORD}"
      certificate-validity-days: 365
      certificate-renewal-warning-days: 30
```

## API Endpoints

### Certificate Management

#### Upload Client Certificate
```http
POST /api/v1/certificates/clients/{clientId}/upload
Content-Type: multipart/form-data

Form data:
- certificate: X.509 certificate file (PEM or DER format)
- uploaded_by: Name of the person uploading (optional)
```

#### Get Client Certificate Information
```http
GET /api/v1/certificates/clients/{clientId}
```

#### Revoke Client Certificate
```http
DELETE /api/v1/certificates/clients/{clientId}
Content-Type: application/json

{
  "reason": "Certificate compromised",
  "revoked_by": "admin"
}
```

#### Validate Certificate
```http
POST /api/v1/certificates/validate
Content-Type: multipart/form-data

Form data:
- certificate: X.509 certificate file to validate
```

#### Get Expiring Certificates
```http
GET /api/v1/certificates/expiring
```

## Client Configuration

### Certificate Requirements

Client certificates must meet the following requirements:

1. **X.509 Version 3**: Only version 3 certificates are supported
2. **Key Usage**: Must include Digital Signature
3. **Valid Date Range**: Must be currently valid (not expired or not yet valid)
4. **Trusted Chain**: Must chain to a trusted root CA in the trust store
5. **Subject DN**: Must follow organizational naming conventions

### Example Certificate Subject DN Patterns

For different client types:

```
# Data Custodian Admin
CN=datacustodian-admin, OU=DataCustodian, O=Utility Company, C=US

# Third Party Application
CN=energy-app-client, OU=ThirdParty, O=Energy Software Inc, C=US

# Administrative Client
CN=system-admin, OU=Admin, O=Green Button Alliance, C=US
```

## Database Schema

### Tables Created

1. **oauth2_client_certificates**: Stores client certificates
2. **oauth2_certificate_validation_log**: Logs all validation attempts
3. **oauth2_certificate_revocation_cache**: Caches revocation status
4. **oauth2_trusted_certificate_authorities**: Trusted CA store
5. **oauth2_certificate_auth_stats**: Authentication statistics

### Key Fields

#### oauth2_client_certificates
- `client_id`: Associated OAuth2 client
- `certificate_data`: DER-encoded certificate
- `certificate_fingerprint`: SHA-256 fingerprint
- `certificate_subject_dn`: Certificate subject distinguished name
- `certificate_issuer_dn`: Certificate issuer distinguished name
- `status`: active, revoked, or expired

## Authentication Flow

### Certificate Authentication Process

1. **TLS Handshake**: Client presents certificate during TLS handshake
2. **Certificate Extraction**: Server extracts certificate from TLS context
3. **Basic Validation**: Check format, expiration, and signature
4. **Chain Validation**: Validate certificate chain to trusted root
5. **Revocation Check**: Check CRL/OCSP for revocation status
6. **Client Mapping**: Map certificate to registered OAuth2 client
7. **Authority Assignment**: Assign appropriate roles and authorities

### Integration with OAuth2 Flow

Certificate authentication integrates with standard OAuth2 flows:

1. **Client Credentials Grant**: Certificate replaces or supplements client secret
2. **Authorization Code Grant**: Certificate authenticates client during token exchange
3. **Refresh Token Grant**: Certificate required for token refresh

## Security Considerations

### Certificate Validation Levels

- **Basic**: Certificate format and expiration only
- **Standard**: Basic + chain validation + revocation checking
- **Strict**: Standard + extended validation + policy constraints

### Trust Store Management

- Use hardware security modules (HSMs) for CA private keys
- Regularly update trust store with new/revoked CAs
- Implement certificate transparency logging
- Monitor certificate usage patterns

### Revocation Handling

- Implement both CRL and OCSP checking
- Cache revocation status for performance
- Handle revocation service outages gracefully
- Provide manual revocation capabilities

## Monitoring and Alerting

### Metrics to Monitor

- Certificate validation success/failure rates
- Certificate expiration dates
- Revocation check performance
- Authentication attempt frequencies
- Trust store update events

### Recommended Alerts

- Certificates expiring within 30 days
- High certificate validation failure rates
- Revocation service outages
- Suspicious authentication patterns
- Trust store update failures

## Troubleshooting

### Common Issues

#### Certificate Validation Failures
```
Check certificate chain completeness
Verify trust store contains root CA
Confirm certificate not expired/revoked
Validate certificate subject DN format
```

#### Performance Issues
```
Optimize revocation checking intervals
Implement certificate validation caching
Monitor database query performance
Review trust store size and organization
```

#### Configuration Problems
```
Verify trust store path and permissions
Confirm trust store password correctness
Check certificate authority configuration
Validate OCSP/CRL endpoint accessibility
```

## Testing

### Certificate Generation for Testing

Create test certificates:

```bash
# Generate CA private key
openssl genrsa -out ca-key.pem 4096

# Generate CA certificate
openssl req -new -x509 -days 3650 -key ca-key.pem -out ca-cert.pem \
  -subj "/CN=ESPI Test Root CA/O=Green Button Alliance/C=US"

# Generate client private key
openssl genrsa -out client-key.pem 2048

# Generate client certificate signing request
openssl req -new -key client-key.pem -out client-csr.pem \
  -subj "/CN=test-client/OU=ThirdParty/O=Test Company/C=US"

# Sign client certificate
openssl x509 -req -days 365 -in client-csr.pem -CA ca-cert.pem \
  -CAkey ca-key.pem -CAcreateserial -out client-cert.pem
```

### Test Scenarios

1. **Valid Certificate Authentication**
2. **Expired Certificate Rejection**
3. **Revoked Certificate Rejection**
4. **Invalid Certificate Chain Rejection**
5. **Certificate Renewal Process**
6. **Mixed Authentication (Certificate + Secret)**

## ESPI 4.0 Compliance

This implementation meets NAESB ESPI 4.0 requirements:

- ✅ X.509 certificate-based client authentication
- ✅ Certificate revocation checking (CRL/OCSP)
- ✅ Trusted certificate authority validation
- ✅ Certificate expiration monitoring
- ✅ Comprehensive audit logging
- ✅ Integration with OAuth2 authorization flows
- ✅ Support for multiple authentication modes

## References

- [NAESB ESPI 4.0 Standard](https://www.naesb.org/)
- [RFC 5280 - X.509 Certificate Profile](https://tools.ietf.org/html/rfc5280)
- [RFC 6960 - OCSP](https://tools.ietf.org/html/rfc6960)
- [Spring Security X.509 Authentication](https://docs.spring.io/spring-security/reference/servlet/authentication/x509.html)