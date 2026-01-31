# Phase 21: ServiceSupplier & Organisation Phone Refactoring - ESPI 4.0 Schema Compliance

## Overview
Implement full ESPI 4.0 schema compliance for ServiceSupplier AND refactor all Organisation phone number handling from PhoneNumberEntity table to embedded TelephoneNumber fields. This comprehensive update affects ServiceSupplier, Customer, and all related infrastructure to prevent CI/CD issues when removing PhoneNumberEntity.

**Related Issue**: #28 - ESPI 4.0 Schema Compliance (Phase 21: ServiceSupplier)

**IMPORTANT**: Issue #28 tracks the multi-phase ESPI 4.0 schema compliance effort. This plan implements Phase 21 only. **DO NOT close Issue #28** when Phase 21 is complete - additional phases (Phase 22+) remain to be implemented.

**Scope Expansion Rationale**:
- SupplierKind enum does NOT match XSD (wrong values, wrong sequence - must be fixed)
- ServiceSupplierEntity uses PhoneNumberEntity (must be refactored)
- CustomerEntity also uses PhoneNumberEntity (must be refactored to prevent build failure)
- PhoneNumberService depends on PhoneNumberEntity (must be deleted)
- CustomerMapper has PhoneNumberEntity logic (must be updated)
- V3 Flyway migration creates phone_numbers table (must be removed)
- Completing all phone refactoring in one phase prevents partial implementation issues

**Critical Discovery**:
- Current SupplierKind enum has wrong values (RETAIL, GENERATION, TRANSMISSION, DISTRIBUTION)
- XSD defines: UTILITY, RETAILER, OTHER, LSE, MDMA, MSP (6 values, not 5)
- ESPI standard uses **ordinal values** (0-5), so sequence is critical
- Enum must be fixed to match XSD exactly for ESPI Sandbox/CMD Certification compatibility

## XSD Structure

**ServiceSupplier extends IdentifiedObject** (customer.xsd lines 1159-1186):

### ServiceSupplier Fields (4 fields)
1. **organisation** (Organisation) - Embedded organisation details
2. **kind** (SupplierKind enum) - Type of supplier
   - Per XSD (lines 2231-2271): UTILITY(0), RETAILER(1), OTHER(2), LSE(3), MDMA(4), MSP(5)
   - ESPI serializes using ordinal values (0-5), not string values
   - Sequence is critical for correct serialization
3. **issuerIdentificationNumber** (String256) - Unique supplier identifier
4. **effectiveDate** (TimeType -> Long) - Date supplier became effective

### Organisation Structure (customer.xsd lines 1089-1125)
Organisation extends IdentifiedObject in XSD but is @Embeddable in implementation:
- **organisationName** (String256)
- **streetAddress** (StreetAddress)
- **postalAddress** (StreetAddress)
- **phone1** (TelephoneNumber) - All 8 TelephoneNumber fields
- **phone2** (TelephoneNumber) - All 8 TelephoneNumber fields
- **electronicAddress** (ElectronicAddress)

### TelephoneNumber Structure (customer.xsd lines 1428-1478)
TelephoneNumber extends Object (NOT IdentifiedObject) - 8 fields:
- countryCode, areaCode, cityCode, localNumber, ext, dialOut, internationalPrefix, ituPhone

### Architecture Decision
- **OrganisationRole**: Unused wrapper - will be removed
- **PhoneNumberEntity**: Polymorphic table pattern - will be removed
- **Embedded TelephoneNumber**: XSD-compliant, type-safe, performant - will be implemented

## Tasks

### Phase A: Verification and Cleanup

#### Task A1: Verify and Fix SupplierKind Enum
**File**: `src/main/java/org/greenbuttonalliance/espi/common/domain/customer/enums/SupplierKind.java`

**Current Issues Found**:
1. ❌ **Wrong values**: RETAIL, GENERATION, TRANSMISSION, DISTRIBUTION are incorrect
2. ❌ **Missing values**: LSE, MDMA, MSP from XSD
3. ❌ **Wrong sequence**: OTHER is in position 6 instead of position 3

**XSD Definition** (customer.xsd lines 2231-2271):
```xml
<xs:enumeration value="utility"/>       <!-- 0: Entity that delivers service to customer -->
<xs:enumeration value="retailer"/>     <!-- 1: Entity that sells but doesn't deliver; deregulated markets -->
<xs:enumeration value="other"/>        <!-- 2: Other kind of supplier -->
<xs:enumeration value="lse"/>          <!-- 3: [extension] Load Serving Entity -->
<xs:enumeration value="mdma"/>         <!-- 4: [extension] Meter Data Management Agent -->
<xs:enumeration value="msp"/>          <!-- 5: [extension] Meter Service Provider -->
```

**ESPI Serialization Note**:
- ESPI standard (Sandbox and CMD Certification Platform) emits **ordinal values** (0, 1, 2, 3, 4, 5)
- NOT string values ("utility", "retailer", etc.)
- Sequence is CRITICAL as ordinal position determines serialized value

**Required Changes**:

Replace entire enum with XSD-compliant values in correct sequence:

```java
package org.greenbuttonalliance.espi.common.domain.customer.enums;

/**
 * Kind of supplier based on ESPI 4.0 customer.xsd specification.
 *
 * Per customer.xsd lines 2231-2271.
 * CRITICAL: Sequence must match XSD exactly as ESPI uses ordinal values (0-5) for serialization.
 *
 * Ordinal mapping:
 * 0 = UTILITY
 * 1 = RETAILER
 * 2 = OTHER
 * 3 = LSE
 * 4 = MDMA
 * 5 = MSP
 */
public enum SupplierKind {
    /**
     * Entity that delivers the service to the customer.
     * Ordinal: 0
     */
    UTILITY,

    /**
     * Entity that sells the service, but does not deliver to the customer.
     * Applies to the deregulated markets.
     * Ordinal: 1
     */
    RETAILER,

    /**
     * Other kind of supplier.
     * Ordinal: 2
     */
    OTHER,

    /**
     * [extension] Load Serving Entity
     * Ordinal: 3
     */
    LSE,

    /**
     * [extension] Meter Data Management Agent
     * Ordinal: 4
     */
    MDMA,

    /**
     * [extension] Meter Service Provider
     * Ordinal: 5
     */
    MSP
}
```

**Verification Checklist**:
- ✅ Location: `customer/enums` directory (already correct)
- ✅ Sequence: Matches XSD exactly (UTILITY=0, RETAILER=1, OTHER=2, LSE=3, MDMA=4, MSP=5)
- ✅ Values: Six enum constants matching XSD
- ✅ Serialization: Uses ordinal values per ESPI standard
- ✅ No custom string values or @JsonValue annotations

**Impact**:
- Existing ServiceSupplierEntity uses this enum (must verify after fix)
- XML/JSON serialization will use ordinals 0-5
- Any existing data with old enum values will need migration (but this is dev system)

#### Task A2: Delete OrganisationRole Class
**File**: `src/main/java/org/greenbuttonalliance/espi/common/domain/customer/entity/OrganisationRole.java`

**Action**: Delete this file completely.

**Rationale**: Unused wrapper with no additional elements beyond Organisation.

#### Task A3: Delete PhoneNumberEntity Class
**File**: `src/main/java/org/greenbuttonalliance/espi/common/domain/customer/entity/PhoneNumberEntity.java`

**Action**: Delete this file completely.

**Rationale**: Being replaced with embedded TelephoneNumber in Organisation. Polymorphic table pattern no longer needed.

#### Task A4: Delete PhoneNumberService
**File**: `src/main/java/org/greenbuttonalliance/espi/common/service/PhoneNumberService.java`

**Action**: Delete this file completely.

**Rationale**: Service for managing PhoneNumberEntity relationships no longer needed with embedded approach.

### Phase B: Organisation and Entity Updates

#### Task B1: Update Organisation to Include phone1/phone2
**File**: `src/main/java/org/greenbuttonalliance/espi/common/domain/customer/entity/Organisation.java`

**Changes Required**:
1. Add phone1 and phone2 embedded fields using existing TelephoneNumber class
2. Remove comment about phone numbers being managed separately (lines 62-63)
3. Update JavaDoc to reflect XSD-compliant structure

**New Fields to Add**:
```java
/**
 * Primary phone number for this organisation.
 */
@Embedded
private TelephoneNumber phone1;

/**
 * Secondary phone number for this organisation.
 */
@Embedded
private TelephoneNumber phone2;
```

**Note**: Column name overrides applied at entity level (ServiceSupplierEntity, CustomerEntity), not in Organisation.

#### Task B2: Update ServiceSupplierEntity for XSD Compliance
**File**: `src/main/java/org/greenbuttonalliance/espi/common/domain/customer/entity/ServiceSupplierEntity.java`

**Changes Required**:

1. **Remove PhoneNumberEntity relationship**:
   - Delete `phoneNumbers` field (lines 94-101)
   - Remove `@OneToMany`, `@JoinColumn`, `@SQLRestriction` annotations
   - Remove import: `org.hibernate.annotations.SQLRestriction`

2. **Update @AttributeOverrides to JDK 25 pattern** (convert from wrapper):
   - Convert from `@AttributeOverrides({...})` to individual `@AttributeOverride`

3. **Add phone field overrides** (16 new overrides for 2 phones × 8 fields each):
```java
// Phone1 overrides
@AttributeOverride(name = "phone1.countryCode", column = @Column(name = "supplier_phone1_country_code"))
@AttributeOverride(name = "phone1.areaCode", column = @Column(name = "supplier_phone1_area_code"))
@AttributeOverride(name = "phone1.cityCode", column = @Column(name = "supplier_phone1_city_code"))
@AttributeOverride(name = "phone1.localNumber", column = @Column(name = "supplier_phone1_local_number"))
@AttributeOverride(name = "phone1.ext", column = @Column(name = "supplier_phone1_ext"))
@AttributeOverride(name = "phone1.dialOut", column = @Column(name = "supplier_phone1_dial_out"))
@AttributeOverride(name = "phone1.internationalPrefix", column = @Column(name = "supplier_phone1_international_prefix"))
@AttributeOverride(name = "phone1.ituPhone", column = @Column(name = "supplier_phone1_itu_phone"))

// Phone2 overrides
@AttributeOverride(name = "phone2.countryCode", column = @Column(name = "supplier_phone2_country_code"))
@AttributeOverride(name = "phone2.areaCode", column = @Column(name = "supplier_phone2_area_code"))
@AttributeOverride(name = "phone2.cityCode", column = @Column(name = "supplier_phone2_city_code"))
@AttributeOverride(name = "phone2.localNumber", column = @Column(name = "supplier_phone2_local_number"))
@AttributeOverride(name = "phone2.ext", column = @Column(name = "supplier_phone2_ext"))
@AttributeOverride(name = "phone2.dialOut", column = @Column(name = "supplier_phone2_dial_out"))
@AttributeOverride(name = "phone2.internationalPrefix", column = @Column(name = "supplier_phone2_international_prefix"))
@AttributeOverride(name = "phone2.ituPhone", column = @Column(name = "supplier_phone2_itu_phone"))
```

4. **Update toString() method** to remove phoneNumbers reference

5. **Add comprehensive JavaDoc**

**Total @AttributeOverride count**: ~34 overrides

#### Task B3: Update CustomerEntity for XSD Compliance
**File**: `src/main/java/org/greenbuttonalliance/espi/common/domain/customer/entity/CustomerEntity.java`

**Changes Required**:

1. **Remove PhoneNumberEntity relationship**:
   - Delete `phoneNumbers` field (lines 162-169)
   - Remove `@OneToMany`, `@JoinColumn`, `@SQLRestriction` annotations

2. **Update @AttributeOverrides to JDK 25 pattern** (convert from wrapper):
   - Convert from `@AttributeOverrides({...})` to individual `@AttributeOverride`

3. **Add phone field overrides** (16 new overrides for 2 phones × 8 fields each):
```java
// Phone1 overrides
@AttributeOverride(name = "phone1.countryCode", column = @Column(name = "customer_phone1_country_code"))
@AttributeOverride(name = "phone1.areaCode", column = @Column(name = "customer_phone1_area_code"))
@AttributeOverride(name = "phone1.cityCode", column = @Column(name = "customer_phone1_city_code"))
@AttributeOverride(name = "phone1.localNumber", column = @Column(name = "customer_phone1_local_number"))
@AttributeOverride(name = "phone1.ext", column = @Column(name = "customer_phone1_ext"))
@AttributeOverride(name = "phone1.dialOut", column = @Column(name = "customer_phone1_dial_out"))
@AttributeOverride(name = "phone1.internationalPrefix", column = @Column(name = "customer_phone1_international_prefix"))
@AttributeOverride(name = "phone1.ituPhone", column = @Column(name = "customer_phone1_itu_phone"))

// Phone2 overrides (similar pattern with customer_phone2_* prefix)
```

4. **Update toString() method** to remove phoneNumbers reference

**Total @AttributeOverride count**: ~35 overrides

### Phase C: DTO Implementation

#### Task C1: Create ServiceSupplierDto
**File**: `src/main/java/org/greenbuttonalliance/espi/common/dto/customer/ServiceSupplierDto.java`

**Requirements**:
- Use JAXB annotations (NOT Jackson)
- Include ONLY 4 ServiceSupplier-specific fields
- NO IdentifiedObject fields (handled by AtomEntryDto)
- Namespace: `http://naesb.org/espi/customer`

**Structure**: [Same as original plan - full DTO structure with JAXB annotations]

#### Task C2: Create/Update OrganisationDto
**File**: `src/main/java/org/greenbuttonalliance/espi/common/dto/customer/OrganisationDto.java`

**Requirements**:
- Include all 6 fields: organisationName, streetAddress, postalAddress, phone1, phone2, electronicAddress
- Use existing TelephoneNumberDto for phone1/phone2

**Structure**: [Same as original plan]

#### Task C3: Create TelephoneNumberDto
**File**: `src/main/java/org/greenbuttonalliance/espi/common/dto/customer/TelephoneNumberDto.java`

**Requirements**:
- Use JAXB annotations
- Include all 8 fields from XSD

**Structure**: [Same as original plan]

#### Task C4: Verify Supporting DTOs Exist
- StreetAddressDto
- ElectronicAddressDto

### Phase D: Mapper Updates

#### Task D1: Implement ServiceSupplierMapper
**File**: `src/main/java/org/greenbuttonalliance/espi/common/mapper/customer/ServiceSupplierMapper.java`

**Changes**:
- Map ONLY 4 ServiceSupplier-specific fields
- Direct field-to-field mapping: `organisation.phone1 → dto.phone1`
- UUID v5 generation from issuerIdentificationNumber
- OffsetDateTime ↔ Long conversion for effectiveDate

**Structure**: [Same as original plan]

#### Task D2: Create OrganisationMapper
**File**: `src/main/java/org/greenbuttonalliance/espi/common/mapper/customer/OrganisationMapper.java`

**Structure**: [Same as original plan]

#### Task D3: Update CustomerMapper
**File**: `src/main/java/org/greenbuttonalliance/espi/common/mapper/customer/CustomerMapper.java`

**Changes Required**:

1. **Remove PhoneNumberEntity imports and logic**:
   - Remove import: `org.greenbuttonalliance.espi.common.domain.customer.entity.PhoneNumberEntity`
   - Remove `@Mapping(target = "phoneNumbers", ignore = true)` (line 80)
   - Delete `extractPhoneByType()` method (lines 206-220)
   - Update `mapOrganisationToDto()` to use direct phone1/phone2 instead of extracting from list

2. **Update mapOrganisationToDto()** method:
```java
@Named("mapOrganisationToDto")
default CustomerDto.OrganisationDto mapOrganisationToDto(CustomerEntity entity) {
    if (entity == null || entity.getOrganisation() == null) {
        return null;
    }

    Organisation org = entity.getOrganisation();

    // Direct mapping from embedded TelephoneNumber
    CustomerDto.TelephoneNumberDto phone1 = telephoneNumberToDto(org.getPhone1());
    CustomerDto.TelephoneNumberDto phone2 = telephoneNumberToDto(org.getPhone2());

    return new CustomerDto.OrganisationDto(
        mapStreetAddressToDto(org.getStreetAddress()),
        mapStreetAddressToDto(org.getPostalAddress()),
        phone1,
        phone2,
        mapElectronicAddressToDto(org.getElectronicAddress()),
        org.getOrganisationName()
    );
}

// Add TelephoneNumber mapping method
default CustomerDto.TelephoneNumberDto telephoneNumberToDto(TelephoneNumber tel) {
    if (tel == null) return null;
    return new CustomerDto.TelephoneNumberDto(
        tel.getCountryCode(),
        tel.getAreaCode(),
        tel.getCityCode(),
        tel.getLocalNumber(),
        tel.getExt(),
        tel.getDialOut(),
        tel.getInternationalPrefix(),
        tel.getItuPhone()
    );
}
```

3. **Remove comment about PhoneNumberEntity** (lines 95-96, 125, 205)

### Phase E: Database Migration

#### Task E1: Update V3 Flyway Migration Script
**File**: `src/main/resources/db/migration/V3__Create_additiional_Base_Tables.sql`

**Purpose**: Update CREATE TABLE statements to include embedded phone columns; remove phone_numbers table

**Changes Required**:

1. **Update service_suppliers CREATE TABLE** - Add 16 phone columns to existing CREATE TABLE statement:
```sql
CREATE TABLE service_suppliers (
    -- ... existing columns ...

    -- Embedded TelephoneNumber fields for Organisation.phone1
    supplier_phone1_country_code VARCHAR(256),
    supplier_phone1_area_code VARCHAR(256),
    supplier_phone1_city_code VARCHAR(256),
    supplier_phone1_local_number VARCHAR(256),
    supplier_phone1_ext VARCHAR(256),
    supplier_phone1_dial_out VARCHAR(256),
    supplier_phone1_international_prefix VARCHAR(256),
    supplier_phone1_itu_phone VARCHAR(256),

    -- Embedded TelephoneNumber fields for Organisation.phone2
    supplier_phone2_country_code VARCHAR(256),
    supplier_phone2_area_code VARCHAR(256),
    supplier_phone2_city_code VARCHAR(256),
    supplier_phone2_local_number VARCHAR(256),
    supplier_phone2_ext VARCHAR(256),
    supplier_phone2_dial_out VARCHAR(256),
    supplier_phone2_international_prefix VARCHAR(256),
    supplier_phone2_itu_phone VARCHAR(256),

    -- ... rest of columns ...
);
```

2. **Update customers CREATE TABLE** - Add 16 phone columns to existing CREATE TABLE statement:
```sql
CREATE TABLE customers (
    -- ... existing columns ...

    -- Embedded TelephoneNumber fields for Organisation.phone1
    customer_phone1_country_code VARCHAR(256),
    customer_phone1_area_code VARCHAR(256),
    customer_phone1_city_code VARCHAR(256),
    customer_phone1_local_number VARCHAR(256),
    customer_phone1_ext VARCHAR(256),
    customer_phone1_dial_out VARCHAR(256),
    customer_phone1_international_prefix VARCHAR(256),
    customer_phone1_itu_phone VARCHAR(256),

    -- Embedded TelephoneNumber fields for Organisation.phone2
    customer_phone2_country_code VARCHAR(256),
    customer_phone2_area_code VARCHAR(256),
    customer_phone2_city_code VARCHAR(256),
    customer_phone2_local_number VARCHAR(256),
    customer_phone2_ext VARCHAR(256),
    customer_phone2_dial_out VARCHAR(256),
    customer_phone2_international_prefix VARCHAR(256),
    customer_phone2_itu_phone VARCHAR(256),

    -- ... rest of columns ...
);
```

3. **Remove phone_numbers table** (if CREATE TABLE phone_numbers exists in V3):
   - Delete the entire CREATE TABLE phone_numbers statement
   - Phone numbers are now embedded, not in separate table

### Phase F: Test Data and Test Updates

#### Task F1: Update TestDataBuilders
**File**: `src/test/java/org/greenbuttonalliance/espi/common/test/TestDataBuilders.java`

**Changes**:

1. **Update createValidServiceSupplier()** - Replace phoneNumbers collection with embedded phone1/phone2:
```java
public static ServiceSupplierEntity createValidServiceSupplier() {
    // ... existing code ...

    // Phone1 (primary)
    TelephoneNumber phone1 = new TelephoneNumber();
    phone1.setCountryCode("+1");
    phone1.setAreaCode(faker.number().digits(3));
    phone1.setLocalNumber(faker.number().digits(7));
    organisation.setPhone1(phone1);

    // Phone2 (secondary)
    TelephoneNumber phone2 = new TelephoneNumber();
    phone2.setCountryCode("+1");
    phone2.setAreaCode(faker.number().digits(3));
    phone2.setLocalNumber(faker.number().digits(7));
    phone2.setExt(faker.number().digits(4));
    organisation.setPhone2(phone2);

    // Remove: supplier.setPhoneNumbers(...)

    return supplier;
}
```

2. **Update createValidCustomer()** - Replace phoneNumbers collection with embedded phone1/phone2 (same pattern)

#### Task F2: Update ServiceLocationRepositoryTest
**File**: `src/test/java/org/greenbuttonalliance/espi/common/repositories/customer/ServiceLocationRepositoryTest.java`

**Changes**:
- Remove `createValidPhoneNumber()` helper method
- Update any tests that reference PhoneNumberEntity

#### Task F3: Create ServiceSupplierRepositoryTest
[Same as original plan - 24 tests]

#### Task F4: Create ServiceSupplier Integration Tests
[Same as original plan - MySQL and PostgreSQL - 10 tests each]

#### Task F5: Create ServiceSupplierMapperTest
[Same as original plan - 10 tests]

#### Task F6: Create OrganisationMapperTest
[Same as original plan - 8 tests]

#### Task F7: Create ServiceSupplierDtoTest
[Same as original plan - 10 tests]

#### Task F8: Create TelephoneNumberDtoTest
[Same as original plan - 4 tests]

### Phase G: Service Integration

#### Task G1: Update DtoExportService
**File**: `src/main/java/org/greenbuttonalliance/espi/common/service/impl/DtoExportServiceImpl.java`

**Changes**:
- Add ServiceSupplierMapper injection
- Implement ServiceSupplier export method
- Ensure AtomEntryDto wrapping
- Add to batch export operations

## Testing Strategy

### Test Breakdown
- **Unit Tests (ServiceSupplier Repository)**: 24 tests
- **Integration Tests (ServiceSupplier MySQL)**: 10 tests
- **Integration Tests (ServiceSupplier PostgreSQL)**: 10 tests
- **Mapper Tests (ServiceSupplierMapper)**: 10 tests
- **Mapper Tests (OrganisationMapper)**: 8 tests
- **DTO Tests (ServiceSupplierDto)**: 10 tests
- **DTO Tests (TelephoneNumberDto)**: 4 tests

**Total New Tests**: ~76 tests

### Current Test Baseline
Based on previous phases: ~736 tests

### Expected Test Count After Phase 21
**Target**: ~812 tests (736 + 76)

### Regression Testing
- All existing Customer tests must pass with embedded phone changes
- CustomerMapper tests must pass with updated phone logic
- Integration tests verify phone columns work on MySQL and PostgreSQL

## Execution Checklist

### Pre-Implementation Review
- [ ] Review XSD structures (ServiceSupplier, Organisation, TelephoneNumber, SupplierKind)
- [ ] Verify SupplierKind enum matches XSD (6 values in correct sequence)
- [ ] Identify all PhoneNumberEntity usages
- [ ] Confirm TelephoneNumber @Embeddable exists in customer/common
- [ ] Review V3 Flyway migration script structure

### Phase A: Verification and Cleanup
- [ ] Verify and fix SupplierKind enum (6 values, correct sequence for ordinals)
- [ ] Delete OrganisationRole.java
- [ ] Delete PhoneNumberEntity.java
- [ ] Delete PhoneNumberService.java

### Phase B: Entity Updates
- [ ] Update Organisation with phone1/phone2 fields
- [ ] Update ServiceSupplierEntity (remove phoneNumbers, add 16 phone @AttributeOverride, JDK 25 pattern)
- [ ] Update CustomerEntity (remove phoneNumbers, add 16 phone @AttributeOverride, JDK 25 pattern)

### Phase C: DTO Implementation
- [ ] Create ServiceSupplierDto (JAXB, 4 fields)
- [ ] Create/update OrganisationDto (6 fields with phone1/phone2)
- [ ] Create TelephoneNumberDto (8 fields)
- [ ] Verify StreetAddressDto and ElectronicAddressDto exist

### Phase D: Mapper Updates
- [ ] Implement ServiceSupplierMapper
- [ ] Create OrganisationMapper
- [ ] Update CustomerMapper (remove PhoneNumberEntity logic)

### Phase E: Database Migration
- [ ] Update V3 service_suppliers CREATE TABLE (add 16 phone columns)
- [ ] Update V3 customers CREATE TABLE (add 16 phone columns)
- [ ] Remove phone_numbers CREATE TABLE from V3

### Phase F: Test Updates
- [ ] Update TestDataBuilders (ServiceSupplier and Customer methods)
- [ ] Update ServiceLocationRepositoryTest (remove PhoneNumberEntity helper)
- [ ] Create ServiceSupplierRepositoryTest (24 tests)
- [ ] Create ServiceSupplierMySQLIntegrationTest (10 tests)
- [ ] Create ServiceSupplierPostgreSQLIntegrationTest (10 tests)
- [ ] Create ServiceSupplierMapperTest (10 tests)
- [ ] Create OrganisationMapperTest (8 tests)
- [ ] Create ServiceSupplierDtoTest (10 tests)
- [ ] Create TelephoneNumberDtoTest (4 tests)

### Phase G: Service Integration
- [ ] Update DtoExportService with ServiceSupplier support

### Final Verification
- [ ] Run full test suite: `mvn clean test`
- [ ] Run integration tests: `mvn verify -Pintegration-tests`
- [ ] Verify test count: ~812 tests
- [ ] All tests passing on MySQL and PostgreSQL
- [ ] No PhoneNumberEntity references remain in codebase
- [ ] XML marshalling validates against customer.xsd

### Documentation and Issue Tracking
- [ ] Update Issue #28 with Phase 21 completion status
- [ ] **DO NOT close Issue #28** - more phases remain (Phase 22+)
- [ ] Document SupplierKind enum fix in issue comment
- [ ] Document phone number architecture change (PhoneNumberEntity → embedded TelephoneNumber)
- [ ] Update CLAUDE.md if needed (phone number patterns, SupplierKind enum)

## Success Criteria

1. ✅ SupplierKind enum verified/fixed (6 values: UTILITY, RETAILER, OTHER, LSE, MDMA, MSP in correct sequence)
2. ✅ OrganisationRole removed
3. ✅ PhoneNumberEntity removed
4. ✅ PhoneNumberService removed
5. ✅ Organisation updated with phone1/phone2
6. ✅ ServiceSupplierEntity refactored (embedded phones, JDK 25 pattern)
7. ✅ CustomerEntity refactored (embedded phones, JDK 25 pattern)
8. ✅ ServiceSupplierDto created (JAXB, 4 fields)
9. ✅ OrganisationDto includes phone1/phone2
10. ✅ TelephoneNumberDto created (8 fields)
11. ✅ ServiceSupplierMapper implemented
12. ✅ OrganisationMapper implemented
13. ✅ CustomerMapper updated (no PhoneNumberEntity)
14. ✅ V3 Flyway script updated (no phone_numbers table, embedded columns in service_suppliers and customers)
15. ✅ All 76 new tests passing
16. ✅ Total test count: ~812 tests
17. ✅ No test regressions
18. ✅ XML validates against customer.xsd
19. ✅ Build succeeds with no PhoneNumberEntity references
20. ✅ SupplierKind enum ordinals match XSD sequence (ESPI Sandbox/CMD compatible)
21. ✅ Issue #28 updated with Phase 21 status (NOT closed - more phases remain)

## Benefits

### XSD Compliance
- ✅ Organisation matches XSD with phone1/phone2
- ✅ TelephoneNumber structure matches XSD (8 fields)
- ✅ Direct entity → DTO mapping

### Type Safety
- ✅ No string discriminators
- ✅ Compile-time checked relationships
- ✅ No polymorphic table risks

### Performance
- ✅ Single table (no JOINs)
- ✅ All data in one row
- ✅ Simpler queries

### Architecture
- ✅ Standard JPA embedded pattern
- ✅ Eliminates unnecessary abstractions (OrganisationRole, PhoneNumberEntity, PhoneNumberService)
- ✅ Cleaner codebase

## References

- **XSD**: `openespi-common/src/main/resources/schema/ESPI_4.0/customer.xsd`
  - ServiceSupplier: lines 1159-1186
  - Organisation: lines 1089-1125
  - TelephoneNumber: lines 1428-1478
- **Entity**: `ServiceSupplierEntity.java`, `CustomerEntity.java`
- **Embeddable**: `Organisation.java`, `TelephoneNumber.java`
- **Pattern Reference**: `CustomerDto.java`, `MeterDto.java`
- **Issue**: #28 (Phase 21: ServiceSupplier)
