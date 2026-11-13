# OpenESPI Authorization Server - Deployment Guide

This comprehensive guide covers deployment scenarios for the OpenESPI Authorization Server, from local development to production environments with NAESB ESPI 4.0 compliance.

## Table of Contents

- [Quick Start](#quick-start)
- [Development Environment](#development-environment)
- [Production Deployment](#production-deployment)
- [Docker Deployment](#docker-deployment)
- [Kubernetes Deployment](#kubernetes-deployment)
- [Security Configuration](#security-configuration)
- [Monitoring and Observability](#monitoring-and-observability)
- [Troubleshooting](#troubleshooting)

## Quick Start

### Prerequisites

- Java 21 or higher
- Maven 3.9+ or Gradle 8+
- Docker and Docker Compose (for containerized deployment)
- MySQL 8.4+ or PostgreSQL 16+ (for database)

### Local Development Setup

1. **Clone the repository:**
```bash
git clone https://github.com/GreenButtonAlliance/OpenESPI-AuthorizationServer-java.git
cd OpenESPI-AuthorizationServer-java
```

2. **Start with Docker Compose:**
```bash
# Development environment with MySQL
docker-compose up -d

# Development environment with PostgreSQL
docker-compose --profile postgresql up -d

# With monitoring stack
docker-compose --profile monitoring up -d
```

3. **Access the application:**
- Authorization Server: http://localhost:8080
- Management Endpoints: http://localhost:8081/actuator
- Grafana (if monitoring enabled): http://localhost:3000

## Development Environment

### Manual Setup

1. **Database Setup:**

   **MySQL:**
   ```bash
   # Create database and user
   mysql -u root -p << EOF
   CREATE DATABASE espi_authserver CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   CREATE USER 'espi_user'@'localhost' IDENTIFIED BY 'espi_password';
   GRANT ALL PRIVILEGES ON espi_authserver.* TO 'espi_user'@'localhost';
   FLUSH PRIVILEGES;
   EOF
   ```

   **PostgreSQL:**
   ```bash
   # Create database and user
   sudo -u postgres psql << EOF
   CREATE DATABASE espi_authserver;
   CREATE USER espi_user WITH PASSWORD 'espi_password';
   GRANT ALL PRIVILEGES ON DATABASE espi_authserver TO espi_user;
   ALTER USER espi_user CREATEDB;
   EOF
   ```

2. **Application Configuration:**

   Create `application-dev.yml`:
   ```yaml
   spring:
     profiles:
       active: dev
     datasource:
       url: jdbc:mysql://localhost:3306/espi_authserver
       username: espi_user
       password: espi_password
     
   espi:
     security:
       require-https: false
     datacustodian:
       base-url: http://localhost:8082/DataCustodian
   
   logging:
     level:
       org.greenbuttonalliance: DEBUG
   ```

3. **Run the application:**
   ```bash
   # Using Maven
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   
   # Using Gradle
   ./gradlew bootRun --args='--spring.profiles.active=dev'
   ```

### IDE Setup

**IntelliJ IDEA:**
1. Import as Maven/Gradle project
2. Set Project SDK to Java 21
3. Configure run configuration with profile: `dev`
4. Enable annotation processing

**Eclipse:**
1. Import as Existing Maven Project
2. Configure Java Build Path with JDK 21
3. Install Spring Tools 4 for Spring Boot support

**VS Code:**
1. Install Java Extension Pack
2. Install Spring Boot Extension Pack
3. Configure Java home in settings

## Production Deployment

### Infrastructure Requirements

**Minimum Requirements:**
- CPU: 2 vCPUs
- Memory: 4GB RAM
- Storage: 50GB SSD
- Network: 1Gbps

**Recommended Requirements:**
- CPU: 4 vCPUs
- Memory: 8GB RAM
- Storage: 100GB SSD
- Network: 10Gbps
- Load Balancer with SSL termination

### Production Configuration

1. **Environment Variables:**
```bash
# Database Configuration
export DATABASE_URL="jdbc:mysql://mysql-server:3306/espi_authserver"
export DATABASE_USERNAME="espi_user"
export DATABASE_PASSWORD="secure_password"

# Security Configuration
export ESPI_SECURITY_REQUIRE_HTTPS=true
export TRUST_STORE_PASSWORD="secure_truststore_password"
export ISSUER_URL="https://auth.yourdomain.com"

# External Services
export DATACUSTODIAN_URL="https://datacustodian.yourdomain.com"
export DATACUSTODIAN_CLIENT_SECRET="secure_datacustodian_secret"

# SSL Configuration
export KEYSTORE_PATH="/etc/ssl/private/authserver.p12"
export KEYSTORE_PASSWORD="secure_keystore_password"
```

2. **Application Configuration (`application-prod.yml`):**
```yaml
spring:
  profiles:
    active: prod
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5

server:
  port: 8080
  ssl:
    enabled: true
    key-store: ${KEYSTORE_PATH}
    key-store-password: ${KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    protocol: TLS
    enabled-protocols: TLSv1.3

espi:
  security:
    require-https: true
    certificate:
      enable-certificate-authentication: true
      trust-store-path: ${TRUST_STORE_PATH}
      trust-store-password: ${TRUST_STORE_PASSWORD}

logging:
  level:
    org.greenbuttonalliance: INFO
    root: WARN
  file:
    name: /var/log/espi/authserver.log
```

### systemd Service Setup

1. **Create service file (`/etc/systemd/system/espi-authserver.service`):**
```ini
[Unit]
Description=OpenESPI Authorization Server
After=network.target mysql.service

[Service]
Type=forking
User=espi
Group=espi
WorkingDirectory=/opt/espi/authserver
ExecStart=/usr/bin/java -jar /opt/espi/authserver/authserver.jar --spring.profiles.active=prod
ExecStop=/bin/kill -15 $MAINPID
SuccessExitStatus=143
TimeoutStopSec=30
Restart=on-failure
RestartSec=5
Environment=JAVA_HOME=/usr/lib/jvm/java-21-openjdk

# Security settings
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=strict
ProtectHome=true
ReadWritePaths=/var/log/espi /opt/espi/authserver/logs

[Install]
WantedBy=multi-user.target
```

2. **Enable and start service:**
```bash
sudo systemctl daemon-reload
sudo systemctl enable espi-authserver
sudo systemctl start espi-authserver
sudo systemctl status espi-authserver
```

## Docker Deployment

### Single Container Deployment

1. **Build image:**
```bash
docker build -f docker/Dockerfile -t espi-authserver:latest .
```

2. **Run container:**
```bash
docker run -d \
  --name espi-authserver \
  -p 8080:8080 \
  -p 8081:8081 \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e DATABASE_URL="jdbc:mysql://host.docker.internal:3306/espi_authserver" \
  -e DATABASE_USERNAME=espi_user \
  -e DATABASE_PASSWORD=espi_password \
  -v /etc/ssl/espi:/app/certificates:ro \
  -v /var/log/espi:/app/logs \
  espi-authserver:latest
```

### Docker Compose Deployment

#### Development Environment

```bash
# Basic development setup
docker-compose up -d

# With PostgreSQL instead of MySQL
docker-compose --profile postgresql up -d

# With monitoring stack (Prometheus + Grafana)
docker-compose --profile monitoring up -d

# With complete logging stack (ELK)
docker-compose --profile logging up -d
```

#### Production Environment

```bash
# Production deployment with external database
docker-compose -f docker-compose.production.yml up -d

# With monitoring and logging
docker-compose -f docker-compose.production.yml --profile monitoring --profile logging up -d
```

**Environment file (`.env`):**
```env
# Domain Configuration
DOMAIN_NAME=auth.yourdomain.com
AUTHORIZATION_SERVER_URL=https://auth.yourdomain.com
DATACUSTODIAN_URL=https://datacustodian.yourdomain.com

# Database Configuration
DATABASE_URL=jdbc:mysql://external-mysql:3306/espi_authserver
DATABASE_USERNAME=espi_user
DATABASE_PASSWORD=secure_password

# Security Configuration
TRUST_STORE_PASSWORD=secure_truststore_password
DATACUSTODIAN_CLIENT_SECRET=secure_datacustodian_secret

# Redis Configuration
REDIS_PASSWORD=secure_redis_password

# Grafana Configuration
GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=secure_admin_password
GRAFANA_SECRET_KEY=secure_secret_key
GRAFANA_DOMAIN=grafana.yourdomain.com

# Database Configuration for Grafana
GRAFANA_DB_HOST=grafana-postgres
GRAFANA_DB_NAME=grafana
GRAFANA_DB_USER=grafana
GRAFANA_DB_PASSWORD=grafana_password

# SMTP Configuration for Grafana
SMTP_HOST=smtp.yourdomain.com
SMTP_USER=noreply@yourdomain.com
SMTP_PASSWORD=smtp_password
SMTP_FROM_ADDRESS=noreply@yourdomain.com
```

### Docker Swarm Deployment

1. **Initialize Swarm:**
```bash
docker swarm init
```

2. **Deploy stack:**
```bash
docker stack deploy -c docker-compose.production.yml espi-stack
```

3. **Scale services:**
```bash
docker service scale espi-stack_authserver=3
```

## Kubernetes Deployment

### Namespace and ConfigMap

```yaml
# namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: espi-authserver
  labels:
    name: espi-authserver
---
# configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: authserver-config
  namespace: espi-authserver
data:
  application.yml: |
    spring:
      profiles:
        active: k8s
      datasource:
        url: jdbc:mysql://mysql-service:3306/espi_authserver
        username: espi_user
        password: ${DATABASE_PASSWORD}
    espi:
      security:
        require-https: true
      datacustodian:
        base-url: ${DATACUSTODIAN_URL}
```

### Secrets

```yaml
# secrets.yaml
apiVersion: v1
kind: Secret
metadata:
  name: authserver-secrets
  namespace: espi-authserver
type: Opaque
data:
  database-password: <base64-encoded-password>
  truststore-password: <base64-encoded-password>
  datacustodian-client-secret: <base64-encoded-secret>
```

### Deployment

```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: authserver
  namespace: espi-authserver
spec:
  replicas: 3
  selector:
    matchLabels:
      app: authserver
  template:
    metadata:
      labels:
        app: authserver
    spec:
      containers:
      - name: authserver
        image: espi-authserver:latest
        ports:
        - containerPort: 8080
        - containerPort: 8081
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "k8s"
        - name: DATABASE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: authserver-secrets
              key: database-password
        - name: DATACUSTODIAN_URL
          value: "https://datacustodian.yourdomain.com"
        volumeMounts:
        - name: config-volume
          mountPath: /app/config
        - name: certificates-volume
          mountPath: /app/certificates
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8081
          initialDelaySeconds: 120
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8081
          initialDelaySeconds: 30
          periodSeconds: 10
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1"
      volumes:
      - name: config-volume
        configMap:
          name: authserver-config
      - name: certificates-volume
        secret:
          secretName: authserver-certificates
```

### Service and Ingress

```yaml
# service.yaml
apiVersion: v1
kind: Service
metadata:
  name: authserver-service
  namespace: espi-authserver
spec:
  selector:
    app: authserver
  ports:
  - name: http
    port: 80
    targetPort: 8080
  - name: management
    port: 8081
    targetPort: 8081
  type: ClusterIP
---
# ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: authserver-ingress
  namespace: espi-authserver
  annotations:
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/force-ssl-redirect: "true"
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
spec:
  tls:
  - hosts:
    - auth.yourdomain.com
    secretName: authserver-tls
  rules:
  - host: auth.yourdomain.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: authserver-service
            port:
              number: 80
```

### Helm Chart Deployment

```bash
# Add Helm repository (if available)
helm repo add espi https://charts.greenbuttonalliance.org
helm repo update

# Install with custom values
helm install authserver espi/authserver \
  --namespace espi-authserver \
  --create-namespace \
  --values production-values.yaml
```

## Security Configuration

### SSL/TLS Configuration

1. **Generate certificates:**
```bash
# Self-signed certificate for testing
keytool -genkeypair \
  -alias authserver \
  -keyalg RSA \
  -keysize 2048 \
  -validity 365 \
  -keystore authserver.p12 \
  -storetype PKCS12 \
  -storepass changeit \
  -dname "CN=auth.yourdomain.com, OU=IT, O=Your Organization, L=City, ST=State, C=US"

# Let's Encrypt certificate (production)
certbot certonly --webroot \
  -w /var/www/html \
  -d auth.yourdomain.com \
  --email admin@yourdomain.com \
  --agree-tos \
  --non-interactive
```

2. **Configure HTTPS in application:**
```yaml
server:
  ssl:
    enabled: true
    key-store: classpath:authserver.p12
    key-store-password: changeit
    key-store-type: PKCS12
    protocol: TLS
    enabled-protocols: TLSv1.3
    ciphers: TLS_AES_256_GCM_SHA384,TLS_CHACHA20_POLY1305_SHA256,TLS_AES_128_GCM_SHA256
```

### Certificate Authentication Setup

1. **Trust store configuration:**
```bash
# Create trust store with CA certificates
keytool -import \
  -alias espi-ca \
  -file espi-ca.crt \
  -keystore truststore.jks \
  -storepass changeit \
  -noprompt
```

2. **Application configuration:**
```yaml
espi:
  security:
    certificate:
      enable-certificate-authentication: true
      trust-store-path: classpath:certificates/truststore.jks
      trust-store-password: changeit
      enable-certificate-revocation-check: true
      enable-ocsp-check: true
```

### Admin API Bearer Access (`espi_admin` client)

These admin REST APIs accept a client_credentials bearer token:

- `/admin/**`
- `/api/v1/oauth2/clients/**`
- `/api/v1/certificates/**`
- `/api/v1/datacustodian/**`

The token is opaque, resolved in-process, and stateless. No session or CSRF token is needed. Only the dedicated `authserver.admin` scope grants admin rights. ESPI scopes such as `DataCustodian_Admin_Access` do **not** grant admin rights.

| Property | Environment variable | Default |
|----------|----------------------|---------|
| `espi.authserver.admin-client.secret` | `ESPI_ADMIN_CLIENT_SECRET` | *(unset: bearer access disabled)* |

- **When the secret is set**, the server seeds an `espi_admin` client at startup:
  - grant type: `client_credentials` only
  - scope: `authserver.admin`
  - tokens: opaque (reference), with a 15-minute access-token TTL
  - the secret is stored as a `{bcrypt}` hash, never in plaintext.
- **On later restarts**, the row is rewritten only when the configured secret no longer matches the stored hash (rotation) or a managed field has drifted.
- **When the secret is unset**, the client is not seeded and the server logs `Admin API bearer access is disabled`.

Supply the secret from a secret store (a Kubernetes Secret, Vault, etc.). Never commit it to a profile YAML file.

Obtain and use a token:

```bash
TOKEN=$(curl -s -u "espi_admin:${ESPI_ADMIN_CLIENT_SECRET}" \
  -d grant_type=client_credentials -d scope=authserver.admin \
  https://auth.example.com/oauth2/token | jq -r .access_token)

curl -H "Authorization: Bearer ${TOKEN}" https://auth.example.com/admin/oauth2/clients
```

Responses:

| Token | Response |
|-------|----------|
| Missing, unknown, revoked or expired | `401` with `WWW-Authenticate: Bearer error="invalid_token"` |
| Valid, but without `authserver.admin` | `403` with `error="insufficient_scope"` |

Requests without an `Authorization: Bearer` header fall through to the session-login chain, which requires `ROLE_ADMIN` or `ROLE_DC_ADMIN`.

`/connect/**` (client registration) is not served by the bearer chain. It remains session-only and admin-only.

### Human Administrators: Password + Passkey (WebAuthn) MFA

Human administrators of the Authorization Server are stored in `admin_users`, with roles in `admin_user_roles` (Flyway `V9_0_0`). Their passkeys are stored in Spring Security's `user_entities` / `user_credentials` tables.

Retail customers never sign in here. They authenticate at the Data Custodian.

A browser session reaches the admin paths only if it has **all** of the following:

- `ROLE_ADMIN` or `ROLE_DC_ADMIN`
- the password factor (form login at `/login`)
- the passkey factor (WebAuthn)

The admin paths are `/admin/**`, `/api/v1/admin/**`, `/api/v1/oauth2/clients/**`, `/api/v1/certificates/**`, `/api/v1/datacustodian/**` and `/connect/**`.

The MFA requirement applies to these paths only. The OAuth2 protocol endpoints, the delegated customer flow, introspection and the `espi_admin` bearer chain are unaffected.

#### Bootstrap the first administrator

| Property | Environment variable |
|----------|----------------------|
| `espi.authserver.admin.bootstrap.username` | `ESPI_BOOTSTRAP_ADMIN_USERNAME` |
| `espi.authserver.admin.bootstrap.password-hash` | `ESPI_BOOTSTRAP_ADMIN_PASSWORD_HASH` |

At startup, if `admin_users` is **empty** and both values are set, the server creates that user with `ROLE_ADMIN`. If any administrator already exists, the values are ignored. Remove them after the first start.

- **Username:** must start with a letter (3-100 of `[A-Za-z0-9._@-]`).
- **Password hash:** must be a bcrypt hash, either bare (`$2a$…`, `$2b$…`, `$2y$…`) or prefixed with `{bcrypt}`. Plaintext or any other scheme is rejected and startup fails.

Generate a hash:

```bash
# htpasswd (apache2-utils); strip the leading ":" it prints
htpasswd -bnBC 12 "" 'your-long-passphrase' | tr -d ':\n'

# or with Python (pip install bcrypt)
python3 -c 'import bcrypt,getpass; print(bcrypt.hashpw(getpass.getpass().encode(), bcrypt.gensalt(12)).decode())'
```

In a shell or `.env` file, quote the value: bcrypt hashes contain `$`.

#### WebAuthn relying party per environment

| Property | Default (dev) | Production example |
|----------|---------------|--------------------|
| `espi.authserver.admin.webauthn.rp-id` | `localhost` | `auth.example.com` |
| `espi.authserver.admin.webauthn.rp-name` | `OpenESPI Authorization Server` | your display name |
| `espi.authserver.admin.webauthn.allowed-origins` | `http://localhost:9999` | `https://auth.example.com` |

- **`rp-id`** is the host name (no scheme or port). It must equal the origin's host or be a registrable parent domain of it.
- **`allowed-origins`** is a comma-separated list of exact origins (`scheme://host[:port]`) that admins use in the browser.
- **Browsers allow WebAuthn only over HTTPS.** The one exception is `http://localhost`.
- **Changing `rp-id` invalidates every enrolled passkey.** Admins must then re-enrol, which an administrator can trigger with "reset passkeys".

#### Enrolment and sign-in flow

1. **First sign-in.** The admin signs in at `/login` with username and password. With no passkey registered, they are redirected to `/webauthn/register`, Spring Security's registration page.
   - Until a passkey is registered, every admin path refuses the session (it lacks the WebAuthn factor).
2. **Register a passkey** on that page. A security key, platform authenticator or synced passkey all work.
3. **Later sign-ins.** After the password step, opening an admin path redirects to `/login?factor.type=webauthn&factor.reason=missing` to complete the passkey step.
   - The page and API both require the two factors together.

#### Admin-user API (`/api/v1/admin/users`, requires `ROLE_ADMIN`)

| Method & path | Purpose |
|---------------|---------|
| `GET /api/v1/admin/users` | List users. Password hashes are never returned. |
| `POST /api/v1/admin/users` `{username, password, roles?}` | Create a user. Hashed server-side; minimum password length 12 (bcrypt maximum 72 bytes); roles `ADMIN` / `DC_ADMIN`, default `ADMIN`. |
| `PUT /api/v1/admin/users/{id}/enabled` `{enabled}` | Enable or disable a user. |
| `PUT /api/v1/admin/users/{id}/password` `{password}` | Reset the password. |
| `PUT /api/v1/admin/users/{id}/roles` `{roles}` | Replace the user's roles. |
| `DELETE /api/v1/admin/users/{id}/passkeys` | Delete all of the user's passkeys, forcing re-enrolment. |
| `DELETE /api/v1/admin/users/{id}` | Delete the user. |

- **Self-protection:** an administrator cannot disable, delete or remove `ROLE_ADMIN` from their own account.
- **Last-admin protection:** nobody can disable, delete or demote the last enabled `ROLE_ADMIN` (409).
- **Access paths:** the API is reachable with a two-factor browser session (CSRF token required) or an `espi_admin` bearer token (no CSRF).
- **`ROLE_DC_ADMIN`** passes the URL rule but not the API's `hasRole('ADMIN')` check.

Not yet implemented (follow-ups): failed-login lockout and rate limiting, and audit events for admin-user changes.

### Database Security

1. **Create dedicated database user:**
```sql
-- MySQL
CREATE USER 'espi_authserver'@'%' IDENTIFIED BY 'secure_random_password';
GRANT SELECT, INSERT, UPDATE, DELETE ON espi_authserver.* TO 'espi_authserver'@'%';
GRANT CREATE, ALTER, INDEX, DROP ON espi_authserver.* TO 'espi_authserver'@'%';
FLUSH PRIVILEGES;

-- PostgreSQL
CREATE USER espi_authserver WITH PASSWORD 'secure_random_password';
GRANT CONNECT ON DATABASE espi_authserver TO espi_authserver;
GRANT USAGE ON SCHEMA public TO espi_authserver;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO espi_authserver;
```

2. **Enable SSL for database connections:**
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/espi_authserver?useSSL=true&requireSSL=true&verifyServerCertificate=true
```

### Authorization Purge and Audit Retention

Spring Authorization Server stores every grant (authorization code, access token, refresh token)
in `oauth2_authorization` and never deletes it. The Authorization Server therefore runs a scheduled
job that removes rows whose tokens have **all** expired, and keeps an audit record of each removal.

**What is purged.** A row is deleted only when every token expiry on it (authorization code, access
token, refresh token, and any OIDC ID token, device code or user code) is earlier than
`now - grace-period`. A row whose refresh token is still valid is never purged, however old its
access token is, so long-lived ESPI authorizations that refresh keep working. Rows with no token
expiry at all (abandoned authorization requests that hold only `state`) and rows holding a token
without an expiry are left alone.

**What is never purged.** `oauth2_authorization_consent` (the record of customer consent) and
`oauth2_registered_client` are never touched, and neither are the Data Custodian's ESPI
`authorizations` rows.

**Audit.** Before a row is deleted, one `AUTHORIZATION_PURGED` row is written to `oauth2_audit_log`
with the client, principal, grant type, granted scopes, and in `additional_data` the authorization
id, first issued time, last token issued time and latest expiry. Each batch runs in one transaction
and the audit row is written only if that instance's delete removed the row, so several AS instances
can run the job at the same time without duplicate audit records.

**Retention rationale.**
- Operational rows are kept for **7 days past their final expiry**, so recent grants are still
  available when troubleshooting a client's failed refresh or a customer's support call.
- Purge audit rows are kept for **13 months** (`395d`), long enough for annual reporting with a
  month of margin. A utility whose regulator requires a longer retention should raise
  `audit-retention` to match. The job deletes only `AUTHORIZATION_PURGED` audit rows; other audit
  event types are not affected.

**Configuration** (`espi.authserver.authorization-purge.*`; durations use Spring's `30s` / `15m` /
`1h` / `7d` format):

| Property | Default | Meaning |
|---|---|---|
| `enabled` | `true` | Set to `false` to disable the job entirely |
| `interval` | `1h` | Delay between the end of one run and the start of the next (must be positive) |
| `initial-delay` | `5m` | Delay after startup before the first run |
| `grace-period` | `7d` | How long a row is kept after its latest token expiry |
| `batch-size` | `500` | Rows deleted and audited per transaction (must be greater than 0) |
| `audit-retention` | `395d` | How long `AUTHORIZATION_PURGED` audit rows are kept |

Invalid values (a negative duration, a zero interval or batch size) stop the application at
startup. Each run that deletes something logs one INFO summary from `AuthorizationPurgeService`.

## Monitoring and Observability

### Metrics Configuration

1. **Prometheus metrics:**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: authserver
      environment: production
```

2. **Custom metrics:**
```java
@Component
public class CustomMetrics {
    private final MeterRegistry meterRegistry;
    private final Counter authSuccessCounter;
    private final Timer authTimer;
    
    public CustomMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.authSuccessCounter = Counter.builder("auth.success")
            .description("Successful authentications")
            .register(meterRegistry);
        this.authTimer = Timer.builder("auth.duration")
            .description("Authentication duration")
            .register(meterRegistry);
    }
}
```

### Health Checks

1. **Custom health indicators:**
```java
@Component
public class DataCustodianHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        try {
            // Check DataCustodian connectivity
            return Health.up()
                .withDetail("datacustodian", "available")
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("datacustodian", "unavailable")
                .withException(e)
                .build();
        }
    }
}
```

### Logging Configuration

1. **Structured logging:**
```yaml
logging:
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{traceId:-},%X{spanId:-}] %logger{36} - %msg%n"
  level:
    org.greenbuttonalliance: INFO
    org.springframework.security: DEBUG
```

2. **ELK Stack integration:**
```yaml
# logback-spring.xml
<configuration>
    <springProfile name="prod">
        <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
            <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
                <providers>
                    <timestamp/>
                    <logLevel/>
                    <loggerName/>
                    <message/>
                    <mdc/>
                    <stackTrace/>
                </providers>
            </encoder>
        </appender>
    </springProfile>
</configuration>
```

## Troubleshooting

### Common Issues

1. **Database Connection Issues:**
```bash
# Check database connectivity
mysql -h mysql-server -u espi_user -p espi_authserver
psql -h postgres-server -U espi_user -d espi_authserver

# Check Flyway migrations
curl http://localhost:8081/actuator/flyway
```

2. **SSL Certificate Issues:**
```bash
# Verify certificate
openssl x509 -in certificate.crt -text -noout

# Test SSL connection
openssl s_client -connect auth.yourdomain.com:443 -servername auth.yourdomain.com
```

3. **Memory Issues:**
```bash
# Check JVM memory usage
curl http://localhost:8081/actuator/metrics/jvm.memory.used

# Generate heap dump
jcmd <pid> GC.run_finalization
jcmd <pid> VM.gc
```

### Performance Tuning

1. **JVM tuning:**
```bash
export JAVA_OPTS="-Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseStringDeduplication"
```

2. **Database connection pool tuning:**
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

3. **Application tuning:**
```yaml
server:
  tomcat:
    threads:
      max: 200
      min-spare: 10
    max-connections: 8192
    accept-count: 100
```

### Debugging

1. **Enable debug logging:**
```yaml
logging:
  level:
    org.greenbuttonalliance: DEBUG
    org.springframework.security: DEBUG
    org.springframework.web: DEBUG
```

2. **Remote debugging:**
```bash
export JAVA_OPTS="$JAVA_OPTS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
```

3. **Profile application:**
```bash
java -jar -Dspring.profiles.active=prod \
     -XX:+FlightRecorder \
     -XX:StartFlightRecording=duration=60s,filename=authserver.jfr \
     authserver.jar
```

## Support and Resources

- **Documentation**: [OpenESPI Documentation](https://github.com/GreenButtonAlliance/OpenESPI-AuthorizationServer-java/wiki)
- **Issues**: [GitHub Issues](https://github.com/GreenButtonAlliance/OpenESPI-AuthorizationServer-java/issues)
- **Community**: [Green Button Alliance](https://www.greenbuttonalliance.org/)
- **NAESB ESPI Standards**: [NAESB.org](https://www.naesb.org/)

## License

This deployment guide is part of the OpenESPI Authorization Server project, licensed under the Apache License 2.0.