# Phase 23: ServiceLocation - ESPI 4.0 Schema Compliance Implementation Plan

## Overview
Implement complete ESPI 4.0 customer.xsd schema compliance for ServiceLocation entity, DTO, repository, service, and mapper layers.

## Issue Reference
Issue #28 - Phase 23: ServiceLocation

## XSD Schema Reference
**customer.xsd Inheritance Chain:**
- **ServiceLocation** (lines 1074-1116) extends **WorkLocation**
- **WorkLocation** (lines 1397-1402) extends **Location**
- **Location** (lines 914-997) extends **IdentifiedObject**

**Total Fields:** 14 fields from Location + 5 fields from ServiceLocation = 19 fields

## Current State Analysis

### ServiceLocationEntity.java ✅ (Mostly Complete)
**Status:** Entity structure is mostly correct
- ✅ Extends IdentifiedObject
- ✅ Has all Location fields (type, mainAddress, secondaryAddress, phoneNumbers, electronicAddress, geoInfoReference, direction, status)
- ✅ Has all ServiceLocation fields (accessMethod, siteAccessProblem, needsInspection, outageBlock)
- ⚠️ Missing `status.remark` field (Status should have 4 fields: value, dateTime, remark, reason)
- ❌ Missing `usagePointHref` String field for cross-stream UsagePoint reference
- ❌ Field order may not match XSD sequence
- ❌ Phone numbers use phone1/phone2 naming in XSD, but entity uses phoneNumbers collection

### ServiceLocationDto.java ❌ (Needs Major Refactoring)
**Status:** DTO has incorrect structure (still has Atom fields, missing Location fields)
- ❌ Has Atom protocol fields (published, updated, selfLink, upLink, relatedLinks) - should be REMOVED
- ❌ Has `positionAddress` String field - should be replaced with Location structure
- ❌ Has `customerAgreement` relationship field - should NOT be in DTO
- ❌ Missing Location fields: type, mainAddress, secondaryAddress, phone1, phone2, electronicAddress, status
- ✅ Has ServiceLocation fields: accessMethod, needsInspection, siteAccessProblem
- ❌ Missing outageBlock field
- ❌ Missing usagePointHref field
- ❌ Field order doesn't match XSD sequence

### ServiceLocationMapper.java ❌ (Does Not Exist)
**Status:** Mapper needs to be created from scratch
- ❌ No mapper file exists
- Needs bidirectional Entity ↔ DTO mappings
- Needs to handle Location embedded types (StreetAddress, ElectronicAddress, Status)
- Needs to handle phone number collection mapping

### ServiceLocationRepository.java ⚠️ (Needs Review)
**Status:** Repository has many non-index queries
- ✅ Extends JpaRepository<ServiceLocationEntity, UUID>
- ⚠️ Has 8 query methods (should review and keep only index-based queries)
- Current queries: findByOutageBlock, findLocationsThatNeedInspection, findLocationsWithAccessProblems, findByMainAddressStreetContaining, findByDirectionContaining, findByType, findByPhone1AreaCode, findByGeoInfoReference
- **Decision needed:** Which queries are truly index-based and required for tests?

### ServiceLocationService.java ⚠️ (Needs Review)
**Status:** Service interface exists, needs schema compliance review

### ServiceLocationServiceImpl.java ⚠️ (Needs Review)
**Status:** Service implementation exists, needs schema compliance review

### Tests
- ✅ ServiceLocationRepositoryTest.java exists
- ❌ ServiceLocationDtoTest.java does NOT exist (needs to be created)
- ❌ ServiceLocationMapperTest.java may be needed

### Flyway Migration
- ✅ V3__Create_additiional_Base_Tables.sql has service_locations table
- ⚠️ Needs review for column order and missing columns (status_remark, usage_point_href)

## XSD Field Mapping

### Location Fields (from IdentifiedObject + Location)
Per customer.xsd lines 914-997:

| XSD Field | Entity Field | DTO Field | Type | Notes |
|-----------|--------------|-----------|------|-------|
| mRID | id (UUID) | uuid | String | IdentifiedObject.mRID |
| description | description | description | String | IdentifiedObject |
| type | type | type | String256 | Location classification |
| mainAddress | mainAddress | mainAddress | StreetAddress | Embedded |
| secondaryAddress | secondaryAddress | secondaryAddress | StreetAddress | Embedded |
| phone1 | phoneNumbers[0] | phone1 | TelephoneNumber | Collection mapping |
| phone2 | phoneNumbers[1] | phone2 | TelephoneNumber | Collection mapping |
| electronicAddress | electronicAddress | electronicAddress | ElectronicAddress | Embedded (8 fields) |
| geoInfoReference | geoInfoReference | geoInfoReference | String256 | |
| direction | direction | direction | String256 | |
| status | status | status | Status | Embedded (4 fields: value, dateTime, remark, reason) |
| positionPoints | - | - | PositionPoint[] | NOT IMPLEMENTED (complex geospatial) |

### WorkLocation Fields
Per customer.xsd lines 1397-1402:
- WorkLocation adds NO fields (just extends Location)

### ServiceLocation Fields
Per customer.xsd lines 1074-1116:

| XSD Field | Entity Field | DTO Field | Type | Notes |
|-----------|--------------|-----------|------|-------|
| accessMethod | accessMethod | accessMethod | String256 | |
| siteAccessProblem | siteAccessProblem | siteAccessProblem | String256 | |
| needsInspection | needsInspection | needsInspection | Boolean | |
| UsagePoints | usagePointHref | usagePointHref | String | Cross-stream reference (NOT Atom link) |
| outageBlock | outageBlock | outageBlock | String32 | Extension field |

## Critical Phase 23 Requirements

### 1. Cross-Stream UsagePoint Reference
**Issue #28 Phase 23 Note:**
> "ServiceLocation exists in customer.xsd PII data stream, UsagePoint exists in espi.xsd non-PII data stream. ServiceLocation stores UsagePoint's rel="self" href URL directly, NOT via Atom link element."

**Implementation:**
- Add `usagePointHref` String field to ServiceLocationEntity
- Add `usagePointHref` String field to ServiceLocationDto
- Store href URL string like: `"https://api.example.com/espi/1_1/resource/UsagePoint/550e8400-e29b-41d4-a716-446655440000"`
- **DO NOT** use Atom LinkDto for this reference

### 2. Status 4-Field Compliance
Per Phase 24 findings, Status embedded type must have 4 fields:
- value (String)
- dateTime (Long - epoch seconds)
- remark (String) - **Currently missing in entity Status @AttributeOverride**
- reason (String)

### 3. ElectronicAddress 8-Field Compliance
Per Phase 24 findings, ElectronicAddress must have all 8 fields:
- lan, mac, email1, email2, web, radio, userID, password
- Entity already has correct @AttributeOverride annotations

### 4. Phone Number Mapping
XSD defines `phone1` and `phone2` as TelephoneNumber elements.
Entity uses `phoneNumbers` collection with PhoneNumberEntity.
Mapper needs to handle:
- Entity collection → DTO phone1/phone2 fields
- DTO phone1/phone2 → Entity collection

### 5. No Atom Fields in DTO
Per Phase 20 customer.xsd compliance pattern:
- Remove: published, updated, selfLink, upLink, relatedLinks
- Only include XSD-defined fields
- DTO should be pure customer.xsd representation

## Implementation Tasks

### Task 1: Update ServiceLocationEntity.java
**File:** `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/domain/customer/entity/ServiceLocationEntity.java`

**Changes:**
1. Add `status.remark` to @AttributeOverride annotations:
```java
@AttributeOverrides({
    @AttributeOverride(name = "value", column = @Column(name = "status_value")),
    @AttributeOverride(name = "dateTime", column = @Column(name = "status_date_time")),
    @AttributeOverride(name = "remark", column = @Column(name = "status_remark")),
    @AttributeOverride(name = "reason", column = @Column(name = "status_reason"))
})
private Status status;
```

2. Add `usagePointHref` String field after `needsInspection`:
```java
/**
 * Reference to UsagePoint resource href URL (cross-stream reference from customer.xsd to usage.xsd).
 * Stores the full href URL, NOT an Atom link element.
 * Example: "https://api.example.com/espi/1_1/resource/UsagePoint/550e8400-e29b-41d4-a716-446655440000"
 */
@Column(name = "usage_point_href", length = 512)
private String usagePointHref;
```

3. Verify field order matches XSD sequence:
   - Location fields: type, mainAddress, secondaryAddress, phoneNumbers, electronicAddress, geoInfoReference, direction, status
   - ServiceLocation fields: accessMethod, siteAccessProblem, needsInspection, usagePointHref, outageBlock

4. Update equals/hashCode if needed (should use UUID-based pattern from Phase 24)

### Task 2: Complete DTO Refactoring
**File:** `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/dto/customer/ServiceLocationDto.java`

**Changes:**
1. **REMOVE all Atom protocol fields:**
   - Remove: `published`, `updated`, `selfLink`, `upLink`, `relatedLinks`
   - Remove: `getSelfHref()`, `getUpHref()`, `generateSelfHref()`, `generateUpHref()` methods

2. **REMOVE relationship fields:**
   - Remove: `customerAgreement` field

3. **ADD Location fields:**
```java
@XmlElement(name = "type")
private String type;

@XmlElement(name = "mainAddress")
private CustomerDto.StreetAddressDto mainAddress;

@XmlElement(name = "secondaryAddress")
private CustomerDto.StreetAddressDto secondaryAddress;

@XmlElement(name = "phone1")
private CustomerDto.TelephoneNumberDto phone1;

@XmlElement(name = "phone2")
private CustomerDto.TelephoneNumberDto phone2;

@XmlElement(name = "electronicAddress")
private CustomerDto.ElectronicAddressDto electronicAddress;

@XmlElement(name = "geoInfoReference")
private String geoInfoReference;

@XmlElement(name = "direction")
private String direction;

@XmlElement(name = "status")
private StatusDto status;
```

4. **ADD ServiceLocation fields:**
```java
@XmlElement(name = "UsagePoints")
private String usagePointHref;

@XmlElement(name = "outageBlock")
private String outageBlock;
```

5. **REPLACE positionAddress with proper Location structure**

6. **Update @XmlType propOrder** to match XSD sequence:
```java
@XmlType(name = "ServiceLocation", namespace = "http://naesb.org/espi/customer", propOrder = {
    // Location fields
    "type", "mainAddress", "secondaryAddress", "phone1", "phone2",
    "electronicAddress", "geoInfoReference", "direction", "status",
    // ServiceLocation fields
    "accessMethod", "siteAccessProblem", "needsInspection", "usagePointHref", "outageBlock"
})
```

7. **Create StatusDto nested class:**
```java
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "LocationStatus", namespace = "http://naesb.org/espi/customer", propOrder = {
    "value", "dateTime", "remark", "reason"
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public static class StatusDto implements Serializable {
    @XmlElement(name = "value")
    private String value;

    @XmlElement(name = "dateTime")
    private Long dateTime;

    @XmlElement(name = "remark")
    private String remark;

    @XmlElement(name = "reason")
    private String reason;
}
```

### Task 3: Create ServiceLocationMapper
**File:** `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/mapper/customer/ServiceLocationMapper.java`

**Implementation:**
```java
@Mapper(componentModel = "spring", uses = {CustomerMapper.class})
public interface ServiceLocationMapper {

    // Entity to DTO
    @Mapping(target = "uuid", source = "id")
    @Mapping(target = "type", source = "type")
    @Mapping(target = "mainAddress", source = "mainAddress")
    @Mapping(target = "secondaryAddress", source = "secondaryAddress")
    @Mapping(target = "phone1", expression = "java(mapPhone1(entity.getPhoneNumbers()))")
    @Mapping(target = "phone2", expression = "java(mapPhone2(entity.getPhoneNumbers()))")
    @Mapping(target = "electronicAddress", source = "electronicAddress")
    @Mapping(target = "geoInfoReference", source = "geoInfoReference")
    @Mapping(target = "direction", source = "direction")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "accessMethod", source = "accessMethod")
    @Mapping(target = "siteAccessProblem", source = "siteAccessProblem")
    @Mapping(target = "needsInspection", source = "needsInspection")
    @Mapping(target = "usagePointHref", source = "usagePointHref")
    @Mapping(target = "outageBlock", source = "outageBlock")
    ServiceLocationDto toDto(ServiceLocationEntity entity);

    // DTO to Entity
    @Mapping(target = "id", source = "uuid")
    @Mapping(target = "phoneNumbers", expression = "java(mapPhoneNumbers(dto.getPhone1(), dto.getPhone2()))")
    // ... all other mappings
    ServiceLocationEntity toEntity(ServiceLocationDto dto);

    // Custom phone number mappings
    default CustomerDto.TelephoneNumberDto mapPhone1(List<PhoneNumberEntity> phoneNumbers) {
        if (phoneNumbers == null || phoneNumbers.isEmpty()) return null;
        return mapTelephoneNumber(phoneNumbers.get(0));
    }

    default CustomerDto.TelephoneNumberDto mapPhone2(List<PhoneNumberEntity> phoneNumbers) {
        if (phoneNumbers == null || phoneNumbers.size() < 2) return null;
        return mapTelephoneNumber(phoneNumbers.get(1));
    }

    default List<PhoneNumberEntity> mapPhoneNumbers(
        CustomerDto.TelephoneNumberDto phone1,
        CustomerDto.TelephoneNumberDto phone2) {
        List<PhoneNumberEntity> phoneNumbers = new ArrayList<>();
        if (phone1 != null) phoneNumbers.add(mapPhoneNumberEntity(phone1, "ServiceLocationEntity"));
        if (phone2 != null) phoneNumbers.add(mapPhoneNumberEntity(phone2, "ServiceLocationEntity"));
        return phoneNumbers;
    }

    // Delegate to CustomerMapper for address/phone/electronicAddress mappings
}
```

### Task 4: Review ServiceLocationRepository
**File:** `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/repositories/customer/ServiceLocationRepository.java`

**Changes:**
Per Issue #28 Phase 23: "Keep ONLY index field queries. Remove all non-index queries not required for tests."

**Review each query:**
1. `findByOutageBlock` - Keep if outageBlock is indexed
2. `findLocationsThatNeedInspection` - Remove (needsInspection is boolean, not indexed)
3. `findLocationsWithAccessProblems` - Remove (not indexed)
4. `findByMainAddressStreetContaining` - Remove (LIKE query, not indexed)
5. `findByDirectionContaining` - Remove (LIKE query, not indexed)
6. `findByType` - Keep if type is indexed
7. `findByPhone1AreaCode` - Remove (complex join, not indexed)
8. `findByGeoInfoReference` - Keep if geoInfoReference is indexed

**Final repository should have only:**
- Inherited JpaRepository methods (findById, findAll, save, delete, etc.)
- Index-based queries required for tests

### Task 5: Review ServiceLocationService
**Files:**
- `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/service/customer/ServiceLocationService.java`
- `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/service/customer/impl/ServiceLocationServiceImpl.java`

**Changes:**
- Review service methods for schema compliance
- Ensure service uses repository index-based queries only
- Add any missing CRUD methods if needed
- Update Javadocs to reference customer.xsd

### Task 6: Update Flyway Migration
**File:** `openespi-common/src/main/resources/db/migration/V3__Create_additiional_Base_Tables.sql`

**Changes:**
1. Add `status_remark VARCHAR(256)` column to service_locations table
2. Add `usage_point_href VARCHAR(512)` column to service_locations table
3. Verify column order matches XSD field sequence
4. Verify all ElectronicAddress columns exist (lan, mac, email1, email2, web, radio, userID, password)

### Task 7: Create ServiceLocationDtoTest
**File:** `openespi-common/src/test/java/org/greenbuttonalliance/espi/common/dto/customer/ServiceLocationDtoTest.java`

**Test Structure (follow CustomerAgreementDtoTest pattern):**
```java
@Nested
@DisplayName("XML Marshalling Tests")
class XmlMarshallingTests {

    @Test
    @DisplayName("Full ServiceLocation marshals to valid XML")
    void testFullServiceLocationMarshalling() {
        // Create ServiceLocationDto with all fields
        // Marshal to XML
        // Verify all fields present
        // Verify namespace is http://naesb.org/espi/customer
    }

    @Test
    @DisplayName("Field order matches customer.xsd sequence")
    void testFieldOrder() {
        // Verify XML output field order matches XSD
    }

    @Test
    @DisplayName("ServiceLocation XML has correct namespace")
    void testNamespace() {
        // Verify xmlns:cust="http://naesb.org/espi/customer"
        // Verify NO xmlns:espi
    }

    @Test
    @DisplayName("UsagePointHref is string, not Atom link")
    void testUsagePointHrefIsString() {
        // Verify usagePointHref field is simple string
        // Verify NO <link> element for UsagePoint
    }
}
```

### Task 8: Update ServiceLocationRepositoryTest
**File:** `openespi-common/src/test/java/org/greenbuttonalliance/espi/common/repositories/customer/ServiceLocationRepositoryTest.java`

**Changes:**
- Add tests for `usagePointHref` field
- Add tests for `status.remark` field
- Verify all Location fields persist correctly
- Follow CustomerAgreementRepositoryTest pattern

### Task 9: Run All Tests
Execute full test suite:
```bash
# Run openespi-common tests
cd openespi-common
mvn test

# Run integration tests
mvn verify -Pintegration-tests
```

Fix any test failures before committing.

## Success Criteria

### Entity
- ✅ All Location fields present and ordered per XSD
- ✅ All ServiceLocation fields present and ordered per XSD
- ✅ Status has 4 fields with @AttributeOverride (value, dateTime, remark, reason)
- ✅ ElectronicAddress has 8 fields with @AttributeOverride
- ✅ usagePointHref String field added
- ✅ Phone numbers handled correctly (collection mapping)

### DTO
- ✅ NO Atom protocol fields (published, updated, selfLink, upLink, relatedLinks)
- ✅ NO relationship fields (customerAgreement)
- ✅ All Location fields present and ordered per XSD
- ✅ All ServiceLocation fields present and ordered per XSD
- ✅ @XmlType propOrder matches XSD sequence
- ✅ StatusDto nested class with 4 fields
- ✅ usagePointHref is String field, NOT LinkDto

### Mapper
- ✅ Bidirectional Entity ↔ DTO mappings
- ✅ Handles embedded types (StreetAddress, ElectronicAddress, Status)
- ✅ Handles phone number collection ↔ phone1/phone2 mapping
- ✅ Handles usagePointHref string mapping

### Repository
- ✅ Only index-based queries remain
- ✅ Non-index queries removed

### Service
- ✅ Service methods comply with schema
- ✅ Javadocs reference customer.xsd

### Migration
- ✅ status_remark column added
- ✅ usage_point_href column added
- ✅ All columns present and ordered

### Tests
- ✅ ServiceLocationDtoTest created with 4+ tests
- ✅ ServiceLocationRepositoryTest updated
- ✅ All tests pass (634+ tests)
- ✅ Integration tests pass
- ✅ XML marshalling produces valid customer.xsd output
- ✅ UsagePointHref is string, NOT Atom link
- ✅ Namespace is http://naesb.org/espi/customer (NO espi namespace contamination)

## Dependencies

### Upstream Dependencies (Must Be Complete First)
- ✅ Phase 20: Customer (base infrastructure) - COMPLETE
- ✅ Phase 18: CustomerAccount - COMPLETE
- ✅ Phase 24: CustomerAgreement - COMPLETE
- ⚠️ TimeConfiguration (for Atom rel='related' links) - Verify if needed for Phase 23
- ⚠️ UsagePoint (for cross-stream reference) - Verify if needed for Phase 23

### Downstream Dependencies (Will Use Phase 23 Results)
- Phase 25: EndDevice (will reference ServiceLocation via Atom links)
- CustomerAgreement (already references ServiceLocation via Atom links)

## Risk Assessment

### High Risk
1. **Phone Number Mapping Complexity**
   - XSD has phone1/phone2 as simple elements
   - Entity has phoneNumbers collection with PhoneNumberEntity join table
   - Mapper must handle collection ↔ individual field mapping

2. **Cross-Stream UsagePoint Reference**
   - Must be string href URL, NOT Atom link
   - Must not create circular dependency between customer.xsd and usage.xsd

### Medium Risk
1. **Repository Query Review**
   - Deciding which queries are truly index-based
   - Ensuring test coverage remains adequate after query removal

2. **DTO Refactoring Scope**
   - Large number of field changes (remove 5 Atom fields, add 11 Location fields)
   - Risk of breaking existing code that uses ServiceLocationDto

### Low Risk
1. **Status 4-Field Compliance**
   - Standard pattern from Phase 24
   - Clear implementation path

2. **ElectronicAddress 8-Field Compliance**
   - Already implemented correctly in entity
   - Just needs DTO and mapper updates

## Timeline Estimate

| Task | Estimated Effort |
|------|-----------------|
| Entity updates (status remark, usagePointHref) | 30 minutes |
| DTO refactoring (remove Atom, add Location fields) | 2 hours |
| Mapper creation | 2 hours |
| Repository review | 1 hour |
| Service review | 30 minutes |
| Migration updates | 30 minutes |
| DtoTest creation | 2 hours |
| RepositoryTest updates | 1 hour |
| Test execution and fixes | 2 hours |
| **Total** | **~11.5 hours** |

## References

- Issue #28 - Phase 23: ServiceLocation
- ESPI 4.0 customer.xsd lines 1074-1116 (ServiceLocation)
- ESPI 4.0 customer.xsd lines 1397-1402 (WorkLocation)
- ESPI 4.0 customer.xsd lines 914-997 (Location)
- Phase 20: Customer (base pattern)
- Phase 18: CustomerAccount (Status 4-field pattern)
- Phase 24: CustomerAgreement (ElectronicAddress 8-field pattern, Status compliance, DTO test pattern)

---

**Document Version:** 1.0
**Created:** 2026-01-27
**Author:** Claude Code
**Status:** Draft - Awaiting User Approval
