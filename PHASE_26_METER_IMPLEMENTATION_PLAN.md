# Phase 26: Meter - ESPI 4.0 Schema Compliance Implementation Plan

## Overview
Implement complete ESPI 4.0 customer.xsd schema compliance for Meter following Phase 25 (EndDevice) patterns. Meter extends EndDevice and adds meter-specific fields.

**Branch**: `feature/schema-compliance-phase-26-meter`
**Issue**: #28 Phase 26

**Scope**:
1. **Phase 26 (Meter)**: Complete implementation with unit and integration tests
2. **Remove non-ID queries** from MeterRepository and MeterService
3. **Rewrite MeterDto** for strict XSD compliance
4. **Create MeterMapper** for entity-to-DTO conversion

## Current State
**Existing**:
- ✅ MeterEntity.java (extends EndDeviceEntity, has 3 Meter fields)
- ✅ MeterRepository.java (HAS NON-ID QUERIES - NEEDS CLEANUP)
- ✅ MeterService.java (HAS NON-ID QUERIES - NEEDS CLEANUP)
- ✅ MeterDto.java (has Atom fields - NEEDS REWRITE)

**Missing**:
- ❌ MeterMapper.java
- ❌ MeterServiceImpl.java (implementation may exist but needs UUID v5 pattern)
- ❌ MeterDtoTest.java
- ❌ MeterRepositoryTest.java
- ❌ MeterMySQLIntegrationTest.java
- ❌ MeterPostgreSQLIntegrationTest.java

## Critical Issues

### MeterRepository.java ⚠️
**Has 11 non-ID custom query methods (MUST REMOVE ALL)**:
- ❌ findBySerialNumber()
- ❌ findByFormNumber()
- ❌ findByUtcNumber()
- ❌ findVirtualMeters()
- ❌ findPhysicalMeters()
- ❌ findPanDevices()
- ❌ findByAmrSystem()
- ❌ findByInstallCode()
- ❌ findByIntervalLengthGreaterThan()
- ❌ findCriticalMeters()
- ❌ All other non-ID queries

**Target**: ONLY inherited JpaRepository methods (findById, findAll, save, delete)

### MeterService.java ⚠️
**Has 18 non-ID service methods (MUST REMOVE ALL)**:
- ❌ findByUuid()
- ❌ findBySerialNumber()
- ❌ findByFormNumber()
- ❌ findByUtcNumber()
- ❌ findVirtualMeters()
- ❌ findPhysicalMeters()
- ❌ findPanDevices()
- ❌ findByAmrSystem()
- ❌ findByInstallCode()
- ❌ findByIntervalLengthGreaterThan()
- ❌ findCriticalMeters()
- ❌ existsBySerialNumber()
- ❌ countMeters()
- ❌ countVirtualMeters()
- ❌ countPhysicalMeters()
- ❌ All other non-ID methods

**Target**: ONLY 6 CRUD methods (findAll, findById, save, deleteById, existsById, count)

### MeterDto.java ❌
**Current (WRONG)**:
- Has Atom fields: id, uuid, published, updated, selfLink, upLink, relatedLinks
- Has description field (goes to AtomEntryDto.title)
- Has serviceLocation embedded DTO (use Atom link)
- Has wrong fields: installDate, removedDate, kh, meterMultiplier (not in XSD)
- Missing ALL 12 Asset fields
- Missing ALL 4 EndDevice fields

**Target (CORRECT)**:
- ONLY 19 XSD fields: 12 Asset + 4 EndDevice + 3 Meter
- NO Atom fields
- NO embedded relationships

## XSD Structure

**Meter extends EndDevice** (customer.xsd lines 243-268):

### Inheritance Chain
```
Object → IdentifiedObject → Asset → EndDevice → Meter
```

### Meter Fields (3 fields)
```xml
<xs:complexType name="Meter">
  <xs:extension base="EndDevice">
    <xs:sequence>
      <xs:element name="formNumber" type="String256" minOccurs="0"/>
      <xs:element name="MeterMultipliers" type="MeterMultiplier" minOccurs="0" maxOccurs="unbounded"/>
      <xs:element name="intervalLength" type="UInt32" minOccurs="0"/>
    </xs:sequence>
  </xs:extension>
</xs:complexType>
```

### Field Count Summary
- **Asset fields**: 12 (aliasName, initialCondition, initialLossOfLife, lifecycleDate, serialNumber, type, utcNumber, lotNumber, electronicAddress, acceptanceTest, status, category)
- **EndDevice fields**: 4 (amrSystem, installCode, isPan, timeZoneOffset)
- **Meter fields**: 3 (formNumber, meterMultipliers, intervalLength)
- **Total**: 19 fields

## Implementation Tasks

### Task 1: Remove Non-ID Queries from MeterRepository

**File**: `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/repositories/customer/MeterRepository.java`

**REMOVE ALL custom queries (lines 40-99)**:
```java
// DELETE THESE METHODS:
Optional<MeterEntity> findBySerialNumber(String serialNumber);
List<MeterEntity> findByFormNumber(String formNumber);
List<MeterEntity> findByUtcNumber(String utcNumber);
List<MeterEntity> findVirtualMeters();
List<MeterEntity> findPhysicalMeters();
List<MeterEntity> findPanDevices();
List<MeterEntity> findByAmrSystem(String amrSystem);
List<MeterEntity> findByInstallCode(String installCode);
List<MeterEntity> findByIntervalLengthGreaterThan(Long seconds);
List<MeterEntity> findCriticalMeters();
```

**KEEP ONLY**:
```java
@Repository
public interface MeterRepository extends JpaRepository<MeterEntity, UUID> {
    // ONLY inherited methods - NO custom queries
}
```

### Task 2: Simplify MeterService to 6 CRUD Methods

**File**: `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/service/customer/MeterService.java`

**REMOVE ALL non-CRUD methods (lines 48-130)**:
```java
// DELETE THESE METHODS:
Optional<MeterEntity> findByUuid(String uuid);
Optional<MeterEntity> findBySerialNumber(String serialNumber);
List<MeterEntity> findByFormNumber(String formNumber);
List<MeterEntity> findByUtcNumber(String utcNumber);
List<MeterEntity> findVirtualMeters();
List<MeterEntity> findPhysicalMeters();
List<MeterEntity> findPanDevices();
List<MeterEntity> findByAmrSystem(String amrSystem);
List<MeterEntity> findByInstallCode(String installCode);
List<MeterEntity> findByIntervalLengthGreaterThan(Long seconds);
List<MeterEntity> findCriticalMeters();
boolean existsBySerialNumber(String serialNumber);
long countMeters();
long countVirtualMeters();
long countPhysicalMeters();
```

**KEEP ONLY 6 CRUD methods**:
```java
public interface MeterService {
    List<MeterEntity> findAll();
    Optional<MeterEntity> findById(UUID id);
    MeterEntity save(MeterEntity meter);
    void deleteById(UUID id);
    boolean existsById(UUID id);
    long count();
}
```

### Task 3: Update MeterEntity.java

**File**: `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/domain/customer/entity/MeterEntity.java`

**Verify Structure**:
- ✅ Extends EndDeviceEntity
- ✅ Has formNumber field
- ✅ Has intervalLength field
- ⚠️ Has meterMultipliers TODO comment (line 57-58)

**Action**: Keep as-is for now. MeterMultiplier collection can be added in future phase.

**Update equals/hashCode**: Verify uses pattern matching for HibernateProxy

### Task 4: Rewrite MeterDto.java

**File**: `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/dto/customer/MeterDto.java`

**REMOVE**:
- ❌ id, uuid, published, updated, relatedLinks, selfLink, upLink (Atom fields)
- ❌ description (goes to AtomEntryDto.title)
- ❌ serviceLocation (use Atom link)
- ❌ installDate, removedDate (not in XSD)
- ❌ kh, meterMultiplier (not in XSD - wrong names, XSD has MeterMultipliers collection)
- ❌ getSelfHref(), getUpHref() methods

**ADD**:
- ✅ All 12 Asset fields (from EndDeviceDto)
- ✅ All 4 EndDevice fields (from EndDeviceDto)
- ✅ All 3 Meter fields (formNumber, meterMultipliers, intervalLength)

**propOrder**:
```java
@XmlType(name = "Meter", namespace = "http://naesb.org/espi/customer", propOrder = {
    // Asset fields (12) - inherited from EndDevice
    "aliasName", "initialCondition", "initialLossOfLife", "lifecycleDate",
    "serialNumber", "type", "utcNumber", "lotNumber",
    "electronicAddress", "acceptanceTest", "status", "category",
    // EndDevice fields (4) - inherited from EndDevice
    "amrSystem", "installCode", "isPan", "timeZoneOffset",
    // Meter fields (3)
    "formNumber", "meterMultipliers", "intervalLength"
})
```

**Structure**:
```java
@XmlRootElement(name = "Meter", namespace = "http://naesb.org/espi/customer")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Meter", namespace = "http://naesb.org/espi/customer", propOrder = {...})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MeterDto {

    // Asset fields (12) - from customer.xsd Asset type
    @XmlElement(name = "aliasName", namespace = "http://naesb.org/espi/customer")
    private String aliasName;

    @XmlElement(name = "initialCondition", namespace = "http://naesb.org/espi/customer")
    private String initialCondition;

    @XmlElement(name = "initialLossOfLife", namespace = "http://naesb.org/espi/customer")
    private String initialLossOfLife;

    @XmlElement(name = "lifecycleDate", namespace = "http://naesb.org/espi/customer")
    private LifecycleDateDto lifecycleDate;

    @XmlElement(name = "serialNumber", namespace = "http://naesb.org/espi/customer")
    private String serialNumber;

    @XmlElement(name = "type", namespace = "http://naesb.org/espi/customer")
    private String type;

    @XmlElement(name = "utcNumber", namespace = "http://naesb.org/espi/customer")
    private String utcNumber;

    @XmlElement(name = "lotNumber", namespace = "http://naesb.org/espi/customer")
    private String lotNumber;

    @XmlElement(name = "electronicAddress", namespace = "http://naesb.org/espi/customer")
    private List<ElectronicAddressDto> electronicAddress;

    @XmlElement(name = "acceptanceTest", namespace = "http://naesb.org/espi/customer")
    private AcceptanceTestDto acceptanceTest;

    @XmlElement(name = "status", namespace = "http://naesb.org/espi/customer")
    private StatusDto status;

    @XmlElement(name = "category", namespace = "http://naesb.org/espi/customer")
    private String category;

    // EndDevice fields (4) - from customer.xsd EndDevice type
    @XmlElement(name = "amrSystem", namespace = "http://naesb.org/espi/customer")
    private String amrSystem;

    @XmlElement(name = "installCode", namespace = "http://naesb.org/espi/customer")
    private String installCode;

    @XmlElement(name = "isPan", namespace = "http://naesb.org/espi/customer")
    private Boolean isPan;

    @XmlElement(name = "timeZoneOffset", namespace = "http://naesb.org/espi/customer")
    private Long timeZoneOffset;

    // Meter fields (3) - from customer.xsd Meter type
    @XmlElement(name = "formNumber", namespace = "http://naesb.org/espi/customer")
    private String formNumber;

    @XmlElement(name = "MeterMultipliers", namespace = "http://naesb.org/espi/customer")
    private List<MeterMultiplierDto> meterMultipliers;

    @XmlElement(name = "intervalLength", namespace = "http://naesb.org/espi/customer")
    private Long intervalLength;
}
```

**Note**: MeterMultiplierDto is a simple embedded object with 2 fields:
```java
public class MeterMultiplierDto {
    private String kind;  // MeterMultiplierKind enum as String
    private Float value;
}
```

### Task 5: Create MeterMapper.java

**File**: `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/mapper/customer/MeterMapper.java`

```java
@Mapper(componentModel = "spring", uses = {
    LifecycleDateMapper.class,
    AcceptanceTestMapper.class,
    ElectronicAddressMapper.class,
    StatusMapper.class
})
public interface MeterMapper {

    @Mapping(target = "id", ignore = true)
    MeterEntity toEntity(MeterDto dto);

    MeterDto toDto(MeterEntity entity);

    // MeterMultiplier mappings (if implementing collection)
    MeterMultiplierDto toDto(MeterMultiplier entity);
    MeterMultiplier toEntity(MeterMultiplierDto dto);
}
```

### Task 6: Rewrite MeterServiceImpl.java

**File**: `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/service/customer/impl/MeterServiceImpl.java`

**Pattern**: Follow EndDeviceServiceImpl pattern with UUID v5 generation

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class MeterServiceImpl implements MeterService {

    private static final String NAMESPACE = "ESPI-METER";
    private final MeterRepository repository;
    private final EspiIdGeneratorService idGenerator;

    @Override
    @Transactional
    public MeterEntity save(MeterEntity meter) {
        if (meter.getId() == null) {
            // ❌ NO random UUID fallback - ESPI requires UUID v5
            if (meter.getSerialNumber() == null) {
                throw new IllegalArgumentException(
                    "SerialNumber is required for Meter UUID generation");
            }
            UUID deterministicId = idGenerator.generateV5UUID(
                NAMESPACE, meter.getSerialNumber());
            meter.setId(deterministicId);
            log.debug("Generated UUID v5 for Meter: {}", deterministicId);
        }
        return repository.save(meter);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MeterEntity> findAll() {
        return repository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MeterEntity> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(UUID id) {
        return repository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return repository.count();
    }
}
```

**CRITICAL**: NO random UUID fallback - ESPI standard requires UUID v5

### Task 7: Register MeterDto in DtoExportServiceImpl

**File**: `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/service/impl/DtoExportServiceImpl.java`

Add to JAXBContext initialization (line ~264):
```java
org.greenbuttonalliance.espi.common.dto.customer.MeterDto.class,
org.greenbuttonalliance.espi.common.dto.customer.MeterMultiplierDto.class,
```

### Task 8: Verify Flyway Migration

Verify meters table in V3__Create_additiional_Base_Tables.sql:
- ✅ Inherits all EndDevice columns (12 Asset + 4 EndDevice = 16)
- ✅ Has form_number column
- ✅ Has interval_length column
- ⚠️ MeterMultipliers collection table (create if needed)

### Task 9: Create Unit Tests

**MeterDtoTest.java** (6+ tests):
```java
@DisplayName("MeterDto XML Marshalling Tests")
class MeterDtoTest {
    // shouldExportMeterWithRealisticData
    // shouldVerifyMeterFieldOrder (19 fields: 12 Asset + 4 EndDevice + 3 Meter)
    // shouldVerifyMeterInheritsFromEndDevice
    // shouldVerifyMeterMultipliersCollection
    // shouldExportMeterWithMinimalData
    // shouldUseCorrectCustomerNamespace
}
```

**MeterRepositoryTest.java** (21+ tests):
```java
@DisplayName("Meter Repository Tests")
class MeterRepositoryTest {

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperationsTest {
        // 5 tests: save, retrieve, update, delete, findAll
    }

    @Nested
    @DisplayName("Asset Field Persistence")
    class AssetFieldPersistenceTest {
        // 5 tests: inherited Asset fields
    }

    @Nested
    @DisplayName("EndDevice Field Persistence")
    class EndDeviceFieldPersistenceTest {
        // 3 tests: inherited EndDevice fields
    }

    @Nested
    @DisplayName("Meter Field Persistence")
    class MeterFieldPersistenceTest {
        // 3 tests: formNumber, intervalLength, meterMultipliers
    }

    @Nested
    @DisplayName("Entity Validation")
    class EntityValidationTest {
        // 2 tests: serialNumber required for UUID generation
    }

    @Nested
    @DisplayName("Base Class Functionality")
    class BaseClassFunctionalityTest {
        // 3 tests: extends EndDevice correctly
    }
}
```

### Task 10: Create Integration Tests

**Files to Create**:
- `MeterMySQLIntegrationTest.java`
- `MeterPostgreSQLIntegrationTest.java`

**Pattern**: Follow EndDeviceMySQLIntegrationTest pattern

**Location**: `openespi-common/src/test/java/org/greenbuttonalliance/espi/common/repositories/integration/`

**MeterMySQLIntegrationTest.java**:
```java
@DisplayName("Meter Integration Tests - MySQL")
@ActiveProfiles({"test", "test-mysql"})
class MeterMySQLIntegrationTest extends BaseTestContainersTest {

    @Container
    private static final MySQLContainer<?> mysql = mysqlContainer;

    @Autowired
    private MeterRepository meterRepository;

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperationsTest {
        // 5+ tests: save, retrieve, update, delete, findAll
    }

    @Nested
    @DisplayName("Asset Field Persistence")
    class AssetFieldPersistenceTest {
        // 3+ tests: LifecycleDate, AcceptanceTest, ElectronicAddress, Status
    }

    @Nested
    @DisplayName("EndDevice Field Persistence")
    class EndDeviceFieldPersistenceTest {
        // 2+ tests: amrSystem, installCode, isPan, timeZoneOffset
    }

    @Nested
    @DisplayName("Meter Field Persistence")
    class MeterFieldPersistenceTest {
        // 3+ tests: formNumber, intervalLength, meterMultipliers collection
    }
}
```

**MeterPostgreSQLIntegrationTest.java**: Same structure with PostgreSQL container

**Expected**: 13+ tests per database (26+ total integration tests for Meter)

### Task 11: Update TestDataBuilders

**File**: `openespi-common/src/test/java/org/greenbuttonalliance/espi/common/test/TestDataBuilders.java`

Add helper method:
```java
public static MeterEntity createValidMeter() {
    MeterEntity meter = new MeterEntity();
    meter.setSerialNumber("METER-" + faker.number().digits(10));
    meter.setFormNumber("12S");
    meter.setIntervalLength(900L); // 15 minutes in seconds

    // Asset fields (inherited from EndDevice)
    meter.setType("Smart Meter");
    meter.setUtcNumber("UTC-" + faker.number().digits(6));
    meter.setLotNumber("LOT-" + faker.number().digits(8));

    // EndDevice fields
    meter.setAmrSystem("AMR-" + faker.company().name());
    meter.setInstallCode("INSTALL-" + faker.number().digits(8));
    meter.setIsPan(false);

    return meter;
}
```

### Task 12: Run All Tests

```bash
cd openespi-common

# Run unit tests
mvn test

# Run integration tests
mvn verify -DskipUnitTests
```

**Expected Results**:
- Unit tests: 680+ pass (654 existing + 26+ new Meter)
- Integration tests: 125+ pass (99 existing + 26+ new Meter)
- **Total**: 805+ tests pass

### Task 13: Commit, Push, PR

Follow Phase 25 git workflow:
1. Create feature branch
2. Commit changes with comprehensive message
3. Push to remote
4. Create PR with detailed description
5. Update Issue #28 (do NOT close)

## Expected File Changes

| File | Type | Description |
|------|------|-------------|
| MeterRepository.java | MODIFY | Remove ALL 11 non-ID query methods |
| MeterService.java | MODIFY | Remove ALL 18 non-ID methods, keep 6 CRUD |
| MeterServiceImpl.java | REWRITE | UUID v5 pattern, 6 CRUD methods only |
| MeterEntity.java | VERIFY | Confirm correct structure (extends EndDevice) |
| MeterDto.java | REWRITE | Remove Atom, add 19 XSD fields |
| MeterMapper.java | CREATE | MapStruct mapper with nested DTOs |
| MeterMultiplierDto.java | CREATE | Embedded object (2 fields) |
| DtoExportServiceImpl.java | MODIFY | Add MeterDto to JAXBContext |
| MeterDtoTest.java | CREATE | 6+ unit tests |
| MeterRepositoryTest.java | CREATE | 21+ unit tests |
| MeterMySQLIntegrationTest.java | CREATE | 13+ integration tests |
| MeterPostgreSQLIntegrationTest.java | CREATE | 13+ integration tests |
| TestDataBuilders.java | MODIFY | Add createValidMeter() |

**Total**: 13 files (6 modified, 7 created)

## Success Criteria

**Phase 26: Meter**
- ✅ Repository: NO non-ID queries (remove all 11)
- ✅ Service: ONLY 6 CRUD methods (remove 18 non-ID methods)
- ✅ DTO: NO Atom fields
- ✅ DTO: All 19 XSD fields (12 Asset + 4 EndDevice + 3 Meter)
- ✅ Mapper: Uses LifecycleDateMapper, AcceptanceTestMapper, etc.
- ✅ UUID v5: NO random fallback
- ✅ Extends: EndDevice correctly

**Testing**
- ✅ Unit Tests: 680+ pass (654 existing + 26+ new)
- ✅ Integration Tests: 125+ pass (99 existing + 26+ new)
- ✅ Total Tests: 805+ pass

**Quality**
- ✅ SonarQube: Zero violations
- ✅ CI/CD: All checks pass

## Critical Notes

1. **UUID v5 Generation**:
   - Use serialNumber as seed
   - ❌ NO random UUID fallback
   - Throw exception if serialNumber is null

2. **Repository Cleanup**:
   - Remove ALL 11 custom query methods
   - Keep ONLY inherited JpaRepository methods

3. **Service Cleanup**:
   - Remove ALL 18 non-CRUD methods
   - Keep ONLY 6 CRUD methods (findAll, findById, save, deleteById, existsById, count)

4. **Meter Extends EndDevice**:
   - Inherits 12 Asset fields
   - Inherits 4 EndDevice fields
   - Adds 3 Meter fields
   - Total: 19 fields

5. **MeterMultipliers Collection**:
   - Optional: Can defer to future phase if needed
   - If implementing: Create MeterMultiplierDto (2 fields: kind, value)
   - XSD allows 0 to unbounded multipliers

6. **Field Name Mapping**:
   - XSD: `MeterMultipliers` (capital M, plural)
   - Entity: `meterMultipliers` (camelCase)
   - DTO: `meterMultipliers` (camelCase, maps to XML MeterMultipliers)

---

**Version**: 1.0
**Created**: 2026-01-29
**Status**: ✅ Ready for Implementation

**Change Log**:
- v1.0: Initial Meter implementation plan based on Phase 25 EndDevice template
