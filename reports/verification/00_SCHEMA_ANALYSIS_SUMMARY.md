# ESPI Schema Analysis Summary

**Analysis Date:** 2026-02-04
**NAESB REQ.21 ESPI Versions Analyzed:** 4.0 and 4.1

---

## Overview

This directory contains comprehensive analysis reports for the NAESB ESPI XML schemas used in the OpenESPI-GreenButton-Java implementation. The schemas define the data model for energy usage data exchange under the Green Button standard.

**Schema Versions:**
- **ESPI 4.0** - Published December 13-15, 2023 (current production)
- **ESPI 4.1** - Pending publication (draft)

---

## Schema Files Analyzed

### ESPI 4.0 Schemas

| Schema | Namespace | Purpose | Report |
|--------|-----------|---------|--------|
| `espi.xsd` | `http://naesb.org/espi` | Energy usage data (UsagePoint, MeterReading, IntervalBlock, etc.) | [espi_enumerations.md](espi_enumerations.md) |
| `customer.xsd` | `http://naesb.org/espi/customer` | Customer/PII data (Customer, CustomerAccount, Meter, etc.) | See 4.1 report |

### ESPI 4.1 Schemas

| Schema | Namespace | Purpose | Report |
|--------|-----------|---------|--------|
| `espi_4.1.xsd` | `http://naesb.org/espi` | Energy usage data (updated) | *Analysis pending* |
| `customer_4.1.xsd` | `http://naesb.org/espi/customer` | Customer/PII data (updated) | [customer_enumerations.md](customer_enumerations.md) |

---

## Combined Statistics

### ESPI 4.0 (espi.xsd + customer.xsd)

| Category | espi.xsd | customer.xsd | Total |
|----------|----------|--------------|-------|
| Basic Types | 7 | 8 | 15 |
| String Types | 7 | 5 | 12 |
| Hex Binary Types | 4 | 4 | 8 |
| Special Types | 2 | 2 | 4 |
| Enumeration Types | 35 | 15 | 50 |
| Complex Types (Resources) | 12 | 14 | 26 |
| Complex Types (Supporting) | 17 | 26+ | 43+ |
| Global Elements | 20 | 38+ | 58+ |

### ESPI 4.1 (Customer Schema Changes)

| Category | customer_4.1.xsd | Change from 4.0 |
|----------|------------------|-----------------|
| Basic Types | 8 | No change |
| String Types | 5 | No change |
| Hex Binary Types | 4 | No change |
| Special Types | 2 | No change |
| Enumeration Types | 15 | No change |
| Complex Types (Resources) | 14 | No change |
| Complex Types (Supporting) | 26+ | 3 commented out |
| New Fields | 5 | Added |

---

## Key Changes: ESPI 4.0 → 4.1

### 1. New Fields Added

| Resource | New Field | Type | Description |
|----------|-----------|------|-------------|
| Customer | customerName | String256 | Customer name |
| CustomerAccount | accountId | String256 | Account identifier |
| CustomerAgreement | agreementId | String256 | Agreement identifier |
| Meter | intervalLength | UInt32 | Interval length in seconds |
| ServiceLocation | outageBlock | String32 | Outage block identifier |

### 2. Types Commented Out in 4.1

| Type | Impact |
|------|--------|
| AssetContainer | EndDevice uses Asset composition instead |
| OrganisationRole | No longer used |
| WorkLocation | ServiceLocation uses Location composition instead |

### 3. Structural Changes (Composition over Inheritance)

ESPI 4.1 shifts from inheritance to composition for several types:

| Resource | 4.0 Pattern | 4.1 Pattern |
|----------|-------------|-------------|
| EndDevice | Extends AssetContainer | Contains Asset element |
| ServiceLocation | Extends WorkLocation | Contains Location element |
| Customer | Direct fields | Contains Organisation element |
| CustomerAccount | Direct fields | Contains Document element |
| CustomerAgreement | Direct fields | Contains Agreement element |

### 4. Enumeration Types (No Changes)

**Important:** The enumeration base types are **consistent** between ESPI 4.0 and 4.1.

| Enumeration | Base Type | Value Format |
|-------------|-----------|--------------|
| CRUDOperation | UInt16 | Numeric (0, 1, 2, 3) |
| CustomerKind | xs:string | String ("residential", etc.) |
| EnrollmentStatus | xs:string | String ("enrolled", etc.) |
| SupplierKind | xs:string | String ("utility", etc.) |
| ServiceKind | xs:string | String ("electricity", etc.) |
| NotificationMethodKind | UInt16 | Numeric (0, 1, 2, 3) |
| MeterMultiplierKind | UInt16 | Numeric (0, 1, 2, 3, 4, 5) |
| UnitMultiplierKind | xs:string | String ("k", "M", "G", etc.) |
| Currency | xs:string | String ("USD", "EUR", etc.) |

---

## Enumeration Types Summary

### customer.xsd Enumerations

#### Numeric Enumerations (UInt16 union)
| Enumeration | Values | Description |
|-------------|--------|-------------|
| CRUDOperation | 4 | CRUD operation types (0=Create, 1=Read, 2=Update, 3=Delete) |
| NotificationMethodKind | 4 | Notification methods (0=call, 1=email, 2=letter, 3=other) |
| MeterMultiplierKind | 6 | Meter multiplier types |

#### String Enumerations (xs:string union)
| Enumeration | Values | Description |
|-------------|--------|-------------|
| CustomerKind | 12+ | Type of customer (residential, commercialIndustrial, etc.) |
| SupplierKind | 3 | Type of supplier (utility, retailer, other) |
| ServiceKind | 11 | Type of service (electricity, gas, water, etc.) |
| EnrollmentStatus | 4 | Program enrollment status |
| RevenueKind | 8 | Types of revenue |
| ProgramDateKind | 2 | Types of program dates |
| UnitMultiplierKind | 13 | SI unit multipliers |
| UnitSymbolKind | 100+ | Unit of measure symbols |
| Currency | 100+ | ISO 4217 currency codes (alphabetic) |

#### String-Based Status Codes
| Enumeration | Values | Description |
|-------------|--------|-------------|
| StatusCode | 12 | HTTP-style status codes |
| MediaType | 15 | MIME content types |

### espi.xsd Enumerations (Numeric UInt16)

These enumerations from espi.xsd use numeric (UInt16) values:

| Enumeration | Values | Description |
|-------------|--------|-------------|
| AccumulationKind | 14 | How readings are accumulated |
| CommodityKind | 14 | Type of commodity |
| DataQualifierKind | 16 | Data quality indicators |
| FlowDirectionKind | 21 | Direction of energy flow |
| MeasurementKind | 100+ | Type of measurement |
| PhaseCodeKind | 20+ | Electrical phase codes |
| QualityOfReading | 20+ | Reading quality indicators |
| ServiceKind | 11 | Type of utility service |
| TimeAttributeKind | 8 | Time period attributes |
| UnitMultiplierKind | 16 | SI unit multipliers |
| UnitSymbolKind | 100+ | Unit of measure symbols |
| Currency | 100+ | ISO 4217 currency codes (numeric) |

### OAuth Enumerations (espi.xsd)

| Enumeration | Values | Description |
|-------------|--------|-------------|
| GrantType | 3 | OAuth 2.0 grant types |
| ResponseType | 1 | OAuth response types |
| TokenType | 1 | OAuth token types |
| OAuthError | 12 | OAuth error codes |
| TokenEndPointMethod | 1 | Token endpoint authentication methods |

---

## Core Resource Types

### Usage Domain Resources (espi.xsd)

| Resource | Base Type | Description |
|----------|-----------|-------------|
| UsagePoint | IdentifiedObject | Logical point for energy measurement |
| MeterReading | IdentifiedObject | Collection of interval blocks |
| IntervalBlock | IdentifiedObject | Time-series interval data |
| IntervalReading | (inline) | Individual reading within an interval |
| ReadingType | IdentifiedObject | Metadata describing measurements |
| ElectricPowerQualitySummary | IdentifiedObject | Power quality metrics |
| UsageSummary | IdentifiedObject | Aggregated usage statistics |
| TimeConfiguration | IdentifiedObject | Timezone and DST settings |
| Authorization | IdentifiedObject | OAuth authorization record |
| ApplicationInformation | IdentifiedObject | Third-party application registration |
| Subscription | IdentifiedObject | Data subscription |
| RetailCustomer | IdentifiedObject | Retail customer (minimal) |

### Customer Domain Resources (customer.xsd)

| Resource | Base Type | Description |
|----------|-----------|-------------|
| Customer | IdentifiedObject | Full customer information (contains Organisation) |
| CustomerAccount | IdentifiedObject | Customer billing account (contains Document) |
| CustomerAgreement | IdentifiedObject | Service agreement (contains Agreement) |
| ServiceSupplier | IdentifiedObject | Utility or energy provider (contains Organisation) |
| ServiceLocation | IdentifiedObject | Physical service location (contains Location) |
| Meter | IdentifiedObject | Physical meter device (contains EndDevice) |
| EndDevice | IdentifiedObject | End device base (contains Asset) |
| Statement | IdentifiedObject | Billing statement |
| DemandResponseProgram | (inline) | DR program enrollment |
| PricingStructure | IdentifiedObject | Pricing/rate structure (contains Document) |
| ProgramDateIdMappings | IdentifiedObject | Program date mappings |
| TimeConfiguration | IdentifiedObject | Timezone and DST settings |

---

## Type Inheritance Hierarchy

### ESPI 4.0/4.1 Common Hierarchy

```
Object
└── IdentifiedObject
    ├── UsagePoint
    ├── MeterReading
    ├── IntervalBlock
    ├── ReadingType
    ├── ElectricPowerQualitySummary
    ├── UsageSummary
    ├── TimeConfiguration
    ├── Authorization
    ├── ApplicationInformation
    ├── Subscription
    ├── RetailCustomer
    ├── Customer (contains Organisation)
    ├── CustomerAccount (contains Document)
    ├── CustomerAgreement (contains Agreement)
    ├── ServiceSupplier (contains Organisation)
    ├── ServiceLocation (contains Location)
    ├── EndDevice (contains Asset)
    ├── Meter (contains EndDevice)
    ├── Statement
    ├── PricingStructure (contains Document)
    └── ProgramDateIdMappings
```

### Composition Types (Supporting)

```
Object
├── Organisation (contact info for Customer, ServiceSupplier)
├── Document (metadata for CustomerAccount, PricingStructure)
├── Agreement (extends Document - for CustomerAgreement)
├── Asset (device info for EndDevice)
├── Location (address info for ServiceLocation)
├── Status
├── Priority
├── StreetAddress
├── TelephoneNumber
├── ElectronicAddress
└── ...
```

---

## Common Patterns

### Union Types for Enumerations

ESPI uses union patterns that allow both defined values and extended values:

**Numeric Union (espi.xsd pattern):**
```xml
<xs:simpleType name="AccumulationKind">
    <xs:union memberTypes="UInt16">
        <xs:simpleType>
            <xs:restriction base="UInt16">
                <xs:enumeration value="0"/>  <!-- none -->
                <xs:enumeration value="1"/>  <!-- bulkQuantity -->
            </xs:restriction>
        </xs:simpleType>
    </xs:union>
</xs:simpleType>
```

**String Union (customer.xsd pattern):**
```xml
<xs:simpleType name="CustomerKind">
    <xs:union memberTypes="xs:string">
        <xs:simpleType>
            <xs:restriction base="xs:string">
                <xs:enumeration value="residential"/>
                <xs:enumeration value="commercialIndustrial"/>
            </xs:restriction>
        </xs:simpleType>
    </xs:union>
</xs:simpleType>
```

### IdentifiedObject Base Type

All major resources extend `IdentifiedObject` which provides:
- `batchItemInfo` - Batch processing information
- `name` - Object name (deprecated)

### Time Representation

- All timestamps use `TimeType` (xs:long) as Unix epoch seconds
- Timezone handled via `TimeConfiguration` (tzOffset, dstOffset, dstStartRule, dstEndRule)
- Duration/intervals use `DateTimeInterval` (start, duration)

### Monetary Values

- Monetary amounts stored as `Int48` in smallest currency unit (cents/pence)
- Currency specified separately:
  - **espi.xsd:** ISO 4217 numeric codes (e.g., 840 for USD)
  - **customer.xsd:** ISO 4217 alphabetic codes (e.g., "USD")

---

## Schema Location

```
openespi-common/src/main/resources/schema/
├── ESPI_4.0/
│   ├── espi.xsd           (usage/energy data)
│   └── customer.xsd       (customer/PII data)
└── ESPI_4.1/
    ├── espi_4.1.xsd       (usage/energy data - updated)
    └── customer_4.1.xsd   (customer/PII data - updated)
```

---

## Related Documentation

- [ESPI 4.0 Enumerations Report](espi_enumerations.md) - Detailed analysis of espi.xsd (4.0)
- [Customer Enumerations Report](customer_enumerations.md) - Detailed analysis of customer_4.1.xsd
- [NAESB ESPI Specification](https://www.naesb.org/) - Official specification

---

## Implementation Notes

### Java Enum Mapping Strategy

#### Numeric Enumerations (UInt16 union)

Used in espi.xsd and some customer.xsd types:

```java
@XmlEnum
public enum AccumulationKind {
    @XmlEnumValue("0") NONE(0),
    @XmlEnumValue("1") BULK_QUANTITY(1),
    @XmlEnumValue("2") CONTINUOUS_CUMULATIVE(2);
    // ...

    private final int value;

    public int getValue() { return value; }
    public static AccumulationKind fromValue(int value) { ... }
}
```

#### String Enumerations (xs:string union)

Used in customer.xsd:

```java
@XmlEnum
public enum CustomerKind {
    @XmlEnumValue("residential")
    RESIDENTIAL("residential"),

    @XmlEnumValue("commercialIndustrial")
    COMMERCIAL_INDUSTRIAL("commercialIndustrial");
    // ...

    private final String value;

    public String getValue() { return value; }
    public static CustomerKind fromValue(String value) { ... }
}
```

### Validation Considerations

1. **Union types** allow undefined values - implement lenient parsing
2. **String length restrictions** must be enforced (String32, String256, etc.)
3. **HexBinary types** have specific byte length requirements

### JAXB/Jakarta XML Binding

- Use `@XmlEnum` and `@XmlEnumValue` for enum mapping
- Handle namespace differences between espi and customer schemas
- Use composition-aware mapping for 4.1 structural changes

---

## Migration Recommendations

### For New Implementations

1. **Support both numeric and string enumerations** based on schema source
2. **Use composition pattern** for Customer, CustomerAccount, etc.
3. **Add new fields** (customerName, accountId, etc.) as optional

### For Existing 4.0 Implementations

1. **Add new fields** to entity classes
2. **Handle removed types** (AssetContainer, WorkLocation, OrganisationRole) through composition
3. **No enum changes needed** - base types are consistent between versions

### Database Schema Considerations

1. **New columns** needed for: customerName, accountId, agreementId, intervalLength, outageBlock
2. **Composition relationships** may require embedded types or separate tables
3. **Enum storage** - numeric enums as INT, string enums as VARCHAR

---

*Generated from NAESB REQ.21 ESPI Version 4.0 (December 2023) and Version 4.1 (Pending Publication) schemas*
