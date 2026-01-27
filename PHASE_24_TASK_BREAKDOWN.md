# Phase 24: CustomerAgreement - Task Breakdown with Dependencies

## Task Dependency Graph

```
PREPARATION PHASE
├─ T1: Create Feature Branch
│
INFRASTRUCTURE PHASE (Can run in parallel after T1)
├─ T2: Create Repository Interface          (depends: T1)
├─ T3: Create Service Interface             (depends: T1)
├─ T4: Create Service Implementation        (depends: T2, T3)
│
MAPPER PHASE (Can run in parallel after T1)
├─ T5: Check/Create DateTimeIntervalDto    (depends: T1)
├─ T6: Check/Create DateTimeIntervalMapper (depends: T5)
│
ENTITY PHASE (Can run in parallel after T1)
├─ T7: Update CustomerAgreementEntity       (depends: T1)
│   ├─ T7a: Add missing Document fields
│   ├─ T7b: Reorder all fields per XSD
│   ├─ T7c: Fix futureStatus type
│   ├─ T7d: Add @AttributeOverride
│   └─ T7e: Update equals/hashCode
│
DTO PHASE (Can start after T5)
├─ T8: Rewrite CustomerAgreementDto         (depends: T5, T7)
│   ├─ T8a: Remove ALL Atom fields
│   ├─ T8b: Remove relationship DTOs
│   ├─ T8c: Add Document fields (9)
│   ├─ T8d: Add Agreement fields (2)
│   └─ T8e: Add CustomerAgreement fields (6)
│
MAPPER UPDATE PHASE (Can start after T6, T8)
├─ T9: Update CustomerAgreementMapper       (depends: T6, T8)
│
DATABASE PHASE (Can run in parallel, but sync before testing)
├─ T10: Update Flyway Migration             (depends: T7)
│
TESTING PHASE (Can only start after all above complete)
├─ T11: Create CustomerAgreementDtoTest     (depends: T8, T9)
├─ T12: Create CustomerAgreementRepositoryTest (depends: T2, T4, T7, T10)
├─ T13: Run All Tests                       (depends: T11, T12)
│
QUALITY PHASE
├─ T14: Fix SonarQube Violations            (depends: T13)
├─ T15: Run Integration Tests               (depends: T14)
│
DELIVERY PHASE
├─ T16: Commit and Push                     (depends: T15)
├─ T17: Create Pull Request                 (depends: T16)
└─ T18: Update Issue #28                    (depends: T17)
```

---

## Detailed Task List

### PREPARATION PHASE

#### T1: Create Feature Branch
**Dependencies**: None
**Complexity**: Low
**Estimated Time**: 2 minutes

**Commands**:
```bash
git checkout main
git pull origin main
git checkout -b feature/schema-compliance-phase-24-customer-agreement
```

**Verification**:
```bash
git branch --show-current  # Should show: feature/schema-compliance-phase-24-customer-agreement
```

---

### INFRASTRUCTURE PHASE

#### T2: Create Repository Interface
**Dependencies**: T1
**Complexity**: Low
**Estimated Time**: 5 minutes
**File**: `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/repositories/customer/CustomerAgreementRepository.java`

**Actions**:
1. Create new file CustomerAgreementRepository.java
2. Extend JpaRepository<CustomerAgreementEntity, UUID>
3. Add @Repository annotation
4. NO custom query methods (use only inherited methods)

**Template**:
```java
@Repository
public interface CustomerAgreementRepository extends JpaRepository<CustomerAgreementEntity, UUID> {
    // Use only inherited methods to avoid H2 keyword conflicts
}
```

**Verification**:
- File compiles successfully
- No custom query methods defined

---

#### T3: Create Service Interface
**Dependencies**: T1
**Complexity**: Low
**Estimated Time**: 10 minutes
**File**: `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/service/customer/CustomerAgreementService.java`

**Actions**:
1. Create new file CustomerAgreementService.java
2. Define 6 essential methods:
   - save(CustomerAgreementEntity)
   - findById(UUID)
   - findAll()
   - deleteById(UUID)
   - existsById(UUID)
   - count()

**Verification**:
- File compiles successfully
- Only essential CRUD methods defined

---

#### T4: Create Service Implementation
**Dependencies**: T2, T3
**Complexity**: Medium
**Estimated Time**: 20 minutes
**File**: `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/service/customer/impl/CustomerAgreementServiceImpl.java`

**Actions**:
1. Create new file CustomerAgreementServiceImpl.java
2. Add @Service, @RequiredArgsConstructor, @Slf4j annotations
3. Inject CustomerAgreementRepository
4. Inject EspiIdGeneratorService
5. Implement all 6 methods from interface
6. Add UUID v5 generation in save() method
   - Namespace: "ESPI-CUSTOMER-AGREEMENT"
   - Seed: agreementId or random UUID

**Verification**:
- File compiles successfully
- All interface methods implemented
- UUID v5 generation working
- Logging statements present

---

### MAPPER PHASE

#### T5: Check/Create DateTimeIntervalDto
**Dependencies**: T1
**Complexity**: Low-Medium
**Estimated Time**: 15 minutes
**File**: `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/dto/DateTimeIntervalDto.java` (if missing)

**Actions**:
1. Check if DateTimeIntervalDto exists in common DTOs
2. If missing, create with fields: start (OffsetDateTime), duration (Long)
3. Add proper JAXB annotations matching usage.xsd DateTimeInterval
4. Use appropriate namespace (likely espi namespace)

**Verification**:
- DTO compiles successfully
- Fields match usage.xsd DateTimeInterval type
- Proper JAXB annotations present

---

#### T6: Check/Create DateTimeIntervalMapper
**Dependencies**: T5
**Complexity**: Low
**Estimated Time**: 10 minutes
**File**: `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/mapper/DateTimeIntervalMapper.java` (if missing)

**Actions**:
1. Check if DateTimeIntervalMapper exists
2. If missing, create MapStruct mapper
3. Map DateTimeInterval (entity) ↔ DateTimeIntervalDto
4. Add @Mapper(componentModel = "spring")

**Verification**:
- Mapper compiles successfully
- MapStruct generates implementation
- Bidirectional mapping works

---

### ENTITY PHASE

#### T7: Update CustomerAgreementEntity
**Dependencies**: T1
**Complexity**: High
**Estimated Time**: 45 minutes
**File**: `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/domain/customer/entity/CustomerAgreementEntity.java`

**Sub-tasks**:

##### T7a: Add Missing Document Fields (3 fields)
**Actions**:
1. Add `type` field (move from line 85 to after class declaration)
2. Add `authorName` field (NEW)
3. Add `electronicAddress` embedded field (NEW) - Organisation.ElectronicAddress
4. Add `docStatus` embedded field (NEW) - Status

**New Code**:
```java
// Document fields - FIRST section
@Column(name = "document_type", length = 256)
private String type;  // MOVED from line 85

@Column(name = "author_name", length = 256)
private String authorName;  // NEW

// createdDateTime (already exists)
// lastModifiedDateTime (already exists)
// revisionNumber (already exists)

@Embedded
private Organisation.ElectronicAddress electronicAddress;  // NEW

// subject (already exists)
// title (already exists)

@Embedded
private Status docStatus;  // NEW
```

##### T7b: Reorder All Fields Per XSD
**Actions**:
1. Reorder to match XSD sequence:
   - Document fields (9): type, authorName, createdDateTime, lastModifiedDateTime, revisionNumber, electronicAddress, subject, title, docStatus
   - Agreement fields (2): signDate, validityInterval
   - CustomerAgreement fields (6): loadMgmt, isPrePay, shutOffDateTime, currency, futureStatus, agreementId

2. Update JavaDoc comments to reflect new order

##### T7c: Fix futureStatus Type
**Actions**:
1. Change from `List<CustomerEntity.Status>` to `List<Status>`
2. Apply @AttributeOverride annotations DIRECTLY (no wrapper):
   ```java
   @ElementCollection(fetch = FetchType.LAZY)
   @CollectionTable(name = "customer_agreement_future_status",
                    joinColumns = @JoinColumn(name = "customer_agreement_id"))
   @AttributeOverride(name = "value", column = @Column(name = "status_value"))
   @AttributeOverride(name = "dateTime", column = @Column(name = "status_date_time"))
   @AttributeOverride(name = "reason", column = @Column(name = "status_reason"))
   private List<Status> futureStatus;
   ```

##### T7d: Add Class-Level @AttributeOverride
**Actions**:
1. Add @AttributeOverride annotations directly on class (no wrapper):
   ```java
   @Entity
   @Table(name = "customer_agreements")
   @AttributeOverride(name = "upLink.rel", column = @Column(name = "customer_agreement_up_link_rel"))
   @AttributeOverride(name = "upLink.href", column = @Column(name = "customer_agreement_up_link_href"))
   @AttributeOverride(name = "upLink.type", column = @Column(name = "customer_agreement_up_link_type"))
   @AttributeOverride(name = "selfLink.rel", column = @Column(name = "customer_agreement_self_link_rel"))
   @AttributeOverride(name = "selfLink.href", column = @Column(name = "customer_agreement_self_link_href"))
   @AttributeOverride(name = "selfLink.type", column = @Column(name = "customer_agreement_self_link_type"))
   ```

##### T7e: Update equals/hashCode for Pattern Matching
**Actions**:
1. Use instanceof with pattern variables:
   ```java
   Class<?> oEffectiveClass = o instanceof HibernateProxy hibernateProxy ?
       hibernateProxy.getHibernateLazyInitializer().getPersistentClass() : o.getClass();
   Class<?> thisEffectiveClass = this instanceof HibernateProxy hibernateProxy ?
       hibernateProxy.getHibernateLazyInitializer().getPersistentClass() : this.getClass();
   ```

**Verification**:
- Entity compiles successfully
- All 17 fields present in correct order
- @AttributeOverride applied directly (no wrapper)
- equals/hashCode use pattern matching
- toString() includes all new fields

---

### DTO PHASE

#### T8: Rewrite CustomerAgreementDto
**Dependencies**: T5 (DateTimeIntervalDto), T7 (Entity updates for reference)
**Complexity**: High
**Estimated Time**: 60 minutes
**File**: `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/dto/customer/CustomerAgreementDto.java`

**Sub-tasks**:

##### T8a: Remove ALL Atom Fields (10 items)
**Actions - DELETE these fields**:
```java
❌ private Long id;                          // Line 53
❌ private OffsetDateTime published;         // Line 59
❌ private OffsetDateTime updated;           // Line 62
❌ private List<LinkDto> relatedLinks;       // Lines 64-66
❌ private LinkDto selfLink;                 // Lines 68-69
❌ private LinkDto upLink;                   // Lines 71-72
❌ private String description;               // Lines 74-75
❌ public String getSelfHref() {...}         // Lines 107-109
❌ public String getUpHref() {...}           // Lines 116-118
❌ public String generateSelfHref() {...}    // Lines 125-130
❌ public String generateUpHref() {...}      // Lines 137-142
```

##### T8b: Remove Relationship DTOs (3 items)
**Actions - DELETE these fields**:
```java
❌ private CustomerAccountDto customerAccount;           // Lines 83-84
❌ private List<ServiceLocationDto> serviceLocations;    // Lines 86-88
❌ private List<StatementDto> statements;                // Lines 90-92
```

##### T8c: Add Document Fields (9 fields)
**Actions - ADD these fields in order**:
```java
// 1. type
@XmlElement(name = "type", namespace = "http://naesb.org/espi/customer")
private String type;

// 2. authorName
@XmlElement(name = "authorName", namespace = "http://naesb.org/espi/customer")
private String authorName;

// 3. createdDateTime
@XmlElement(name = "createdDateTime", namespace = "http://naesb.org/espi/customer")
private OffsetDateTime createdDateTime;

// 4. lastModifiedDateTime
@XmlElement(name = "lastModifiedDateTime", namespace = "http://naesb.org/espi/customer")
private OffsetDateTime lastModifiedDateTime;

// 5. revisionNumber
@XmlElement(name = "revisionNumber", namespace = "http://naesb.org/espi/customer")
private String revisionNumber;

// 6. electronicAddress
@XmlElement(name = "electronicAddress", namespace = "http://naesb.org/espi/customer")
private CustomerDto.ElectronicAddressDto electronicAddress;

// 7. subject
@XmlElement(name = "subject", namespace = "http://naesb.org/espi/customer")
private String subject;

// 8. title
@XmlElement(name = "title", namespace = "http://naesb.org/espi/customer")
private String title;

// 9. docStatus
@XmlElement(name = "docStatus", namespace = "http://naesb.org/espi/customer")
private CustomerAccountDto.StatusDto docStatus;
```

##### T8d: Add Agreement Fields (2 fields)
**Actions - ADD these fields**:
```java
// 10. signDate (already exists, verify annotation)
@XmlElement(name = "signDate", namespace = "http://naesb.org/espi/customer")
private OffsetDateTime signDate;

// 11. validityInterval - CHANGE TYPE from String to DateTimeIntervalDto
@XmlElement(name = "validityInterval", namespace = "http://naesb.org/espi/customer")
private DateTimeIntervalDto validityInterval;  // WAS: String
```

##### T8e: Add CustomerAgreement Fields (6 fields)
**Actions - ADD these fields**:
```java
// 12. loadMgmt
@XmlElement(name = "loadMgmt", namespace = "http://naesb.org/espi/customer")
private String loadMgmt;

// 13. isPrePay
@XmlElement(name = "isPrePay", namespace = "http://naesb.org/espi/customer")
private Boolean isPrePay;

// 14. shutOffDateTime
@XmlElement(name = "shutOffDateTime", namespace = "http://naesb.org/espi/customer")
private OffsetDateTime shutOffDateTime;

// 15. currency
@XmlElement(name = "currency", namespace = "http://naesb.org/espi/customer")
private String currency;

// 16. futureStatus
@XmlElement(name = "Status", namespace = "http://naesb.org/espi/customer")
@XmlElementWrapper(name = "futureStatus", namespace = "http://naesb.org/espi/customer")
private List<CustomerAccountDto.StatusDto> futureStatus;

// 17. agreementId
@XmlElement(name = "agreementId", namespace = "http://naesb.org/espi/customer")
private String agreementId;
```

**Update @XmlType propOrder**:
```java
@XmlType(name = "CustomerAgreement", namespace = "http://naesb.org/espi/customer", propOrder = {
    "type", "authorName", "createdDateTime", "lastModifiedDateTime", "revisionNumber",
    "electronicAddress", "subject", "title", "docStatus",
    "signDate", "validityInterval",
    "loadMgmt", "isPrePay", "shutOffDateTime", "currency", "futureStatus", "agreementId"
})
```

**Update Constructor(s)**:
```java
@AllArgsConstructor  // Will generate constructor for all 18 fields (uuid + 17 XSD fields)
```

**Verification**:
- DTO compiles successfully
- NO Atom fields present
- NO relationship DTOs present
- All 17 XSD fields present in correct order
- propOrder matches field order exactly
- All fields use customer namespace

---

### MAPPER UPDATE PHASE

#### T9: Update CustomerAgreementMapper
**Dependencies**: T6 (DateTimeIntervalMapper), T8 (DTO rewrite)
**Complexity**: Medium
**Estimated Time**: 30 minutes
**File**: `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/mapper/customer/CustomerAgreementMapper.java`

**Actions**:
1. Update @Mapper uses clause:
   ```java
   @Mapper(componentModel = "spring", uses = {
       StatusMapper.class,              // From Phase 18
       ElectronicAddressMapper.class,   // From Phase 18
       DateTimeIntervalMapper.class     // From T6
   })
   ```

2. Add/Update all 17 field mappings:
   ```java
   @Mapping(target = "uuid", source = "id")

   // Document fields (9)
   @Mapping(target = "type", source = "type")
   @Mapping(target = "authorName", source = "authorName")
   @Mapping(target = "createdDateTime", source = "createdDateTime")
   @Mapping(target = "lastModifiedDateTime", source = "lastModifiedDateTime")
   @Mapping(target = "revisionNumber", source = "revisionNumber")
   @Mapping(target = "electronicAddress", source = "electronicAddress")
   @Mapping(target = "subject", source = "subject")
   @Mapping(target = "title", source = "title")
   @Mapping(target = "docStatus", source = "docStatus")

   // Agreement fields (2)
   @Mapping(target = "signDate", source = "signDate")
   @Mapping(target = "validityInterval", source = "validityInterval")

   // CustomerAgreement fields (6)
   @Mapping(target = "loadMgmt", source = "loadMgmt")
   @Mapping(target = "isPrePay", source = "isPrePay")
   @Mapping(target = "shutOffDateTime", source = "shutOffDateTime")
   @Mapping(target = "currency", source = "currency")
   @Mapping(target = "futureStatus", source = "futureStatus")
   @Mapping(target = "agreementId", source = "agreementId")

   CustomerAgreementDto toDto(CustomerAgreementEntity entity);

   @InheritInverseConfiguration
   CustomerAgreementEntity toEntity(CustomerAgreementDto dto);
   ```

3. Remove any old mappings for deleted fields (published, updated, links, description, etc.)

**Verification**:
- Mapper compiles successfully
- MapStruct generates implementation without warnings
- All 17 fields mapped bidirectionally
- No unmapped target warnings for IdentifiedObject fields

---

### DATABASE PHASE

#### T10: Update Flyway Migration
**Dependencies**: T7 (Entity updates)
**Complexity**: Medium
**Estimated Time**: 20 minutes
**File**: `openespi-common/src/main/resources/db/migration/V3__Create_additiional_Base_Tables.sql`

**Actions**:
1. Add missing Document field columns to customer_agreements table:
   ```sql
   -- Add missing Document fields (3 new columns)
   ALTER TABLE customer_agreements ADD COLUMN IF NOT EXISTS document_type VARCHAR(256);
   ALTER TABLE customer_agreements ADD COLUMN IF NOT EXISTS author_name VARCHAR(256);

   -- Add Document.docStatus embedded fields (3 new columns)
   ALTER TABLE customer_agreements ADD COLUMN IF NOT EXISTS status_value VARCHAR(256);
   ALTER TABLE customer_agreements ADD COLUMN IF NOT EXISTS status_date_time TIMESTAMP WITH TIME ZONE;
   ALTER TABLE customer_agreements ADD COLUMN IF NOT EXISTS status_reason VARCHAR(512);

   -- Add Document.electronicAddress embedded fields (4 new columns)
   ALTER TABLE customer_agreements ADD COLUMN IF NOT EXISTS email1 VARCHAR(256);
   ALTER TABLE customer_agreements ADD COLUMN IF NOT EXISTS email2 VARCHAR(256);
   ALTER TABLE customer_agreements ADD COLUMN IF NOT EXISTS web VARCHAR(256);
   ALTER TABLE customer_agreements ADD COLUMN IF NOT EXISTS radio VARCHAR(256);
   ```

2. Verify column order comments match new XSD order:
   ```sql
   -- Expected column order (matching customer.xsd):
   -- Document fields: document_type, author_name, created_date_time, last_modified_date_time,
   --                  revision_number, email1, email2, web, radio, subject, title,
   --                  status_value, status_date_time, status_reason
   -- Agreement fields: sign_date, validity_interval_start, validity_interval_duration
   -- CustomerAgreement: load_mgmt, is_pre_pay, shut_off_date_time, currency, agreement_id
   ```

**Verification**:
- SQL script has no syntax errors
- All 10 new columns added (3 Document + 3 Status + 4 ElectronicAddress)
- H2, MySQL, PostgreSQL compatible syntax used

---

### TESTING PHASE

#### T11: Create CustomerAgreementDtoTest
**Dependencies**: T8 (DTO rewrite), T9 (Mapper update)
**Complexity**: Medium
**Estimated Time**: 45 minutes
**File**: `openespi-common/src/test/java/org/greenbuttonalliance/espi/common/dto/customer/CustomerAgreementDtoTest.java`

**Actions**:
1. Create test class following Phase 18 CustomerAccountDtoTest pattern
2. Add @DisplayName("CustomerAgreementDto XML Marshalling Tests")
3. Setup DtoExportServiceImpl in @BeforeEach

**Test 1: shouldExportCustomerAgreementWithCompleteFields**
```java
@Test
@DisplayName("Should export CustomerAgreement with complete Document/Agreement fields")
void shouldExportCustomerAgreementWithCompleteFields() {
    // Create DTO with all 17 fields populated
    // Wrap in CustomerAtomEntryDto and AtomFeedDto
    // Export to XML
    // Assert ALL 17 fields present in correct customer namespace
    // Assert NO Atom fields inside <cust:CustomerAgreement>
}
```

**Test 2: shouldVerifyCustomerAgreementFieldOrder**
```java
@Test
@DisplayName("Should verify CustomerAgreement field order matches customer.xsd")
void shouldVerifyCustomerAgreementFieldOrder() {
    // Create DTO with all fields
    // Export to XML
    // Assert Document field order (9 fields)
    // Assert Agreement field order (2 fields)
    // Assert CustomerAgreement field order (6 fields)
    // Use indexOf() to verify sequence matches XSD
}
```

**Test 3: shouldUseCorrectCustomerNamespace**
```java
@Test
@DisplayName("Should use correct customer namespace")
void shouldUseCorrectCustomerNamespace() {
    // Create minimal DTO
    // Export to XML
    // Assert xmlns:cust="http://naesb.org/espi/customer"
    // Assert <cust:CustomerAgreement> present
    // Assert NO espi: namespace for CustomerAgreement
}
```

**SonarQube Best Practices**:
- ✅ NO throws IOException on test methods
- ✅ Chain assertions using fluent API
- ✅ Use meaningful test data

**Verification**:
- All 3 tests pass
- XML validates against customer.xsd structure
- Field order correct
- Customer namespace used

---

#### T12: Create CustomerAgreementRepositoryTest
**Dependencies**: T2 (Repository), T4 (Service), T7 (Entity), T10 (Migration)
**Complexity**: High
**Estimated Time**: 90 minutes
**File**: `openespi-common/src/test/java/org/greenbuttonalliance/espi/common/repositories/customer/CustomerAgreementRepositoryTest.java`

**Actions**:
1. Create test class following Phase 18 CustomerAccountRepositoryTest pattern
2. Extend BaseRepositoryTest
3. Add @Autowired CustomerAgreementRepository
4. Create helper methods: createValidCustomerAgreement()

**Test Structure** (21+ tests in 5 nested classes):

##### Nested Class 1: CRUD Operations (7 tests)
```java
@Nested
@DisplayName("CRUD Operations")
class CrudOperationsTest {
    @Test void shouldSaveAndRetrieveCustomerAgreementSuccessfully()
    @Test void shouldUpdateCustomerAgreementSuccessfully()
    @Test void shouldDeleteCustomerAgreementSuccessfully()
    @Test void shouldFindAllCustomerAgreements()
    @Test void shouldCheckIfCustomerAgreementExists()
    @Test void shouldCountCustomerAgreementsCorrectly()
    @Test void shouldHandleNullOptionalFields()
}
```

##### Nested Class 2: Document Field Persistence (3 tests)
```java
@Nested
@DisplayName("Document Field Persistence")
class DocumentFieldPersistenceTest {
    @Test void shouldPersistAllDocumentBaseFieldsCorrectly()
    @Test void shouldPersistDocumentElectronicAddressEmbeddedObject()
    @Test void shouldPersistDocumentDocStatusEmbeddedObject()
}
```

##### Nested Class 3: Agreement Field Persistence (2 tests)
```java
@Nested
@DisplayName("Agreement Field Persistence")
class AgreementFieldPersistenceTest {
    @Test void shouldPersistSignDateFieldCorrectly()
    @Test void shouldPersistValidityIntervalEmbeddedObject()
}
```

##### Nested Class 4: CustomerAgreement Field Persistence (4 tests)
```java
@Nested
@DisplayName("CustomerAgreement Field Persistence")
class CustomerAgreementFieldPersistenceTest {
    @Test void shouldPersistAllCustomerAgreementSpecificFieldsCorrectly()
    @Test void shouldPersistFutureStatusCollectionCorrectly()
    @Test void shouldHandleNullOptionalFieldsCorrectly()
    @Test void shouldPersistCurrencyFieldCorrectly()
}
```

##### Nested Class 5: Base Class Functionality (5 tests)
```java
@Nested
@DisplayName("Base Class Functionality")
class BaseClassTest {
    @Test void shouldInheritIdentifiedObjectFunctionality()
    @Test void shouldUpdateTimestampsOnModification()
    @Test void shouldGenerateUniqueIdsForDifferentEntities()
    @Test void shouldHandleEqualsAndHashCodeCorrectly()
    @Test void shouldGenerateMeaningfulToStringRepresentation()
}
```

**SonarQube Best Practices**:
- ✅ NO Thread.sleep() - remove from timestamp tests
- ✅ NO empty catch blocks
- ✅ Chain assertions using fluent API
- ✅ Use hasSameHashCodeAs() for hashCode tests
- ✅ NO unused variables

**Verification**:
- All 21+ tests pass
- Tests run on H2 in-memory database
- All CRUD operations work
- All fields persist correctly
- Embedded objects work
- Collections persist

---

#### T13: Run All Tests
**Dependencies**: T11 (DTO tests), T12 (Repository tests)
**Complexity**: Low
**Estimated Time**: 5 minutes

**Actions**:
```bash
# Run only CustomerAgreement tests
mvn test -pl openespi-common -Dtest=CustomerAgreementDtoTest,CustomerAgreementRepositoryTest

# Run all tests in openespi-common
mvn test -pl openespi-common

# Run all tests in entire project
mvn test
```

**Verification**:
- All 609+ existing tests pass
- All 24+ new CustomerAgreement tests pass
- No test failures
- No compilation errors

---

### QUALITY PHASE

#### T14: Fix SonarQube Violations
**Dependencies**: T13 (All tests passing)
**Complexity**: Medium
**Estimated Time**: 30 minutes

**Actions**:
1. Run SonarQube analysis (if available) or manual code review
2. Check for common violations:
   - ❌ Unnecessary IOException throws
   - ❌ Multiple separate assertions (should chain)
   - ❌ @AttributeOverrides wrapper (should apply directly)
   - ❌ Non-serializable embedded objects
   - ❌ Old instanceof patterns (should use pattern matching)
   - ❌ Unused variables
   - ❌ Thread.sleep() usage
   - ❌ Empty catch blocks
   - ❌ Commented-out code
   - ❌ Manual hashCode comparisons (should use hasSameHashCodeAs)

3. Fix any violations found
4. Re-run tests after each fix

**Verification**:
- Zero SonarQube violations
- All tests still pass
- Code follows Phase 18 quality standards

---

#### T15: Run Integration Tests
**Dependencies**: T14 (SonarQube clean)
**Complexity**: Medium
**Estimated Time**: 10 minutes

**Actions**:
```bash
# Run integration tests with TestContainers
mvn verify -pl openespi-common

# This will test against:
# - H2 (in-memory)
# - MySQL 9.5 (TestContainer)
# - PostgreSQL 18 (TestContainer)
```

**Verification**:
- All integration tests pass on H2
- All integration tests pass on MySQL 9.5
- All integration tests pass on PostgreSQL 18
- TestContainers start and stop cleanly
- No database-specific errors

---

### DELIVERY PHASE

#### T16: Commit and Push
**Dependencies**: T15 (Integration tests pass)
**Complexity**: Low
**Estimated Time**: 10 minutes

**Actions**:
1. Stage all modified and new files:
   ```bash
   git add openespi-common/src/main/java/org/greenbuttonalliance/espi/common/domain/customer/entity/CustomerAgreementEntity.java
   git add openespi-common/src/main/java/org/greenbuttonalliance/espi/common/dto/customer/CustomerAgreementDto.java
   git add openespi-common/src/main/java/org/greenbuttonalliance/espi/common/dto/DateTimeIntervalDto.java  # if new
   git add openespi-common/src/main/java/org/greenbuttonalliance/espi/common/mapper/customer/CustomerAgreementMapper.java
   git add openespi-common/src/main/java/org/greenbuttonalliance/espi/common/mapper/DateTimeIntervalMapper.java  # if new
   git add openespi-common/src/main/java/org/greenbuttonalliance/espi/common/repositories/customer/CustomerAgreementRepository.java
   git add openespi-common/src/main/java/org/greenbuttonalliance/espi/common/service/customer/CustomerAgreementService.java
   git add openespi-common/src/main/java/org/greenbuttonalliance/espi/common/service/customer/impl/CustomerAgreementServiceImpl.java
   git add openespi-common/src/main/resources/db/migration/V3__Create_additiional_Base_Tables.sql
   git add openespi-common/src/test/java/org/greenbuttonalliance/espi/common/dto/customer/CustomerAgreementDtoTest.java
   git add openespi-common/src/test/java/org/greenbuttonalliance/espi/common/repositories/customer/CustomerAgreementRepositoryTest.java
   ```

2. Commit with detailed message:
   ```bash
   git commit -m "feat: ESPI 4.0 Schema Compliance - Phase 24: CustomerAgreement Implementation

   Implement complete CustomerAgreement ESPI 4.0 schema compliance with Document and
   Agreement base class fields, embedded objects, and comprehensive testing.

   <Full commit message from plan>
   "
   ```

3. Push to remote:
   ```bash
   git push -u origin feature/schema-compliance-phase-24-customer-agreement
   ```

**Verification**:
- All files committed
- Commit message follows convention
- Branch pushed to remote
- No uncommitted changes remain

---

#### T17: Create Pull Request
**Dependencies**: T16 (Code pushed)
**Complexity**: Low
**Estimated Time**: 15 minutes

**Actions**:
```bash
gh pr create \
  --base main \
  --head feature/schema-compliance-phase-24-customer-agreement \
  --title "feat: ESPI 4.0 Schema Compliance - Phase 24: CustomerAgreement Implementation" \
  --body "$(cat <<'EOF'
## Summary

Implement complete CustomerAgreement ESPI 4.0 schema compliance...

<Full PR description with:>
- Key achievements
- Changes overview
- SonarQube fixes
- Test results
- ESPI 4.0 compliance
- Breaking changes (if any)
- Checklist
EOF
)"
```

**Verification**:
- PR created successfully
- PR description complete
- Base branch is main
- CI/CD checks start automatically

---

#### T18: Update Issue #28
**Dependencies**: T17 (PR created)
**Complexity**: Low
**Estimated Time**: 5 minutes

**Actions**:
```bash
gh issue comment 28 --body "$(cat <<'EOF'
## Phase 24: CustomerAgreement Implementation - ✅ COMPLETED

**Pull Request:** #XX
**Branch:** feature/schema-compliance-phase-24-customer-agreement
**Status:** Ready for review

### Summary
Completed full ESPI 4.0 schema compliance implementation for CustomerAgreement...

### Implementation Details
<Details from plan>

### Test Results
- ✅ All 633+ tests passing (24 new CustomerAgreement tests)
- ✅ Zero SonarQube violations
- ✅ Integration tests pass (H2, MySQL, PostgreSQL)

EOF
)"
```

**Verification**:
- Comment posted to Issue #28
- Issue remains OPEN (not closed)
- PR link included

---

## Task Execution Strategy

### Recommended Execution Order

**Phase 1: Quick Wins** (Parallel - 30 minutes total)
- Start all infrastructure tasks: T2, T3, T4 (can work in parallel)
- Start mapper phase: T5, T6 (can work in parallel)

**Phase 2: Core Implementation** (Sequential - 90 minutes total)
- T7: Update Entity (must complete before T8)
- T8: Rewrite DTO (needs T5, T7)
- T9: Update Mapper (needs T6, T8)
- T10: Update Migration (needs T7, can parallel with T8/T9)

**Phase 3: Testing** (Sequential - 140 minutes total)
- T11: Create DTO Test (needs T8, T9)
- T12: Create Repository Test (needs T2, T4, T7, T10)
- T13: Run All Tests (needs T11, T12)

**Phase 4: Quality & Delivery** (Sequential - 70 minutes total)
- T14: Fix SonarQube (needs T13)
- T15: Integration Tests (needs T14)
- T16: Commit & Push (needs T15)
- T17: Create PR (needs T16)
- T18: Update Issue (needs T17)

### Total Estimated Time
- **Minimum**: 5.5 hours (if everything goes perfectly)
- **Expected**: 7-8 hours (with normal debugging/fixes)
- **Maximum**: 10-12 hours (if major issues encountered)

### Critical Path
```
T1 → T7 → T8 → T9 → T11 → T12 → T13 → T14 → T15 → T16 → T17 → T18
```

### Parallelization Opportunities
- T2, T3, T5 can run in parallel after T1
- T6 can start immediately after T5
- T4 can start after T2 and T3 complete
- T10 can run in parallel with T8/T9
- All tests (T11, T12) need sequential execution

---

## Risk Areas

### High Risk
- **T7**: Entity reordering - complex, many fields, easy to make mistakes
- **T8**: DTO rewrite - must remove ALL Atom fields correctly
- **T12**: Repository tests - 21+ tests, lots of edge cases

### Medium Risk
- **T9**: Mapper updates - MapStruct unmapped field warnings
- **T10**: Migration - SQL syntax differences between databases
- **T14**: SonarQube - may discover unexpected violations

### Low Risk
- **T2, T3, T4**: Infrastructure - straightforward CRUD
- **T5, T6**: DateTimeInterval - may already exist
- **T16-T18**: Delivery - mechanical steps

---

## Success Criteria Checklist

- [ ] T1: Feature branch created
- [ ] T2: Repository interface created
- [ ] T3: Service interface created
- [ ] T4: Service implementation created with UUID v5
- [ ] T5: DateTimeIntervalDto exists/created
- [ ] T6: DateTimeIntervalMapper exists/created
- [ ] T7a-e: Entity updated with all fields in correct order
- [ ] T8a-e: DTO rewritten with ONLY XSD fields (NO Atom fields)
- [ ] T9: Mapper updated with all 17 field mappings
- [ ] T10: Migration adds 10 new columns
- [ ] T11: 3 DTO tests created and passing
- [ ] T12: 21+ repository tests created and passing
- [ ] T13: All 633+ tests passing
- [ ] T14: Zero SonarQube violations
- [ ] T15: Integration tests pass on all databases
- [ ] T16: Code committed and pushed
- [ ] T17: Pull request created
- [ ] T18: Issue #28 updated

---

## Quick Reference: Files Modified/Created

### Modified (3 files)
1. `CustomerAgreementEntity.java` - Add fields, reorder, fix types
2. `CustomerAgreementDto.java` - Complete rewrite, remove Atom fields
3. `V3__Create_additiional_Base_Tables.sql` - Add 10 columns

### Created (7-9 files)
4. `CustomerAgreementRepository.java` - NEW
5. `CustomerAgreementService.java` - NEW
6. `CustomerAgreementServiceImpl.java` - NEW
7. `DateTimeIntervalDto.java` - NEW (if not exists)
8. `DateTimeIntervalMapper.java` - NEW (if not exists)
9. `CustomerAgreementDtoTest.java` - NEW
10. `CustomerAgreementRepositoryTest.java` - NEW
11. `CustomerAgreementMapper.java` - MODIFIED (update mappings)

### Total: 10-12 files

---

**Plan Created**: 2026-01-25
**Ready for Execution**: Yes
**Estimated Completion**: 7-8 hours of focused work
