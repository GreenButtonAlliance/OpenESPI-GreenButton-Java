# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

OpenESPI-GreenButton-Java is a monorepo implementation of the NAESB REQ.21 Energy Services Provider Interface (ESPI) specification for Green Button energy data standards, targeting REQ.21 version 4.1 (whose official schemas are content-identical to 4.0 — see "ESPI Compliance" below). The project has been migrated to Java 25, Jakarta EE 10+, and Spring Boot 4.0.

## Build and Test Commands

### Build All Modules
```bash
# From repository root - builds all modules (handoff, common, datacustodian, thirdparty, authserver)
mvn clean install

# Build only core modules (common, datacustodian, thirdparty) - omits authserver
# NOTE: this profile's module list does not include openespi-handoff, which openespi-datacustodian requires
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

# TestContainers tests run as part of the normal surefire `mvn test` (requires Docker).
# There are no failsafe *IT classes and no separate integration-test profile.
mvn test -pl openespi-common

# What CI runs per module (matrix in .github/workflows/ci.yml): install upstream, then verify.
# For openespi-authserver this is the full suite (unit + MySQL Testcontainers tests) since #129;
# add -Dgroups=testcontainers-it to run only the Docker-backed integration tests.
mvn install -DskipTests -pl <module> -am && mvn verify -pl <module>
```

### Run Applications
```bash
# Data Custodian (default: dev-mysql profile)
cd openespi-datacustodian && mvn spring-boot:run

# With specific profile
cd openespi-datacustodian && mvn spring-boot:run -Dspring-boot.run.profiles=dev-postgresql

# Authorization Server
cd openespi-authserver && mvn spring-boot:run

# Third Party
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
The monorepo has five modules with these dependencies:
- **openespi-handoff** → Pure-infrastructure signed-handoff codec (HMAC + Jackson + JPA `handoff_nonces`) for the AS↔DC customer-flow redirect contract. Carries NO ESPI domain types.
- **openespi-common** → Foundation ESPI library (no dependencies on other modules)
- **openespi-datacustodian** → Depends on openespi-common and openespi-handoff (handoff is used by `web/authorize/` and contributes Flyway `V4__Create_Handoff_Nonces.sql`)
- **openespi-authserver** → Depends on openespi-handoff only (NO dependency on openespi-common)
- **openespi-thirdparty** → Depends on openespi-common

IMPORTANT: The Authorization Server (openespi-authserver) does NOT depend on openespi-common. It has its own entity definitions, repositories, and services for OAuth2 authorization. Its only shared module is openespi-handoff, which must stay free of ESPI domain types to preserve this invariant.

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
- `CustomerAgreementService` - Customer agreements
- `CustomerService` - Customer information
- `EndDeviceService` - End device information
- `MeterService` - Meter device information
- `ProgramDateIdMappingsService` - Program date/ID mappings
- `ServiceLocationService` - Service location data
- `StatementService` - Billing statements

Note: ServiceSupplier has an entity, DTO, and mapper but no repository or service yet.

**Cross-Cutting Services**:
- `AuthorizationService` - OAuth2 authorization management
- `SubscriptionService` - Third-party subscriptions
- `ApplicationInformationService` - Application registration
- `NotificationService` - Event notifications
- `BatchListService` - Batch operations
- `DtoExportService` - Entity to DTO mapping and XML export

### REST API Structure
REST controllers are in `openespi-datacustodian/src/main/java/org/greenbuttonalliance/espi/datacustodian/web/`:
- **web/api/** - ESPI RESTful API endpoints. GET-only (feed + entry) for ApplicationInformation, Authorization, Customer, CustomerAccount, UsagePoint, MeterReading, ReadingType, IntervalBlock, UsageSummary, ElectricPowerQualitySummary. `web/api/scope/` holds `ScopeResolver`, `ResourceScope`, `EspiQueryOptions` (not yet wired into controllers).
- **web/custodian/** - Data custodian admin portal (home, BatchList notification send, settings)
- **web/customer/** - Retail customer portal; most classes here are original legacy code with `@Controller` commented out
- **web/authorize/** - Customer authorization (per-FB consent) screen delegated from the AS
- **internal/backchannel/** - AS→DC subscription-provisioning back-channel

Current state (see GitHub epic #119 "CANONICAL BUILD PLAN" for the roadmap):
- All write endpoints (POST/PUT/DELETE) were deliberately removed in #166 and are deferred.
- Not yet implemented: `Batch/Subscription`, `Batch/Bulk`, `Batch/RetailCustomer`, `Subscription/{id}`, RetailCustomer-scoped URIs, LocalTimeParameters, ReadServiceStatus, and the CustomerAgreement/ServiceSupplier/ServiceLocation/Meter/EndDevice/Statement/ProgramIdMappings endpoints.
- Known defect: Subscription-scoped GETs currently ignore `subscriptionId` and return all records.
- The ten per-resource controllers duplicate find/map/marshal logic; the E1 resource engine (#187, branch `feature/187-e1-resource-engine-usagepoint-triad`, not yet merged) replaces them with one generic controller driven by `EspiResource` + `ResourceFbPolicy` (added in `openespi-common/.../common/scope/` on that branch, alongside the existing `FunctionBlock`).

### DTO and Mapping Layer
The project uses MapStruct for entity-to-DTO mappings:
- **DTOs**: `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/dto/`
- **Mappers**: `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/mapper/`

DTOs mirror ESPI XML schema structure and are used exclusively for XML representations. JSON is only used by openespi-authserver for OAuth2 operations.

#### JAXB Annotation Guidelines
**CRITICAL: JAXB annotations MUST only be applied to DTO classes, NEVER to entity or embeddable classes.**

- **Entity/Embeddable Classes** (`domain/` packages):
  - Use JPA annotations only: `@Entity`, `@Table`, `@Column`, `@Embeddable`, etc.
  - **ABSOLUTELY NO JAXB annotations**: Do not use `@XmlType`, `@XmlAccessorType`, `@XmlElement`, `@XmlRootElement`
  - Purpose: JPA persistence layer only

- **DTO Classes** (`dto/` packages):
  - Use JAXB annotations: `@XmlType`, `@XmlAccessorType`, `@XmlElement`, `@XmlRootElement`
  - NO JPA annotations
  - Purpose: XML marshalling/unmarshalling only

**If an embeddable class needs XML serialization but has no DTO:** Create a corresponding DTO class rather than adding JAXB annotations to the embeddable. This rule has NO exceptions.

**Rationale:** When both entity and DTO classes have JAXB annotations with the same XML type name and namespace, JAXB throws `IllegalAnnotationsException` due to duplicate type definitions when both are loaded in the same context. This strict separation ensures:
1. Clean architecture (persistence vs. presentation layers)
2. No JAXB namespace conflicts
3. Entities can be refactored without affecting XML schema
4. DTOs can be optimized for XML without affecting database schema

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
- **Shared scripts**: `openespi-common/src/main/resources/db/migration/` - `V1__Create_Base_Tables.sql`, `V3__Create_additiional_Base_Tables.sql`
- **Vendor-specific scripts**: `openespi-common/src/main/resources/db/vendor/{mysql,postgres,h2}/V2__*_Specific_Tables.sql`
- **Authorization Server**: has its own Flyway scripts under `openespi-authserver/src/main/resources/db/vendor/`; these currently diverge by vendor (MySQL V3–V6 are `.disabled`; see #145, #152)
- **Run migrations**: Automatically on application startup with `spring.flyway.enabled=true`

IMPORTANT: When adding/modifying entities, ensure Flyway migration scripts are updated to match JPA entity definitions.

## Key Technologies

### Spring Boot 4.0 Stack
- **Spring Boot**: 4.0.1
- **Spring Security**: 7.x (OAuth2 Resource Server and Client)
- **Spring Data JPA**: 4.x with Hibernate 7.x
- **Spring Authorization Server**: ships inside Spring Security 7 (for openespi-authserver only)

### Persistence
- **JPA/Hibernate**: 7.x with Jakarta Persistence API 3.2
- **UUID Primary Keys**: All entities use UUID Version 5 instead of Long IDs
- **Flyway**: Database migration management
- **HikariCP**: Connection pooling

### Testing
- **JUnit 5**: Test framework
- **Mockito**: Mocking framework
- **TestContainers**: Docker-based integration testing for MySQL and PostgreSQL
- **Spring Boot Test**: Auto-configuration for testing

### Build Tools
- **Maven**: 3.9+
- **MapStruct**: 1.6.3 for DTO mapping
- **Lombok**: 1.18.42 for reducing boilerplate

## ESPI Compliance (REQ.21 v4.1)

This codebase implements NAESB REQ.21 ESPI for Green Button data exchange, targeting version 4.1. REQ.21 v4.1 changes no wire behavior or schema relative to 4.0; its only substantive change is the deprecation of SFTP bulk (FB_34, FB_66), already reflected in `FunctionBlock.DEPRECATED_IDS`.

### XML Schema Files
- **`schema/ESPI_4.0/`** (`openespi-common/src/main/resources/`): `espi.xsd` (usage data), `customer.xsd` (customer data). These are content-identical to the official REQ.21 v4.1 schemas (`version="4.0.20231213"`) and are the validation target for certification.
- **`schema/ESPI_4.1/customer_4.1.xsd`**: a deliberate GBA refactoring of the customer schema that simplifies the structure and removes unnecessary intermediate classes. It is NOT the official NAESB file. `ESPI_4.1/atom.xsd` is the Atom schema.

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

### Testing Requirements
- All tests must pass before committing: `mvn test`
- Run tests for affected module and its dependents: `mvn test -pl <module> -am`
- TestContainers-based tests (Docker required) run inside the normal `mvn test`; there are no failsafe `*IT` classes and no separate integration-test profile (removed in #193)
- openespi-authserver runs its full suite in CI (#129); its Docker-backed tests are tagged `@Tag("testcontainers-it")` (via `AbstractAuthserverIntegrationTest`) so they can be run alone with `-Dgroups=testcontainers-it`
- **IMPORTANT**: Use AssertJ chained assertions - combine all assertions for a single object into one assertion chain
  - Good: `assertThat(retrieved).isPresent().hasValueSatisfying(device -> assertThat(device).extracting(...).containsExactly(...))`
  - Bad: Multiple separate `assertThat()` statements for the same object
  - This improves readability and provides better failure messages

### Common Development Patterns

#### File Modification Best Practices
**CRITICAL: Always use the Edit tool with visible diffs instead of sed/awk for file modifications**

- **NEVER use sed, awk, or bash text manipulation** for modifying source code or test files
- **ALWAYS use the Edit tool** which shows explicit diffs for review
- **Benefits of Edit tool**:
  - Changes are visible and reviewable before execution
  - Prevents cascading errors from bulk updates
  - Easy to verify correctness with explicit old_string → new_string diff
  - Catches errors immediately rather than discovering them during compilation

- **When multiple files need the same change**:
  - Use multiple Edit tool calls (one per file) in a single message
  - Show exactly what's changing in each file
  - Do NOT use sed/awk loops even if it seems more efficient

- **Exception**: Only use bash text tools (grep, find) for **searching/analysis**, never for modifications

**Example - WRONG approach:**
```bash
# DON'T DO THIS - sed makes changes invisibly
sed -i '' 's/OldClass/NewClass/g' src/**/*.java
```

**Example - CORRECT approach:**
```
# DO THIS - Use Edit tool with explicit diffs
Edit tool call showing:
old_string: "import com.example.OldClass;"
new_string: "import com.example.NewClass;"
```

This practice prevents issues like missing imports, broken references, and compilation errors that are difficult to track down.

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
- **toString() method sequencing**: Entity toString() methods MUST follow the exact database field sequence from Flyway migration scripts. Standard sequence is: id, description, created, updated, published, upLink, selfLink, [type-specific fields in database column order], relatedLinks (if present as last field). Always verify toString() matches the CREATE TABLE statement column order.

#### REST Controller Development
- Controllers in `openespi-datacustodian` implement ESPI REST API
- Use `@RestController` and `@RequestMapping` annotations
- New ESPI resource endpoints should be built on the E1 resource engine (#187) rather than as new per-resource controllers
- FB-to-resource gating must have a single source of truth - `ResourceFbPolicy` / `EspiResource` once #187 merges (until then, `SecurityConfiguration` holds the FB gates) - do not duplicate FB rules in controllers or security config
- Legacy controllers (`Long` ids, field `@Autowired`, commented-out bodies) remain in DC `web/customer/`, `web/custodian/RetailCustomerController`, `UploadController`, and most of openespi-thirdparty `web/`; port or delete them, do not copy their patterns

## Migration Status

The codebase has been migrated to Spring Boot 4.0.1 and Java 25. Key migration achievements:
- Java 25 upgrade complete across all modules
- Jakarta EE 10+ migration complete (javax → jakarta namespace)
- Spring Boot 4.0.1 migration complete for all modules
- UUID Version 5 primary keys migrated from Long IDs (RetailCustomer intentionally keeps its `Long` id - it is not an Atom resource)
- OAuth2 modernized with Spring Security 7.x patterns: opaque reference tokens + introspection (#122, #160); customer login/consent delegated from the AS to the DC via openespi-handoff; OIDC removed (#134)
- Spring Data JPA 4.x with Hibernate 7.x
- Schema-compliance Phases 1-26 (entities, DTOs, mappers, Flyway for usage and customer domains) merged Dec 2025-Feb 2026

Still in progress (logic migration from the original OpenESPI code):
- ESPI REST API completion (see "REST API Structure" above)
- openespi-thirdparty: notification receiver works, but fetched data is not imported (#89) and most portal controllers are original legacy code whose views have no templates
- `RestTemplate` still used in 6 main-code files (e.g. `NotificationServiceImpl`, AS `DataCustodianIntegrationService`); migrate to `RestClient`/`WebClient`
- Entity field type/length verification against the XSDs (#101 Phases 2-3)

### Known Issues
The source of truth for status and roadmap is GitHub epic #119 ("CANONICAL BUILD PLAN") and its sub-issues. The root `PHASE_*.md`, `MULTI_PHASE_SCHEMA_COMPLIANCE_PLAN.md`, `ISSUE_101_*.md`, `openespi-common/Phase_A-Flyway_Migration_Inventory.md`, `openespi-common/PROFILE_MIGRATION_SUMMARY.md`, `openespi-datacustodian/controll-update-status.md`, and `openespi-authserver/MIGRATION_ROADMAP.md` are historical and stale.

Known rule violations / defects to be aware of:
- `domain/common/DateTimeInterval` carries `@XmlType` despite being an `@Embeddable` (violates the JAXB rule above; collides with `DateTimeIntervalDto`)
- `MeterMapper` ignores meter multipliers (silently dropped on export/import)
- Five orphaned `*_related_links` tables are still created in the vendor V2 scripts

## Current Technology Stack

Current versions:
- **Java**: 25
- **Spring Boot**: 4.0.1
- **Spring Security**: 7.x
- **Spring Data JPA**: 4.x
- **Hibernate**: 7.x
- **Jakarta EE**: 10+

## OAuth2 Security

### Authorization Flow
The system implements OAuth2 authorization code flow:
1. Third-party application requests authorization from openespi-authserver
2. The AS delegates customer login and per-Function-Block consent to the Data Custodian (signed handoff via openespi-handoff, `/authorize/delegate` → DC → `/oauth2/authorize/continue`)
3. Authorization server issues authorization code; on token exchange it calls the DC back-channel to provision the Subscription
4. Third-party exchanges code for an opaque access token (response includes `resourceURI`, `authorizationURI`, and `customerResourceURI` when FB_51 is granted)
5. Third-party accesses data custodian resources with the token; the DC validates it by introspection and enforces FB scopes

### Security Configuration
- **Authorization Server**: `openespi-authserver` - Spring Authorization Server (depends only on openespi-handoff)
- **Resource Server**: `openespi-datacustodian` - Validates opaque tokens via introspection; enforces FB scopes in `SecurityConfiguration` (moving to `ResourceFbPolicy` with #187)
- **OAuth2 Client**: `openespi-thirdparty` - Third-party application using WebClient with OAuth2

## Troubleshooting

### Build Failures
- Ensure Java 25 is installed: `java -version`
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
- **openespi-handoff/README.md** - AS↔DC signed-handoff contract
- **openespi-common/CONTRIBUTING.md** - Contribution guidelines
- **openespi-authserver/DEPLOYMENT_GUIDE.md** - Production deployment
- **openespi-authserver/CERTIFICATE_AUTHENTICATION.md** - Client certificate auth