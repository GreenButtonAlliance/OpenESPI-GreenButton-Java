# OpenESPI GreenButton Java - Project Guidelines

## Project Overview

This is a complete monorepo implementation of the NAESB Energy Services Provider Interface (ESPI) 4.0 specification for Green Button energy data standards. The project provides OAuth2-based energy data exchange capabilities between utilities, third-party applications, and consumers.

**Key Technologies:**
- Java 21 (LTS)
- Spring Boot 3.5.0 (Jakarta EE 9+)
- Maven 3.9+ multi-module build
- OAuth2 authorization framework
- Green Button energy data standards

## Project Structure

The project is organized as a Maven multi-module monorepo with 4 main modules:

### Core Modules
- **`openespi-common/`** - Shared domain models, services, and utilities (Production ready)
- **`openespi-datacustodian/`** - OAuth2 resource server for energy data (Production ready)  
- **`openespi-authserver/`** - OAuth2 authorization server (Production ready)
- **`openespi-thirdparty/`** - Client application (Partial Spring Boot migration)

### Module Dependencies
```
openespi-thirdparty ──┐
openespi-datacustodian ├──► openespi-common
openespi-authserver ──┘
```

### Key Directories
- `src/main/java/` - Java source code
- `src/main/resources/` - Configuration files, templates
- `src/test/java/` - Unit and integration tests
- `etc/` - Database scripts (init, cleanup, seed data)
- `docker/` - Docker configuration (authserver only)
- `target` - Directory for build artifacts. DO NOT EDIT FILES IN THIS DIRECTORY

## Testing Requirements

**Always run tests** to verify correctness of proposed solutions:

### Running Tests
```bash
# Test all modules
mvn clean test

# Test specific module with dependencies
mvn test -pl openespi-datacustodian -am

# Test only Spring Boot 3.5 modules
mvn test -Pspring-boot-only

# Module-specific testing
cd openespi-common && mvn test
cd openespi-datacustodian && mvn test -am
cd openespi-authserver && mvn test -am
```

### Test Categories
- **Unit tests** - Fast, isolated component testing
- **Integration tests** - Database and service integration
- **OAuth2 flow tests** - Authorization and token exchange
- **Green Button compliance tests** - ESPI specification adherence

## Build Instructions

**Build the project** before submitting results to ensure compilation success:

### Standard Build
```bash
# Build all modules (recommended)
mvn clean install

# Build only production-ready Spring Boot 3.5 modules
mvn clean install -Pspring-boot-only
```

### Development Build
```bash
# Quick compile without tests
mvn clean compile

# Package without running tests (use sparingly)
mvn clean package -DskipTests
```

### Running Applications
```bash
# Data Custodian (port 8080)
cd openespi-datacustodian && mvn spring-boot:run

# Authorization Server (port 8081)
cd openespi-authserver && mvn spring-boot:run

# Third Party (port 8082)
cd openespi-thirdparty && mvn spring-boot:run
```

## Code Style Guidelines

### Java Standards
- **Java 21** features encouraged (records, pattern matching, text blocks)
- **Jakarta EE 9+** APIs (jakarta.* packages, not javax.*)
- **Spring Boot 3.5** patterns and conventions
- **Maven** standard directory layout

### Naming Conventions
- Classes: `PascalCase` (e.g., `UsagePointService`)
- Methods/Variables: `camelCase` (e.g., `findByRetailCustomerId`)
- Constants: `UPPER_SNAKE_CASE` (e.g., `DEFAULT_TIMEOUT`)
- Packages: `lowercase.with.dots` (e.g., `org.greenbuttonalliance.espi.common`)

### Architecture Patterns
- **Service Layer** - Business logic in `*Service` interfaces with `*ServiceImpl` implementations
- **Repository Layer** - Data access via Spring Data JPA repositories
- **Controller Layer** - REST endpoints with proper HTTP status codes
- **DTO Pattern** - Data transfer objects for API boundaries

### Spring Boot Conventions
- Use `@Service`, `@Repository`, `@Controller` annotations
- Configuration via `application.yml` (preferred over `.properties`)
- Profile-specific configs: `application-{profile}.yml`
- Auto-configuration over manual bean definitions

### Testing Standards
- Test classes end with `Test` or `Tests`
- Use `@SpringBootTest` for integration tests
- Use `@MockBean` for mocking Spring components
- Arrange-Act-Assert pattern in test methods

## Migration Status Awareness

When working on the project, be aware of the migration status:

### Production Ready (Spring Boot 3.5)
- `openespi-common` - Full Spring Boot 3.5 migration ✅
- `openespi-datacustodian` - Full Spring Boot 3.5 migration ✅  
- `openespi-authserver` - Full Spring Boot 3.5 migration ✅

### Partial Migration
- `openespi-thirdparty` - Java 21 + Jakarta ready, Spring Boot migration in progress ⚠️

### Special Considerations
- When modifying `openespi-thirdparty`, be mindful of ongoing Spring Boot migration
- Prefer Jakarta EE APIs over legacy javax APIs
- Use Spring Boot 3.5 patterns in production-ready modules

## Java Coding Hints and Conventions
* The @MockBean annotation is deprecated. Use @MockitoBean instead.
* When writing unit tests, verify any required properties have values, unless testing an exception condition.
* When writing unit tests, be sure to test exception conditions, such as validation constraint errors, and ensure the correct exception is thrown.
* When writing unit tests for classes which implement an interface NEVER create a test implementation of the interface for the class under test.
* When adding properties to JPA entities or DTOs, add the new properties after other properties, but above the user, dateCreated, and dateUpdated properties.

## Spring Conventions
* For dependency injection, favor using private final variables in conjunction with Lombok's @RequiredArgsConstructor.

### DTO Conventions
- Use DTOs for Spring MVC controllers
- Name DTOs with `Dto` suffix
- For HTTP Get and List operations use <classname>Dto
- For HTTP Create operations use <classname>Dto. Do not add the id, version, createdDate, or dateUpdated properties to the creation DTO. Ignore the id, version, createdDate, and dateUpdated these properties in MapStruct mappings.
- For HTTP Update operations use <classname>Dto. The update DTO should NOT include the id, createdDate, and dateUpdated properties. The version property should be used to check for optimistic locking.
- For HTTP Patch operations use <classname>Dto. The patch DTO should NOT include the id, createdDate, and dateUpdated properties. The version property should be used to check for optimistic locking. Patch operations should be used to update a single property. The PatchDTO should NOT have validation annotations preventing null or empty values.

### JPA Conventions
- Use an `Interger` version property annotated with `@Version` for optimistic locking
- When mapping enumerations to database columns, use `@Enumerated(EnumType.STRING)` to store the enum name instead of the ordinal value.
- Use a property named `createdDate` of type `LocalDateTime` with `@CreationTimestamp` for the creation date. The column description should use `updatabele = false` to prevent updates to the createdDate property.
- Use a property named `dateUpdated` of type `LocalDateTime` with `@UpdateTimestamp` for the last update date.
- Do not use the Lombok `@Data` annotation on JPA entities. Use `@Getter` and `@Setter` instead.
- Do not add the '@Repository' annotation to the repository interface. The Spring Data JPA will automatically create the implementation at runtime.
- Use the `@Transactional` annotation on the service class to enable transaction management. The `@Transactional` annotation should be used on the service class and not on the repository interface.
- Use the `@Transactional(readOnly = true)` annotation on the read-only methods of the service class to enable read-only transactions. This will improve performance and reduce locking on the database.
- When adding methods to the repository interface, try to avoid using the `@Query` annotation. Use the Spring Data JPA method naming conventions to create the query methods. This will improve readability and maintainability of the code.
- In services when testing the return values of optionals, throw the `NotFoundException` if the optional is empty. This will provide a 404 response to the client. The `NotFoundException` should be thrown in the service class and not in the controller class.

### Mapstruct Conventions
- When creating mappers for patch operations, use the annotation `@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE, nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)` to ignore null values in the source object. This will prevent null values from overwriting existing values in the target object.
- Mapper implementations are generated at compile time. If the context is not loading because of missing dependencies, compile java/main to generate the mappers.

### Unit Test Conventions
- When creating unit tests, use datafaker to generate realistic test data values.
- When creating or updating tests, use the Junit `@DisplayName` annotation to provide a human readable name for the test. This will improve the quality of the test report.
- When creating or updating tests, use the Junit `@Nested` annotation to group related tests. This will improve the readability of the test report.
- When investigating test failures of transaction tests, verify the service implementation uses saveAndFlush() to save the entity. This will ensure the entity is saved to the database before the transaction is committed.
