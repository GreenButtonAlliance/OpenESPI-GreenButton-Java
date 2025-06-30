# OpenESPI GreenButton Java

Complete monorepo implementation of the NAESB Energy Services Provider Interface (ESPI) 4.0 specification for Green Button energy data standards.

## 🚀 Quick Start

```bash
# Clone the monorepo
git clone https://github.com/GreenButtonAlliance/OpenESPI-GreenButton-Java.git
cd OpenESPI-GreenButton-Java

# Build all modules (Java 21 + Jakarta EE throughout)
mvn clean install

# Run Spring Boot 3.5 modules
cd openespi-datacustodian && mvn spring-boot:run
cd openespi-authserver && mvn spring-boot:run
```

## 📦 Modules

| Module | Description | Java | Jakarta EE | Spring Boot | Status |
|--------|-------------|------|------------|-------------|--------|
| **openespi-common** | Shared domain models, services | 21 ✅ | 9+ ✅ | 3.5.0 ✅ | **Production** |
| **openespi-datacustodian** | OAuth2 resource server | 21 ✅ | 9+ ✅ | 3.5.0 ✅ | **Production** |
| **openespi-authserver** | OAuth2 authorization server | 21 ✅ | 9+ ✅ | 3.5.0 ✅ | **Production** |
| **openespi-thirdparty** | Client application | 21 ✅ | 9+ ✅ | 4.0.6 ⚠️ | **Partial Migration** |

## 🏗️ Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Third Party   │───▶│ Authorization   │───▶│ Data Custodian  │
│ (Java 21+Jakarta)│    │ Server (SB 3.5) │    │ Server (SB 3.5) │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 ▼
                    ┌─────────────────┐
                    │ OpenESPI Common │
                    │ (Spring Boot 3.5)│
                    └─────────────────┘
```

## ✨ Migration Achievements

**All modules now support:**
- ✅ **Java 21** - Modern JVM with performance improvements
- ✅ **Jakarta EE 9+** - Modern enterprise Java APIs
- ✅ **Consistent build system** - Maven 3.9+ throughout

**Spring Boot 3.5 modules:**
- ✅ **openespi-common** - Foundation library
- ✅ **openespi-datacustodian** - Resource server
- ✅ **openespi-authserver** - Authorization server

**Partially migrated:**
- ⚠️ **openespi-thirdparty** - Java 21 + Jakarta ready, Spring Boot migration in progress

## 🛠️ Development

### All Modules (Recommended)
```bash
# Build everything - all modules are Java 21 compatible
mvn clean install

# Test specific module
mvn test -pl openespi-datacustodian -am
```

### Spring Boot 3.5 Only
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
- Java 1.7 → Java 21 upgrade
- javax.servlet → jakarta.servlet migration  
- JSP/JSTL Jakarta compatibility
- Modern Maven toolchain

**📝 Next Steps:**
- Spring Framework → Spring Boot 3.5 migration
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

### Integration Tests
```bash
# TestContainers integration tests
mvn verify -pl openespi-common -Pintegration-tests

# Cross-module integration
mvn verify -Pfull-integration
```

## 🚀 Deployment

Each module has independent deployment capabilities:
- **Common**: Maven Central library
- **DataCustodian**: Kubernetes/Docker deployment
- **AuthServer**: Kubernetes/Docker deployment  
- **ThirdParty**: WAR deployment or future containerization

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

**Migration Strategy:** All modules use `main` branches to preserve maximum migration work and ensure Java 21 + Jakarta EE consistency across the ecosystem.

**Built with ❤️ by the Green Button Alliance community**