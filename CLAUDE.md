# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

OpenESPI-GreenButton-Java is a monorepo implementation of the NAESB Energy Services Provider Interface (ESPI) 4.0 specification for Green Button energy data standards. The project has been migrated to Java 21, Jakarta EE 9+, and Spring Boot 3.5.

## Build and Test Commands

### Build All Modules
```bash
# From repository root
mvn clean install

# Build only fully-migrated Spring Boot 3.5 modules (excludes thirdparty)
mvn clean install -Pspring-boot-only
```

### Run Tests
```bash
# Test all modules
mvn test

# Test specific module with dependencies
mvn test -pl openespi-datacustodian -am

# Test specific module only
cd openespi-common && mvn test

# Integration tests with TestContainers (requires Docker)
mvn verify -pl openespi-common -Pintegration-tests
```

### Run Applications
```bash
# Data Custodian (default: dev-mysql profile)
cd openespi-datacustodian && mvn spring-boot:run

# With specific profile
cd openespi-datacustodian && mvn spring-boot:run -Dspring-boot.run.profiles=dev-postgresql

# Authorization Server
cd openespi-authserver && mvn spring-boot:run

# Third Party (when migration complete)
cd openespi-thirdparty && mvn spring-boot:run
```

### Run Single Test
```bash
# Run specific test class
mvn test -Dtest=UsagePointRepositoryTest

# Run specific test method
mvn test -Dtest=UsagePointRepositoryTest#testFindById

# Run test in specific module
mvn test -pl openespi-common -Dtest=UsagePointRepositoryTest
```

## Architecture

### Module Dependencies
The monorepo follows a layered architecture with these dependencies:
- **openespi-common** → Foundation library (no dependencies on other modules)
- **openespi-datacustodian** → Depends on openespi-common
- **openespi-authserver** → Independent module (NO dependency on openespi-common)
- **openespi-thirdparty** → Depends on openespi-common

IMPORTANT: The Authorization Server (openespi-authserver) is completely independent and does NOT depend on openespi-common. It has its own entity definitions, repositories, and services for OAuth2 authorization.

### Domain Model Structure
The domain model is organized in `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/domain/`:
- **domain/common/** - Shared base entities (IdentifiedObject, LinkType, etc.)
- **domain/usage/** - ESPI energy usage entities (UsagePoint, MeterReading, IntervalBlock, ReadingType, RetailCustomer, etc.)
- **domain/customer/** - Customer entities from customer.xsd schema (CustomerAccount, CustomerAgreement, ServiceSupplier, Meter, etc.)

All ESPI domain entities extend `IdentifiedObject` which provides:
- UUID primary key (`id`)
- Self-reference URI (`selfLink`)
- Up-reference URI (`upLink`)
- XML marshalling support

### Service Layer Pattern
Services follow a consistent interface-implementation pattern:
- **Interfaces**: `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/service/`
- **Implementations**: `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/service/impl/`

Services are organized by domain:

**Usage Domain Services** (energy data from usage.xsd):
- `UsagePointService` - Energy usage point management
- `MeterReadingService` - Meter reading data
- `IntervalBlockService` - Time-series interval data
- `ReadingTypeService` - Reading type metadata
- `UsageSummaryService` - Usage aggregation summaries
- `ElectricPowerQualitySummaryService` - Power quality metrics
- `TimeConfigurationService` - Timezone and time parameters
- `RetailCustomerService` - Retail customer management (part of usage.xsd)

**Customer Domain Services** (from customer.xsd, in `service/customer/`):
- `CustomerAccountService` - Customer account management
- `CustomerService` - Customer information
- `MeterService` - Meter device information
- `ServiceLocationService` - Service location data
- `StatementService` - Billing statements

Note: Entities exist for CustomerAgreement and ServiceSupplier but services have not been implemented yet.

**Cross-Cutting Services**:
- `AuthorizationService` - OAuth2 authorization management
- `SubscriptionService` - Third-party subscriptions
- `ApplicationInformationService` - Application registration
- `NotificationService` - Event notifications
- `BatchListService` - Batch operations
- `DtoExportService` - Entity to DTO mapping and XML export

### REST API Structure
REST controllers are in `openespi-datacustodian/src/main/java/org/greenbuttonalliance/espi/datacustodian/web/`:
- **web/api/** - ESPI RESTful API endpoints (many currently `.disabled` during migration)
- **web/custodian/** - Data custodian-specific endpoints
- **web/customer/** - Retail customer portal endpoints

Note: Many REST controllers have `.disabled` extension during the Spring Boot 3.5 migration. They need to be re-enabled and tested after core functionality is validated.

### DTO and Mapping Layer
The project uses MapStruct for entity-to-DTO mappings:
- **DTOs**: `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/dto/`
- **Mappers**: `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/mapper/`

DTOs mirror ESPI XML schema structure and are used exclusively for XML representations. JSON is only used by openespi-authserver for OAuth2 operations.

## Database Management

### Supported Databases
- **MySQL 8.0+** (primary development database)
- **PostgreSQL 15+** (production recommended)
- **H2** (testing only via TestContainers)

### Spring Profiles
Application behavior is controlled via Spring profiles:
- `dev-mysql` - MySQL development (default)
- `dev-postgresql` - PostgreSQL development
- `local` - Local development settings
- `docker` - Docker container deployment
- `prod` - Production settings
- `test` - Test configuration with H2
- `testcontainers` - Integration tests with TestContainers

### Database Migrations
Flyway manages database schema migrations:
- **Location**: `openespi-common/src/main/resources/db/migration/`
- **MySQL scripts**: `V1__Create_Base_Tables.sql`, `V3__Create_additiional_Base_Tables.sql`
- **Run migrations**: Automatically on application startup with `spring.flyway.enabled=true`

IMPORTANT: When adding/modifying entities, ensure Flyway migration scripts are updated to match JPA entity definitions.

## Key Technologies

### Spring Boot 3.5 Stack
- **Spring Boot**: 3.5.0
- **Spring Security**: 6.x (OAuth2 Resource Server and Client)
- **Spring Data JPA**: 3.x with Hibernate 6.x
- **Spring Authorization Server**: Latest (for openespi-authserver only)

### Persistence
- **JPA/Hibernate**: 6.x with Jakarta Persistence API
- **UUID Primary Keys**: All entities use UUID instead of Long IDs
- **Flyway**: Database migration management
- **HikariCP**: Connection pooling

### Testing
- **JUnit 5**: Test framework
- **Mockito**: Mocking framework
- **TestContainers**: Docker-based integration testing for MySQL and PostgreSQL
- **Spring Boot Test**: Auto-configuration for testing

### Build Tools
- **Maven**: 3.9+
- **MapStruct**: 1.6.0 for DTO mapping
- **Lombok**: 1.18.34 for reducing boilerplate

## ESPI 4.0 Compliance

This codebase implements the NAESB ESPI 4.0 specification for Green Button data exchange.

### XML Schema Files
- **Location**: `openespi-common/src/main/resources/schema/ESPI_4.0/`
- **Files**: `espi.xsd` (usage data), `customer.xsd` (customer data)

### Atom Feed Format
ESPI uses Atom XML feeds for data exchange. Key patterns:
- Each resource has both feed (collection) and entry (single item) representations
- Feed and Entry self-links use pattern: `https://{host}/DataCustodian/espi/1_1/resource/{resource}/{id}` (where {resource} is the entity being received or sent)
- Entry links include `rel="self"` (resource URL), `rel="up"` (parent collection), `rel="related"` (related resources)
- All IDs are Type 5 UUIDs in format: `urn:uuid:xxxxxxxx-xxxx-5xxx-xxxx-xxxxxxxxxxxx`
- Link type attributes use `espi-entry/{resource}` or `espi-feed/{resource}` format

### Core ESPI Resources

**Usage Domain Resources** (from usage.xsd):
- **UsagePoint**: Represents a metering point (e.g., electric meter)
- **MeterReading**: Collection of interval blocks for a usage point
- **IntervalBlock**: Time-series data with individual reading intervals
- **ReadingType**: Metadata describing measurement characteristics
- **ElectricPowerQualitySummary**: Power quality metrics
- **UsageSummary**: Aggregated usage statistics
- **LocalTimeParameters**: Timezone configuration
- **RetailCustomer**: Retail customer information

**Customer Domain Resources** (from customer.xsd):
- **CustomerAccount**: Customer billing account
- **CustomerAgreement**: Agreement between customer and supplier
- **ServiceSupplier**: Utility or energy service provider
- **Meter**: Physical meter device information
- **ServiceLocation**: Physical location of service delivery
- **Statement**: Billing statement information

## Development Workflow

### Branch Strategy
- **Main branch**: `main` (production-ready code)
- **Feature branches**: `feature/description` for new features
- **Bug fixes**: `fix/description` for bug fixes
- **Always create feature branch before making changes** - never commit directly to main
- See BRANCH_STRATEGY.md for detailed workflow

### Testing Requirements
- All tests must pass before committing: `mvn test`
- Run tests for affected module and its dependents: `mvn test -pl <module> -am`
- Integration tests should pass before PR merge: `mvn verify -Pfull-integration`

### Common Development Patterns

#### Adding New ESPI Entity
1. Create entity in `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/domain/usage/` (or `/customer/`)
2. Extend `IdentifiedObject` base class
3. Add JPA annotations (`@Entity`, `@Table`, `@Column`)
4. Create repository interface in `repositories/`
5. Create service interface and implementation in `service/` and `service/impl/`
6. Create DTO in `dto/` package
7. Create MapStruct mapper interface in `mapper/`
8. Add Flyway migration script in `db/migration/`
9. Write unit tests and integration tests

#### JPA Mapping Guidelines
- All entities use `@Id` with `UUID` type, not `Long`
- Use `@JoinColumn` for foreign keys with explicit `name` attribute
- Avoid `@Where` annotation conflicts with `@JoinColumn` on the same field
- Be careful with `@ElementCollection` and `@CollectionTable` for embedded collections
- Phone numbers and addresses are embedded collections using `@ElementCollection`

#### REST Controller Development
- Controllers in `openespi-datacustodian` implement ESPI REST API
- Use `@RestController` and `@RequestMapping` annotations
- Follow existing patterns in enabled controllers (e.g., `UsagePointController`, `MeterReadingController`)
- Many controllers currently disabled (`.disabled` extension) - re-enable after testing core functionality

## Migration Status

The codebase is actively being migrated to Spring Boot 3.5. Key migration achievements:
- Java 21 upgrade complete across all modules
- Jakarta EE 9+ migration complete (javax → jakarta namespace)
- Spring Boot 3.5 migration complete for common, datacustodian, authserver
- UUID primary keys migrated from Long IDs
- OAuth2 modernized with Spring Security 6.x patterns
- RestTemplate replaced with WebClient

### Known Issues
Check migration status documents for current issues:
- `2025-07-15_Claude_Code_Spring_Boot_3.5_Migration_Plan.md` - Overall migration plan
- `openespi-common/SPRING_BOOT_3.5_MIGRATION_STATUS.md` - Common module status
- `openespi-authserver/MIGRATION_ROADMAP.md` - Auth server status

## Future Updates

Planned technology upgrades:
- **Java 25**: Upgrade from Java 21 to Java 25 LTS when released
- **Spring Boot 4.0**: Migrate from Spring Boot 3.5 to Spring Boot 4.0

## OAuth2 Security

### Authorization Flow
The system implements OAuth2 authorization code flow:
1. Third-party application requests authorization
2. Retail customer authenticates with openespi-authserver
3. Authorization server issues authorization code
4. Third-party exchanges code for access token
5. Third-party accesses data custodian resources with token

### Security Configuration
- **Authorization Server**: `openespi-authserver` - Spring Authorization Server (independent module)
- **Resource Server**: `openespi-datacustodian` - Validates OAuth2 tokens
- **OAuth2 Client**: `openespi-thirdparty` - Third-party application using WebClient with OAuth2

## Troubleshooting

### Build Failures
- Ensure Java 21 is installed: `java -version`
- Clean build: `mvn clean install`
- Check for profile-specific issues: review active Spring profile

### Test Failures
- Ensure Docker is running for TestContainers tests
- Check database connection settings in application-test.yml
- Review test logs in `target/surefire-reports/`

### Application Startup Issues
- Check Spring profile is correctly set: `SPRING_PROFILES_ACTIVE`
- Verify database is accessible and schema is current
- Review application logs for JPA mapping errors
- Check for duplicate column mappings in entities

### JPA Mapping Errors
- Look for duplicate `@Column` or `@JoinColumn` definitions
- Check for conflicts between `@Where` and relationship mappings
- Verify Flyway migration scripts match entity definitions
- Review entity inheritance structure for mapping conflicts

## Additional Documentation

- **README.md** - Project overview and quick start
- **BRANCH_STRATEGY.md** - Git workflow and branching strategy
- **2025-07-15_Claude_Code_Spring_Boot_3.5_Migration_Plan.md** - Migration status
- **openespi-common/CONTRIBUTING.md** - Contribution guidelines
- **openespi-authserver/DEPLOYMENT_GUIDE.md** - Production deployment
- **openespi-authserver/CERTIFICATE_AUTHENTICATION.md** - Client certificate auth