# Phase 25: EndDevice - ESPI 4.0 Schema Compliance Implementation Plan

## Overview
Implement complete ESPI 4.0 customer.xsd schema compliance for EndDevice following Phase 18/23/24 patterns. This phase also includes backfilling missing integration tests for Phases 18, 23, and 24.

**Branch**: `feature/schema-compliance-phase-25-end-device`
**Issue**: #28 Phase 25

**Scope**:
1. **Phase 25 (EndDevice)**: Complete implementation with unit and integration tests
2. **Phase 18 Backfill**: Add missing CustomerAccount integration tests
3. **Phase 23 Backfill**: Add missing ServiceLocation integration tests
4. **Phase 24 Backfill**: Add missing CustomerAgreement integration tests

## Current State
**Existing**:
- ✅ EndDeviceEntity.java (extends IdentifiedObject, has Asset fields inline, has EndDevice fields)
- ✅ EndDeviceDto.java (has Atom fields - NEEDS REWRITE)

**Missing**:
- ❌ EndDeviceMapper.java
- ❌ EndDeviceRepository.java
- ❌ EndDeviceService.java + EndDeviceServiceImpl.java
- ❌ EndDeviceDtoTest.java
- ❌ EndDeviceRepositoryTest.java

## Critical Issues

### EndDeviceEntity.java ⚠️
- ❌ **Status field type**: Uses `CustomerEntity.Status` → must use shared `Status`
- ⚠️ **Field order**: Verify matches XSD (Asset fields, then EndDevice fields)

### EndDeviceDto.java ❌
**Current (WRONG)**:
- Has Atom fields: published, updated, selfLink, upLink, relatedLinks
- Has description field
- Has serviceLocation embedded DTO
- Missing ALL 12 Asset fields

**Target (CORRECT)**:
- ONLY 16 XSD fields: 12 Asset + 4 EndDevice
- NO Atom fields
- NO embedded relationships

## Implementation Tasks

### Task 1: Verify No Non-ID Queries

✅ **Verified**: No EndDeviceRepository or EndDeviceService exists yet
**Result**: No queries to remove

### Task 2: Update EndDeviceEntity.java

**File**: `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/domain/customer/entity/EndDeviceEntity.java`

**Fix Status Type** (line 147):
```java
// BEFORE:
@Embedded
private CustomerEntity.Status status;

// AFTER (use repeatable @AttributeOverride, no @AttributeOverrides wrapper):
@Embedded
@AttributeOverride(name = "value", column = @Column(name = "status_value"))
@AttributeOverride(name = "dateTime", column = @Column(name = "status_date_time"))
@AttributeOverride(name = "remark", column = @Column(name = "status_remark"))
@AttributeOverride(name = "reason", column = @Column(name = "status_reason"))
private Status status;
```

**Verify Field Order**:
1. Asset fields (12): type, utcNumber, serialNumber, lotNumber, purchasePrice, critical, electronicAddress, lifecycle, acceptanceTest, initialCondition, initialLossOfLife, status
2. EndDevice fields (4): isVirtual, isPan, installCode, amrSystem

**Update equals/hashCode**: Use pattern matching for HibernateProxy

### Task 3: Rewrite EndDeviceDto.java

**File**: `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/dto/customer/EndDeviceDto.java`

**REMOVE**:
- ❌ id, published, updated, relatedLinks, selfLink, upLink (Atom fields)
- ❌ description (goes to AtomEntryDto.title)
- ❌ serviceLocation (use Atom link)
- ❌ getSelfHref(), getUpHref() methods

**ADD**:
- ✅ All 12 Asset fields
- ✅ All 4 EndDevice fields
- ✅ Nested LifecycleDateDto (2 fields)
- ✅ Nested AcceptanceTestDto (4 fields)

**propOrder**:
```java
@XmlType(name = "EndDevice", namespace = "http://naesb.org/espi/customer", propOrder = {
    // Asset fields (12)
    "type", "utcNumber", "serialNumber", "lotNumber", "purchasePrice", "critical",
    "electronicAddress", "lifecycle", "acceptanceTest", "initialCondition",
    "initialLossOfLife", "status",
    // EndDevice fields (4)
    "isVirtual", "isPan", "installCode", "amrSystem"
})
```

### Task 4: Create EndDeviceMapper.java

**File**: `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/mapper/customer/EndDeviceMapper.java`

```java
@Mapper(componentModel = "spring", uses = {CustomerMapper.class})
public interface EndDeviceMapper {

    @Mapping(target = "uuid", source = "id")
    // Asset fields (12 mappings)
    // EndDevice fields (4 mappings)
    EndDeviceDto toDto(EndDeviceEntity entity);

    @InheritInverseConfiguration
    @Mapping(target = "id", source = "uuid")
    EndDeviceEntity toEntity(EndDeviceDto dto);

    // LifecycleDate and AcceptanceTest mappings
}
```

### Task 5: Create EndDeviceRepository.java

**File**: `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/repositories/customer/EndDeviceRepository.java`

```java
@Repository
public interface EndDeviceRepository extends JpaRepository<EndDeviceEntity, UUID> {
    // ONLY inherited methods - NO custom queries
}
```

### Task 6: Create EndDeviceService.java + EndDeviceServiceImpl.java

**Service Interface**: 6 CRUD methods

**Service Implementation**:
```java
@Service
@RequiredArgsConstructor
public class EndDeviceServiceImpl implements EndDeviceService {

    private static final String NAMESPACE = "ESPI-END-DEVICE";
    private final EndDeviceRepository repository;
    private final EspiIdGeneratorService idGenerator;

    @Override
    @Transactional
    public EndDeviceEntity save(EndDeviceEntity endDevice) {
        if (endDevice.getId() == null) {
            // ❌ NO random UUID fallback - ESPI requires UUID v5
            if (endDevice.getSerialNumber() == null) {
                throw new IllegalArgumentException(
                    "SerialNumber is required for EndDevice UUID generation");
            }
            UUID deterministicId = idGenerator.generateV5UUID(
                NAMESPACE, endDevice.getSerialNumber());
            endDevice.setId(deterministicId);
            log.debug("Generated UUID v5 for EndDevice: {}", deterministicId);
        }
        return repository.save(endDevice);
    }

    // ... other CRUD methods ...
}
```

**CRITICAL**: NO random UUID fallback - ESPI standard requires UUID v5

### Task 7: Register EndDeviceDto in DtoExportServiceImpl

Add to JAXBContext initialization (line ~264):
```java
org.greenbuttonalliance.espi.common.dto.customer.EndDeviceDto.class,
```

### Task 8: Verify Flyway Migration

Verify end_devices table has all columns including `status_remark`.

### Task 9: Create Unit Tests

**EndDeviceDtoTest.java** (6+ tests):
- shouldExportEndDeviceWithRealisticData
- shouldVerifyEndDeviceFieldOrder
- shouldVerifyStatus4FieldCompliance
- shouldVerifyElectronicAddress8FieldCompliance
- shouldExportEndDeviceWithMinimalData
- shouldUseCorrectCustomerNamespace

**EndDeviceRepositoryTest.java** (21+ tests):
- CRUD Operations (7)
- Asset Field Persistence (5)
- EndDevice Field Persistence (3)
- Base Class Functionality (5)

### Task 10: Create Missing Integration Tests for Phase 18 (CustomerAccount)

**Files to Create**:
- `CustomerAccountMySQLIntegrationTest.java`
- `CustomerAccountPostgreSQLIntegrationTest.java`

**Pattern**: Follow `CustomerMySQLIntegrationTest.java` pattern

**Location**: `openespi-common/src/test/java/org/greenbuttonalliance/espi/common/integration/customer/`

**CustomerAccountMySQLIntegrationTest.java**:
```java
@DisplayName("CustomerAccount Integration Tests - MySQL")
@ActiveProfiles({"test", "test-mysql"})
class CustomerAccountMySQLIntegrationTest extends BaseTestContainersTest {

    @Container
    private static final MySQLContainer<?> mysql = mysqlContainer;

    @Autowired
    private CustomerAccountRepository customerAccountRepository;

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperationsTest {
        // 7+ tests: save, retrieve, update, delete, findAll, exists, count
    }

    @Nested
    @DisplayName("Field Persistence")
    class FieldPersistenceTest {
        // 3+ tests: billingAddress, currency, budgetBill, accountType
    }

    @Nested
    @DisplayName("Relationship Persistence")
    class RelationshipPersistenceTest {
        // 2+ tests: Customer relationship via Atom links
    }
}
```

**CustomerAccountPostgreSQLIntegrationTest.java**: Same structure with PostgreSQL container

**Expected**: 12+ tests per database (24+ total integration tests for CustomerAccount)

### Task 11: Create Missing Integration Tests for Phase 23 (ServiceLocation)

**Files to Create**:
- `ServiceLocationMySQLIntegrationTest.java`
- `ServiceLocationPostgreSQLIntegrationTest.java`

**Pattern**: Follow `CustomerMySQLIntegrationTest.java` pattern

**Location**: `openespi-common/src/test/java/org/greenbuttonalliance/espi/common/integration/customer/`

**ServiceLocationMySQLIntegrationTest.java**:
```java
@DisplayName("ServiceLocation Integration Tests - MySQL")
@ActiveProfiles({"test", "test-mysql"})
class ServiceLocationMySQLIntegrationTest extends BaseTestContainersTest {

    @Container
    private static final MySQLContainer<?> mysql = mysqlContainer;

    @Autowired
    private ServiceLocationRepository serviceLocationRepository;

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperationsTest {
        // 7+ tests: save, retrieve, update, delete, findAll, exists, count
    }

    @Nested
    @DisplayName("Location Field Persistence")
    class LocationFieldPersistenceTest {
        // 4+ tests: mainAddress, phone1, electronicAddress, status, positionPoints
    }

    @Nested
    @DisplayName("ServiceLocation Field Persistence")
    class ServiceLocationFieldPersistenceTest {
        // 2+ tests: accessMethod, siteAccessProblem, needsInspection, outageBlock
    }

    @Nested
    @DisplayName("Relationship Persistence")
    class RelationshipPersistenceTest {
        // 2+ tests: UsagePoint cross-stream references via hrefs
    }
}
```

**ServiceLocationPostgreSQLIntegrationTest.java**: Same structure with PostgreSQL container

**Expected**: 12+ tests per database (24+ total integration tests for ServiceLocation)

### Task 12: Create Missing Integration Tests for Phase 24 (CustomerAgreement)

**Files to Create**:
- `CustomerAgreementMySQLIntegrationTest.java`
- `CustomerAgreementPostgreSQLIntegrationTest.java`

**Pattern**: Follow `CustomerMySQLIntegrationTest.java` pattern

**Location**: `openespi-common/src/test/java/org/greenbuttonalliance/espi/common/integration/customer/`

**CustomerAgreementMySQLIntegrationTest.java**:
```java
@DisplayName("CustomerAgreement Integration Tests - MySQL")
@ActiveProfiles({"test", "test-mysql"})
class CustomerAgreementMySQLIntegrationTest extends BaseTestContainersTest {

    @Container
    private static final MySQLContainer<?> mysql = mysqlContainer;

    @Autowired
    private CustomerAgreementRepository customerAgreementRepository;

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperationsTest {
        // 7+ tests: save, retrieve, update, delete, findAll, exists, count
    }

    @Nested
    @DisplayName("Field Persistence")
    class FieldPersistenceTest {
        // 3+ tests: signDate, loadMgmt, validityInterval, budgetBill
    }

    @Nested
    @DisplayName("Relationship Persistence")
    class RelationshipPersistenceTest {
        // 2+ tests: CustomerAccount, ServiceLocation relationships
    }
}
```

**CustomerAgreementPostgreSQLIntegrationTest.java**: Same structure with PostgreSQL container

**Expected**: 12+ tests per database (24+ total integration tests for CustomerAgreement)

### Task 13: Create Integration Tests for Phase 25 (EndDevice)

**Files to Create**:
- `EndDeviceMySQLIntegrationTest.java`
- `EndDevicePostgreSQLIntegrationTest.java`

**Pattern**: Follow `CustomerMySQLIntegrationTest.java` pattern

**Location**: `openespi-common/src/test/java/org/greenbuttonalliance/espi/common/integration/customer/`

**EndDeviceMySQLIntegrationTest.java**:
```java
@DisplayName("EndDevice Integration Tests - MySQL")
@ActiveProfiles({"test", "test-mysql"})
class EndDeviceMySQLIntegrationTest extends BaseTestContainersTest {

    @Container
    private static final org.testcontainers.containers.MySQLContainer<?> mysql = mysqlContainer;

    @Autowired
    private EndDeviceRepository endDeviceRepository;

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperationsTest {
        // 7+ tests for save, retrieve, update, delete, findAll, exists, count
    }

    @Nested
    @DisplayName("Asset Field Persistence")
    class AssetFieldPersistenceTest {
        // 3+ tests for Asset fields, ElectronicAddress, Status
    }

    @Nested
    @DisplayName("EndDevice Field Persistence")
    class EndDeviceFieldPersistenceTest {
        // 2+ tests for isVirtual, isPan, installCode, amrSystem
    }

    @Nested
    @DisplayName("Relationship Persistence")
    class RelationshipPersistenceTest {
        // 2+ tests for ServiceLocation relationship via Atom links
    }
}
```

**EndDevicePostgreSQLIntegrationTest.java**: Same structure with PostgreSQL container

**Expected**: 12+ tests per database (24+ total integration tests for EndDevice)

### Task 14: Run All Tests

```bash
cd openespi-common

# Run unit tests
mvn test

# Run integration tests
mvn verify -DskipUnitTests
```

**Expected Results**:
- Unit tests: 660+ pass (636 existing + 24 new EndDevice)
- Integration tests: 96+ new integration tests
  - 24 CustomerAccount (12 MySQL + 12 PostgreSQL)
  - 24 ServiceLocation (12 MySQL + 12 PostgreSQL)
  - 24 CustomerAgreement (12 MySQL + 12 PostgreSQL)
  - 24 EndDevice (12 MySQL + 12 PostgreSQL)
- **Total**: 756+ tests pass

### Task 15: Run SonarQube Analysis

```bash
cd openespi-common

# Run SonarQube analysis with test coverage
mvn clean verify sonar:sonar \
  -Dsonar.projectKey=openespi-greenbutton-java \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=<sonar-token>
```

**Verify in SonarQube Dashboard**:
- ✅ Zero code smells
- ✅ Zero bugs
- ✅ Zero vulnerabilities
- ✅ Zero security hotspots
- ✅ Coverage metrics acceptable
- ✅ Quality Gate: PASSED

**Fix any issues before proceeding to commit.**

### Task 16: Commit, Push, PR

Follow Phase 18/23/24 git workflow. Update Issue #28 (do NOT close).

## Expected File Changes

| File | Type | Description |
|------|------|-------------|
| **Phase 25: EndDevice Files** | | |
| EndDeviceEntity.java | MODIFY | Fix Status type |
| EndDeviceDto.java | REWRITE | Remove Atom, add 16 XSD fields |
| EndDeviceMapper.java | CREATE | MapStruct mapper |
| EndDeviceRepository.java | CREATE | JpaRepository |
| EndDeviceService.java | CREATE | Service interface |
| EndDeviceServiceImpl.java | CREATE | Service with UUID v5 (NO fallback) |
| DtoExportServiceImpl.java | MODIFY | Add to JAXBContext |
| EndDeviceDtoTest.java | CREATE | 6+ unit tests |
| EndDeviceRepositoryTest.java | CREATE | 21+ unit tests |
| EndDeviceMySQLIntegrationTest.java | CREATE | 12+ integration tests |
| EndDevicePostgreSQLIntegrationTest.java | CREATE | 12+ integration tests |
| **Phase 18: CustomerAccount Missing Tests** | | |
| CustomerAccountMySQLIntegrationTest.java | CREATE | 12+ integration tests |
| CustomerAccountPostgreSQLIntegrationTest.java | CREATE | 12+ integration tests |
| **Phase 23: ServiceLocation Missing Tests** | | |
| ServiceLocationMySQLIntegrationTest.java | CREATE | 12+ integration tests |
| ServiceLocationPostgreSQLIntegrationTest.java | CREATE | 12+ integration tests |
| **Phase 24: CustomerAgreement Missing Tests** | | |
| CustomerAgreementMySQLIntegrationTest.java | CREATE | 12+ integration tests |
| CustomerAgreementPostgreSQLIntegrationTest.java | CREATE | 12+ integration tests |

**Total**: 17 files (2 modified, 15 created)

## Success Criteria

**Phase 25: EndDevice**
- ✅ Status: Uses shared `Status`
- ✅ DTO: NO Atom fields
- ✅ DTO: All 16 XSD fields
- ✅ Repository: NO non-ID queries
- ✅ UUID v5: NO random fallback
- ✅ @AttributeOverride: No wrapper

**Testing**
- ✅ Unit Tests: 660+ pass (636 existing + 24 new EndDevice)
- ✅ Integration Tests: 96+ pass
  - 24 CustomerAccount (Phase 18 backfill)
  - 24 ServiceLocation (Phase 23 backfill)
  - 24 CustomerAgreement (Phase 24 backfill)
  - 24 EndDevice (Phase 25 new)
- ✅ Total Tests: 756+ pass

**Quality**
- ✅ SonarQube: Zero violations
- ✅ CI/CD: All checks pass

## Critical Notes

1. **UUID v5 Generation**:
   - Use serialNumber as seed
   - ❌ NO random UUID fallback
   - Throw exception if serialNumber is null

2. **@AttributeOverride**: Apply directly (no wrapper)

3. **Asset Embedded**: Fields inline in EndDevice

4. **Repository**: NO non-ID custom queries

5. **Integration Test Backfill**:
   - Phases 18, 23, 24 merged without integration tests
   - This phase adds 96 missing integration tests (24 per phase × 3 phases + 24 for EndDevice)
   - All tests follow CustomerMySQLIntegrationTest.java pattern
   - Each phase gets MySQL and PostgreSQL coverage

---

**Version**: 2.0
**Created**: 2026-01-27
**Updated**: 2026-01-27
**Status**: ✅ Ready for Implementation

**Change Log**:
- v1.0: Initial EndDevice implementation plan
- v2.0: Added missing integration tests for Phases 18, 23, 24 (96 additional tests)
