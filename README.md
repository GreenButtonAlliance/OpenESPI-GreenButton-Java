# OpenESPI GreenButton Java

Complete monorepo implementation of the NAESB Energy Services Provider Interface (ESPI) 4.0 specification for Green Button energy data standards.

## 🚀 Quick Start

```bash
# Clone the monorepo
git clone https://github.com/GreenButtonAlliance/OpenESPI-GreenButton-Java.git
cd OpenESPI-GreenButton-Java

# Build all modules (Java 25 + Jakarta EE throughout)
mvn clean install

# Run Spring Boot 3.5 modules
cd openespi-datacustodian && mvn spring-boot:run
cd openespi-authserver && mvn spring-boot:run
```

## 📦 Modules

| Module | Description | Java | Jakarta EE | Spring Boot | Status |
|--------|-------------|------|------------|-------------|--------|
| **openespi-common** | Shared domain models, services | 25 ✅ | 11 ✅       | 4.0.1 ✅     | **Production** |
| **openespi-datacustodian** | OAuth2 resource server | 25 ✅ | 11 ✅       | 4.0.1 ✅     | **Production** |
| **openespi-authserver** | OAuth2 authorization server | 25 ✅ | 11 ✅       | 4.0.1 ✅     | **Production** |
| **openespi-thirdparty** | Client application | 25 ✅ | ✅          | 4.0.1 ⚠️    | **Partial Migration** |

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
- **openespi-common**: Foundation library (no dependencies on other modules)
- **openespi-datacustodian**: Depends on openespi-common
- **openespi-authserver**: **Independent** (no dependency on openespi-common)
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
- Docker Desktop installed and running
- Minimum 4GB RAM allocated to Docker
- Ports 3306 (MySQL) and 5432 (PostgreSQL) available

**Quick Start:**
```bash
# Verify Docker is running
docker --version && docker ps

# Run all integration tests (H2, MySQL, PostgreSQL)
mvn verify -pl openespi-common -Pintegration-tests

# Run specific database tests
mvn test -Dtest=ComplexRelationshipMySQLIntegrationTest -pl openespi-common
mvn test -Dtest=ComplexRelationshipPostgreSQLIntegrationTest -pl openespi-common

# Cross-module integration
mvn verify -Pfull-integration
```

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

Each module has independent deployment capabilities:
- **Common**: Maven Central library (shared dependency)
- **DataCustodian**: Kubernetes/Docker deployment (Spring Boot 3.5 JAR)
- **AuthServer**: Kubernetes/Docker deployment (Spring Boot 3.5 JAR)
- **ThirdParty**: WAR deployment to Tomcat/Jetty ⚠️ **Temporary** - awaiting Spring Boot 3.5 migration for containerization

**Note on ThirdParty Deployment:**
ThirdParty currently uses legacy WAR packaging because it runs on Spring Framework (not Spring Boot) with JSP/JSTL views. Once the Spring Boot 3.5 migration completes, it will switch to executable JAR packaging with embedded server and Docker/Kubernetes deployment like the other modules.

### Docker Build
```bash
# Build individual service images
cd openespi-datacustodian && docker build -t openespi-datacustodian .
cd openespi-authserver && docker build -t openespi-authserver .
```

### Kubernetes Deployment
```bash
# Deploy with Helm
helm install openespi-datacustodian ./helm/datacustodian
helm install openespi-authserver ./helm/authserver
```

## 🌿 Branch Structure

This monorepo was created by consolidating separate repositories:

### Original Sources
- **openespi-common**: From OpenESPI-Common-java (main branch)
- **openespi-datacustodian**: From OpenESPI-DataCustodian-java (main branch)
- **openespi-authserver**: From OpenESPI-AuthorizationServer-java (main branch)
- **openespi-thirdparty**: From OpenESPI-ThirdParty-java (main branch)

All original commit history has been preserved using git subtree.

## 📚 Documentation

- [Migration Status](./MIGRATION_STATUS.md) - Detailed migration information
- [Development Guide](./docs/DEVELOPMENT.md) - Development workflow
- [Deployment Guide](./docs/DEPLOYMENT.md) - Production deployment
- [API Documentation](./docs/API.md) - REST API reference

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