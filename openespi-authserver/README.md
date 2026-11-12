# OpenESPI-AuthServer

[![CI/CD Pipeline](https://github.com/GreenButtonAlliance/OpenESPI-AuthorizationServer-java/actions/workflows/ci.yml/badge.svg)](https://github.com/GreenButtonAlliance/OpenESPI-AuthorizationServer-java/actions/workflows/ci.yml)
[![Java Version](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![OAuth2](https://img.shields.io/badge/OAuth2-Authorization%20Server-blue.svg)](https://spring.io/projects/spring-authorization-server)
[![NAESB ESPI](https://img.shields.io/badge/NAESB-ESPI%204.0-green.svg)](https://www.naesb.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

This is the AuthServer module of the OpenESPI GreenButton Java monorepo. It is a modern Spring Boot 3.5 OAuth2 Authorization Server built with Spring Authorization Server 1.3+, providing secure authentication and authorization services for the Green Button ecosystem.

> **🚀 Migration Complete**: This module has been fully migrated to Spring Boot 3.5 with Java 21, featuring enterprise-grade OAuth2 flows, comprehensive testing suite, and NAESB ESPI 4.0 compliance with TLS 1.3 enforcement.

## Overview

This module provides OAuth2 Authorization Server capabilities as part of the OpenESPI GreenButton Java monorepo. It offers secure OAuth2 authorization flows compliant with Green Button Alliance ESPI standards, working seamlessly with the `openespi-datacustodian` resource server module.

**⚠️ ESPI Compliance Notice**: The ESPI standard requires **opaque access tokens**, not JWT tokens. This Authorization Server defaults to ESPI-compliant opaque tokens. JWT token support is experimental and may be considered for future ESPI versions.

## 🔄 CI/CD Status

| Build Stage | Status | Details |
|------------|--------|---------|
| **Build & Test** | [![CI/CD Pipeline](https://github.com/GreenButtonAlliance/OpenESPI-AuthorizationServer-java/actions/workflows/ci.yml/badge.svg)](https://github.com/GreenButtonAlliance/OpenESPI-AuthorizationServer-java/actions/workflows/ci.yml) | Java 21, Maven 3.8+, Spring Boot 3.5 |
| **Code Quality** | ![SonarCloud](https://img.shields.io/badge/SonarCloud-Configured-brightgreen.svg) | Code quality analysis on main branch |
| **Security Scan** | ![Security](https://img.shields.io/badge/Security-Trivy%20%2B%20OWASP-blue.svg) | OWASP dependency check, Trivy container scan |
| **Docker Build** | ![Docker](https://img.shields.io/badge/Docker-Multi--stage-blue.svg) | Automated container builds on main |
| **Integration Tests** | ![Tests](https://img.shields.io/badge/Tests-OAuth2%20Flows-green.svg) | End-to-end OAuth2 authorization testing |

### Pipeline Features
- ✅ **Java 21** with Maven 3.8+ compilation
- ✅ **Unit & Integration Tests** with JaCoCo coverage reporting
- ✅ **Security Scanning** with OWASP dependency check and Trivy
- ✅ **Code Quality** analysis with SonarCloud integration
- ✅ **Docker Container** builds with security hardening
- ✅ **OAuth2 Flow Testing** with live authorization server validation
- ✅ **Multi-Database Testing** with TestContainers (MySQL, PostgreSQL)
- ✅ **Artifact Management** with automated JAR and coverage uploads

### Key Features

#### 🚀 **Core OAuth2/OIDC Features**
- **Spring Authorization Server 1.3+**: Modern OAuth2/OIDC implementation with Java 21
- **NAESB ESPI 4.0 Compliance**: TLS 1.3 ONLY enforcement with approved cipher suites
- **ESPI Opaque Tokens**: Standard ESPI-compliant opaque access tokens (default)
- **JWT Support (Experimental)**: Optional JWT tokens for future ESPI versions
- **Multi-Grant Support**: authorization_code, client_credentials, refresh_token flows
- **Dynamic Client Registration**: RFC 7591 compliant with ESPI validation
- **OIDC UserInfo Endpoint**: ESPI-specific claims and Green Button Alliance extensions

#### 🔐 **Security & Authentication**
- **Certificate-Based Authentication**: X.509 client certificate support with trust stores
- **HTTPS Enforcement**: Production-grade TLS 1.3 with Perfect Forward Secrecy
- **OAuth2 Consent Management**: Professional consent UI with granular permissions
- **Security Headers**: Comprehensive HSTS, CSP, and NAESB ESPI 4.0 compliance headers

#### 🛠️ **Enterprise Management**
- **OAuth2 Client Management API**: Comprehensive CRUD operations with pagination
- **DataCustodian Integration**: Seamless integration with OpenESPI-DataCustodian-java
- **Admin APIs**: Token and client management with metrics and analytics
- **Database-Backed Storage**: JDBC repository with MySQL, PostgreSQL, H2 support
- **Flyway Migrations**: Database schema management with ESPI 4.0 enhancements

#### 🐳 **DevOps & Deployment**
- **Docker Containerization**: Multi-stage builds with security hardening
- **Kubernetes Ready**: Helm charts and deployment manifests included
- **Monitoring Integration**: Prometheus metrics and Grafana dashboards
- **CI/CD Pipeline**: 5-stage GitHub Actions with security scanning

### Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                OpenESPI Authorization Server                 │
├─────────────────────────────────────────────────────────────┤
│  OAuth2 Endpoints:                                          │
│  • /oauth2/authorize  (Authorization endpoint)              │
│  • /oauth2/token      (Token endpoint)                      │
│  • /oauth2/jwks       (JSON Web Key Set)                    │
│  • /oauth2/consent    (User consent page)                   │
│                                                             │
│  Client Registration:                                       │
│  • /oauth2/register            (RFC 7591 client registration)│
│                                                             │
│  Admin Endpoints:                                           │
│  • /admin/oauth2/tokens        (Token management)          │
│  • /admin/oauth2/clients       (Client management)         │
│  • /admin/oauth2/authorizations (Authorization management) │
├─────────────────────────────────────────────────────────────┤
│  ESPI Components:                                           │
│  • EspiTokenCustomizer         (JWT token enhancement)     │
│  • ConsentController           (ESPI-aware consent flow)   │
│  • OAuthAdminController        (Administrative APIs)       │
│  • JdbcRegisteredClientRepository (Database-backed clients) │
└─────────────────────────────────────────────────────────────┘
```

## 📊 Migration Status

### ✅ **Completed (Phase 1)**
| Component | Status | Details |
|-----------|--------|---------|
| **Database Migration** | ✅ Complete | Flyway scripts for MySQL/PostgreSQL with ESPI 4.0 enhancements |
| **OAuth2 Client Management** | ✅ Complete | RESTful CRUD API with pagination and bulk operations |
| **DataCustodian Integration** | ✅ Complete | User verification, scope validation, authorization grants |
| **OIDC UserInfo Endpoint** | ✅ Complete | ESPI claims, Green Button extensions, consent-aware release |
| **OAuth2 Consent Pages** | ✅ Complete | Professional UI with granular permissions and mobile support |
| **API Documentation** | ✅ Complete | OpenAPI 3.0, integration guides, Postman collections |
| **Certificate Authentication** | ✅ Complete | X.509 client certificates with trust store management |
| **Deployment Infrastructure** | ✅ Complete | Docker, Kubernetes, monitoring, CI/CD pipeline |
| **Spring Boot 3.5 Migration** | ✅ Complete | API compatibility fixes, Jakarta validation, OAuth2 updates |

### 📋 **Next Steps (Phase 2)**
- **Performance Optimization**: Redis caching, query optimization, load testing
- **Security Enhancements**: Automated certificate renewal, advanced monitoring
- **Production Monitoring**: Prometheus/Grafana integration, alerting rules
- **Documentation**: Interactive Swagger UI, SDK generation, tutorials

---

## Quick Start

### Prerequisites

- Java 21 or higher
- Maven 3.8+
- MySQL 8.0+ or PostgreSQL 13+ (for production)

### Local Development (H2 Database)

```bash
# Clone the monorepo (contains this module)
git clone https://github.com/GreenButtonAlliance/OpenESPI-GreenButton-Java.git
cd OpenESPI-GreenButton-Java

# Build all modules including openespi-authserver
mvn clean install

# Run the AuthServer module with local H2 profile
cd openespi-authserver && mvn spring-boot:run -P local

# Access H2 Console: http://localhost:9999/h2-console
# JDBC URL: jdbc:h2:mem:oauth2_authserver
# Username: sa
# Password: (empty)
```

### MySQL Development

```bash
# Start MySQL and create database
mysql -u root -p
CREATE DATABASE oauth2_authserver;
CREATE USER 'openespi_user'@'localhost' IDENTIFIED BY 'openespi_password';
GRANT ALL PRIVILEGES ON oauth2_authserver.* TO 'openespi_user'@'localhost';

# Run the AuthServer module with MySQL profile
cd openespi-authserver && mvn spring-boot:run -P dev-mysql
```

## Configuration

### ESPI Token Format

**Default (ESPI Compliant)**:
```yaml
espi:
  token:
    format: opaque  # ESPI standard - opaque access tokens
```

**Experimental JWT Support**:
```yaml
espi:
  token:
    format: jwt     # Experimental - for future ESPI versions
```

### Application Profiles

| Profile | Database | Purpose |
|---------|----------|---------|
| `local` | H2 in-memory | Local development |
| `dev-mysql` | MySQL | Development with persistent storage |
| `dev-postgresql` | PostgreSQL | Development with PostgreSQL |
| `prod` | Environment-specific | Production deployment |

### Environment Variables

For production deployment:

```bash
# Database Configuration
DB_URL=jdbc:mysql://localhost:3306/oauth2_authserver
DB_USERNAME=openespi_user
DB_PASSWORD=secure_password

# OAuth2 Configuration
OAUTH2_ISSUER_URI=https://auth.yourdomain.com
JWT_KEYSTORE_PATH=/path/to/keystore.p12
JWT_KEYSTORE_PASSWORD=keystore_password

# SSL Configuration (if using HTTPS)
SERVER_SSL_ENABLED=true
SERVER_SSL_KEYSTORE=/path/to/ssl-keystore.p12
SERVER_SSL_KEYSTORE_PASSWORD=ssl_password
```

## OAuth2 Flows

### Authorization Code Flow (Customer Consent)

```http
# 1. Authorization Request
GET /oauth2/authorize?
    response_type=code&
    client_id=third_party&
    redirect_uri=http://localhost:9090/callback&
    scope=FB=4_5_15;IntervalDuration=3600&
    state=random_state

# 2. User Login and Consent (via browser)

# 3. Authorization Code Response
HTTP/1.1 302 Found
Location: http://localhost:9090/callback?code=auth_code&state=random_state

# 4. Token Exchange
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded
Authorization: Basic dGhpcmRfcGFydHk6c2VjcmV0

grant_type=authorization_code&
code=auth_code&
redirect_uri=http://localhost:9090/callback&
client_id=third_party
```

### Client Credentials Flow (Admin Access)

```http
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded
Authorization: Basic ZGF0YV9jdXN0b2RpYW5fYWRtaW46c2VjcmV0

grant_type=client_credentials&
scope=DataCustodian_Admin_Access
```

### Token Structure

**ESPI Standard (Default)**: Opaque access tokens as per ESPI specification

**Experimental JWT Support**: Optional JWT tokens include custom claims:

```json
{
  "sub": "customer@example.com",
  "aud": "third_party",
  "iss": "http://localhost:9999",
  "exp": 1735689600,
  "iat": 1735686000,
  "scope": ["FB=4_5_15;IntervalDuration=3600", "openid"],
  "authorizationURI": "http://localhost:8080/espi/1_1/resource/Authorization/123",
  "resourceURI": "http://localhost:8080/espi/1_1/resource/",
  "espi_version": "1.1",
  "espi_client_type": "customer",
  "espi_grant_type": "authorization_code",
  "data_custodian_endpoint": "http://localhost:8080",
  "client_name": "ThirdParty Application"
}
```

## API Endpoints

### OAuth2 Standard Endpoints

- `GET /oauth2/authorize` - Authorization endpoint
- `POST /oauth2/token` - Token endpoint  
- `GET /oauth2/jwks` - JSON Web Key Set
- `POST /oauth2/revoke` - Token revocation
- `POST /oauth2/introspect` - Token introspection
- `GET /.well-known/oauth-authorization-server` - Discovery endpoint

### Client Registration

- `POST /oauth2/register` - OAuth 2.0 Dynamic Client Registration (RFC 7591), bearer `client.create` initial access token

### ESPI Custom Endpoints

- `GET /oauth2/consent` - User consent page
- `POST /oauth2/consent` - Process consent

### Administrative APIs

- `GET /admin/oauth2/tokens` - List active tokens
- `DELETE /admin/oauth2/tokens/{tokenId}` - Revoke token
- `GET /admin/oauth2/clients` - List registered clients
- `GET /admin/oauth2/clients/{clientId}` - Get specific client
- `DELETE /admin/oauth2/clients/{clientId}` - Delete client
- `DELETE /admin/oauth2/clients/{clientId}/tokens` - Revoke all client tokens
- `GET /admin/oauth2/authorizations` - List authorizations

## Dynamic Client Registration

The Authorization Server supports RFC 7591 (OAuth 2.0 Dynamic Client Registration) at
`POST /oauth2/register`, with opaque tokens only (no OIDC/JWK/JWT). A registrant needs a single-use
`client.create` initial access token from the `espi_registrar` client (enabled by setting
`ESPI_REGISTRAR_CLIENT_SECRET`) and may register only a confidential ESPI Third Party
`authorization_code` client with `client_secret_basic` and ESPI Function Block scopes.
RFC 7592 read/update and `registration_access_token` are not supported (see #203).

See [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md#dynamic-client-registration-rfc-7591) for the full
procedure and registration policy.

## Testing

```bash
# Run unit tests for this module only
mvn test -pl openespi-authserver

# Run integration tests for this module
mvn verify -pl openespi-authserver

# Run with test coverage
mvn test jacoco:report -pl openespi-authserver

# Run all tests in the monorepo
mvn clean test
```

## Database Schema

The authorization server uses Spring Authorization Server's standard schema with ESPI extensions:

- `oauth2_registered_client` - OAuth2 client registrations
- `oauth2_authorization` - Authorization grants and tokens
- `oauth2_authorization_consent` - User consent records
- `espi_application_info` - ESPI-specific application metadata

## Security Considerations

### Production Checklist

- [ ] Use external database (MySQL/PostgreSQL)
- [ ] Configure proper JWT signing keys
- [ ] Enable HTTPS/TLS
- [ ] Set secure session configuration
- [ ] Configure CORS policies
- [ ] Enable audit logging
- [ ] Set up monitoring and alerting
- [ ] Regular security updates

### JWT Key Management

For production, replace the auto-generated RSA key pair:

```java
@Bean
public JWKSource<SecurityContext> jwkSource() {
    // Load from external keystore
    KeyStore keyStore = KeyStore.getInstance("PKCS12");
    keyStore.load(new FileInputStream("/path/to/keystore.p12"), 
                  keystorePassword.toCharArray());
    // ... configure RSAKey from keystore
}
```

## Integration

### With OpenESPI DataCustodian

The DataCustodian (Resource Server) validates tokens from this Authorization Server:

**ESPI Standard (Opaque Tokens)**:
```yaml
# DataCustodian application.yml
spring:
  security:
    oauth2:
      resourceserver:
        opaque-token:
          introspection-uri: http://localhost:9999/oauth2/introspect
          client-id: datacustodian_resource_server
          client-secret: secret
```

**Experimental (JWT Tokens)**:
```yaml
# DataCustodian application.yml (experimental)
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:9999
```

### With OpenESPI ThirdParty

ThirdParty applications use this server for OAuth2 flows:

```yaml
# ThirdParty application.yml  
spring:
  security:
    oauth2:
      client:
        registration:
          datacustodian:
            client-id: third_party
            client-secret: secret
            authorization-grant-type: authorization_code
            redirect-uri: http://localhost:9090/oauth/callback
        provider:
          datacustodian:
            authorization-uri: http://localhost:9999/oauth2/authorize
            token-uri: http://localhost:9999/oauth2/token
            jwk-set-uri: http://localhost:9999/oauth2/jwks
```

## Monitoring

### Health Endpoints

- `GET /actuator/health` - Application health
- `GET /actuator/info` - Application information
- `GET /actuator/metrics` - Metrics
- `GET /actuator/env` - Environment properties

### Logging

Configure logging for OAuth2 events:

```yaml
logging:
  level:
    org.springframework.security.oauth2: DEBUG
    org.greenbuttonalliance.espi: DEBUG
```

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for details.

## Green Button Alliance

This project is part of the [Green Button Alliance](https://www.greenbuttonalliance.org/) initiative for standardized energy data access.

For more information about OpenESPI and Green Button:
- [Green Button Alliance](https://www.greenbuttonalliance.org/)
- [OpenESPI Specification](https://www.greenbuttonalliance.org/developers)
- [NAESB REQ.21 Standard](https://www.naesb.org/)