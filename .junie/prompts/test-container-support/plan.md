# Test Container Support Implementation Plan

## Overview
This implementation plan addresses the database migration refactoring and test container integration for the OpenESPI GreenButton Java project to resolve Flyway migration failures across H2, MySQL, and PostgreSQL databases.

## Phase 1: Database Migration Refactoring

### 1.1 Analyze Current Migration Structure
- **Task**: Extract table definitions from existing MySQL migration
- **File**: `openespi-common/src/main/resources/db/migration/mysql/V1__Create_Complete_Schema_Mysql.sql`
- **Action**: Identify vendor-neutral vs vendor-specific tables
- **Steps**:
  1. Open existing MySQL migration file (1680 lines)
  2. Identify problematic tables: `time_configurations` (lines 228-254), `usage_points` (lines 337-407)
  3. Extract all other table definitions for base migration
  4. Document BLOB column mappings for vendor-specific implementations
- **Duration**: 2 hours

### 1.2 Create Base Migration File
- **Task**: Create vendor-neutral base migration
- **File**: `openespi-common/src/main/resources/db/migration/V1__Create_Base_Tables.sql`
- **Content**: All tables except `time_configurations` and `usage_points`
- **Steps**:
  1. Create new migration file with header comment
  2. Copy all table definitions except problematic ones
  3. Include all related_links tables except time_configuration_related_links and usage_point_related_links
  4. Validate SQL syntax for H2/MySQL/PostgreSQL compatibility
- **Tables to Include**: 
  - `identified_object_related_links`
  - `application_information`
  - `retail_customers`
  - `service_delivery_points`
  - `authorizations`
  - `reading_types`
  - `meter_readings`
  - `interval_readings`
  - All other non-problematic tables
- **Duration**: 4 hours

### 1.3 Create Vendor-Specific Directory Structure
- **Task**: Create vendor-specific migration directories
- **Commands**:
  ```bash
  mkdir -p openespi-common/src/main/resources/db/vendor/h2
  mkdir -p openespi-common/src/main/resources/db/vendor/mysql
  mkdir -p openespi-common/src/main/resources/db/vendor/postgres
  ```
- **Structure**:
  ```
  openespi-common/src/main/resources/db/
  ├── migration/V1__Create_Base_Tables.sql
  └── vendor/
      ├── h2/V2__H2_Specific_Tables.sql
      ├── mysql/V2__MySQL_Specific_Tables.sql
      └── postgres/V2__PostgreSQL_Specific_Tables.sql
  ```
- **Duration**: 1 hour

## Phase 2: Vendor-Specific Table Implementations

### 2.1 H2 Migration Implementation
- **File**: `db/vendor/h2/V2__H2_Specific_Tables.sql`
- **Binary Type**: `BINARY` for byte arrays
- **Tables**: `time_configurations`, `usage_points`
- **Steps**:
  1. Create H2-specific migration file with proper header
  2. Implement `time_configurations` table with `BINARY` columns
  3. Implement `usage_points` table with `BINARY` column
  4. Add related_links tables for both entities
  5. Test H2 syntax compatibility
- **Code Example**:
  ```sql
  -- Time Configuration Table (H2)
  CREATE TABLE time_configurations (
      id UUID PRIMARY KEY,
      uuid VARCHAR(36) NOT NULL UNIQUE,
      -- ... standard columns ...
      dst_end_rule BINARY,
      dst_start_rule BINARY,
      tz_offset BIGINT,
      -- ... indexes ...
  );
  
  -- Usage Point Table (H2)
  CREATE TABLE usage_points (
      id UUID PRIMARY KEY,
      uuid VARCHAR(36) NOT NULL UNIQUE,
      -- ... standard columns ...
      role_flags BINARY,
      service_category_kind VARCHAR(50),
      -- ... foreign keys and indexes ...
  );
  ```
- **Duration**: 2 hours

### 2.2 MySQL Migration Implementation  
- **File**: `db/vendor/mysql/V2__MySQL_Specific_Tables.sql`
- **Binary Type**: `BLOB` for byte arrays
- **Tables**: `time_configurations`, `usage_points`
- **Steps**:
  1. Extract existing table definitions from original MySQL migration
  2. Ensure BLOB columns are properly defined
  3. Maintain all existing indexes and foreign keys
  4. Add related_links tables
  5. Validate MySQL 8.0 compatibility
- **Code Example**:
  ```sql
  -- Time Configuration Table (MySQL)
  CREATE TABLE time_configurations (
      id UUID PRIMARY KEY,
      uuid VARCHAR(36) NOT NULL UNIQUE,
      -- ... standard columns ...
      dst_end_rule BLOB,
      dst_start_rule BLOB,
      tz_offset BIGINT,
      -- ... indexes ...
  );
  
  -- Usage Point Table (MySQL)
  CREATE TABLE usage_points (
      id UUID PRIMARY KEY,
      uuid VARCHAR(36) NOT NULL UNIQUE,
      -- ... standard columns ...
      role_flags BLOB,
      service_category_kind VARCHAR(50),
      -- ... foreign keys and indexes ...
  );
  ```
- **Duration**: 2 hours

### 2.3 PostgreSQL Migration Implementation
- **File**: `db/vendor/postgres/V2__PostgreSQL_Specific_Tables.sql`
- **Binary Type**: `BYTEA` for byte arrays
- **Tables**: `time_configurations`, `usage_points`
- **Steps**:
  1. Create PostgreSQL-specific migration file
  2. Convert BLOB columns to BYTEA type
  3. Ensure UUID type compatibility
  4. Add proper PostgreSQL indexes
  5. Test PostgreSQL 15 compatibility
- **Code Example**:
  ```sql
  -- Time Configuration Table (PostgreSQL)
  CREATE TABLE time_configurations (
      id UUID PRIMARY KEY,
      uuid VARCHAR(36) NOT NULL UNIQUE,
      -- ... standard columns ...
      dst_end_rule BYTEA,
      dst_start_rule BYTEA,
      tz_offset BIGINT,
      -- ... indexes ...
  );
  
  -- Usage Point Table (PostgreSQL)
  CREATE TABLE usage_points (
      id UUID PRIMARY KEY,
      uuid VARCHAR(36) NOT NULL UNIQUE,
      -- ... standard columns ...
      role_flags BYTEA,
      service_category_kind VARCHAR(50),
      -- ... foreign keys and indexes ...
  );
  ```
- **Duration**: 2 hours

## Phase 3: Spring Boot Configuration Updates

### 3.1 Default H2 Configuration
- **File**: `openespi-common/src/main/resources/application.yml`
- **Flyway Locations**: `db/migration`, `db/vendor/h2`
- **Steps**:
  1. Update existing application.yml or create if missing
  2. Configure H2 in-memory database settings
  3. Set Flyway locations to include vendor-specific path
  4. Enable Hibernate schema validation
- **Configuration**:
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
- **Duration**: 1 hour

### 3.2 Development Profile Configurations
- **Files**: `application-dev-mysql.yml`, `application-dev-postgresql.yml`
- **Steps**:
  1. Create MySQL development profile configuration
  2. Create PostgreSQL development profile configuration
  3. Configure vendor-specific Flyway locations
  4. Set appropriate database dialects
- **MySQL Configuration** (`application-dev-mysql.yml`):
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
- **PostgreSQL Configuration** (`application-dev-postgresql.yml`):
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
- **Duration**: 1 hour

## Phase 4: Test Container Implementation

### 4.1 Maven Dependencies
- **File**: `openespi-common/pom.xml`
- **Dependencies**: testcontainers-junit-jupiter, mysql, postgresql
- **Steps**:
  1. Add Testcontainers BOM for version management
  2. Add JUnit Jupiter integration dependency
  3. Add MySQL and PostgreSQL container dependencies
  4. Verify existing database driver dependencies
- **Dependencies to Add**:
  ```xml
  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers-bom</artifactId>
        <version>1.19.3</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>
  
  <dependencies>
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
- **Duration**: 30 minutes

### 4.2 MySQL Test Container
- **File**: `openespi-common/src/test/java/org/greenbuttonalliance/espi/common/migration/DataCustodianApplicationMysqlTest.java`
- **Container**: MySQL 8.0
- **Steps**:
  1. Update existing test class with Testcontainers annotations
  2. Configure MySQL container with proper settings
  3. Implement dynamic property source configuration
  4. Add comprehensive test methods
- **Implementation**:
  ```java
  @SpringBootTest(classes = { TestApplication.class })
  @Testcontainers
  @ActiveProfiles("test-mysql")
  @DisplayName("MySQL Test Container Integration Tests")
  class DataCustodianApplicationMysqlTest {

      @Container
      static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
              .withDatabaseName("openespi_test")
              .withUsername("test")
              .withPassword("test")
              .withReuse(true);

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
          assertThat(mysql.isRunning()).isTrue();
      }

      @Test
      @DisplayName("MySQL Test Container - Flyway Migrations Execute Successfully")
      void flywayMigrationsExecute() {
          // Validate migration execution
      }
  }
  ```
- **Duration**: 2 hours

### 4.3 PostgreSQL Test Container
- **File**: `openespi-common/src/test/java/org/greenbuttonalliance/espi/common/migration/DataCustodianApplicationPostgresTest.java`
- **Container**: PostgreSQL 15
- **Steps**:
  1. Update existing test class with Testcontainers annotations
  2. Configure PostgreSQL container with proper settings
  3. Implement dynamic property source configuration
  4. Add comprehensive test methods
- **Implementation**:
  ```java
  @SpringBootTest(classes = { TestApplication.class })
  @Testcontainers
  @ActiveProfiles("test-postgres")
  @DisplayName("PostgreSQL Test Container Integration Tests")
  class DataCustodianApplicationPostgresTest {

      @Container
      static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
              .withDatabaseName("openespi_test")
              .withUsername("test")
              .withPassword("test")
              .withReuse(true);

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
          assertThat(postgres.isRunning()).isTrue();
      }

      @Test
      @DisplayName("PostgreSQL Test Container - Flyway Migrations Execute Successfully")
      void flywayMigrationsExecute() {
          // Validate migration execution
      }
  }
  ```
- **Duration**: 2 hours

### 4.4 H2 Test Implementation
- **File**: `openespi-common/src/test/java/org/greenbuttonalliance/espi/common/migration/DataCustodianApplicationH2Test.java`
- **Database**: H2 in-memory
- **Steps**:
  1. Create new H2-specific test class
  2. Configure for default test profile
  3. Add migration validation tests
  4. Test JPA entity mappings
- **Implementation**:
  ```java
  @SpringBootTest(classes = { TestApplication.class })
  @ActiveProfiles("test")
  @DisplayName("H2 In-Memory Database Integration Tests")
  class DataCustodianApplicationH2Test {

      @Autowired
      private Flyway flyway;

      @Test
      @DisplayName("H2 In-Memory Database - Application Context Loads Successfully")
      void contextLoads() {
          // Test validates H2-specific migrations and application context loading
      }

      @Test
      @DisplayName("H2 Database - Flyway Migrations Execute Successfully")
      void flywayMigrationsExecute() {
          assertThat(flyway.info().applied()).isNotEmpty();
          assertThat(flyway.info().pending()).isEmpty();
      }

      @Test
      @DisplayName("H2 Database - Binary Column Types Work Correctly")
      void binaryColumnTypesWork() {
          // Test byte array persistence and retrieval
      }
  }
  ```
- **Duration**: 1 hour

## Phase 5: Testing and Validation

### 5.1 Unit Testing
- **Task**: Test individual migration scripts and JPA mappings
- **Steps**:
  1. Create migration-specific unit tests
  2. Test JPA entity mappings for byte array properties
  3. Validate Flyway configuration for each vendor
  4. Test database-specific column type handling
- **Test Categories**:
  - Migration script syntax validation
  - JPA entity byte array mapping tests
  - Flyway configuration validation
  - Database dialect compatibility tests
- **Duration**: 4 hours

### 5.2 Integration Testing
- **Task**: Full application context loading with all database vendors
- **Steps**:
  1. Run complete test suite with H2 database
  2. Run complete test suite with MySQL test container
  3. Run complete test suite with PostgreSQL test container
  4. Validate all Spring Boot configurations
- **Test Commands**:
  ```bash
  # Test H2 configuration
  mvn test -Dtest=DataCustodianApplicationH2Test
  
  # Test MySQL with test container
  mvn test -Dtest=DataCustodianApplicationMysqlTest
  
  # Test PostgreSQL with test container
  mvn test -Dtest=DataCustodianApplicationPostgresTest
  
  # Run all migration tests
  mvn test -Dtest="*Migration*Test"
  ```
- **Duration**: 6 hours

### 5.3 Cross-Vendor Compatibility
- **Task**: Data persistence and retrieval tests across all vendors
- **Steps**:
  1. Create test data with byte array properties
  2. Test data persistence across all database vendors
  3. Validate byte array retrieval and integrity
  4. Test cross-vendor data migration scenarios
- **Test Scenarios**:
  - UsagePointEntity with roleFlags byte array
  - TimeConfigurationEntity with dstEndRule and dstStartRule byte arrays
  - Data integrity validation across vendors
  - Performance comparison between vendors
- **Duration**: 4 hours

## Phase 6: Configuration Cleanup and Optimization

### 6.1 Test Configuration Cleanup
- **Task**: Remove redundant test configuration files
- **Files to Update**:
  - `application-test-mysql.yml` - Keep minimal configuration
  - `application-test-postgres.yml` - Keep minimal configuration
- **Steps**:
  1. Remove hardcoded database connection strings
  2. Keep only JPA and logging configurations
  3. Ensure test profiles work with dynamic properties
- **Duration**: 1 hour

### 6.2 Documentation Updates
- **Task**: Update project documentation
- **Files to Update**:
  - `README.md` - Add test container setup instructions
  - Migration documentation - Document new structure
- **Steps**:
  1. Document new migration structure
  2. Add test container prerequisites
  3. Update development setup instructions
- **Duration**: 2 hours

## Dependency Management

### Required Dependencies
- **Testcontainers BOM**: 1.19.3
- **JUnit Jupiter**: (managed by Spring Boot)
- **MySQL Connector**: (existing)
- **PostgreSQL Driver**: (existing)
- **H2 Database**: (existing)
- **Flyway Core**: (existing)

### Docker Requirements
- **Docker Desktop**: Latest version
- **MySQL Image**: mysql:8.0
- **PostgreSQL Image**: postgres:15
- **Minimum Docker Memory**: 4GB allocated

### Environment Setup
```bash
# Verify Docker is running
docker --version
docker ps

# Pull required images (optional - will be pulled automatically)
docker pull mysql:8.0
docker pull postgres:15

# Verify Maven version
mvn --version
```

## Troubleshooting Guide

### Common Issues and Solutions

#### 1. Docker Container Startup Failures
**Symptoms**: Test containers fail to start
**Solutions**:
- Verify Docker Desktop is running
- Check available memory allocation (minimum 4GB)
- Ensure ports 3306 and 5432 are not in use
- Clear Docker cache: `docker system prune`

#### 2. Flyway Migration Failures
**Symptoms**: Migration scripts fail to execute
**Solutions**:
- Validate SQL syntax for specific database vendor
- Check migration file naming convention (V1__, V2__)
- Verify Flyway locations configuration
- Test migrations individually

#### 3. JPA Entity Mapping Issues
**Symptoms**: Byte array properties not persisting correctly
**Solutions**:
- Verify @Lob annotation on byte array properties
- Check database column type mappings
- Test with simple byte array data first
- Enable SQL logging for debugging

#### 4. Test Profile Configuration Issues
**Symptoms**: Wrong database dialect or configuration
**Solutions**:
- Verify @ActiveProfiles annotation
- Check application-{profile}.yml files
- Ensure dynamic properties override correctly
- Test profile activation with logging

### Debugging Commands
```bash
# Enable debug logging for Flyway
mvn test -Dlogging.level.org.flywaydb=DEBUG

# Enable SQL logging
mvn test -Dlogging.level.org.hibernate.SQL=DEBUG

# Run specific test with verbose output
mvn test -Dtest=DataCustodianApplicationMysqlTest -X

# Check Docker container logs
docker logs <container_id>
```

## Validation Procedures

### Pre-Implementation Validation
1. **Environment Check**:
   ```bash
   # Verify Java version
   java --version  # Should be Java 21
   
   # Verify Maven version
   mvn --version   # Should be 3.9+
   
   # Verify Docker
   docker --version && docker ps
   ```

2. **Baseline Test**:
   ```bash
   # Run existing tests to establish baseline
   mvn test -pl openespi-common
   ```

### Post-Implementation Validation
1. **Migration Validation**:
   ```bash
   # Test each database vendor individually
   mvn test -Dtest=DataCustodianApplicationH2Test
   mvn test -Dtest=DataCustodianApplicationMysqlTest
   mvn test -Dtest=DataCustodianApplicationPostgresTest
   ```

2. **Integration Validation**:
   ```bash
   # Run full test suite
   mvn clean test -pl openespi-common
   
   # Run all modules
   mvn clean test
   ```

3. **Performance Validation**:
   ```bash
   # Measure test execution time
   time mvn test -Dtest="*Migration*Test"
   ```

## Implementation Timeline
- **Total Duration**: 35.5 hours
- **Phase 1**: 7 hours (Database Migration Refactoring)
- **Phase 2**: 6 hours (Vendor-Specific Implementations)
- **Phase 3**: 2 hours (Spring Boot Configuration)
- **Phase 4**: 5.5 hours (Test Container Implementation)
- **Phase 5**: 14 hours (Testing and Validation)
- **Phase 6**: 3 hours (Cleanup and Documentation)

## Success Criteria Checklist
- [ ] All database vendors (H2, MySQL, PostgreSQL) have working Flyway migrations
- [ ] Test containers successfully start and configure databases dynamically
- [ ] Application context loads successfully for all database configurations
- [ ] JPA entities with byte array properties work correctly across all vendors
- [ ] No hardcoded database connection strings in test files
- [ ] Flyway migration scripts execute without errors
- [ ] All existing functionality remains intact
- [ ] Test execution time is reasonable (< 5 minutes for migration tests)
- [ ] Documentation is updated with new setup instructions
- [ ] CI/CD pipeline compatibility is maintained

## Risk Mitigation Strategies

### Technical Risks
- **Risk**: Migration script syntax errors
- **Mitigation**: Test each vendor migration individually before integration
- **Contingency**: Rollback to original structure and fix incrementally

- **Risk**: Test container startup failures in CI/CD
- **Mitigation**: Use container reuse and proper resource allocation
- **Contingency**: Fallback to embedded database testing

- **Risk**: JPA mapping incompatibilities
- **Mitigation**: Test byte array persistence across all vendors early
- **Contingency**: Use database-specific entity configurations

### Process Risks
- **Risk**: Breaking existing functionality
- **Mitigation**: Comprehensive regression testing after each phase
- **Contingency**: Git branch strategy with easy rollback

- **Risk**: Performance degradation
- **Mitigation**: Monitor test execution times and optimize container startup
- **Contingency**: Implement parallel test execution

## Final Implementation Checklist

### Phase 1 Completion
- [ ] Base migration file created and validated
- [ ] Vendor-specific directory structure established
- [ ] Original MySQL migration analyzed and documented

### Phase 2 Completion
- [ ] H2 migration with BINARY columns implemented
- [ ] MySQL migration with BLOB columns implemented
- [ ] PostgreSQL migration with BYTEA columns implemented
- [ ] All vendor migrations tested individually

### Phase 3 Completion
- [ ] Default H2 configuration updated
- [ ] MySQL development profile created
- [ ] PostgreSQL development profile created
- [ ] Flyway locations configured correctly

### Phase 4 Completion
- [ ] Maven dependencies added and verified
- [ ] MySQL test container implemented and tested
- [ ] PostgreSQL test container implemented and tested
- [ ] H2 test implementation completed

### Phase 5 Completion
- [ ] Unit tests for migrations created
- [ ] Integration tests passing for all vendors
- [ ] Cross-vendor compatibility validated
- [ ] Performance benchmarks established

### Phase 6 Completion
- [ ] Redundant configurations removed
- [ ] Documentation updated
- [ ] Final validation completed
- [ ] Implementation ready for production use