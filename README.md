# OpenESPI GreenButton Java

> [!WARNING]
> **Pre-release: not for production use.** This repository is under active development and has
> not had its first release. The customer authorization flow is incomplete, and known security
> issues are being fixed under private advisories. Do not deploy it with real customer data. To
> report a vulnerability, see [SECURITY.md](./SECURITY.md); please do not open a public issue.

Monorepo implementation of the NAESB Energy Services Provider Interface (ESPI) specification (REQ.21 v4.1) for Green Button energy data standards.

## 🚀 Quick Start

```bash
# Clone the monorepo
git clone https://github.com/GreenButtonAlliance/OpenESPI-GreenButton-Java.git
cd OpenESPI-GreenButton-Java

# Build all modules (Java 25 + Jakarta EE throughout)
mvn clean install

# Run the Spring Boot applications (H2, no external database: add -Dspring-boot.run.profiles=local)
cd openespi-datacustodian && mvn spring-boot:run
cd openespi-authserver && mvn spring-boot:run
```

## 📦 Modules

All modules build with Java 25, Jakarta EE 11 and Spring Boot 4.1. Status is tracked in the
[CANONICAL BUILD PLAN epic (#119)](https://github.com/GreenButtonAlliance/OpenESPI-GreenButton-Java/issues/119).

| Module | Description | Status |
|--------|-------------|--------|
| **openespi-handoff** | Signed AS↔DC customer-flow handoff codec | Pre-release |
| **openespi-common** | Shared ESPI domain model, DTOs, mappers, services | Pre-release |
| **openespi-datacustodian** | ESPI resource server (opaque-token introspection) | Pre-release: GET API only; write endpoints deferred |
| **openespi-authserver** | OAuth2 authorization server | Pre-release: delegated customer flow incomplete (#232) |
| **openespi-thirdparty** | ESPI third-party client | Pre-release: data import not implemented (#89) |

## 🏗️ Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Third Party   │───▶│ Authorization   │───▶│ Data Custodian  │
│(Java 25+Jakarta)│    │ Server (SB 4.0) │    │ Server (SB 4.0) │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                                              │
         │                                              │
         └──────────────────────────────────────────────┘
                                 ▼
                    ┌─────────────────┐
                    │ OpenESPI Common │
                    │(Spring Boot 4.0)│
                    └─────────────────┘
```

**Module Dependencies:**
- **openespi-handoff**: Infrastructure-only handoff codec (no ESPI domain types)
- **openespi-common**: Foundation library (no dependencies on other modules)
- **openespi-datacustodian**: Depends on openespi-common and openespi-handoff
- **openespi-authserver**: Depends on openespi-handoff only (**no** dependency on openespi-common)
- **openespi-thirdparty**: Depends on openespi-common

## ✨ Migration Achievements

**All modules now support:**
- ✅ **Java 25** - Modern JVM with performance improvements
- ✅ **Jakarta EE 11+** - Modern enterprise Java APIs
- ✅ **Consistent build system** - Maven 3.9+ throughout

**Spring Boot 4.0 modules:**
- ✅ **openespi-common** - Foundation library
- ✅ **openespi-datacustodian** - Resource server
- ✅ **openespi-authserver** - Authorization server

**Partially migrated:**
- ⚠️ **openespi-thirdparty** - Java 25 + Jakarta ready, Spring Boot migration in progress

## 🛠️ Development

### All Modules (Recommended)
```bash
# Build everything - all modules are Java 25 compatible
mvn clean install

# Test specific module
mvn test -pl openespi-datacustodian -am
```

### Spring Boot 4.0 Only
```bash
# Build only fully-migrated modules
mvn clean install -Pspring-boot-only
```

### Module-Specific Development
```bash
# Work on Common module
cd openespi-common
mvn clean test
mvn spring-boot:run

# Work on DataCustodian
cd openespi-datacustodian
mvn clean test -am  # Test with dependencies
mvn spring-boot:run

# Work on AuthServer
cd openespi-authserver
mvn clean test -am
mvn spring-boot:run
```

## 📋 ThirdParty Migration Status

The ThirdParty module preserves important migration work from the main branch:

**✅ Completed (from main branch):**
- Java 1.7 → Java 25 upgrade
- javax.servlet → jakarta.servlet migration  
- JSP/JSTL Jakarta compatibility
- Modern Maven toolchain

**📝 Next Steps:**
- Spring Framework → Spring Boot 4.0 migration
- OAuth2 client modernization
- Configuration externalization

## 🧪 Testing

### Unit Tests
```bash
# Test all modules
mvn test

# Test specific module
mvn test -pl openespi-common
```

### Integration Tests with TestContainers

**Prerequisites:**
- A reachable Docker daemon (any of the following works):
  - Docker Desktop on Windows (WSL2 backend) or macOS
  - Docker Engine in WSL2 (without Docker Desktop)
  - Docker Engine native on Linux
  - Rancher Desktop or Podman with Docker socket compatibility
- Minimum 4GB RAM allocated to Docker
- TestContainers auto-detects the Docker socket — no project configuration required

> **Note for WSL2 users running Docker Engine without Docker Desktop:** make sure the daemon is running (`sudo systemctl start docker`) and your user is in the `docker` group (`sudo usermod -aG docker $USER`, then re-login). If `dockerd` fails to start, check `/etc/docker/daemon.json` for a `hosts` entry that conflicts with the systemd unit's `-H fd://` flag.

**Quick Start:**
```bash
# Verify Docker is reachable (any setup above)
docker --version && docker ps

# Run all openespi-common tests, including the Testcontainers ones (H2, MySQL, PostgreSQL)
mvn verify -pl openespi-common -am

# Run specific database tests
mvn test -Dtest=ComplexRelationshipMySQLIntegrationTest -pl openespi-common
mvn test -Dtest=ComplexRelationshipPostgreSQLIntegrationTest -pl openespi-common

# Authorization Server: only the Testcontainers suite runs until #129 (same as CI)
mvn verify -pl openespi-authserver -am -Dgroups=testcontainers-it -DfailIfNoTests=false
```

CI, release and versioning: see [`.github/CI_CD_SETUP.md`](.github/CI_CD_SETUP.md) and [`RELEASING.md`](RELEASING.md).

**What Gets Tested:**
- ✅ **H2**: In-memory database with vendor-neutral migrations
- ✅ **MySQL 8.0**: TestContainers with BLOB column types
- ✅ **PostgreSQL 15**: TestContainers with BYTEA column types
- ✅ **Flyway Migrations**: Vendor-specific migration paths
- ✅ **JPA Relationships**: Complex entity hierarchies
- ✅ **Transaction Boundaries**: Data consistency across transactions
- ✅ **Bulk Operations**: saveAll, deleteAll operations

**TestContainers Configuration:**
- MySQL container: `mysql:8.0`
- PostgreSQL container: `postgres:15-alpine`
- Container reuse enabled for faster test execution
- Automatic cleanup after tests complete

**Troubleshooting:**
```bash
# If containers fail to start
docker system prune  # Clean up Docker cache
docker pull mysql:8.0
docker pull postgres:15-alpine

# View running containers during tests
docker ps

# Check container logs
docker logs <container_id>
```

## 🗄️ Database Migrations

### Vendor-Specific Migration Structure

The project uses Flyway with a vendor-specific migration strategy to support H2, MySQL, and PostgreSQL:

```
openespi-common/src/main/resources/db/
├── migration/
│   ├── V1__Create_Base_Tables.sql           # Vendor-neutral base tables
│   └── V3__Create_additiional_Base_Tables.sql  # Additional shared tables
└── vendor/
    ├── h2/                                  # (Future: H2-specific migrations)
    ├── mysql/                               # (Future: MySQL-specific migrations)
    └── postgres/                            # (Future: PostgreSQL-specific migrations)
```

**Current Implementation:**
- All migrations are currently in the base `db/migration` directory
- Tables use vendor-neutral SQL syntax compatible with all three databases
- Future enhancements will move vendor-specific tables to `/vendor/{database}/` paths

**Migration Locations by Profile:**
- `test` (H2): `classpath:db/migration`
- `test-mysql`: `classpath:db/migration,classpath:db/vendor/mysql`
- `test-postgresql`: `classpath:db/migration,classpath:db/vendor/postgres`
- `dev-mysql`: `classpath:db/migration,classpath:db/vendor/mysql`
- `dev-postgresql`: `classpath:db/migration,classpath:db/vendor/postgres`

**Vendor-Specific Column Types:**
| Type | H2 | MySQL | PostgreSQL |
|------|------|-------|------------|
| Binary Data | `BINARY` | `BLOB` | `BYTEA` |
| UUID | `UUID` | `CHAR(36)` | `UUID` |
| Timestamps | `TIMESTAMP` | `DATETIME` | `TIMESTAMP` |

**Running Migrations:**
```bash
# Migrations run automatically on application startup
mvn spring-boot:run

# Test migrations with specific database
mvn test -Dtest=ComplexRelationshipMySQLIntegrationTest
mvn test -Dtest=ComplexRelationshipPostgreSQLIntegrationTest

# Validate migration status
mvn flyway:info -pl openespi-common
```

## 🚀 Deployment

There is no release yet; see the warning at the top. The three applications are executable
Spring Boot JARs (Thymeleaf views, embedded Tomcat). Each exposes `/actuator/health` (with
`liveness` and `readiness` groups) for orchestrators; other actuator endpoints require an admin
access token.

### Container images
```bash
# OCI image via Cloud Native Buildpacks (what the release workflow uses for the Data Custodian)
mvn spring-boot:build-image -DskipTests -pl openespi-datacustodian

# The Authorization Server also has a Dockerfile
cd openespi-authserver && docker build -t openespi-authserver .
```

Releasing is described in [RELEASING.md](./RELEASING.md) and CI in
[.github/CI_CD_SETUP.md](./.github/CI_CD_SETUP.md).

## 🌿 Branch Structure

This monorepo was created by consolidating separate repositories:

### Original Sources
- **openespi-common**: From OpenESPI-Common-java (main branch)
- **openespi-datacustodian**: From OpenESPI-DataCustodian-java (main branch)
- **openespi-authserver**: From OpenESPI-AuthorizationServer-java (main branch)
- **openespi-thirdparty**: From OpenESPI-ThirdParty-java (main branch)

All original commit history has been preserved using git subtree.

## 📚 Documentation

- [CANONICAL BUILD PLAN (#119)](https://github.com/GreenButtonAlliance/OpenESPI-GreenButton-Java/issues/119) - status and roadmap
- [CI/CD setup](./.github/CI_CD_SETUP.md) and [Releasing](./RELEASING.md)
- [openespi-handoff](./openespi-handoff/README.md) - AS↔DC signed-handoff contract
- [Authorization Server deployment guide](./openespi-authserver/DEPLOYMENT_GUIDE.md)
- [Contributing guidelines](./openespi-common/CONTRIBUTING.md)
- [Green Button API documentation](https://greenbuttonalliance.github.io/OpenESPI-GreenButton-API-Documentation/)
- [Security policy](./SECURITY.md)

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes in the appropriate module
4. Test your changes (`mvn test -pl <module> -am`)
5. Commit your changes (`git commit -m 'Add amazing feature'`)
6. Push to the branch (`git push origin feature/amazing-feature`)
7. Open a Pull Request

### Development Guidelines
- Follow existing code style in each module
- Write tests for new functionality
- Update documentation as needed
- Ensure CI/CD pipelines pass

## 📄 License

Licensed under the Apache License 2.0. See [LICENSE](./LICENSE) for details.

## 📧 Contact

- **Green Button Alliance**: https://www.greenbuttonalliance.org
- **Issues**: https://github.com/GreenButtonAlliance/OpenESPI-GreenButton-Java/issues
- **Discussions**: https://github.com/GreenButtonAlliance/OpenESPI-GreenButton-Java/discussions

---

**Migration Strategy:** All modules use `main` branches to preserve maximum migration work and ensure Java 25 + Jakarta EE consistency across the ecosystem.

**Built with ❤️ by the Green Button Alliance community**