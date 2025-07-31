# Test Container Support Requirements for OpenESPI GreenButton Java

## Problem Statement

The OpenESPI GreenButton Java project currently has failing Flyway migrations due to database-specific data type incompatibilities. The JPA entities `UsagePointEntity` and `TimeConfigurationEntity` contain byte array properties (`byte[]`) that require vendor-specific database column types:

- **UsagePointEntity**: `roleFlags` property (maps to `role_flags` BLOB column)
- **TimeConfigurationEntity**: `dstEndRule` and `dstStartRule` properties (map to `dst_end_rule` and `dst_start_rule` BLOB columns)

The current migration structure uses MySQL-specific BLOB types that are incompatible with H2 and PostgreSQL databases, causing test failures across the project modules.

## Current State Analysis

### Existing Database Migration Structure
- Location: `openespi-common/src/main/resources/db/migration/mysql/V1__Create_Complete_Schema_Mysql.sql`
- Contains MySQL-specific BLOB column definitions
- Single vendor-specific migration file (1680 lines)

### Problematic Database Columns
1. **time_configurations table**:
   - `dst_end_rule BLOB`
   - `dst_start_rule BLOB`

2. **usage_points table**:
   - `role_flags BLOB`

### Current Test Configuration
- **MySQL Test**: `DataCustodianApplicationMysqlTest.java` (uses `test-mysql` profile)
- **PostgreSQL Test**: `DataCustodianApplicationPostgresTest.java` (uses `test-postgres` profile)
- Both tests currently use H2 with database-specific dialects
- Tests are expected to fail due to migration script errors
- No H2-specific test exists

## Requirements

### 1. Database Migration Refactoring

#### 1.1 Create Vendor-Neutral Base Migration
- **Location**: `openespi-common/src/main/resources/db/migration/V1__Create_Base_Tables.sql`
- **Content**: Extract all table definitions from the existing MySQL migration EXCEPT `time_configurations` and `usage_points` tables
- **Purpose**: Provide database-agnostic table definitions for all tables that don't require vendor-specific data types

#### 1.2 Create Vendor-Specific Migration Structure
```
openespi-common/src/main/resources/db/
├── migration/                    # Base migrations (vendor-neutral)
│   └── V1__Create_Base_Tables.sql
└── vendor/                       # Vendor-specific migrations
    ├── h2/
    │   └── V2__H2_Specific_Tables.sql
    ├── mysql/
    │   └── V2__MySQL_Specific_Tables.sql
    └── postgres/
        └── V2__PostgreSQL_Specific_Tables.sql
```

#### 1.3 Vendor-Specific Table Definitions

**H2 Migration** (`V2__H2_Specific_Tables.sql`):
```sql
-- Time Configuration Table (H2)
CREATE TABLE time_configurations (
    -- ... standard columns ...
    dst_end_rule   BINARY,
    dst_start_rule BINARY,
    -- ... rest of columns ...
);

-- Usage Point Table (H2)  
CREATE TABLE usage_points (
    -- ... standard columns ...
    role_flags BINARY,
    -- ... rest of columns ...
);
```

**MySQL Migration** (`V2__MySQL_Specific_Tables.sql`):
```sql
-- Time Configuration Table (MySQL)
CREATE TABLE time_configurations (
    -- ... standard columns ...
    dst_end_rule   BLOB,
    dst_start_rule BLOB,
    -- ... rest of columns ...
);

-- Usage Point Table (MySQL)
CREATE TABLE usage_points (
    -- ... standard columns ...
    role_flags BLOB,
    -- ... rest of columns ...
);
```

**PostgreSQL Migration** (`V2__PostgreSQL_Specific_Tables.sql`):
```sql
-- Time Configuration Table (PostgreSQL)
CREATE TABLE time_configurations (
    -- ... standard columns ...
    dst_end_rule   BYTEA,
    dst_start_rule BYTEA,
    -- ... rest of columns ...
);

-- Usage Point Table (PostgreSQL)
CREATE TABLE usage_points (
    -- ... standard columns ...
    role_flags BYTEA,
    -- ... rest of columns ...
);
```

### 2. Spring Boot Application Configuration Updates

#### 2.1 Default Profile (H2 Database)
**File**: `openespi-common/src/main/resources/application.yml`
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  jpa:
    hibernate:
      ddl-auto: validate
    database-platform: org.hibernate.dialect.H2Dialect
  flyway:
    enabled: true
    locations: 
      - classpath:db/migration
      - classpath:db/vendor/h2
```

#### 2.2 MySQL Development Profile
**File**: `openespi-common/src/main/resources/application-dev-mysql.yml`
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/openespi_dev
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: ${DB_USERNAME:openespi}
    password: ${DB_PASSWORD:openespi}
  jpa:
    hibernate:
      ddl-auto: validate
    database-platform: org.hibernate.dialect.MySQLDialect
  flyway:
    enabled: true
    locations:
      - classpath:db/migration
      - classpath:db/vendor/mysql
```

#### 2.3 PostgreSQL Development Profile
**File**: `openespi-common/src/main/resources/application-dev-postgresql.yml`
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/openespi_dev
    driver-class-name: org.postgresql.Driver
    username: ${DB_USERNAME:openespi}
    password: ${DB_PASSWORD:openespi}
  jpa:
    hibernate:
      ddl-auto: validate
    database-platform: org.hibernate.dialect.PostgreSQLDialect
  flyway:
    enabled: true
    locations:
      - classpath:db/migration
      - classpath:db/vendor/postgres
```

### 3. Test Container Implementation

#### 3.1 Update MySQL Test
**File**: `openespi-common/src/test/java/org/greenbuttonalliance/espi/common/migration/DataCustodianApplicationMysqlTest.java`

```java
@SpringBootTest(classes = { TestApplication.class })
@Testcontainers
@ActiveProfiles("test-mysql")
class DataCustodianApplicationMysqlTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("openespi_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.flyway.locations", () -> "classpath:db/migration,classpath:db/vendor/mysql");
    }

    @Test
    @DisplayName("MySQL Test Container - Application Context Loads Successfully")
    void contextLoads() {
        // Test validates MySQL-specific migrations and application context loading
    }
}
```

#### 3.2 Update PostgreSQL Test
**File**: `openespi-common/src/test/java/org/greenbuttonalliance/espi/common/migration/DataCustodianApplicationPostgresTest.java`

```java
@SpringBootTest(classes = { TestApplication.class })
@Testcontainers
@ActiveProfiles("test-postgres")
class DataCustodianApplicationPostgresTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("openespi_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.locations", () -> "classpath:db/migration,classpath:db/vendor/postgres");
    }

    @Test
    @DisplayName("PostgreSQL Test Container - Application Context Loads Successfully")
    void contextLoads() {
        // Test validates PostgreSQL-specific migrations and application context loading
    }
}
```

#### 3.3 Create H2 Test
**File**: `openespi-common/src/test/java/org/greenbuttonalliance/espi/common/migration/DataCustodianApplicationH2Test.java`

```java
@SpringBootTest(classes = { TestApplication.class })
@ActiveProfiles("test")
class DataCustodianApplicationH2Test {

    @Test
    @DisplayName("H2 In-Memory Database - Application Context Loads Successfully")
    void contextLoads() {
        // Test validates H2-specific migrations and application context loading
        // Uses default application.yml configuration with H2 in-memory database
    }

    @Test
    @DisplayName("H2 Database - Flyway Migrations Execute Successfully")
    void flywayMigrationsExecute() {
        // Additional test to specifically validate Flyway migration execution
    }
}
```

### 4. Test Configuration Files

#### 4.1 Remove Redundant Configuration
- **Remove**: Redundant database configuration from existing `application-test-mysql.yml` and `application-test-postgres.yml`
- **Reason**: Test containers will provide dynamic configuration via `@DynamicPropertySource`

#### 4.2 Minimal Test Profiles
**File**: `openespi-common/src/test/resources/application-test-mysql.yml`
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
    database-platform: org.hibernate.dialect.MySQLDialect
  flyway:
    enabled: true
logging:
  level:
    org.flywaydb: DEBUG
    org.hibernate.SQL: DEBUG
```

**File**: `openespi-common/src/test/resources/application-test-postgres.yml`
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
    database-platform: org.hibernate.dialect.PostgreSQLDialect
  flyway:
    enabled: true
logging:
  level:
    org.flywaydb: DEBUG
    org.hibernate.SQL: DEBUG
```

### 5. Maven Dependencies

#### 5.1 Required Test Dependencies
Add to `openespi-common/pom.xml`:
```xml
<dependencies>
    <!-- Existing dependencies -->
    
    <!-- Test Containers -->
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>mysql</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>postgresql</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### 6. Implementation Steps

1. **Create base migration file** by extracting non-problematic tables from existing MySQL migration
2. **Create vendor-specific migration files** for the three problematic tables
3. **Update application configuration files** with proper Flyway locations for each database vendor
4. **Implement test container configurations** for MySQL and PostgreSQL tests
5. **Create H2 test** for default profile validation
6. **Remove redundant test configurations** and rely on dynamic property sources
7. **Add required Maven dependencies** for test containers
8. **Run tests** to validate all database vendors work correctly

### 7. Success Criteria

- [ ] All three database vendors (H2, MySQL, PostgreSQL) have working Flyway migrations
- [ ] Test containers successfully start and configure databases dynamically
- [ ] Application context loads successfully for all database configurations
- [ ] JPA entities with byte array properties work correctly across all vendors
- [ ] No hardcoded database connection strings in test files
- [ ] Flyway migration scripts execute without errors
- [ ] All existing functionality remains intact

### 8. Testing Strategy

#### 8.1 Unit Testing
- Test each migration script individually
- Validate JPA entity mappings for byte array properties
- Test Flyway configuration for each vendor

#### 8.2 Integration Testing
- Full application context loading with each database vendor
- End-to-end data persistence and retrieval tests
- Cross-vendor data compatibility validation

#### 8.3 Continuous Integration
- Ensure all tests run in CI/CD pipeline
- Test containers should work in containerized environments
- Validate performance impact of test container startup

## Notes

- This refactoring maintains backward compatibility with existing data
- The vendor-specific approach allows for database-optimized column types
- Test containers provide isolated, reproducible test environments
- The migration structure supports future database vendor additions
- All changes follow Spring Boot 3.5 and Jakarta EE 9+ standards