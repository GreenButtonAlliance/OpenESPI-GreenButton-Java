# Issue #101 Implementation Plan
## Verify Entity Field Types and Lengths Match ESPI 4.0 XSD Schemas

**Issue**: [#101 - Verify Entity Field Types and Lengths Match ESPI 4.0 XSD Schemas](https://github.com/GreenButtonAlliance/OpenESPI-GreenButton-Java/issues/101)

**Objective**: Systematically verify that all JPA entity fields (types, lengths, nullability, relationships) match the ESPI 4.0 XSD schema definitions in `espi.xsd` and `customer.xsd`.

---

## Overview

This plan provides repeatable verification tasks for each entity that extends `IdentifiedObject` and all supporting classes that inherit from `Object`. Each task follows a consistent pattern:

1. **Extract XSD Definition** - Locate the element/type definition in the XSD schema
2. **Compare Field Types** - Verify Java field types match XSD types
3. **Verify String Lengths** - Check `@Column(length=...)` matches XSD `maxLength`
4. **Check Nullability** - Ensure `nullable` matches XSD `minOccurs`
5. **Validate Relationships** - Verify collections and embedded objects match XSD structure
6. **Review Flyway Scripts** - Ensure database schema matches entity definitions
7. **Document Discrepancies** - Record any mismatches with justification or corrections
8. **Test Validation** - Run unit tests to verify no regressions

---

## Understanding ESPI XSD BasicTypes

The ESPI 4.0 XSD schemas (`espi.xsd` and `customer.xsd`) use **BasicTypes** (simpleType definitions) to define field types and lengths. Instead of inline `maxLength` facets on elements, the schemas define reusable types like `String256`, `String512`, `UInt48`, etc.

### XSD BasicTypes Pattern

**Example from espi.xsd:**
```xml
<!-- Element definition uses a BasicType -->
<xs:element name="client_secret" type="String512">

<!-- BasicType definition with restriction -->
<xs:simpleType name="String512">
    <xs:annotation>
        <xs:documentation>[extension] Character string of max length 512</xs:documentation>
    </xs:annotation>
    <xs:restriction base="xs:string">
        <xs:maxLength value="512"/>
    </xs:restriction>
</xs:simpleType>
```

### String BasicTypes Reference

| XSD Type | maxLength | Java Type | @Column Mapping | Usage |
|----------|-----------|-----------|-----------------|-------|
| `String8` | 8 | `String` | `@Column(length=8)` | Short codes, flags |
| `String16` | 16 | `String` | `@Column(length=16)` | Short identifiers |
| `String32` | 32 | `String` | `@Column(length=32)` | Standard codes |
| `String64` | 64 | `String` | `@Column(length=64)` | Medium text fields |
| `String256` | 256 | `String` | `@Column(length=256)` | Standard text fields |
| `String512` | 512 | `String` | `@Column(length=512)` | Long text fields |

### Numeric BasicTypes Reference

| XSD Type | Base Type | Range | Java Type | JPA Mapping |
|----------|-----------|-------|-----------|-------------|
| `UInt8` | xs:unsignedByte | 0 to 255 | `Short` or `Integer` | Standard mapping |
| `UInt16` | xs:unsignedShort | 0 to 65,535 | `Integer` | Standard mapping |
| `UInt32` | xs:unsignedInt | 0 to 4,294,967,295 | `Long` | Standard mapping |
| `UInt48` | xs:unsignedLong | 0 to 281,474,976,710,655 | `Long` | Standard mapping |
| `Int16` | xs:short | -32,768 to 32,767 | `Short` or `Integer` | Standard mapping |
| `Int48` | xs:long | 48-bit signed | `Long` | Standard mapping |

### Binary BasicTypes Reference

| XSD Type | Length (bytes) | Java Type | JPA Mapping |
|----------|----------------|-----------|-------------|
| `HexBinary8` | 8 | `byte[]` | `@Column(length=8)` or store as hex String(16) |
| `HexBinary16` | 16 | `byte[]` | `@Column(length=16)` or store as hex String(32) |
| `HexBinary32` | 32 | `byte[]` | `@Column(length=32)` or store as hex String(64) |
| `HexBinary128` | 128 | `byte[]` | `@Column(length=128)` or store as hex String(256) |

### Special BasicTypes

| XSD Type | Description | Java Type | Notes |
|----------|-------------|-----------|-------|
| `UUIDType` | UUID (Type 5) | `UUID` or `String` | Standard UUID format |
| `TimeType` | Unix timestamp (seconds since epoch) | `Long` | ESPI uses seconds, not millis |
| `Currency` | ISO 4217 currency code | `String` | 3-character code (USD, EUR, etc.) |
| `PerCent` | Percentage value | `BigDecimal` | May need precision constraints |
| `StatusCode` | Status code enumeration | `String` or Enum | Context-dependent |

### Enumeration BasicTypes

Both schemas define numerous enumeration types (e.g., `ServiceKind`, `CustomerKind`, `AmiBillingReadyKind`). These should map to Java enums:

```java
// XSD enumeration
<xs:simpleType name="ServiceKind">
    <xs:restriction base="xs:unsignedByte">
        <xs:enumeration value="0"/> <!-- electricity -->
        <xs:enumeration value="1"/> <!-- gas -->
        <!-- etc -->
    </xs:restriction>
</xs:simpleType>

// Java enum mapping
@Enumerated(EnumType.ORDINAL)
@Column(name = "service_kind")
private ServiceKind serviceKind;
```

### How to Trace BasicTypes in Verification

**Step 1: Find the element definition**
```bash
grep -A 3 "element name=\"client_secret\"" openespi-common/src/main/resources/schema/ESPI_4.0/espi.xsd
```
Output:
```xml
<xs:element name="client_secret" type="String512">
```

**Step 2: Find the BasicType definition**
```bash
grep -B 2 -A 5 "simpleType name=\"String512\"" openespi-common/src/main/resources/schema/ESPI_4.0/espi.xsd
```
Output:
```xml
<xs:simpleType name="String512">
    <xs:restriction base="xs:string">
        <xs:maxLength value="512"/>
    </xs:restriction>
</xs:simpleType>
```

**Step 3: Verify Java entity mapping**
```java
@Column(name = "client_secret", length = 512)
private String clientSecret;
```

### Quick Reference Scripts

Create these helper scripts for BasicType lookup:

```bash
# scripts/find-basictype.sh
#!/bin/bash
TYPE_NAME=$1
SCHEMA_FILE=${2:-espi.xsd}

echo "=== BasicType Definition: $TYPE_NAME ==="
grep -B 2 -A 10 "simpleType name=\"$TYPE_NAME\"" \
    openespi-common/src/main/resources/schema/ESPI_4.0/$SCHEMA_FILE
```

```bash
# scripts/find-element-type.sh
#!/bin/bash
ELEMENT_NAME=$1
SCHEMA_FILE=${2:-espi.xsd}

echo "=== Element Type Reference: $ELEMENT_NAME ==="
grep -B 1 -A 3 "element name=\"$ELEMENT_NAME\"" \
    openespi-common/src/main/resources/schema/ESPI_4.0/$SCHEMA_FILE | \
    grep "type="
```

### Complete BasicType Inventory

**espi.xsd simpleTypes (51 total):**
- String types: String8, String16, String32, String64, String256, String512
- Numeric types: Int16, Int48, UInt8, UInt16, UInt32, UInt48
- Binary types: HexBinary8, HexBinary16, HexBinary32, HexBinary128
- Enumerations: AccumulationKind, AmiBillingReadyKind, CommodityKind, DataQualifierKind, FlowDirectionKind, MeasurementKind, PhaseCodeKind, QualityOfReading, ServiceKind, TimeAttributeKind, UnitMultiplierKind, UnitSymbolKind, UsagePointConnectedKind
- OAuth types: GrantType, ResponseType, TokenType, OAuthError
- Other: UUIDType, TimeType, Currency

**customer.xsd simpleTypes (33 total):**
- String types: String32, String64, String256, String512
- Numeric types: Int16, Int48, UInt8, UInt16, UInt32, UInt48, PerCent
- Binary types: HexBinary8, HexBinary16, HexBinary32, HexBinary128
- Enumerations: CustomerKind, MeterMultiplierKind, NotificationMethodKind, SupplierKind, ProgramDateKind, RevenueKind, MediaType
- Shared with espi.xsd: ServiceKind, Currency, UUIDType, TimeType, StatusCode

### Verification Checklist for BasicTypes

When verifying each entity field:

- [ ] **Identify the XSD element type** (e.g., `type="String256"`)
- [ ] **Locate the BasicType definition** (e.g., `<xs:simpleType name="String256">`)
- [ ] **Extract the restriction value** (e.g., `<xs:maxLength value="256"/>`)
- [ ] **Verify Java field type matches** (e.g., `String` for String types)
- [ ] **Verify @Column length matches** (e.g., `@Column(length=256)`)
- [ ] **Check for enum types** (should map to Java enum, not String)
- [ ] **Validate numeric ranges** (ensure Java type can hold XSD range)

---

## Phase 0: Enumeration Migration Verification

**CRITICAL**: Before verifying entity fields, ensure all XSD enumerations have been migrated from legacy code and are in the correct directory structure.

### Required Directory Structure

- **Usage Domain Enums**: `domain/usage/enums/` (currently missing - enums are in `domain/common/`)
- **Customer Domain Enums**: `domain/customer/enums/` ✓ (exists)
- **Common/Shared Enums**: `domain/common/` (for enums used by both domains)

### Enumeration Inventory from XSD Schemas

#### espi.xsd Enumerations (33 total)

| Enum Name | Currently Implemented? | Location | Status | Action Required |
|-----------|----------------------|----------|--------|-----------------|
| AccumulationKind | ❌ No | - | Missing | Implement in usage/enums |
| AmiBillingReadyKind | ✓ Yes | common/ | ⚠️ Wrong location | Move to usage/enums |
| AnodeType | ❌ No | - | Missing | Implement in usage/enums |
| ApnodeType | ❌ No | - | Missing | Implement in usage/enums |
| AuthorizationStatus | ❌ No | - | Missing | Implement in usage/enums |
| CommodityKind | ❌ No | - | Missing | Implement in usage/enums |
| CRUDOperation | ❌ No | - | Missing | Implement in common/ (shared) |
| Currency | ❌ No | - | Missing | Implement in common/ (shared) |
| DataCustodianApplicationStatus | ❌ No | - | Missing | Implement in usage/enums |
| DataQualifierKind | ❌ No | - | Missing | Implement in usage/enums |
| DstRuleType | ❌ No | - | Missing | Implement in common/ (shared) |
| ESPIServiceStatus | ❌ No | - | Missing | Implement in usage/enums |
| EnrollmentStatus | ❌ No | - | Missing | Implement in common/ (shared) |
| FlowDirectionKind | ❌ No | - | Missing | Implement in usage/enums |
| GrantType | ✓ Yes | common/ | ⚠️ Wrong location | Move to usage/enums |
| ItemKind | ❌ No | - | Missing | Implement in usage/enums |
| MeasurementKind | ❌ No | - | Missing | Implement in usage/enums |
| OAuthError | ✓ Yes | common/ | ⚠️ Wrong location | Move to usage/enums |
| ParticipationCategoryMPM | ❌ No | - | Missing | Implement in usage/enums |
| PhaseCodeKind | ✓ Yes | common/ | ⚠️ Wrong location | Move to usage/enums |
| QualityOfReading | ❌ No | - | Missing | Implement in usage/enums |
| ResponseType | ✓ Yes | common/ | ⚠️ Wrong location | Move to usage/enums |
| ServiceKind | ⚠️ Named ServiceCategory | common/ | ⚠️ Wrong name & location | Rename to ServiceKind, move to usage/enums |
| StatusCode | ❌ No | - | Missing | Implement in common/ (shared) |
| ThirdPartyApplicationType | ❌ No | - | Missing | Implement in usage/enums |
| ThirdPartyApplicationUse | ❌ No | - | Missing | Implement in usage/enums |
| ThirdPartyApplicatonStatus | ❌ No | - | Missing | Implement in usage/enums |
| TimeAttributeKind | ❌ No | - | Missing | Implement in usage/enums |
| TokenEndPointMethod | ❌ No | - | Missing | Implement in usage/enums |
| TokenType | ✓ Yes | common/ | ⚠️ Wrong location | Move to usage/enums |
| UnitMultiplierKind | ❌ No | - | Missing | Implement in common/ (shared) |
| UnitSymbolKind | ❌ No | - | Missing | Implement in common/ (shared) |
| UsagePointConnectedKind | ✓ Yes | common/ | ⚠️ Wrong location | Move to usage/enums |
| tOUorCPPorConsumptionTier | ❌ No | - | Missing | Implement in usage/enums |

**Summary**: 8 implemented, 25 missing, 8 in wrong location

#### customer.xsd Enumerations (16 total)

| Enum Name | Currently Implemented? | Location | Status | Action Required |
|-----------|----------------------|----------|--------|-----------------|
| CRUDOperation | ❌ No | - | Missing | Implement in common/ (shared with espi.xsd) |
| Currency | ❌ No | - | Missing | Implement in common/ (shared with espi.xsd) |
| CustomerKind | ✓ Yes | customer/enums/ | ✓ Correct | None |
| DstRuleType | ❌ No | - | Missing | Implement in common/ (shared with espi.xsd) |
| EnrollmentStatus | ❌ No | - | Missing | Implement in common/ (shared with espi.xsd) |
| MediaType | ❌ No | - | Missing | Implement in customer/enums |
| MeterMultiplierKind | ✓ Yes | customer/enums/ | ✓ Correct | None |
| NotificationMethodKind | ✓ Yes | customer/enums/ | ✓ Correct | None |
| ProgramDateKind | ✓ Yes | customer/enums/ | ✓ Correct | None |
| RevenueKind | ❌ No | - | Missing | Implement in customer/enums |
| ServiceKind | ⚠️ Named ServiceCategory | common/ | ⚠️ Wrong location | See espi.xsd (shared enum) |
| StatusCode | ❌ No | - | Missing | Implement in common/ (shared with espi.xsd) |
| SupplierKind | ✓ Yes | customer/enums/ | ✓ Correct | None |
| UnitMultiplierKind | ❌ No | - | Missing | Implement in common/ (shared with espi.xsd) |
| UnitSymbolKind | ❌ No | - | Missing | Implement in common/ (shared with espi.xsd) |

**Summary**: 5 implemented correctly, 10 missing, 1 misnamed/mislocated (ServiceKind)

### Phase 0 Tasks: Enumeration Migration

| Task ID | Enum Name | XSD Source | Target Location | Priority | Dependencies |
|---------|-----------|------------|-----------------|----------|--------------|
| 0.1 | Create usage/enums directory | - | domain/usage/enums/ | High | None |
| 0.2 | AccumulationKind | espi.xsd | usage/enums/ | High | ReadingType |
| 0.3 | CommodityKind | espi.xsd | usage/enums/ | High | ReadingType |
| 0.4 | DataQualifierKind | espi.xsd | usage/enums/ | High | ReadingType |
| 0.5 | FlowDirectionKind | espi.xsd | usage/enums/ | High | ReadingType |
| 0.6 | MeasurementKind | espi.xsd | usage/enums/ | High | ReadingType |
| 0.7 | TimeAttributeKind | espi.xsd | usage/enums/ | High | ReadingType |
| 0.8 | QualityOfReading | espi.xsd | usage/enums/ | High | ReadingQuality |
| 0.9 | ServiceKind (rename from ServiceCategory) | espi.xsd, customer.xsd | common/ (shared) | High | UsagePoint, multiple |
| 0.10 | UnitMultiplierKind | espi.xsd, customer.xsd | common/ (shared) | High | ReadingType |
| 0.11 | UnitSymbolKind | espi.xsd, customer.xsd | common/ (shared) | High | ReadingType |
| 0.12 | Currency | espi.xsd, customer.xsd | common/ (shared) | Medium | LineItem |
| 0.13 | StatusCode | espi.xsd, customer.xsd | common/ (shared) | Medium | Multiple |
| 0.14 | CRUDOperation | espi.xsd, customer.xsd | common/ (shared) | Low | Subscription |
| 0.15 | DstRuleType | espi.xsd, customer.xsd | common/ (shared) | Medium | TimeConfiguration |
| 0.16 | EnrollmentStatus | espi.xsd, customer.xsd | common/ (shared) | Medium | Subscription |
| 0.17 | AuthorizationStatus | espi.xsd | usage/enums/ | High | Authorization |
| 0.18 | DataCustodianApplicationStatus | espi.xsd | usage/enums/ | High | ApplicationInformation |
| 0.19 | ESPIServiceStatus | espi.xsd | usage/enums/ | Medium | Subscription |
| 0.20 | ItemKind | espi.xsd | usage/enums/ | Medium | LineItem |
| 0.21 | ThirdPartyApplicationType | espi.xsd | usage/enums/ | Medium | ApplicationInformation |
| 0.22 | ThirdPartyApplicationUse | espi.xsd | usage/enums/ | Medium | ApplicationInformation |
| 0.23 | ThirdPartyApplicatonStatus | espi.xsd | usage/enums/ | Medium | ApplicationInformation |
| 0.24 | TokenEndPointMethod | espi.xsd | usage/enums/ | Low | ApplicationInformation |
| 0.25 | AnodeType | espi.xsd | usage/enums/ | Low | AggregatedNode |
| 0.26 | ApnodeType | espi.xsd | usage/enums/ | Low | Pnode |
| 0.27 | ParticipationCategoryMPM | espi.xsd | usage/enums/ | Low | Subscription |
| 0.28 | tOUorCPPorConsumptionTier | espi.xsd | usage/enums/ | Low | ReadingType |
| 0.29 | MediaType | customer.xsd | customer/enums/ | Medium | Document |
| 0.30 | RevenueKind | customer.xsd | customer/enums/ | Medium | CustomerAccount |
| 0.31 | Move AmiBillingReadyKind | - | usage/enums/ | Medium | UsagePoint |
| 0.32 | Move GrantType | - | usage/enums/ | Medium | ApplicationInformation |
| 0.33 | Move OAuthError | - | usage/enums/ | Medium | Authorization |
| 0.34 | Move PhaseCodeKind | - | usage/enums/ | Medium | ReadingType |
| 0.35 | Move ResponseType | - | usage/enums/ | Medium | ApplicationInformation |
| 0.36 | Move TokenType | - | usage/enums/ | Medium | ApplicationInformation |
| 0.37 | Move UsagePointConnectedKind | - | usage/enums/ | Medium | UsagePoint |

**Phase 0 Total Tasks**: 37 (1 directory creation + 30 new enums + 6 relocations)

### Enumeration Verification Template

For each enumeration, verify:

1. **Extract XSD Definition**
```bash
grep -B 5 -A 30 "simpleType name=\"EnumName\"" openespi-common/src/main/resources/schema/ESPI_4.0/{espi|customer}.xsd
```

2. **Check Enumeration Values**
   - All XSD enumeration values present
   - Correct numeric or string values
   - Correct value mapping (ordinal vs value)

3. **Verify Java Implementation**
   - Enum constants match XSD enumeration values
   - Naming convention: UPPER_SNAKE_CASE for Java constants
   - Include `fromValue()` method for reverse lookup
   - Include `getValue()` method if needed

4. **Example: ServiceKind Enumeration**

**XSD Definition (espi.xsd):**
```xml
<xs:simpleType name="ServiceKind">
    <xs:restriction base="UInt16">
        <xs:enumeration value="0">  <!-- electricity -->
        <xs:enumeration value="1">  <!-- gas -->
        <xs:enumeration value="2">  <!-- water -->
        <xs:enumeration value="3">  <!-- time -->
        <xs:enumeration value="4">  <!-- heat -->
        <xs:enumeration value="5">  <!-- refuse -->
        <xs:enumeration value="6">  <!-- sewerage -->
        <xs:enumeration value="7">  <!-- rates -->
        <xs:enumeration value="8">  <!-- tvLicense -->
        <xs:enumeration value="9">  <!-- internet -->
    </xs:restriction>
</xs:simpleType>
```

**Current Implementation (ServiceCategory.java - INCORRECT NAME):**
```java
// ISSUE: Should be named ServiceKind, not ServiceCategory
// ServiceCategory is a complexType in XSD, not the enum
public enum ServiceCategory {
    ELECTRICITY(0L),
    GAS(1L),
    WATER(2L),
    TIME(3L),
    HEAT(4L),
    REFUSE(5L),
    SEWERAGE(6L),
    RATES(7L),
    TV_LICENSE(8L),
    INTERNET(9L);
    // ... fromValue() and getValue() methods
}
```

**Corrected Implementation (ServiceKind.java):**
```java
package org.greenbuttonalliance.espi.common.domain.common;

/**
 * ServiceKind enumeration from ESPI 4.0 specification.
 * Defines the kind of service represented by a UsagePoint.
 * Defined in both espi.xsd and customer.xsd as a shared type.
 */
public enum ServiceKind {
    ELECTRICITY(0),
    GAS(1),
    WATER(2),
    TIME(3),
    HEAT(4),
    REFUSE(5),
    SEWERAGE(6),
    RATES(7),
    TV_LICENSE(8),
    INTERNET(9);

    private final int value;

    ServiceKind(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static ServiceKind fromValue(int value) {
        for (ServiceKind kind : ServiceKind.values()) {
            if (kind.value == value) {
                return kind;
            }
        }
        throw new IllegalArgumentException("Invalid ServiceKind value: " + value);
    }
}
```

### Acceptance Criteria for Phase 0

- [ ] `domain/usage/enums/` directory created
- [ ] All 30 missing enumerations implemented
- [ ] All 7 mislocated enums moved to correct directories
- [ ] ServiceCategory renamed to ServiceKind
- [ ] All enum values match XSD definitions
- [ ] All enums include `fromValue()` method
- [ ] All enums properly annotated with Javadoc referencing XSD
- [ ] Update all entity references to use correct enum names/locations
- [ ] Update imports in all affected entities
- [ ] All tests updated and passing
- [ ] Flyway migration scripts updated if needed (enum storage strategy)

---

## Phase 1: Foundation - Supporting Classes (Object Hierarchy)

These classes form the foundation used by all entities. Verify these first to ensure consistent base types after Phase 0 enumeration migration is complete.

### Common Domain Supporting Classes

| Task ID | Class | Type | Location | XSD Source |
|---------|-------|------|----------|------------|
| 1.1 | `RationalNumber` | @Embeddable | domain/common | espi.xsd |
| 1.2 | `DateTimeInterval` | @Embeddable | domain/common | espi.xsd |
| 1.3 | `SummaryMeasurement` | @Embeddable | domain/common | espi.xsd |
| 1.4 | `LinkType` | @Embeddable | domain/common | espi.xsd |
| 1.5 | `ReadingInterharmonic` | @Embeddable | domain/common | espi.xsd |
| 1.6 | `BillingChargeSource` | @Embeddable | domain/common | espi.xsd |
| 1.7 | `ServiceKind` | Enum | domain/common (shared) | espi.xsd, customer.xsd |
| 1.8 | `UnitMultiplierKind` | Enum | domain/common (shared) | espi.xsd, customer.xsd |
| 1.9 | `UnitSymbolKind` | Enum | domain/common (shared) | espi.xsd, customer.xsd |
| 1.10 | `Currency` | Enum | domain/common (shared) | espi.xsd, customer.xsd |
| 1.11 | `StatusCode` | Enum | domain/common (shared) | espi.xsd, customer.xsd |
| 1.12 | `CRUDOperation` | Enum | domain/common (shared) | espi.xsd, customer.xsd |
| 1.13 | `DstRuleType` | Enum | domain/common (shared) | espi.xsd, customer.xsd |
| 1.14 | `EnrollmentStatus` | Enum | domain/common (shared) | espi.xsd, customer.xsd |

**Note**: Usage domain enums (PhaseCodeKind, AmiBillingReadyKind, etc.) are verified in Phase 0 and relocated to `usage/enums/` directory.

### Customer Domain Supporting Classes

| Task ID | Class | Type | Location | XSD Source |
|---------|-------|------|----------|------------|
| 1.15 | `TelephoneNumber` | @Embeddable | customer/common | customer.xsd |
| 1.16 | `ElectronicAddress` | @Embeddable | customer/common | customer.xsd |
| 1.17 | `StreetAddress` | @Embeddable | customer/common | customer.xsd |
| 1.18 | `MeterMultiplier` | @Embeddable | customer/common | customer.xsd |
| 1.19 | `ProgramDateIdMapping` | @Embeddable | customer/common | customer.xsd |
| 1.20 | `CustomerKind` | Enum | customer/enums | customer.xsd |
| 1.21 | `MeterMultiplierKind` | Enum | customer/enums | customer.xsd |
| 1.22 | `NotificationMethodKind` | Enum | customer/enums | customer.xsd |
| 1.23 | `SupplierKind` | Enum | customer/enums | customer.xsd |
| 1.24 | `ProgramDateKind` | Enum | customer/enums | customer.xsd |
| 1.25 | `MediaType` | Enum | customer/enums | customer.xsd |
| 1.26 | `RevenueKind` | Enum | customer/enums | customer.xsd |

### Customer Entity Base Classes

| Task ID | Class | Type | Location | XSD Source |
|---------|-------|------|----------|------------|
| 1.27 | `Asset` | Base Class | customer/entity | customer.xsd |
| 1.28 | `AssetContainer` | Base Class | customer/entity | customer.xsd |
| 1.29 | `Agreement` | Base Class | customer/entity | customer.xsd |
| 1.30 | `Organisation` | Base Class | customer/entity | customer.xsd |
| 1.31 | `Location` | Base Class | customer/entity | customer.xsd |
| 1.32 | `Document` | Base Class | customer/entity | customer.xsd |
| 1.33 | `Status` | @Embeddable | customer/entity | customer.xsd |
| 1.34 | `WorkLocation` | @Embeddable | customer/entity | customer.xsd |
| 1.35 | `AccountNotification` | @Embeddable | customer/entity | customer.xsd |
| 1.36 | `EndDeviceFields` | @Embeddable | customer/entity | customer.xsd |

**Phase 1 Total Tasks**: 36 (after Phase 0 enumeration migration)

---

## Phase 2: Usage Domain Entities (espi.xsd)

These entities represent energy usage data from the `espi.xsd` schema. All extend `IdentifiedObject`.

### Core Usage Resources

| Task ID | Entity | XSD Element | Priority | Dependencies |
|---------|--------|-------------|----------|--------------|
| 2.1 | `UsagePointEntity` | UsagePoint | High | ServiceCategory, DateTimeInterval |
| 2.2 | `MeterReadingEntity` | MeterReading | High | DateTimeInterval |
| 2.3 | `IntervalBlockEntity` | IntervalBlock | High | DateTimeInterval, IntervalReadingEntity |
| 2.4 | `ReadingTypeEntity` | ReadingType | High | RationalNumber, DateTimeInterval |
| 2.5 | `IntervalReadingEntity` | IntervalReading | High | DateTimeInterval, ReadingQualityEntity |
| 2.6 | `ReadingQualityEntity` | ReadingQuality | Medium | - |

### Summary and Quality Resources

| Task ID | Entity | XSD Element | Priority | Dependencies |
|---------|--------|-------------|----------|--------------|
| 2.7 | `ElectricPowerQualitySummaryEntity` | ElectricPowerQualitySummary | Medium | SummaryMeasurement, DateTimeInterval |
| 2.8 | `UsageSummaryEntity` | UsageSummary | Medium | SummaryMeasurement, DateTimeInterval |

### Configuration Resources

| Task ID | Entity | XSD Element | Priority | Dependencies |
|---------|--------|-------------|----------|--------------|
| 2.9 | `TimeConfigurationEntity` | LocalTimeParameters | High | DateTimeInterval |

### Customer and Application Resources

| Task ID | Entity | XSD Element | Priority | Dependencies |
|---------|--------|-------------|----------|--------------|
| 2.10 | `RetailCustomerEntity` | RetailCustomer | High | - |
| 2.11 | `ApplicationInformationEntity` | ApplicationInformation | High | - |
| 2.12 | `AuthorizationEntity` | Authorization | High | - |
| 2.13 | `SubscriptionEntity` | Subscription | Medium | - |

### Batch and Line Item Resources

| Task ID | Entity | XSD Element | Priority | Dependencies |
|---------|--------|-------------|----------|--------------|
| 2.14 | `BatchListEntity` | BatchList | Low | - |
| 2.15 | `LineItemEntity` | LineItem | Medium | DateTimeInterval |

### Reference Resources

| Task ID | Entity | XSD Element | Priority | Dependencies |
|---------|--------|-------------|----------|--------------|
| 2.16 | `AggregatedNodeRefEntity` | AggregatedNodeRef | Low | - |
| 2.17 | `PnodeRefEntity` | PnodeRef | Low | - |
| 2.18 | `ServiceDeliveryPointEntity` | ServiceDeliveryPoint | Low | - |
| 2.19 | `TariffRiderRefEntity` | TariffRiderRef | Low | - |

**Phase 2 Total Tasks**: 19

---

## Phase 3: Customer Domain Entities (customer.xsd)

These entities represent customer and service provider data from the `customer.xsd` schema. All extend `IdentifiedObject`.

### Customer Account and Agreement

| Task ID | Entity | XSD Element | Priority | Dependencies |
|---------|--------|-------------|----------|--------------|
| 3.1 | `CustomerAccountEntity` | CustomerAccount | High | TelephoneNumber, ElectronicAddress, StreetAddress |
| 3.2 | `CustomerEntity` | Customer | High | CustomerKind, TelephoneNumber, ElectronicAddress |
| 3.3 | `CustomerAgreementEntity` | CustomerAgreement | High | Agreement |

### Service Supplier and Location

| Task ID | Entity | XSD Element | Priority | Dependencies |
|---------|--------|-------------|----------|--------------|
| 3.4 | `ServiceSupplierEntity` | ServiceSupplier | High | SupplierKind, Organisation |
| 3.5 | `ServiceLocationEntity` | ServiceLocation | High | Location, StreetAddress |

### Meter and Device

| Task ID | Entity | XSD Element | Priority | Dependencies |
|---------|--------|-------------|----------|--------------|
| 3.6 | `MeterEntity` | Meter | High | MeterMultiplier, EndDeviceFields |
| 3.7 | `EndDeviceEntity` | EndDevice | High | EndDeviceFields, Asset |

### Statement and Program Mappings

| Task ID | Entity | XSD Element | Priority | Dependencies |
|---------|--------|-------------|----------|--------------|
| 3.8 | `StatementEntity` | Statement | Medium | Document, Status |
| 3.9 | `StatementRefEntity` | StatementRef | Low | - |
| 3.10 | `ProgramDateIdMappingsEntity` | ProgramDateIdMappings | Medium | ProgramDateIdMapping |

**Phase 3 Total Tasks**: 10

---

## Verification Task Template

For each entity, follow this repeatable checklist:

### Step 1: Locate XSD Definition
```bash
# Search for the element definition in XSD
grep -A 50 "name=\"EntityName\"" openespi-common/src/main/resources/schema/ESPI_4.0/espi.xsd
# OR
grep -A 50 "name=\"EntityName\"" openespi-common/src/main/resources/schema/ESPI_4.0/customer.xsd
```

### Step 2: Open Entity Source
```bash
# View the entity implementation
cat openespi-common/src/main/java/org/greenbuttonalliance/espi/common/domain/{usage|customer}/EntityName.java
```

### Step 3: Field-by-Field Verification

Create a comparison table for each entity using the BasicTypes reference:

| Field Name | XSD Element Type | BasicType Restriction | minOccurs | Java Type | @Column(length) | @Column(nullable) | Status | Notes |
|------------|------------------|----------------------|-----------|-----------|-----------------|-------------------|--------|-------|
| clientSecret | String512 | maxLength=512 | 1 | String | 512 | false | ✓ | Matches |
| clientName | String256 | maxLength=256 | 1 | String | 256 | false | ✓ | Matches |
| scope | String256 | maxLength=256 | 0 | String | 256 | true | ✓ | Optional field |
| amiBillingReady | AmiBillingReadyKind | enum (0-7) | 0 | AmiBillingReadyKind | n/a | true | ✓ | Enum type |
| duration | UInt32 | 0 to 4,294,967,295 | 0 | Long | n/a | true | ✓ | Numeric type |
| published | TimeType | Unix timestamp | 0 | Long | n/a | true | ✓ | ESPI timestamp |

**Example Verification Process:**

1. **XSD Element**: `<xs:element name="clientSecret" type="String512">`
2. **BasicType Lookup**: `String512` → `<xs:maxLength value="512"/>`
3. **Java Field**: `@Column(name="client_secret", length=512) private String clientSecret;`
4. **Result**: ✓ Type matches (String), length matches (512), nullability correct

### Step 4: Check for Common Issues

- [ ] **String field length**: All `String` fields have explicit `@Column(length=...)` matching XSD `maxLength`
- [ ] **Numeric precision**: `Long`, `Integer`, `BigInteger`, `BigDecimal` correctly map XSD numeric types
- [ ] **Temporal types**: Date/time fields use `Long` (UNIX timestamps) or `Instant` appropriately
- [ ] **Enum mappings**: All enum fields reference correct enum types from domain model
- [ ] **Embedded collections**: `@ElementCollection` properly configured with `@CollectionTable`
- [ ] **Relationships**: `@OneToMany`, `@ManyToOne`, `@ManyToMany` match XSD sequences
- [ ] **Nullable constraints**: `nullable=false` used when `minOccurs="1"` in XSD

### Step 5: Flyway Migration Verification
```bash
# Check database column definitions match entity
grep -A 10 "CREATE TABLE entity_name" openespi-common/src/main/resources/db/migration/*.sql
```

Verify:
- Column types match JPA field types
- VARCHAR lengths match `@Column(length=...)`
- NOT NULL constraints match `nullable=false`
- Foreign key relationships exist for `@JoinColumn` fields

### Step 6: Run Tests
```bash
# Test the specific entity
mvn test -Dtest=EntityNameTest

# Test repository
mvn test -Dtest=EntityNameRepositoryTest

# Test full module
cd openespi-common && mvn test
```

### Step 7: Document Results

Create a finding document for each entity:

```markdown
## EntityName Verification Report

**XSD Reference**: espi.xsd line XXX (or customer.xsd line YYY)
**Entity Location**: openespi-common/src/main/java/.../EntityName.java
**Verification Date**: YYYY-MM-DD

### Findings

#### ✓ Correct Mappings
- Field `abc`: Type matches, length correct
- Field `xyz`: Properly nullable

#### ⚠️ Discrepancies Found
1. **Field `clientName`**
   - XSD Element Type: `String256`
   - BasicType Restriction: `maxLength="256"`
   - Entity: `@Column(length=512)`
   - **Action**: Update to length=256

2. **Field `clientSecret`**
   - XSD: `type="String512"` with `minOccurs="1"`
   - BasicType Restriction: `maxLength="512"`
   - Entity: `@Column(length=512, nullable=true)`
   - **Action**: Change to nullable=false (minOccurs=1 requires NOT NULL)

3. **Field `amiBillingReady`**
   - XSD Element Type: `AmiBillingReadyKind` (enumeration)
   - Entity: `@Column(name="ami_billing_ready") private String amiBillingReady;`
   - **Action**: Change to enum type `private AmiBillingReadyKind amiBillingReady;`

#### 📝 Justifications
- Field `customExtension`: Not in XSD but needed for internal processing
- Field `legacyId`: Required for migration compatibility

### Flyway Migration Required
- [ ] Update column length for `description`
- [ ] Add NOT NULL constraint for `status`

### Test Results
- Unit tests: ✓ PASS (15/15)
- Integration tests: ✓ PASS (8/8)
- Repository tests: ✓ PASS (12/12)
```

---

## Implementation Strategy

### Recommended Order of Execution

1. **Phase 1 (Foundation)**: Complete all 34 supporting class verifications first
   - Start with embeddables (Tasks 1.1-1.6, 1.15-1.19)
   - Then enums (Tasks 1.7-1.14, 1.20-1.24)
   - Finally base classes (Tasks 1.25-1.34)

2. **Phase 2 (Usage Domain)**: Process by dependency order
   - **Week 1**: Core resources (Tasks 2.1-2.6) - 6 entities
   - **Week 2**: Summaries and config (Tasks 2.7-2.9) - 3 entities
   - **Week 3**: Customer/app resources (Tasks 2.10-2.13) - 4 entities
   - **Week 4**: Batch and refs (Tasks 2.14-2.19) - 6 entities

3. **Phase 3 (Customer Domain)**: Process by logical grouping
   - **Week 5**: Account/Agreement (Tasks 3.1-3.3) - 3 entities
   - **Week 6**: Supplier/Location (Tasks 3.4-3.5) - 2 entities
   - **Week 7**: Meter/Device (Tasks 3.6-3.7) - 2 entities
   - **Week 8**: Statement/Mappings (Tasks 3.8-3.10) - 3 entities

### Parallel Execution

Tasks within the same phase (after Phase 1) can be executed in parallel by different team members:

- **Developer A**: Usage core resources (2.1-2.6)
- **Developer B**: Usage summaries and config (2.7-2.9)
- **Developer C**: Customer account entities (3.1-3.3)

### Tools and Scripts

Create helper scripts to automate verification:

```bash
# scripts/verify-entity.sh
#!/bin/bash
ENTITY_NAME=$1
XSD_FILE=$2

echo "=== Verifying $ENTITY_NAME against $XSD_FILE ==="
echo ""
echo "XSD Definition:"
grep -A 50 "name=\"$ENTITY_NAME\"" openespi-common/src/main/resources/schema/ESPI_4.0/$XSD_FILE
echo ""
echo "Entity Source:"
find openespi-common/src/main/java -name "${ENTITY_NAME}.java" -exec cat {} \;
```

---

## Acceptance Criteria Summary

**For Each Entity:**
- [ ] XSD definition extracted and documented
- [ ] All field types verified against XSD
- [ ] All string lengths match XSD maxLength
- [ ] All nullability constraints match XSD minOccurs
- [ ] All relationships (collections, embedded objects) verified
- [ ] Flyway migration scripts reviewed and aligned
- [ ] Any discrepancies documented with justification or corrected
- [ ] Unit tests pass after corrections
- [ ] Integration tests pass after corrections
- [ ] Verification report created

**For the Complete Project:**
- [ ] All 102 tasks completed (37 + 36 + 19 + 10)
  - Phase 0: 37 enumeration migration tasks
  - Phase 1: 36 supporting class tasks
  - Phase 2: 19 usage domain entity tasks
  - Phase 3: 10 customer domain entity tasks
- [ ] Master discrepancy report compiled
- [ ] Database migration scripts updated if needed
- [ ] All tests passing (`mvn clean test`)
- [ ] Documentation updated with any justified deviations
- [ ] Code review completed
- [ ] Pull request merged

---

## Success Metrics

- **100% Enumeration Migration**: All 37 XSD enumerations implemented and correctly located
- **100% Coverage**: All 102 entities/classes/enums verified
- **Schema Compliance**: All field types, lengths, and constraints match XSD
- **Test Stability**: 100% test pass rate maintained
- **Documentation**: Complete verification reports for each entity
- **Migration Alignment**: Database schema matches entity definitions
- **Directory Structure**: Proper separation of usage/enums, customer/enums, and common enums

---

## Risk Mitigation

### Potential Risks

1. **XSD Interpretation**: Complex XSD types may have multiple valid Java representations
   - **Mitigation**: Document interpretation decisions; consult ESPI specification

2. **Breaking Changes**: Corrections may break existing data or APIs
   - **Mitigation**: Test thoroughly; provide migration scripts; version API if needed

3. **Performance Impact**: Adding constraints may affect query performance
   - **Mitigation**: Benchmark queries before/after; add indexes as needed

4. **Legacy Data**: Existing data may violate new constraints
   - **Mitigation**: Data migration scripts; validate existing data first

### Rollback Plan

- Keep separate feature branch for all changes
- Tag database before Flyway migrations
- Maintain detailed change log for easy reversion
- Run full integration test suite before merge

---

## Deliverables

1. **Enumeration Migration**: Complete migration of 37 XSD enumerations to correct directories
2. **Directory Structure**: New `domain/usage/enums/` directory with all usage domain enums
3. **Verification Reports**: Individual report for each of 102 entities/classes/enums
4. **Master Summary Report**: Consolidated findings across all entities and enumerations
5. **Flyway Migration Scripts**: Updated database schemas (if required)
6. **Test Suite**: All unit and integration tests passing
7. **Documentation**: Updated CLAUDE.md and entity documentation
8. **Pull Request**: Complete with detailed description and linked issue

---

## Appendix A: Complete Verification Example Using BasicTypes

This section provides a step-by-step walkthrough of verifying `ApplicationInformationEntity` against the `espi.xsd` schema using BasicTypes.

### Example Entity: ApplicationInformationEntity

**Entity Location**: `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/domain/usage/ApplicationInformationEntity.java`

**XSD Location**: `openespi-common/src/main/resources/schema/ESPI_4.0/espi.xsd`

### Step 1: Extract XSD ComplexType Definition

```bash
grep -A 200 "<xs:complexType name=\"ApplicationInformation\">" openespi-common/src/main/resources/schema/ESPI_4.0/espi.xsd | head -80
```

**XSD Definition (excerpt):**
```xml
<xs:complexType name="ApplicationInformation">
    <xs:sequence>
        <xs:element name="dataCustodianId" type="String256" minOccurs="0">
            <xs:annotation>
                <xs:documentation>Data Custodian's Identifier</xs:documentation>
            </xs:annotation>
        </xs:element>

        <xs:element name="dataCustodianApplicationStatus" type="DataCustodianApplicationStatus" minOccurs="0">
            <xs:annotation>
                <xs:documentation>Status of application registration</xs:documentation>
            </xs:annotation>
        </xs:element>

        <xs:element name="thirdPartyApplicationDescription" type="String256" minOccurs="0">
            <xs:annotation>
                <xs:documentation>Description of the third party application</xs:documentation>
            </xs:annotation>
        </xs:element>

        <xs:element name="client_secret" type="String512">
            <xs:annotation>
                <xs:documentation>OAuth2 client secret</xs:documentation>
            </xs:annotation>
        </xs:element>

        <xs:element name="client_id" type="String256">
            <xs:annotation>
                <xs:documentation>OAuth2 client identifier</xs:documentation>
            </xs:annotation>
        </xs:element>

        <xs:element name="client_id_issued_at" type="TimeType" minOccurs="0">
            <xs:annotation>
                <xs:documentation>Timestamp when client_id was issued</xs:documentation>
            </xs:annotation>
        </xs:element>

        <xs:element name="client_name" type="String256">
            <xs:annotation>
                <xs:documentation>Human-readable client name</xs:documentation>
            </xs:annotation>
        </xs:element>

        <xs:element name="redirect_uri" type="xs:anyURI" minOccurs="0" maxOccurs="unbounded">
            <xs:annotation>
                <xs:documentation>OAuth2 redirect URIs</xs:documentation>
            </xs:annotation>
        </xs:element>

        <xs:element name="grant_types" type="GrantType" maxOccurs="unbounded">
            <xs:annotation>
                <xs:documentation>OAuth2 grant types</xs:documentation>
            </xs:annotation>
        </xs:element>

        <xs:element name="scope" type="String256" maxOccurs="unbounded">
            <xs:annotation>
                <xs:documentation>OAuth2 scopes</xs:documentation>
            </xs:annotation>
        </xs:element>
    </xs:sequence>
</xs:complexType>
```

### Step 2: Resolve BasicTypes

For each field, look up the BasicType definition:

**Example 1: String256**
```bash
grep -B 2 -A 5 "simpleType name=\"String256\"" openespi-common/src/main/resources/schema/ESPI_4.0/espi.xsd
```

```xml
<xs:simpleType name="String256">
    <xs:annotation>
        <xs:documentation>Character string of max length 256</xs:documentation>
    </xs:annotation>
    <xs:restriction base="xs:string">
        <xs:maxLength value="256"/>
    </xs:restriction>
</xs:simpleType>
```

**Example 2: String512**
```bash
grep -B 2 -A 5 "simpleType name=\"String512\"" openespi-common/src/main/resources/schema/ESPI_4.0/espi.xsd
```

```xml
<xs:simpleType name="String512">
    <xs:annotation>
        <xs:documentation>[extension] Character string of max length 512</xs:documentation>
    </xs:annotation>
    <xs:restriction base="xs:string">
        <xs:maxLength value="512"/>
    </xs:restriction>
</xs:simpleType>
```

**Example 3: TimeType**
```bash
grep -B 2 -A 5 "simpleType name=\"TimeType\"" openespi-common/src/main/resources/schema/ESPI_4.0/espi.xsd
```

```xml
<xs:simpleType name="TimeType">
    <xs:annotation>
        <xs:documentation>Time as UNIX timestamp (seconds since epoch)</xs:documentation>
    </xs:annotation>
    <xs:restriction base="xs:long"/>
</xs:simpleType>
```

**Example 4: GrantType (Enumeration)**
```bash
grep -B 2 -A 15 "simpleType name=\"GrantType\"" openespi-common/src/main/resources/schema/ESPI_4.0/espi.xsd
```

```xml
<xs:simpleType name="GrantType">
    <xs:annotation>
        <xs:documentation>OAuth2 grant type enumeration</xs:documentation>
    </xs:annotation>
    <xs:restriction base="xs:string">
        <xs:enumeration value="authorization_code"/>
        <xs:enumeration value="client_credentials"/>
        <xs:enumeration value="refresh_token"/>
    </xs:restriction>
</xs:simpleType>
```

### Step 3: Create Field Mapping Table

| Field Name | XSD Element Type | BasicType Definition | minOccurs | maxOccurs | Java Type Expected | @Column Expected |
|------------|------------------|---------------------|-----------|-----------|-------------------|------------------|
| dataCustodianId | String256 | maxLength=256 | 0 | 1 | String | length=256, nullable=true |
| dataCustodianApplicationStatus | DataCustodianApplicationStatus | enum | 0 | 1 | DataCustodianApplicationStatus (enum) | nullable=true |
| thirdPartyApplicationDescription | String256 | maxLength=256 | 0 | 1 | String | length=256, nullable=true |
| client_secret | String512 | maxLength=512 | 1 | 1 | String | length=512, nullable=false |
| client_id | String256 | maxLength=256 | 1 | 1 | String | length=256, nullable=false |
| client_id_issued_at | TimeType | xs:long | 0 | 1 | Long | nullable=true |
| client_name | String256 | maxLength=256 | 1 | 1 | String | length=256, nullable=false |
| redirect_uri | xs:anyURI | xs:anyURI | 0 | unbounded | Set<String> or List<String> | @ElementCollection |
| grant_types | GrantType | enum | 1 | unbounded | Set<GrantType> | @ElementCollection, @Enumerated |
| scope | String256 | maxLength=256 | 1 | unbounded | Set<String> | @ElementCollection |

### Step 4: Review Current Entity Implementation

```bash
cat openespi-common/src/main/java/org/greenbuttonalliance/espi/common/domain/usage/ApplicationInformationEntity.java
```

**Hypothetical Current Implementation:**
```java
@Entity
@Table(name = "application_information")
public class ApplicationInformationEntity extends IdentifiedObject {

    @Column(name = "data_custodian_id", length = 256)
    private String dataCustodianId;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_custodian_application_status")
    private DataCustodianApplicationStatus dataCustodianApplicationStatus;

    @Column(name = "third_party_application_description", length = 512)  // ❌ WRONG LENGTH
    private String thirdPartyApplicationDescription;

    @Column(name = "client_secret", length = 512, nullable = false)
    private String clientSecret;

    @Column(name = "client_id", length = 256, nullable = false)
    private String clientId;

    @Column(name = "client_id_issued_at")  // ✓ Correct
    private Long clientIdIssuedAt;

    @Column(name = "client_name", length = 256)  // ❌ MISSING nullable=false
    private String clientName;

    @ElementCollection
    @CollectionTable(name = "application_redirect_uris",
                     joinColumns = @JoinColumn(name = "application_information_id"))
    @Column(name = "redirect_uri", length = 512)
    private Set<String> redirectUris;

    @ElementCollection
    @CollectionTable(name = "application_grant_types",
                     joinColumns = @JoinColumn(name = "application_information_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "grant_type")
    private Set<GrantType> grantTypes;

    @ElementCollection
    @CollectionTable(name = "application_scopes",
                     joinColumns = @JoinColumn(name = "application_information_id"))
    @Column(name = "scope", length = 256)
    private Set<String> scopes;
}
```

### Step 5: Document Discrepancies

**Verification Results:**

| Field | Status | Issue | Required Action |
|-------|--------|-------|-----------------|
| dataCustodianId | ✓ | - | None |
| dataCustodianApplicationStatus | ✓ | - | None |
| thirdPartyApplicationDescription | ❌ | Length is 512, should be 256 | Change `length=256` |
| clientSecret | ✓ | - | None |
| clientId | ✓ | - | None |
| clientIdIssuedAt | ✓ | - | None |
| clientName | ❌ | Missing `nullable=false` | Add `nullable=false` (minOccurs=1) |
| redirectUris | ✓ | - | None |
| grantTypes | ⚠️ | Should validate minOccurs=1 | Consider @NotEmpty validation |
| scopes | ⚠️ | Should validate minOccurs=1 | Consider @NotEmpty validation |

### Step 6: Create Corrected Implementation

```java
@Entity
@Table(name = "application_information")
public class ApplicationInformationEntity extends IdentifiedObject {

    @Column(name = "data_custodian_id", length = 256)
    private String dataCustodianId;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_custodian_application_status")
    private DataCustodianApplicationStatus dataCustodianApplicationStatus;

    // FIXED: Changed length from 512 to 256 to match String256 BasicType
    @Column(name = "third_party_application_description", length = 256)
    private String thirdPartyApplicationDescription;

    @Column(name = "client_secret", length = 512, nullable = false)
    private String clientSecret;

    @Column(name = "client_id", length = 256, nullable = false)
    private String clientId;

    @Column(name = "client_id_issued_at")
    private Long clientIdIssuedAt;

    // FIXED: Added nullable=false because minOccurs=1 in XSD
    @Column(name = "client_name", length = 256, nullable = false)
    private String clientName;

    @ElementCollection
    @CollectionTable(name = "application_redirect_uris",
                     joinColumns = @JoinColumn(name = "application_information_id"))
    @Column(name = "redirect_uri", length = 512)
    private Set<String> redirectUris;

    @ElementCollection
    @CollectionTable(name = "application_grant_types",
                     joinColumns = @JoinColumn(name = "application_information_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "grant_type")
    @NotEmpty  // Added validation for minOccurs=1, maxOccurs=unbounded
    private Set<GrantType> grantTypes;

    @ElementCollection
    @CollectionTable(name = "application_scopes",
                     joinColumns = @JoinColumn(name = "application_information_id"))
    @Column(name = "scope", length = 256)
    @NotEmpty  // Added validation for minOccurs=1, maxOccurs=unbounded
    private Set<String> scopes;
}
```

### Step 7: Update Flyway Migration

**Check existing migration:**
```bash
grep -A 30 "CREATE TABLE application_information" openespi-common/src/main/resources/db/migration/*.sql
```

**Required Migration Script** (if schema exists):
```sql
-- V5__Fix_ApplicationInformation_Schema_Compliance.sql

-- Fix thirdPartyApplicationDescription length (512 → 256)
ALTER TABLE application_information
MODIFY COLUMN third_party_application_description VARCHAR(256);

-- Add NOT NULL constraint to client_name (minOccurs=1)
ALTER TABLE application_information
MODIFY COLUMN client_name VARCHAR(256) NOT NULL;
```

### Step 8: Validate with Tests

```bash
# Run entity tests
mvn test -Dtest=ApplicationInformationEntityTest

# Run repository tests
mvn test -Dtest=ApplicationInformationRepositoryTest

# Run full module tests
cd openespi-common && mvn test
```

### Step 9: Create Verification Report

```markdown
# ApplicationInformationEntity Verification Report

**Entity**: ApplicationInformationEntity
**XSD Reference**: espi.xsd, lines 53-249 (ApplicationInformation complexType)
**Verification Date**: 2026-02-03
**Verified By**: Development Team

## XSD BasicTypes Resolved

| Field | XSD Type | BasicType Definition | Resolved Value |
|-------|----------|---------------------|----------------|
| dataCustodianId | String256 | maxLength restriction | 256 |
| thirdPartyApplicationDescription | String256 | maxLength restriction | 256 |
| client_secret | String512 | maxLength restriction | 512 |
| client_id | String256 | maxLength restriction | 256 |
| client_id_issued_at | TimeType | xs:long | Long (UNIX timestamp) |
| client_name | String256 | maxLength restriction | 256 |
| grant_types | GrantType | enum restriction | String enum values |
| scope | String256 | maxLength restriction | 256 |

## Discrepancies Found and Corrected

### 1. thirdPartyApplicationDescription Length Mismatch
- **XSD**: `type="String256"` → maxLength=256
- **Entity (before)**: `@Column(length=512)`
- **Entity (after)**: `@Column(length=256)`
- **Migration**: Required - ALTER COLUMN to VARCHAR(256)

### 2. clientName Nullability Mismatch
- **XSD**: `minOccurs="1"` (required field)
- **Entity (before)**: `@Column(nullable=true)` (implicit)
- **Entity (after)**: `@Column(nullable=false)`
- **Migration**: Required - ADD NOT NULL constraint

### 3. Collection Validation
- **XSD**: grantTypes and scopes have `minOccurs="1"` (at least one required)
- **Entity (before)**: No validation
- **Entity (after)**: Added `@NotEmpty` annotation
- **Migration**: Not required (validation only)

## Test Results

- ✅ ApplicationInformationEntityTest: 12/12 passed
- ✅ ApplicationInformationRepositoryTest: 8/8 passed
- ✅ Integration tests: 5/5 passed

## Migration Scripts

- ✅ Created: V5__Fix_ApplicationInformation_Schema_Compliance.sql
- ✅ Tested on dev database
- ✅ Ready for deployment

## Conclusion

ApplicationInformationEntity is now fully compliant with ESPI 4.0 espi.xsd schema definition. All BasicType references have been correctly interpreted and mapped to appropriate Java types and JPA annotations.
```

---

## Appendix B: Quick Reference Commands

### Extract All BasicType Definitions

```bash
# List all String BasicTypes from espi.xsd
grep -E "simpleType name=\"String[0-9]+\"" openespi-common/src/main/resources/schema/ESPI_4.0/espi.xsd

# List all numeric BasicTypes
grep -E "simpleType name=\"(U?Int|UInt)[0-9]+\"" openespi-common/src/main/resources/schema/ESPI_4.0/espi.xsd

# List all enum BasicTypes
grep "simpleType name=" openespi-common/src/main/resources/schema/ESPI_4.0/espi.xsd | grep -v "String\|Int\|HexBinary\|Time\|UUID"
```

### Find Element's BasicType

```bash
# Find what type an element uses
grep -i "element name=\"clientSecret\"" openespi-common/src/main/resources/schema/ESPI_4.0/espi.xsd

# Find the BasicType definition
TYPE="String512"
grep -B 2 -A 5 "simpleType name=\"$TYPE\"" openespi-common/src/main/resources/schema/ESPI_4.0/espi.xsd
```

### Verify Entity Field Mappings

```bash
# Find all @Column annotations in an entity
grep "@Column" openespi-common/src/main/java/org/greenbuttonalliance/espi/common/domain/usage/ApplicationInformationEntity.java

# Find all String fields with their lengths
grep -B 1 "private String" openespi-common/src/main/java/org/greenbuttonalliance/espi/common/domain/usage/ApplicationInformationEntity.java | grep -E "@Column|private String"
```

### Compare Multiple Entities

```bash
# Find all entities with String256 fields that might be wrong
for file in openespi-common/src/main/java/org/greenbuttonalliance/espi/common/domain/usage/*.java; do
    echo "=== $(basename $file) ==="
    grep -n "@Column.*length.*256" "$file"
done
```

---

## Conclusion

This systematic approach ensures complete ESPI 4.0 schema compliance across all domain entities, enumerations, and supporting classes. The plan addresses a critical gap in the current implementation: 25 missing enumerations and 8 enumerations in incorrect locations.

### Implementation Phases Summary

1. **Phase 0 (Enumerations)**: Migrate 37 XSD enumerations, create proper directory structure
2. **Phase 1 (Foundation)**: Verify 36 supporting classes and shared enumerations
3. **Phase 2 (Usage Domain)**: Verify 19 usage domain entities
4. **Phase 3 (Customer Domain)**: Verify 10 customer domain entities

By following the repeatable task template and verification checklist, the team can efficiently validate 102 components with confidence in correctness and consistency.

### Key Improvements

- ✅ All XSD enumerations migrated from legacy code
- ✅ Proper directory structure: `usage/enums/`, `customer/enums/`, `common/`
- ✅ Complete BasicTypes interpretation and mapping
- ✅ ServiceKind naming corrected (was incorrectly named ServiceCategory)
- ✅ Comprehensive verification reports for each component

**Total Effort Estimate**: 10-12 weeks with 2-3 developers working in parallel
- Phase 0: 2-3 weeks (enumeration migration is foundational)
- Phases 1-3: 8-9 weeks (entity and type verification)

**Priority**: High - Missing enumerations and schema compliance are blocking full ESPI 4.0 certification

**Related Issues**: #28 (Phase 17 pattern), Spring Boot 4.0 migration