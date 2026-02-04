# ESPI 4.0 Schema Analysis Summary

**Analysis Date:** 2026-02-03
**NAESB REQ.21 ESPI Version:** 4.0
**Schema Publication Dates:** December 13-15, 2023

---

## Overview

This directory contains comprehensive analysis reports for the NAESB ESPI 4.0 XML schemas used in the OpenESPI-GreenButton-Java implementation. The schemas define the data model for energy usage data exchange under the Green Button standard.

## Schema Files Analyzed

| Schema | Namespace | Purpose | Report |
|--------|-----------|---------|--------|
| `espi.xsd` | `http://naesb.org/espi` | Energy usage data (UsagePoint, MeterReading, IntervalBlock, etc.) | [espi_enumerations.md](espi_enumerations.md) |
| `customer.xsd` | `http://naesb.org/espi/customer` | Customer/PII data (Customer, CustomerAccount, Meter, etc.) | [customer_enumerations.md](customer_enumerations.md) |

---

## Combined Statistics

| Category | espi.xsd | customer.xsd | Total |
|----------|----------|--------------|-------|
| Basic Types | 7 | 8 | 15 |
| String Types | 7 | 5 | 12 |
| Hex Binary Types | 4 | 4 | 8 |
| Special Types | 2 | 2 | 4 |
| Enumeration Types | 35 | 16 | 51 |
| Complex Types (Resources) | 12 | 15 | 27 |
| Complex Types (Supporting) | 17 | 30 | 47 |
| Global Elements | 20 | 45 | 65 |

---

## Key Enumeration Types

### Usage Domain (espi.xsd)

| Enumeration | Values | Description |
|-------------|--------|-------------|
| AccumulationKind | 14 | How readings are accumulated (instantaneous, cumulative, delta, etc.) |
| CommodityKind | 14 | Type of commodity (electricity, gas, water, etc.) |
| Currency | 100+ | ISO 4217 currency codes |
| DataQualifierKind | 16 | Data quality indicators (average, maximum, minimum, etc.) |
| FlowDirectionKind | 21 | Direction of energy flow (forward, reverse, net, etc.) |
| MeasurementKind | 100+ | Type of measurement (energy, power, voltage, current, etc.) |
| PhaseCodeKind | 20+ | Electrical phase codes (A, B, C, AB, ABC, etc.) |
| QualityOfReading | 20+ | Reading quality indicators |
| ServiceKind | 11 | Type of utility service |
| TimeAttributeKind | 8 | Time period attributes (billing, seasonal, etc.) |
| UnitMultiplierKind | 16 | SI unit multipliers (kilo, mega, milli, etc.) |
| UnitSymbolKind | 100+ | Unit of measure symbols (W, Wh, VA, VAr, etc.) |

### Customer Domain (customer.xsd)

| Enumeration | Values | Description |
|-------------|--------|-------------|
| CustomerKind | 15 | Type of customer (residential, commercial, industrial, etc.) |
| SupplierKind | 6 | Type of service supplier (utility, municipality, etc.) |
| ServiceKind | 11 | Type of utility service |
| EnrollmentStatus | 5 | Program enrollment status |
| NotificationMethodKind | 5 | Customer notification methods |
| MeterMultiplierKind | 6 | Types of meter multipliers |
| RevenueKind | 7 | Types of revenue (fees, rates, rebates) |
| ProgramDateKind | 4 | Types of program dates |
| CRUDOperation | 4 | CRUD operation types |
| MediaType | 15 | MIME content types |
| StatusCode | 12+ | HTTP-style status codes |

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
| Customer | Organisation | Full customer information |
| CustomerAccount | Document | Customer billing account |
| CustomerAgreement | Agreement | Service agreement |
| ServiceSupplier | Organisation | Utility or energy provider |
| ServiceLocation | WorkLocation | Physical service location |
| Meter | EndDevice | Physical meter device |
| EndDevice | AssetContainer | End device (base for Meter) |
| Statement | Document | Billing statement |
| DemandResponseProgram | IdentifiedObject | DR program enrollment |
| PricingStructure | Document | Pricing/rate structure |
| ProgramDateIdMappings | IdentifiedObject | Program date mappings |
| TimeConfiguration | IdentifiedObject | Timezone and DST settings |
| UsagePoint | IdentifiedObject | Usage point (customer context) |

---

## Type Inheritance Hierarchy

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
    ├── DemandResponseProgram
    ├── ProgramDateIdMappings
    └── Document
        ├── CustomerAccount
        ├── PricingStructure
        ├── Statement
        └── Agreement
            └── CustomerAgreement

Organisation (extends IdentifiedObject)
├── Customer
└── ServiceSupplier

Location (extends IdentifiedObject)
└── WorkLocation
    └── ServiceLocation

Asset (extends IdentifiedObject)
└── AssetContainer
    └── EndDevice
        └── Meter
```

---

## Common Patterns

### Union Types for Enumerations

ESPI uses a union pattern for numeric enumerations that allows both defined values and extended values:

```xml
<xs:simpleType name="AccumulationKind">
    <xs:union memberTypes="UInt16">
        <xs:simpleType>
            <xs:restriction base="xs:string">
                <xs:enumeration value="0"/>  <!-- none -->
                <xs:enumeration value="1"/>  <!-- bulkQuantity -->
                <!-- ... -->
            </xs:restriction>
        </xs:simpleType>
    </xs:union>
</xs:simpleType>
```

### IdentifiedObject Base Type

All major resources extend `IdentifiedObject` which provides:
- `description` (String512) - Human-readable description
- Extension capability from `Object` base type

### Time Representation

- All timestamps use `TimeType` (xs:long) as Unix epoch seconds
- Timezone handled via `TimeConfiguration` (tzOffset, dstOffset, dstStartRule, dstEndRule)
- Duration/intervals use `DateTimeInterval` (start, end, duration)

### Monetary Values

- Monetary amounts stored as `Int48` in smallest currency unit (cents/pence)
- Currency specified separately using ISO 4217 numeric codes

---

## Schema Location

The schema files are located at:
```
openespi-common/src/main/resources/schema/ESPI_4.0/
├── espi.xsd        (usage/energy data)
└── customer.xsd    (customer/PII data)
```

---

## Related Documentation

- [ESPI Enumerations Report](espi_enumerations.md) - Detailed analysis of espi.xsd
- [Customer Enumerations Report](customer_enumerations.md) - Detailed analysis of customer.xsd
- [NAESB ESPI 4.0 Specification](https://www.naesb.org/) - Official specification

---

## Implementation Notes

### Java Enum Mapping

When implementing Java enums for ESPI types:

1. **Numeric enumerations** (UInt16 union) - Map to Java enum with integer value field
2. **String enumerations** - Map to Java enum with string value field
3. **Large enumerations** (100+ values) - Consider using lookup maps for efficiency

### Validation Considerations

1. **Union types** allow undefined numeric values - implement lenient parsing
2. **String length restrictions** must be enforced (String32, String256, etc.)
3. **HexBinary types** have specific byte length requirements

### JAXB/Jakarta XML Binding

- Use `@XmlEnum` and `@XmlEnumValue` for enum mapping
- Consider custom adapters for union types
- Handle namespace differences between espi and customer schemas

---

*Generated from NAESB REQ.21 ESPI Version 4.0 schemas (December 2023)*
