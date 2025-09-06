# JPA Test Coverage Requirements for OpenESPI-GreenButton-Java

## Project Context

This document outlines comprehensive requirements for implementing JPA test coverage in the `openespi-common` module of the OpenESPI-GreenButton-Java project. The project uses Spring Boot 3.5, Jakarta EE 9+, JUnit 5, and follows Green Button Alliance ESPI 4.0 specifications.

**Key Project Conventions:**
- Java 21 (LTS) with modern language features
- Jakarta EE 9+ APIs (jakarta.* packages, not javax.*)
- Spring Boot 3.5 patterns and auto-configuration
- Maven standard directory layout
- DataFaker for realistic test data generation
- Lombok with @RequiredArgsConstructor for dependency injection

## Objectives

### Primary Goal
Achieve comprehensive test coverage for all JPA entities and repositories in the `org.greenbuttonalliance.espi.common.domain` package using `@DataJpaTest` with H2 in-memory database.

### Success Criteria
- 100% coverage of CRUD operations for all 22+ repository interfaces
- Complete testing of all custom query methods in repositories
- Validation of JPA relationships and cascade behaviors
- Verification of entity validation constraints
- Testing of inheritance patterns and base class functionality

## Scope

### In Scope

#### JPA Entities (27+ entities identified)

**Usage Domain Entities** (`org.greenbuttonalliance.espi.common.domain.usage`):
- ApplicationInformationEntity, ReadingTypeEntity, UsagePointEntity, UsageSummaryEntity
- ElectricPowerQualitySummaryEntity, IntervalBlockEntity, IntervalReadingEntity
- TimeConfigurationEntity, AggregatedNodeRefEntity, AuthorizationEntity
- BatchListEntity, LineItemEntity, MeterReadingEntity, PnodeRefEntity
- ReadingQualityEntity, RetailCustomerEntity, ServiceDeliveryPointEntity, SubscriptionEntity

**Customer Domain Entities** (`org.greenbuttonalliance.espi.common.domain.customer.entity`):
- CustomerAgreementEntity, CustomerEntity, ServiceSupplierEntity, AccountNotification
- Agreement, CustomerAccountEntity, OrganisationRole, PhoneNumberEntity
- ProgramDateIdMappingsEntity, ServiceLocationEntity, StatementEntity
- Asset, AssetContainer, Document, Location, Organisation, WorkLocation, MeterEntity

**Common Domain Entities** (`org.greenbuttonalliance.espi.common.domain.common`):
- IdentifiedObject (base class), DateTimeInterval, RationalNumber, ReadingInterharmonic
- ServiceCategory, SummaryMeasurement, LinkType, and enum types

#### Repository Interfaces (22 repositories identified)

**Usage Repositories**:
- AggregatedNodeRefRepository, ApplicationInformationRepository, AuthorizationRepository
- BatchListRepository, ElectricPowerQualitySummaryRepository, IntervalBlockRepository
- LineItemRepository, MeterReadingRepository, PnodeRefRepository, ReadingTypeRepository
- ResourceRepository, RetailCustomerRepository, ServiceDeliveryPointRepository
- SubscriptionRepository, TimeConfigurationRepository, UsagePointRepository, UsageSummaryRepository

**Customer Repositories**:
- CustomerAccountRepository, CustomerRepository, MeterRepository
- ServiceLocationRepository, StatementRepository

### Out of Scope
- Service layer testing (covered separately)
- Integration tests with external systems
- Performance testing
- XML marshalling/unmarshalling tests (covered in existing MigrationVerificationTest)

## Technical Requirements

### Test Infrastructure

#### Test Configuration
- Use `@DataJpaTest` annotation for JPA slice testing
- Activate `test` profile for H2 in-memory database
- Use `@TestPropertySource` if additional configuration needed
- Follow existing test patterns from `MigrationVerificationTest`

#### Dependencies and Annotations
```java
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Entity Name Repository Tests")
class EntityNameRepositoryTest {
    // Test implementation
}
```

#### Required Test Dependencies
- JUnit 5 (`@Test`, `@DisplayName`, `@Nested`)
- Spring Boot Test (`@DataJpaTest`, `@TestEntityManager`)
- AssertJ or standard assertions
- DataFaker for realistic test data generation
- H2 database (in-memory)
- Mockito with `@MockitoBean` (NOT `@MockBean` - deprecated)
- Jakarta Validation API for constraint testing

### JPA Entity Testing Conventions

#### Project-Specific JPA Patterns
Based on the OpenESPI-GreenButton-Java project guidelines, the following JPA patterns must be tested:

**Optimistic Locking:**
- All entities use `Integer version` property with `@Version` annotation
- Test version increment on entity updates
- Test optimistic locking exceptions (`OptimisticLockException`)

**Enum Mapping:**
- All enums use `@Enumerated(EnumType.STRING)` to store enum names
- Test enum persistence and retrieval
- Verify database stores string values, not ordinals

**Timestamp Management:**
- `createdDate` property with `@CreationTimestamp` (updatable = false)
- `dateUpdated` property with `@UpdateTimestamp`
- Test automatic timestamp generation and updates

**Transaction Patterns:**
- Service methods use `@Transactional` for write operations
- Read-only methods use `@Transactional(readOnly = true)`
- Test transaction rollback and commit behavior
- Use `saveAndFlush()` in transaction tests to ensure database persistence

**Exception Handling:**
- Services throw `NotFoundException` for empty Optional results
- Test proper exception propagation from repository to service layer

### Test Categories

#### 1. CRUD Operations Testing

**Requirements:**
- Test all basic JPA repository operations: `save()`, `findById()`, `findAll()`, `deleteById()`, `existsById()`, `count()`
- Verify entity persistence and retrieval
- Test batch operations where applicable
- Validate optimistic locking with version fields (`@Version Integer version`)
- Test `saveAndFlush()` for transaction verification
- Verify automatic timestamp generation (`createdDate`, `dateUpdated`)
- Test enum persistence with `@Enumerated(EnumType.STRING)`

**Implementation Guidelines:**
```java
@Nested
@DisplayName("CRUD Operations")
class CrudOperationsTest {
    
    @Test
    @DisplayName("Should save and retrieve entity successfully")
    void shouldSaveAndRetrieveEntity() {
        // Arrange: Create entity with DataFaker
        CustomerEntity customer = createValidCustomer();
        
        // Act: Save and flush to ensure database persistence
        CustomerEntity saved = repository.saveAndFlush(customer);
        entityManager.clear(); // Clear persistence context
        
        // Assert: Verify all properties including timestamps
        CustomerEntity retrieved = repository.findById(saved.getId()).orElseThrow();
        assertThat(retrieved.getCustomerName()).isEqualTo(customer.getCustomerName());
        assertThat(retrieved.getVersion()).isEqualTo(0); // Initial version
        assertThat(retrieved.getCreatedDate()).isNotNull();
        assertThat(retrieved.getDateUpdated()).isNotNull();
    }
    
    @Test
    @DisplayName("Should increment version on entity update")
    void shouldIncrementVersionOnUpdate() {
        // Arrange: Save initial entity
        CustomerEntity customer = repository.saveAndFlush(createValidCustomer());
        Integer initialVersion = customer.getVersion();
        
        // Act: Update entity
        customer.setCustomerName(faker.company().name());
        CustomerEntity updated = repository.saveAndFlush(customer);
        
        // Assert: Version should be incremented
        assertThat(updated.getVersion()).isEqualTo(initialVersion + 1);
        assertThat(updated.getDateUpdated()).isAfter(customer.getCreatedDate());
    }
    
    @Test
    @DisplayName("Should persist enum as string value")
    void shouldPersistEnumAsString() {
        // Arrange: Create entity with enum
        CustomerEntity customer = createValidCustomer();
        customer.setKind(CustomerKind.RESIDENTIAL);
        
        // Act: Save and retrieve
        CustomerEntity saved = repository.saveAndFlush(customer);
        entityManager.clear();
        
        // Assert: Enum should be persisted as string
        CustomerEntity retrieved = repository.findById(saved.getId()).orElseThrow();
        assertThat(retrieved.getKind()).isEqualTo(CustomerKind.RESIDENTIAL);
        
        // Verify database stores string value (not ordinal)
        String kindValue = (String) entityManager.createNativeQuery(
            "SELECT kind FROM customer WHERE id = ?")
            .setParameter(1, saved.getId().toString())
            .getSingleResult();
        assertThat(kindValue).isEqualTo("RESIDENTIAL");
    }
    
    // Additional CRUD tests...
}
```

#### 2. Custom Query Methods Testing

**Requirements:**
- Test all custom `@Query` annotated methods in repositories
- Verify query parameters and result sets
- Test edge cases (empty results, null parameters)
- Validate query performance and correctness

**Example Custom Queries to Test:**
- Date range queries (`findByIssueDateTimeAfter`, `findByIssueDateTimeBetween`)
- String pattern matching (`findByDescriptionContaining`)
- Collection size queries (`findStatementsWithReferences`)
- Sorting and ordering (`findRecentStatements`)
- Complex joins and aggregations

**Implementation Guidelines:**
```java
@Nested
@DisplayName("Custom Query Methods")
class CustomQueryMethodsTest {
    
    @Test
    @DisplayName("Should find entities by date range")
    void shouldFindEntitiesByDateRange() {
        // Test date range queries with boundary conditions
    }
    
    @Test
    @DisplayName("Should handle empty results gracefully")
    void shouldHandleEmptyResults() {
        // Test queries that return empty collections
    }
}
```

#### 3. JPA Relationships Testing

**Requirements:**
- Test all relationship mappings (`@OneToMany`, `@ManyToOne`, `@OneToOne`, `@ManyToMany`)
- Verify cascade operations (`CascadeType.ALL`, `CascadeType.PERSIST`, etc.)
- Test lazy vs eager loading behavior
- Validate orphan removal
- Test bidirectional relationship consistency

**Key Relationships to Test:**
- StatementEntity ↔ StatementRefEntity (OneToMany with CASCADE.ALL)
- StatementEntity ↔ CustomerEntity (ManyToOne)
- IdentifiedObject inheritance hierarchy
- Usage point relationships with meter readings and interval blocks

**Implementation Guidelines:**
```java
@Nested
@DisplayName("JPA Relationships")
class RelationshipsTest {
    
    @Test
    @DisplayName("Should cascade save child entities")
    void shouldCascadeSaveChildEntities() {
        // Test cascade operations
    }
    
    @Test
    @DisplayName("Should maintain bidirectional relationship consistency")
    void shouldMaintainBidirectionalConsistency() {
        // Test relationship integrity
    }
}
```

#### 4. Entity Validation Testing

**Requirements:**
- Test Jakarta validation constraints (`@NotNull`, `@Size`, `@Valid`, etc.)
- Verify custom validation logic
- Test validation error messages
- Validate constraint violation handling

**Implementation Guidelines:**
```java
@Nested
@DisplayName("Entity Validation")
class ValidationTest {
    
    @Autowired
    private Validator validator;
    
    @Test
    @DisplayName("Should validate required fields")
    void shouldValidateRequiredFields() {
        // Test validation constraints
    }
    
    @Test
    @DisplayName("Should reject invalid data")
    void shouldRejectInvalidData() {
        // Test constraint violations
    }
}
```

#### 5. Base Class Functionality Testing

**Requirements:**
- Test IdentifiedObject base class functionality
- Verify UUID generation and management
- Test timestamp behavior (`@CreationTimestamp`, `@UpdateTimestamp`)
- Validate link management methods
- Test merge functionality
- Verify equals/hashCode implementation with Hibernate proxies

**Implementation Guidelines:**
```java
@Nested
@DisplayName("IdentifiedObject Base Functionality")
class BaseClassTest {
    
    @Test
    @DisplayName("Should auto-generate UUID on entity creation")
    void shouldAutoGenerateUuid() {
        // Test UUID generation
    }
    
    @Test
    @DisplayName("Should update timestamps on save")
    void shouldUpdateTimestamps() {
        // Test timestamp behavior
    }
    
    @Test
    @DisplayName("Should manage ESPI links correctly")
    void shouldManageEspiLinks() {
        // Test link management methods
    }
}
```

## Implementation Guidelines

### Test Structure and Organization

#### File Naming Convention
- Repository tests: `{EntityName}RepositoryTest.java`
- Location: `src/test/java/org/greenbuttonalliance/espi/common/repositories/{domain}/`

#### Test Class Structure
```java
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Entity Name Repository Tests")
class EntityNameRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private EntityNameRepository repository;
    
    @Autowired
    private Validator validator;
    
    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperationsTest { /* ... */ }
    
    @Nested
    @DisplayName("Custom Query Methods")
    class CustomQueryMethodsTest { /* ... */ }
    
    @Nested
    @DisplayName("JPA Relationships")
    class RelationshipsTest { /* ... */ }
    
    @Nested
    @DisplayName("Entity Validation")
    class ValidationTest { /* ... */ }
    
    @Nested
    @DisplayName("Base Class Functionality")
    class BaseClassTest { /* ... */ }
    
    // Helper methods for test data creation
    private EntityName createValidEntity() {
        // Use DataFaker for realistic test data
    }
}
```

## DTO and MapStruct Testing Considerations

### DTO Testing Patterns
Based on project guidelines, test DTOs following these conventions:

**DTO Types:**
- **Get/List DTOs**: Include all properties for read operations
- **Create DTOs**: Exclude `id`, `version`, `createdDate`, `dateUpdated` properties
- **Update DTOs**: Exclude `id`, `createdDate`, `dateUpdated` (include `version` for optimistic locking)
- **Patch DTOs**: Exclude `id`, `createdDate`, `dateUpdated` (include `version`, no validation preventing null/empty)

**MapStruct Testing:**
- Test mapper implementations generated at compile time
- For patch operations, verify `@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)`
- Test null value handling in patch mappings
- Verify entity-to-DTO and DTO-to-entity conversions

```java
@Test
@DisplayName("Should map entity to DTO correctly")
void shouldMapEntityToDto() {
    // Arrange: Create entity with all properties
    CustomerEntity entity = createValidCustomer();
    entity = repository.saveAndFlush(entity);
    
    // Act: Map to DTO
    CustomerDto dto = customerMapper.toDto(entity);
    
    // Assert: Verify all properties mapped correctly
    assertThat(dto.getId()).isEqualTo(entity.getId());
    assertThat(dto.getCustomerName()).isEqualTo(entity.getCustomerName());
    assertThat(dto.getVersion()).isEqualTo(entity.getVersion());
    // Verify timestamps are mapped
    assertThat(dto.getCreatedDate()).isEqualTo(entity.getCreatedDate());
    assertThat(dto.getDateUpdated()).isEqualTo(entity.getDateUpdated());
}

@Test
@DisplayName("Should ignore null values in patch mapping")
void shouldIgnoreNullValuesInPatchMapping() {
    // Arrange: Create existing entity and patch DTO with partial data
    CustomerEntity existing = repository.saveAndFlush(createValidCustomer());
    CustomerPatchDto patchDto = new CustomerPatchDto();
    patchDto.setCustomerName("Updated Name");
    // Other fields are null
    
    // Act: Apply patch mapping
    customerMapper.updateEntityFromPatchDto(patchDto, existing);
    
    // Assert: Only non-null fields should be updated
    assertThat(existing.getCustomerName()).isEqualTo("Updated Name");
    assertThat(existing.getKind()).isNotNull(); // Should retain original value
}
```

### Test Data Management

#### Use DataFaker for Realistic Data
```java
private final Faker faker = new Faker();

private CustomerEntity createValidCustomer() {
    CustomerEntity customer = new CustomerEntity();
    customer.setCustomerName(faker.company().name());
    customer.setKind(CustomerKind.RESIDENTIAL);
    customer.setPucNumber(faker.number().digits(10));
    customer.setSpecialNeedsFlag(faker.bool().bool());
    customer.setStatus(faker.options().option(ServiceStatus.class));
    return customer;
}

private StatementEntity createValidStatement() {
    StatementEntity statement = new StatementEntity();
    statement.setDescription(faker.lorem().sentence());
    statement.setIssueDateTime(OffsetDateTime.now().minusDays(faker.number().numberBetween(1, 30)));
    statement.setStatementDate(LocalDate.now().minusDays(faker.number().numberBetween(1, 30)));
    statement.setBillingPeriodStart(LocalDate.now().minusDays(60));
    statement.setBillingPeriodEnd(LocalDate.now().minusDays(30));
    return statement;
}

private UsagePointEntity createValidUsagePoint() {
    UsagePointEntity usagePoint = new UsagePointEntity();
    usagePoint.setDescription(faker.address().streetAddress());
    usagePoint.setRoleFlags(faker.number().randomNumber());
    usagePoint.setStatus(faker.number().numberBetween(0, 2));
    
    // Create embedded ServiceCategory
    ServiceCategory serviceCategory = new ServiceCategory();
    serviceCategory.setKind(faker.number().numberBetween(0, 40));
    usagePoint.setServiceCategory(serviceCategory);
    
    return usagePoint;
}
```

#### Test Data Isolation
- Use `@Transactional` with rollback for test isolation
- Create fresh test data for each test method
- Use `TestEntityManager.flush()` and `TestEntityManager.clear()` when needed

### Error Handling and Edge Cases

#### Test Edge Cases
- Null parameter handling
- Empty collections
- Boundary value testing for dates and numbers
- Invalid UUID formats
- Constraint violations

#### Exception Testing
```java
@Test
@DisplayName("Should throw NotFoundException for non-existent entity")
void shouldThrowNotFoundExceptionForNonExistentEntity() {
    // Test exception scenarios
    assertThatThrownBy(() -> repository.findById(UUID.randomUUID()))
        .isInstanceOf(NotFoundException.class);
}
```

## Quality Standards

### Code Coverage
- Minimum 90% line coverage for repository interfaces
- 100% coverage of custom query methods
- Complete coverage of relationship mappings

### Test Quality
- Use descriptive test names with `@DisplayName`
- Follow Arrange-Act-Assert pattern
- Include both positive and negative test cases
- Test boundary conditions and edge cases

### Performance Considerations
- Keep tests fast (< 100ms per test method)
- Use in-memory H2 database for speed
- Minimize test data setup overhead
- Use `@DirtiesContext` sparingly

## Acceptance Criteria

### Functional Requirements
- [ ] All 22+ repository interfaces have comprehensive test coverage
- [ ] All custom query methods are tested with various scenarios
- [ ] All JPA relationships are validated for correct behavior
- [ ] Entity validation constraints are thoroughly tested
- [ ] Base class functionality (IdentifiedObject) is completely tested

### Technical Requirements
- [ ] Tests use `@DataJpaTest` with H2 in-memory database
- [ ] Tests activate `test` profile
- [ ] All tests use JUnit 5 with `@DisplayName` annotations
- [ ] Tests are organized using `@Nested` classes
- [ ] DataFaker is used for realistic test data generation

### Quality Requirements
- [ ] Minimum 90% code coverage for repository layer
- [ ] All tests pass consistently
- [ ] Tests execute in under 30 seconds total
- [ ] No test dependencies or ordering requirements
- [ ] Clear, descriptive test names and documentation

## Deliverables

1. **Test Classes**: Complete test coverage for all repository interfaces
2. **Test Data Utilities**: Helper classes for creating test entities
3. **Documentation**: Updated README with testing guidelines
4. **CI Integration**: Tests integrated into build pipeline

## Timeline and Priorities

### Phase 1 (High Priority)
- Core usage entities: UsagePointEntity, MeterReadingEntity, IntervalBlockEntity
- Customer entities: CustomerEntity, StatementEntity
- Base class testing: IdentifiedObject

### Phase 2 (Medium Priority)
- Remaining usage entities
- Complex relationship testing
- Advanced query method testing

### Phase 3 (Low Priority)
- Edge case testing
- Performance optimization
- Documentation updates

## References

- [Spring Boot Testing Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/spring-boot-features.html#boot-features-testing)
- [Spring Data JPA Testing](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#testing)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Green Button Alliance ESPI Specification](https://www.greenbuttonalliance.org/)
- [Project Guidelines](../../../README.md)