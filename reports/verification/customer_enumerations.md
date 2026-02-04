# ESPI 4.1 Customer Schema Analysis Report

**Schema Version:** NAESB REQ.21 ESPI Version 4.1 (Pending Publication)
**Namespace:** `http://naesb.org/espi/customer`
**Analysis Date:** 2026-02-04

---

## Table of Contents

1. [Basic Types (Primitives)](#1-basic-types-primitives)
2. [String Types](#2-string-types)
3. [Hex Binary Types](#3-hex-binary-types)
4. [Special Types](#4-special-types)
5. [Enumeration Types](#5-enumeration-types)
6. [Complex Types (Resources)](#6-complex-types-resources)
7. [Complex Types (Supporting)](#7-complex-types-supporting)
8. [Global Elements](#8-global-elements)
9. [Changes from ESPI 4.0](#9-changes-from-espi-40)

---

## 1. Basic Types (Primitives)

| Type Name | Base Type | Description | Range/Restriction |
|-----------|-----------|-------------|-------------------|
| `UInt8` | `xs:unsignedByte` | Unsigned 8-bit integer | 0 to 255 (2^8-1) |
| `UInt16` | `xs:unsignedShort` | Unsigned 16-bit integer | 0 to 65,535 (2^16-1) |
| `UInt32` | `xs:unsignedInt` | Unsigned 32-bit integer | 0 to 4,294,967,295 (2^32-1) |
| `UInt48` | `xs:unsignedLong` | Unsigned 48-bit integer | 0 to 281,474,976,710,655 (2^48-1) |
| `Int16` | `xs:short` | Signed 16-bit integer | -32,768 to 32,767 |
| `Int48` | `xs:long` | Signed 48-bit integer | -140,737,488,355,328 to +140,737,488,355,328 |
| `TimeType` | `xs:long` | Unix timestamp (seconds since 1970-01-01) | Signed 64-bit |
| `PerCent` | `xs:integer` | Percentage value | 0 to 100 |

---

## 2. String Types

| Type Name | Base Type | Max Length | Description |
|-----------|-----------|------------|-------------|
| `String32` | `xs:string` | 32 | Character string of max length 32 |
| `String64` | `xs:string` | 64 | Character string of max length 64 |
| `String256` | `xs:string` | 256 | Character string of max length 256 |
| `String512` | `xs:string` | 512 | Character string of max length 512 |
| `name` | `String64` | 64 | Name string (restricted String64) |

---

## 3. Hex Binary Types

| Type Name | Base Type | Max Bytes | Bits | Description |
|-----------|-----------|-----------|------|-------------|
| `HexBinary8` | `xs:hexBinary` | 1 | 8 | 2 hex characters |
| `HexBinary16` | `xs:hexBinary` | 2 | 16 | 4 hex characters |
| `HexBinary32` | `xs:hexBinary` | 4 | 32 | 8 hex characters |
| `HexBinary128` | `xs:hexBinary` | 16 | 128 | 32 hex characters |

---

## 4. Special Types

| Type Name | Base Type | Pattern/Restriction | Description |
|-----------|-----------|---------------------|-------------|
| `UUIDType` | `xs:string` | `[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}` | UUID pattern |
| `DstRuleType` | `HexBinary32` | N/A | Bit-encoded DST rule |

---

## 5. Enumeration Types

### 5.1 Status and Operation Enumerations

#### CRUDOperation
**Base:** `UInt16` (union with numeric values)

| Value | Name | Description |
|-------|------|-------------|
| 0 | Create | Resource was created |
| 1 | Read | Resource was read |
| 2 | Update | Resource was updated |
| 3 | Delete | Resource was deleted |

---

#### EnrollmentStatus
**Base:** `xs:string` (union with string values)

| Value | Description |
|-------|-------------|
| `unenrolled` | Currently NOT enrolled in the Demand Response program |
| `enrolled` | Currently enrolled in the Demand Response program |
| `enrolledPending` | Currently pending enrollment in the Demand Response program |
| `unenrolledPending` | Currently pending un-enrollment from the Demand Response program |

---

#### StatusCode
**Base:** `String32`

HTTP-style status codes for ESPI operations.

| Value | Description |
|-------|-------------|
| `100` | Continue |
| `200` | OK - Successful request |
| `201` | Created - Resource created |
| `204` | No Content |
| `301` | Moved Permanently |
| `400` | Bad Request |
| `401` | Unauthorized |
| `403` | Forbidden |
| `404` | Not Found |
| `500` | Internal Server Error |
| `501` | Not Implemented |
| `503` | Service Unavailable |

---

### 5.2 Customer and Service Enumerations

#### CustomerKind
**Base:** `xs:string` (union with string values)

| Value | Description |
|-------|-------------|
| `residential` | Residential customer |
| `residentialAndCommercial` | Residential and commercial customer |
| `residentialAndStreetlight` | Residential and streetlight customer |
| `residentialStreetlightOthers` | Residential streetlight and others |
| `residentialFarmService` | Residential farm service customer |
| `commercialIndustrial` | Commercial or industrial customer |
| `pumpingLoad` | Pumping load customer |
| `windMachine` | Wind machine customer |
| `energyServiceScheduler` | Energy service scheduler customer |
| `energyServiceSupplier` | Energy service supplier customer |
| `other` | Other customer type |
| `internalUse` | Internal use customer |

---

#### SupplierKind
**Base:** `xs:string` (union with string values)

| Value | Description |
|-------|-------------|
| `utility` | Entity that delivers the service to the customer |
| `retailer` | Entity that sells the service, but does not deliver to the customer |
| `other` | Other kind of supplier |

---

#### ServiceKind
**Base:** `xs:string` (union with string values)

| Value | Description |
|-------|-------------|
| `electricity` | Electric service |
| `gas` | Natural gas service |
| `water` | Water service |
| `time` | Time service |
| `heat` | Heat service |
| `refuse` | Refuse/waste service |
| `sewerage` | Sewerage service |
| `rates` | Rates information |
| `tvLicence` | TV license service |
| `internet` | Internet service |
| `other` | Other service type |

---

#### RevenueKind
**Base:** `xs:string` (union with string values)

| Value | Description |
|-------|-------------|
| `nonResidentialSales` | Non-residential sales |
| `residentialSales` | Residential sales |
| `industrialSales` | Industrial sales |
| `otherSales` | Other sales |
| `streetlightingSales` | Streetlighting sales |
| `irrigationSales` | Irrigation sales |
| `transmission` | Transmission |
| `other` | Other revenue type |

---

### 5.3 Notification and Communication Enumerations

#### NotificationMethodKind
**Base:** `UInt16` (union with numeric values)

| Value | Name | Description |
|-------|------|-------------|
| 0 | call | Notification via phone call |
| 1 | email | Notification via email |
| 2 | letter | Notification via postal letter |
| 3 | other | Other notification method |

---

#### MediaType
**Base:** `String64`

MIME media types for content negotiation.

| Value | Description |
|-------|-------------|
| `application/atom+xml` | Atom XML feed |
| `application/pdf` | PDF document |
| `application/xml` | Generic XML |
| `application/json` | JSON data |
| `application/octet-stream` | Binary data |
| `text/html` | HTML document |
| `text/plain` | Plain text |
| `text/xml` | XML text |
| `image/gif` | GIF image |
| `image/jpeg` | JPEG image |
| `image/png` | PNG image |
| `application/vnd.ms-excel` | Excel spreadsheet |
| `application/zip` | ZIP archive |
| `multipart/form-data` | Form data |
| `text/csv` | CSV data |

---

### 5.4 Program and Date Enumerations

#### ProgramDateKind
**Base:** `xs:string` (union with string values)

| Value | Description |
|-------|-------------|
| `enrollment` | Enrollment date |
| `cancellation` | Cancellation date |

---

### 5.5 Meter and Measurement Enumerations

#### MeterMultiplierKind
**Base:** `UInt16` (union with numeric values)

| Value | Name | Description |
|-------|------|-------------|
| 0 | kH | Test dial multiplier |
| 1 | kR | Register multiplier |
| 2 | kE | Element test multiplier |
| 3 | ctRatio | Current transformer ratio |
| 4 | ptRatio | Potential transformer ratio |
| 5 | transformerRatio | Transformer ratio |

---

#### UnitMultiplierKind
**Base:** `xs:string` (union with string values)

SI unit multipliers as powers of 10.

| Value | Symbol | Multiplier |
|-------|--------|------------|
| `p` | p | 10^-12 (pico) |
| `n` | n | 10^-9 (nano) |
| `micro` | µ | 10^-6 (micro) |
| `m` | m | 10^-3 (milli) |
| `c` | c | 10^-2 (centi) |
| `d` | d | 10^-1 (deci) |
| `none` | (none) | 10^0 = 1 |
| `da` | da | 10^1 (deca) |
| `h` | h | 10^2 (hecto) |
| `k` | k | 10^3 (kilo) |
| `M` | M | 10^6 (mega) |
| `G` | G | 10^9 (giga) |
| `T` | T | 10^12 (tera) |

---

#### UnitSymbolKind
**Base:** `xs:string` (union with string values)

Unit of measurement symbols. Includes 100+ unit types such as:
- SI base units: `A`, `m`, `K`, `mol`, `cd`, `s`
- Power/Energy: `W`, `VA`, `VAr`, `Wh`, `VAh`, `VArh`
- Electrical: `V`, `ohm`, `F`, `H`, `S`
- Frequency: `Hz`, `rad`, `sr`
- Volume/Flow: `m3`, `ft3`, `litre`, `usGal`
- Miscellaneous: `none`, `count`, `money`, `status`

---

#### Currency
**Base:** `xs:string` (union with string values)

ISO 4217 currency codes (alphabetic). Examples:

| Value | Description |
|-------|-------------|
| `AUD` | Australian Dollar |
| `CAD` | Canadian Dollar |
| `EUR` | Euro |
| `GBP` | British Pound Sterling |
| `JPY` | Japanese Yen |
| `USD` | United States Dollar |

---

## 6. Complex Types (Resources)

### 6.1 Core Resource Types

#### Customer
**Extends:** `IdentifiedObject`

Organization receiving services from a utility.

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| organisation | Organisation | 0..1 | Customer's contact information |
| kind | CustomerKind | 0..1 | Type of customer |
| specialNeed | String256 | 0..1 | Special needs of customer |
| vip | xs:boolean | 0..1 | VIP status flag |
| pucNumber | String256 | 0..1 | Public utility commission number |
| status | Status | 0..1 | Customer status |
| priority | Priority | 0..1 | Customer priority |
| locale | String256 | 0..1 | Customer locale |
| **customerName** | String256 | 0..1 | Customer name *(New in 4.1)* |

---

#### CustomerAccount
**Extends:** `IdentifiedObject`

Assignment of a group of products and services purchased by a customer.

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| document | Document | 0..1 | Basic account information |
| billingCycle | String256 | 0..1 | Billing cycle designation |
| budgetBill | String256 | 0..1 | Budget billing program code |
| lastBillAmount | Int48 | 0..1 | Amount of last bill in cents |
| notifications | AccountNotification | 0..* | Account notifications |
| contactInfo | Organisation | 0..1 | Contact information |
| **accountId** | String256 | 0..1 | Account identifier *(New in 4.1)* |

---

#### CustomerAgreement
**Extends:** `IdentifiedObject`

Agreement between customer and service supplier for services.

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| agreement | Agreement | 0..1 | Formal agreement details |
| loadMgmt | String256 | 0..1 | Load management code |
| isPrePay | xs:boolean | 0..1 | Prepay customer flag |
| shutOffDateTime | TimeType | 0..1 | Scheduled shutoff date/time |
| DemandResponseProgram | DemandResponseProgram | 0..* | Demand response programs |
| PricingStructures | PricingStructure | 0..* | Pricing structures |
| currency | Currency | 0..1 | Currency for monetary amounts |
| futureStatus | Status | 0..* | Future status changes |
| **agreementId** | String256 | 0..1 | Agreement identifier *(New in 4.1)* |

---

#### ServiceSupplier
**Extends:** `IdentifiedObject`

Organization that provides services to customers.

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| organisation | Organisation | 0..1 | Supplier's contact information |
| kind | SupplierKind | 0..1 | Type of supplier |
| issuerIdentificationNumber | String256 | 0..1 | IIN for payment cards |
| effectiveDate | TimeType | 0..1 | Date supplier became effective |

---

#### ServiceLocation
**Extends:** `IdentifiedObject`

Location of a customer's meter or service point.

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| location | Location | 0..1 | Location details |
| accessMethod | String256 | 0..1 | Method to access location |
| siteAccessProblem | String256 | 0..1 | Access problem description |
| needsInspection | xs:boolean | 0..1 | Inspection needed flag |
| UsagePoints | UsagePoints | 0..* | Associated usage points |
| **outageBlock** | String32 | 0..1 | Outage block identifier *(New in 4.1)* |

---

### 6.2 Device and Meter Types

#### EndDevice
**Extends:** `IdentifiedObject`

Asset that performs end device functions (metering, load control).

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| asset | Asset | 0..1 | Asset information |
| isVirtual | xs:boolean | 0..1 | Virtual device flag |
| isPan | xs:boolean | 0..1 | Part of PAN flag |
| installCode | String256 | 0..1 | Installation code |
| amrSystem | String256 | 0..1 | AMR system identifier |

---

#### Meter
**Extends:** `IdentifiedObject`

Physical meter device with associated multipliers.

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| endDevice | EndDevice | 0..1 | End device information |
| formNumber | String256 | 0..1 | Meter form number |
| MeterMultipliers | MeterMultiplier | 0..* | Meter multipliers |
| **intervalLength** | UInt32 | 0..1 | Interval length in seconds *(New in 4.1)* |

---

### 6.3 Program and Billing Types

#### DemandResponseProgram
**Type:** Complex (not extending IdentifiedObject)

Demand response program that customers can enroll in.

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| programName | String256 | 0..1 | Program name |
| enrollmentStatus | EnrollmentStatus | 0..1 | Enrollment status |
| programDescription | xs:anyURI | 0..1 | URI of program description |
| programDate | ProgramDate | 0..* | Program dates |
| capacityReservationLevel | SummaryMeasurement | 0..1 | Reserved capacity |
| DRProgramNomination | SummaryMeasurement | 0..1 | DR nomination |

---

#### PricingStructure
**Extends:** `IdentifiedObject`

Pricing structure for services.

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| document | Document | 0..1 | Document information |
| revenueKind | RevenueKind | 0..1 | Revenue type |
| code | String256 | 0..1 | Pricing code |
| dailyCeilingUsage | Int48 | 0..1 | Daily ceiling usage |
| dailyEstimatedUsage | Int48 | 0..1 | Daily estimated usage |
| dailyFloorUsage | Int48 | 0..1 | Daily floor usage |
| taxExemption | xs:boolean | 0..1 | Tax exemption flag |

---

#### Statement
**Extends:** `IdentifiedObject`

Billing statement for a customer account.

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| issueDateTime | TimeType | 0..1 | Statement issue date/time |
| statementRef | StatementRef | 0..* | Statement references |

---

#### ProgramDateIdMappings
**Extends:** `IdentifiedObject`

Container for program date ID mappings.

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| programDateIdMapping | ProgramDateIdMapping | 0..1 | Individual mapping |

---

### 6.4 Time Configuration

#### TimeConfiguration
**Extends:** `IdentifiedObject`

Time zone and DST configuration (also known as LocalTimeParameters).

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| dstEndRule | DstRuleType | 1 | DST end rule (hex encoded) |
| dstOffset | TimeType | 1 | DST offset in seconds |
| dstStartRule | DstRuleType | 1 | DST start rule (hex encoded) |
| tzOffset | TimeType | 1 | Timezone offset in seconds |

---

## 7. Complex Types (Supporting)

### 7.1 Base Types

#### Object
Base type for all ESPI objects.

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| (empty) | | | Base object with no elements |

---

#### IdentifiedObject
**Extends:** `Object`

Base type for identified resources.

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| batchItemInfo | BatchItemInfo | 0..1 | Batch processing info |
| name | name | 0..1 | Object name (deprecated) |

---

#### Document
**Extends:** `Object`

Parent type for document resources.

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| type | String256 | 0..1 | Document type |
| authorName | String256 | 0..1 | Author name |
| createdDateTime | TimeType | 0..1 | Creation timestamp |
| lastModifiedDateTime | TimeType | 0..1 | Last modification timestamp |
| revisionNumber | String256 | 0..1 | Revision number |
| electronicAddress | ElectronicAddress | 0..1 | Electronic address |
| subject | String256 | 0..1 | Document subject |
| title | String256 | 0..1 | Document title |
| docStatus | Status | 0..1 | Document status |
| status | Status | 0..1 | Subject matter status |
| comment | String256 | 0..1 | Free text comment |

---

#### Agreement
**Extends:** `Document`

Formal agreement for services.

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| signDate | TimeType | 0..1 | Date agreement was signed |
| validityInterval | DateTimeInterval | 0..1 | Validity period |

---

### 7.2 Organisation Types

#### Organisation
**Extends:** `Object`

Organization participating in the utility domain.

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| names | name | 0..* | Organization names |
| streetAddress | StreetAddress | 0..1 | Street address |
| postalAddress | StreetAddress | 0..1 | Postal address |
| phone1 | TelephoneNumber | 0..1 | Primary phone |
| phone2 | TelephoneNumber | 0..1 | Secondary phone |
| electronicAddress | ElectronicAddress | 0..1 | Electronic address |

---

### 7.3 Asset Types

#### Asset
**Extends:** `Object`

Physical asset owned by an organization.

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| type | String256 | 0..1 | Asset type |
| utcNumber | String256 | 0..1 | UTC asset number |
| serialNumber | String256 | 0..1 | Serial number |
| lotNumber | String256 | 0..1 | Lot number |
| purchasePrice | Int48 | 0..1 | Purchase price in cents |
| critical | xs:boolean | 0..1 | Critical asset flag |
| electronicAddress | ElectronicAddress | 0..1 | Electronic address |
| lifecycle | LifecycleDate | 0..1 | Lifecycle dates |
| acceptanceTest | AcceptanceTest | 0..1 | Acceptance test data |
| initialCondition | String256 | 0..1 | Initial condition |
| initialLossOfLife | PerCent | 0..1 | Initial loss of life |
| status | Status | 0..1 | Asset status |

**Note:** `AssetContainer` type is commented out in 4.1.

---

### 7.4 Location Types

#### Location
**Extends:** `Object`

Geographic location.

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| type | String256 | 0..1 | Location type |
| mainAddress | StreetAddress | 0..1 | Main address |
| secondaryAddress | StreetAddress | 0..1 | Secondary address |
| phone1 | TelephoneNumber | 0..1 | Primary phone |
| phone2 | TelephoneNumber | 0..1 | Secondary phone |
| electronicAddress | ElectronicAddress | 0..1 | Electronic address |
| status | Status | 0..1 | Location status |
| PositionPoints | PositionPoint | 0..* | Geographic coordinates |
| geoInfoReference | String256 | 0..1 | GIS reference |
| direction | String256 | 0..1 | Directions |

**Note:** `WorkLocation` type is commented out in 4.1.

---

### 7.5 Other Supporting Types

*(DateTimeInterval, Status, Priority, MeterMultiplier, AcceptanceTest, LifecycleDate, SummaryMeasurement, AccountNotification, ProgramDate, ProgramDateIdMapping, BatchItemInfo, BatchListType, StatementRef, StreetAddress, StreetDetail, TownDetail, TelephoneNumber, ElectronicAddress, PositionPoint, UsagePoint, UsagePoints)*

These types remain consistent with ESPI 4.0 - see full schema for details.

---

## 8. Global Elements

The customer_4.1.xsd schema defines global elements for all resource and supporting types.

### Resource Elements
- Customer, CustomerAccount, CustomerAgreement
- EndDevice, Meter
- ServiceLocation, ServiceSupplier
- Statement, ProgramDateIdMappings
- PricingStructure, DemandResponseProgram
- TimeConfiguration, LocalTimeParameters
- UsagePoint, UsagePoints

### Supporting Type Elements
- AcceptanceTest, AccountNotification, Agreement, Asset
- BatchItemInfo, BatchList, DateTimeInterval, Document
- ElectronicAddress, IdentifiedObject, LifecycleDate, Location
- MeterMultiplier, Object, Organisation, PositionPoint
- Priority, ProgramDate, ProgramDateIdMapping, Status
- StatementRef, StreetAddress, StreetDetail, SummaryMeasurement
- TelephoneNumber, TownDetail

### Elements Commented Out in 4.1
- `AssetContainer`
- `OrganisationRole`
- `WorkLocation`

---

## 9. Changes from ESPI 4.0

### 9.1 New Fields Added

| Resource | New Field | Type | Description |
|----------|-----------|------|-------------|
| Customer | customerName | String256 | Customer name |
| CustomerAccount | accountId | String256 | Account identifier |
| CustomerAgreement | agreementId | String256 | Agreement identifier |
| Meter | intervalLength | UInt32 | Interval length in seconds |
| ServiceLocation | outageBlock | String32 | Outage block identifier |

### 9.2 Types Commented Out/Removed

| Type | Status | Impact |
|------|--------|--------|
| AssetContainer | Commented out | EndDevice uses Asset composition instead |
| OrganisationRole | Commented out | No longer used |
| WorkLocation | Commented out | ServiceLocation uses Location composition instead |

### 9.3 Structural Changes

| Resource | Change |
|----------|--------|
| EndDevice | Now contains `Asset` element instead of extending AssetContainer |
| ServiceLocation | Now contains `Location` element instead of extending WorkLocation |
| Customer | Now contains `Organisation` element for contact info |
| CustomerAccount | Now contains `Document` element for basic info |
| CustomerAgreement | Now contains `Agreement` element for formal agreement |

### 9.4 Enumeration Types (No Changes)

The enumeration base types are **consistent** between ESPI 4.0 and 4.1:

| Enumeration | Base Type | Value Format |
|-------------|-----------|--------------|
| CRUDOperation | UInt16 | Numeric (0, 1, 2, 3) |
| CustomerKind | xs:string | String ("residential", etc.) |
| EnrollmentStatus | xs:string | String ("enrolled", etc.) |
| SupplierKind | xs:string | String ("utility", etc.) |
| ServiceKind | xs:string | String ("electricity", etc.) |
| RevenueKind | xs:string | String ("residentialSales", etc.) |
| NotificationMethodKind | UInt16 | Numeric (0, 1, 2, 3) |
| MeterMultiplierKind | UInt16 | Numeric (0, 1, 2, 3, 4, 5) |
| UnitMultiplierKind | xs:string | String ("k", "M", "G", etc.) |
| UnitSymbolKind | xs:string | String ("W", "Wh", "V", etc.) |
| Currency | xs:string | String ("USD", "EUR", etc.) |

---

## Summary Statistics

| Category | Count |
|----------|-------|
| Basic Types (Primitives) | 8 |
| String Types | 5 |
| Hex Binary Types | 4 |
| Special Types | 2 |
| Enumeration Types | 15 |
| Complex Types (Resources) | 14 |
| Complex Types (Supporting) | 26+ |
| Global Elements | 38+ |

---

## Implementation Notes

### Java Enum Mapping

For customer.xsd enumerations, use the following patterns:

**Numeric Enums (UInt16):**
```java
@XmlEnum
public enum CRUDOperation {
    @XmlEnumValue("0") CREATE(0),
    @XmlEnumValue("1") READ(1),
    @XmlEnumValue("2") UPDATE(2),
    @XmlEnumValue("3") DELETE(3);

    private final int value;
    // ...
}
```

**String Enums (xs:string):**
```java
@XmlEnum
public enum CustomerKind {
    @XmlEnumValue("residential")
    RESIDENTIAL("residential"),

    @XmlEnumValue("commercialIndustrial")
    COMMERCIAL_INDUSTRIAL("commercialIndustrial");

    private final String value;
    // ...
}
```

### Composition Pattern

ESPI 4.1 uses composition instead of inheritance for some types:
- `EndDevice` contains `Asset` instead of extending `AssetContainer`
- `ServiceLocation` contains `Location` instead of extending `WorkLocation`
- `Customer` contains `Organisation` for contact information

---

*Generated from NAESB REQ.21 ESPI Version 4.1 (Pending Publication) customer_4.1.xsd schema*
