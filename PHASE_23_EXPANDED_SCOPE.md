# Phase 23 Expanded Scope: Full Embedded Type Compliance

## Overview
Phase 23 expanded to include full customer.xsd compliance for ALL embedded types used by ServiceLocation and other customer entities.

## Compliance Gaps Identified

### 1. PhoneNumberEntity (TelephoneNumber) - Missing 4 Fields
**Current:** 4 fields (areaCode, cityCode, localNumber, extension)
**XSD Required:** 8 fields (customer.xsd lines 1428-1478)

**Missing Fields:**
- `countryCode` (String256)
- `dialOut` (String256)
- `internationalPrefix` (String256)
- `ituPhone` (String256)

### 2. Organisation.StreetAddress - Simplified Structure
**Current:** 5 simple string fields
**XSD Required:** Complex nested structure with 23+ fields

**Current Implementation:**
```java
public static class StreetAddress {
    private String streetDetail;      // Should be StreetDetail (12 fields)
    private String townDetail;        // Should be TownDetail (6 fields)
    private String stateOrProvince;   // Should be in TownDetail
    private String postalCode;        // Correct
    private String country;           // Should be in TownDetail
}
```

**XSD Structure (customer.xsd lines 1285-1320):**
```xml
<xs:complexType name="StreetAddress">
    <xs:element name="streetDetail" type="StreetDetail"/>  <!-- 12 fields -->
    <xs:element name="townDetail" type="TownDetail"/>      <!-- 6 fields -->
    <xs:element name="status" type="Status"/>              <!-- 4 fields -->
    <xs:element name="postalCode" type="String256"/>
    <xs:element name="poBox" type="String256"/>
</xs:complexType>
```

**Required Nested Types:**

#### StreetDetail (12 fields, customer.xsd lines 1321-1391):
- number
- name
- suffix
- prefix
- type
- code
- buildingName
- suiteNumber
- addressGeneral
- addressGeneral2
- addressGeneral3
- withinTownLimits

#### TownDetail (6 fields, customer.xsd lines 1478-1519):
- code
- section
- name
- county
- stateOrProvince
- country

### 3. Status - Missing remark Field
**Current:** 3 fields (value, dateTime, reason)
**XSD Required:** 4 fields (value, dateTime, remark, reason)

**Status:** Already being fixed in Phase 23 for ServiceLocationEntity

## Implementation Strategy

### Option A: Pragmatic Approach (Recommended)
Keep simplified StreetAddress structure for now, add TODO comments, plan separate phase for full compliance later.

**Rationale:**
- Full StreetAddress compliance affects 5+ entities (Customer, CustomerAccount, CustomerAgreement, ServiceLocation, ServiceSupplier)
- Would require significant migration script changes
- Phase 23 scope already large
- Can defer to dedicated StreetAddress/TownDetail/StreetDetail compliance phase

**Phase 23 Actions:**
1. ✅ Fix PhoneNumberEntity (add 4 missing fields)
2. ✅ Fix Status remark field
3. ✅ Add usagePointHrefs collection
4. ✅ Complete ServiceLocationDto refactoring
5. ⚠️ Keep simplified StreetAddress with TODO
6. ✅ Create ServiceLocationMapper
7. ✅ Update tests and migration

### Option B: Full Compliance Approach
Implement complete StreetAddress/StreetDetail/TownDetail structure now.

**Rationale:**
- Achieves full XSD compliance immediately
- Avoids technical debt
- Eliminates need for future refactoring phase

**Phase 23 Actions:**
1. Create StreetDetail embeddable (12 fields)
2. Create TownDetail embeddable (6 fields)
3. Update Organisation.StreetAddress to use nested embeddables
4. Fix PhoneNumberEntity (add 4 missing fields)
5. Fix Status remark field
6. Add usagePointHrefs collection
7. Update all entity @AttributeOverride annotations (5+ entities)
8. Complete ServiceLocationDto refactoring
9. Update all DTOs for StreetAddress changes
10. Create ServiceLocationMapper
11. Update migration scripts (significant changes)
12. Update all tests

## Decision: Option A (Pragmatic)

Proceeding with **Option A** to keep Phase 23 manageable while still making significant progress.

**Deferred to Future Phase:**
- Full StreetAddress/StreetDetail/TownDetail compliance
- Will create separate "Phase XX: StreetAddress Schema Compliance" issue

## Phase 23 Expanded Task List

### Completed
- ✅ T1: Create Feature Branch
- ✅ T2: Update ServiceLocationEntity with Status Remark Field
- ✅ T3: Add usagePointHrefs Collection to ServiceLocationEntity
- ✅ T4: Verify ServiceLocationEntity Field Order
- ✅ T5: Refactor ServiceLocationDto - Remove Atom Fields
- ✅ T6: Refactor ServiceLocationDto - Remove Relationship Fields

### In Progress
- 🔄 T7: Add PhoneNumberEntity Missing Fields (NEW)
- 🔄 T8: Update PhoneNumberEntity Migration (NEW)
- 🔄 T9: Add Location Fields to ServiceLocationDto
- 🔄 T10: Add ServiceLocation Fields to DTO
- 🔄 T11: Update ServiceLocationDto propOrder

### Pending
- ⏳ T12: Create ServiceLocationMapper Interface
- ⏳ T13: Review and Clean Up ServiceLocationRepository
- ⏳ T14: Update Flyway Migration Script
- ⏳ T15: Create ServiceLocationDtoTest
- ⏳ T16: Update ServiceLocationRepositoryTest
- ⏳ T17: Run All Tests and Fix Failures
- ⏳ T18: Run Integration Tests
- ⏳ T19: Commit and Push Changes
- ⏳ T20: Create Pull Request
- ⏳ T21: Update Issue #28

## Next Steps

1. Add 4 missing fields to PhoneNumberEntity
2. Update phone_numbers migration table
3. Update CustomerDto.TelephoneNumberDto to include all 8 fields
4. Continue with ServiceLocationDto refactoring
5. Complete Phase 23 with full PhoneNumber compliance
6. Add TODO comments for StreetAddress full compliance
7. Create follow-up issue for StreetAddress/StreetDetail/TownDetail compliance
