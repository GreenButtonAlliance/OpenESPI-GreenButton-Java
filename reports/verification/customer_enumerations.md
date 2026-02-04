# ESPI 4.0 Customer Schema Analysis Report

**Schema Version:** NAESB REQ.21 ESPI Version 4.0.20231215
**Published:** December 15, 2023
**Namespace:** `http://naesb.org/espi/customer`

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
| `PerCent` | `xs:float` | Percentage value | 0.0 to 100.0 |

---

## 2. String Types

| Type Name | Base Type | Max Length | Description |
|-----------|-----------|------------|-------------|
| `String32` | `xs:string` | 32 | Character string of max length 32 |
| `String64` | `xs:string` | 64 | Character string of max length 64 |
| `String256` | `xs:string` | 256 | Character string of max length 256 |
| `String512` | `xs:string` | 512 | Character string of max length 512 |
| `name` | `xs:normalizedString` | 64 | Normalized name string with pattern `[\s\S]*` |

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
**Base:** `UInt16` (union)

| Value | Name | Description |
|-------|------|-------------|
| 0 | Created | Resource was created |
| 1 | Deleted | Resource was deleted |
| 2 | Updated | Resource was updated |
| 3 | Unspecified | Operation unspecified |

---

#### EnrollmentStatus
**Base:** `UInt16` (union)

| Value | Name | Description |
|-------|------|-------------|
| 0 | Enrolled | Customer is enrolled |
| 1 | EnrollmentPending | Enrollment is pending |
| 2 | UnEnrolled | Customer is not enrolled |
| 3 | UnEnrolledPending | Unenrollment is pending |
| 4 | Enrolling | Customer is in process of enrolling |

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
**Base:** `UInt16` (union)

| Value | Name | Description |
|-------|------|-------------|
| 0 | energyServiceScheduler | Energy service scheduler customer |
| 1 | energyServiceSupplier | Energy service supplier customer |
| 2 | other | Other customer type |
| 3 | commercialIndustrial | Commercial or industrial customer |
| 4 | internalUse | Internal use customer |
| 5 | pumpingLoad | Pumping load customer |
| 6 | residential | Residential customer |
| 7 | residentialAndCommercial | Residential and commercial customer |
| 8 | residentialAndStreetlight | Residential and streetlight customer |
| 9 | residentialFarmService | Residential farm service customer |
| 10 | residentialStreetlightOthers | Residential streetlight and others |
| 11 | streetLight | Streetlight customer |
| 12 | energyServiceConsumer | Energy service consumer |
| 13 | gridOperator | Grid operator |
| 14 | windMachine | Wind machine customer |

---

#### SupplierKind
**Base:** `UInt16` (union)

| Value | Name | Description |
|-------|------|-------------|
| 0 | utility | Traditional utility supplier |
| 1 | municipality | Municipal utility |
| 2 | privateEnterprise | Private enterprise supplier |
| 3 | cooperative | Cooperative utility |
| 4 | retailer | Retail energy supplier |
| 5 | other | Other supplier type |

---

#### ServiceKind
**Base:** `UInt16` (union)

| Value | Name | Description |
|-------|------|-------------|
| 0 | electricity | Electric service |
| 1 | gas | Natural gas service |
| 2 | water | Water service |
| 3 | time | Time service |
| 4 | heat | Heat service |
| 5 | refuse | Refuse/waste service |
| 6 | sewerage | Sewerage service |
| 7 | rates | Rates information |
| 8 | tvLicense | TV license service |
| 9 | internet | Internet service |
| 10 | other | Other service type |

---

#### RevenueKind
**Base:** `UInt16` (union)

| Value | Name | Description |
|-------|------|-------------|
| 0 | fees | Revenue from fees |
| 1 | rates | Revenue from rates |
| 2 | cancellationFees | Revenue from cancellation fees |
| 3 | lateFees | Revenue from late fees |
| 4 | connectorFees | Revenue from connector fees |
| 5 | rebates | Rebates (negative revenue) |
| 6 | other | Other revenue type |

---

### 5.3 Notification and Communication Enumerations

#### NotificationMethodKind
**Base:** `UInt16` (union)

| Value | Name | Description |
|-------|------|-------------|
| 0 | email | Notification via email |
| 1 | inPerson | In-person notification |
| 2 | phone | Notification via phone call |
| 3 | postal | Notification via postal mail |
| 4 | sms | Notification via SMS text message |

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
**Base:** `UInt16` (union)

| Value | Name | Description |
|-------|------|-------------|
| 0 | signUp | Sign-up date |
| 1 | cancel | Cancellation date |
| 2 | enroll | Enrollment date |
| 3 | deferred | Deferred date |

---

### 5.5 Meter and Measurement Enumerations

#### MeterMultiplierKind
**Base:** `UInt16` (union)

| Value | Name | Description |
|-------|------|-------------|
| 0 | kH | Test dial multiplier |
| 1 | kE | Element test multiplier |
| 2 | kP | Potential transformer multiplier |
| 3 | kC | Current transformer multiplier |
| 4 | kR | Register multiplier |
| 5 | transformerRatio | Transformer ratio |

---

#### UnitMultiplierKind
**Base:** `Int16` (union)

SI unit multipliers as powers of 10.

| Value | Name | Symbol | Multiplier |
|-------|------|--------|------------|
| -12 | pico | p | 10^-12 |
| -9 | nano | n | 10^-9 |
| -6 | micro | µ | 10^-6 |
| -3 | milli | m | 10^-3 |
| -2 | centi | c | 10^-2 |
| -1 | deci | d | 10^-1 |
| 0 | none | (none) | 10^0 = 1 |
| 1 | deca | da | 10^1 |
| 2 | hecto | h | 10^2 |
| 3 | kilo | k | 10^3 |
| 6 | mega | M | 10^6 |
| 9 | giga | G | 10^9 |
| 12 | tera | T | 10^12 |
| 15 | peta | P | 10^15 |
| 18 | exa | E | 10^18 |
| 21 | yotta | Y | 10^21 |

---

#### UnitSymbolKind
**Base:** `UInt16` (union)

Unit of measurement symbols. Comprehensive list of 100+ unit types.

##### SI Base Units
| Value | Symbol | Description |
|-------|--------|-------------|
| 5 | A | Ampere - Electric current |
| 2 | m | Meter - Length |
| 6 | K | Kelvin - Temperature |
| 7 | mol | Mole - Amount of substance |
| 8 | cd | Candela - Luminous intensity |
| 1 | s | Second - Time |

##### Power and Energy
| Value | Symbol | Description |
|-------|--------|-------------|
| 38 | W | Watt - Real power |
| 61 | VA | Volt-ampere - Apparent power |
| 63 | VAr | Volt-ampere reactive - Reactive power |
| 64 | Varh | Volt-ampere-reactive hour - Reactive energy |
| 72 | Wh | Watt-hour - Real energy |
| 71 | VAh | Volt-ampere-hour - Apparent energy |
| 132 | btu | BTU - British Thermal Units |
| 133 | btuPerH | BTU/h - BTU per hour |
| 169 | therm | Therm - Energy |

##### Electrical
| Value | Symbol | Description |
|-------|--------|-------------|
| 29 | V | Volt - Electric potential |
| 30 | ohm | Ohm - Resistance |
| 31 | F | Farad - Capacitance |
| 32 | H | Henry - Inductance |
| 33 | S | Siemens - Conductance |
| 69 | A2 | Ampere squared |
| 105 | A2h | Ampere-squared hour |
| 106 | Ah | Ampere-hour |
| 67 | V2 | Volt squared |
| 104 | V2h | Volt-squared hour |

##### Frequency and Angular
| Value | Symbol | Description |
|-------|--------|-------------|
| 27 | Hz | Hertz - Frequency |
| 10 | rad | Radian - Plane angle |
| 11 | sr | Steradian - Solid angle |
| 54 | radPerSec | Radians per second - Angular velocity |
| 75 | HzPerSec | Hertz per second - Rate of change of frequency |
| 154 | rev | Revolutions |
| 4 | revPerSec | Revolutions per second |

##### Force, Pressure, and Mass
| Value | Symbol | Description |
|-------|--------|-------------|
| 3 | kg | Kilogram - Mass |
| 25 | N | Newton - Force |
| 26 | Pa | Pascal - Pressure |
| 140 | paG | Pascal gauge pressure |
| 155 | paA | Pascal absolute pressure |
| 141 | psiA | PSI absolute |
| 142 | psiG | PSI gauge |
| 23 | J | Joule - Energy/Work |
| 165 | jPerKg | Joules per kilogram |

##### Volume and Flow
| Value | Symbol | Description |
|-------|--------|-------------|
| 41 | m2 | Square meter - Area |
| 42 | m3 | Cubic meter - Volume |
| 119 | ft3 | Cubic feet |
| 120 | ft3compensated | Cubic feet compensated |
| 128 | usGal | US gallons |
| 130 | imperialGal | Imperial gallons |
| 134 | litre | Litre |
| 156 | litreUncompensated | Litre uncompensated |
| 157 | litreCompensated | Litre compensated |
| 45 | m3PerSec | Cubic meters per second |
| 125 | m3PerH | Cubic meters per hour |
| 82 | litrePerSec | Litres per second |
| 137 | litrePerH | Litres per hour |

##### Power Factor and Ratios
| Value | Symbol | Description |
|-------|--------|-------------|
| 65 | cosTheta | Power factor (cos θ) |
| 153 | WPerVA | Power factor (W/VA) |
| 148 | mPerM | Ratio of length |
| 150 | HzPerHz | Ratio of frequency |
| 151 | VPerV | Ratio of voltages |
| 152 | APerA | Ratio of current |
| 168 | WPerW | Ratio of power |

##### Temperature and Thermal
| Value | Symbol | Description |
|-------|--------|-------------|
| 23 | degC | Degree Celsius |
| 50 | wPerMK | Thermal conductivity (W/m·K) |
| 51 | jPerK | Heat capacity (J/K) |

##### Magnetic
| Value | Symbol | Description |
|-------|--------|-------------|
| 36 | wb | Weber - Magnetic flux |
| 37 | t | Tesla - Magnetic flux density |

##### Miscellaneous
| Value | Symbol | Description |
|-------|--------|-------------|
| 0 | none | Not applicable |
| 76 | char | Characters |
| 77 | charPerSec | Characters per second |
| 80 | money | Generic monetary unit |
| 108 | timeStamp | ISO 8601 timestamp |
| 109 | status | Status (boolean-like) |
| 111 | count | Counter value |
| 114 | code | Encoded application value |
| 118 | meCode | EndDeviceEventCode |

##### Metering Constants
| Value | Symbol | Description |
|-------|--------|-------------|
| 115 | WhPerRev | Active energy metering constant |
| 116 | VArhPerRev | Reactive energy metering constant |
| 117 | VAhPerRev | Apparent energy metering constant |
| 107 | WhPerM3 | Energy per volume |

---

#### Currency
**Base:** `UInt16` (union)

ISO 4217 currency codes (numeric). Selected common values:

| Value | Code | Description |
|-------|------|-------------|
| 36 | AUD | Australian Dollar |
| 124 | CAD | Canadian Dollar |
| 156 | CNY | Chinese Yuan |
| 208 | DKK | Danish Krone |
| 978 | EUR | Euro |
| 826 | GBP | British Pound Sterling |
| 344 | HKD | Hong Kong Dollar |
| 356 | INR | Indian Rupee |
| 392 | JPY | Japanese Yen |
| 484 | MXN | Mexican Peso |
| 578 | NOK | Norwegian Krone |
| 554 | NZD | New Zealand Dollar |
| 752 | SEK | Swedish Krona |
| 756 | CHF | Swiss Franc |
| 840 | USD | United States Dollar |

---

## 6. Complex Types (Resources)

### 6.1 Core Resource Types

#### Customer
**Extends:** `Organisation`

Organization receiving services from a utility.

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| kind | CustomerKind | 0..1 | Type of customer |
| specialNeed | String256 | 0..1 | Special needs of customer |
| vip | xs:boolean | 0..1 | VIP status flag |
| pucNumber | String256 | 0..1 | Public utility commission number |
| status | Status | 0..1 | Customer status |
| locale | String32 | 0..1 | Customer locale |

---

#### CustomerAccount
**Extends:** `Document`

Assignment of a group of products and services purchased by a customer.

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| accountId | String64 | 0..1 | Legacy account identifier |
| billingCycle | String32 | 0..1 | Billing cycle designation |
| budgetBill | String32 | 0..1 | Budget billing program code |
| lastBillAmount | Int48 | 0..1 | Amount of last bill in cents |
| contactInfo | String512 | 0..1 | Contact information |
| notifications | AccountNotification | 0..* | Account notifications |

---

#### CustomerAgreement
**Extends:** `Agreement`

Agreement between customer and service supplier for services.

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| loadMgmt | String256 | 0..1 | Load management code |
| isPrePay | xs:boolean | 0..1 | Prepay customer flag |
| shutOffDateTime | TimeType | 0..1 | Scheduled shutoff date/time |
| demandResponseProgram | DemandResponseProgram | 0..* | Demand response programs |
| pricingStructure | PricingStructure | 0..* | Pricing structures |
| status | Status | 0..1 | Agreement status |

---

#### ServiceSupplier
**Extends:** `Organisation`

Organization that provides services to customers.

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| kind | SupplierKind | 0..1 | Type of supplier |
| issuerIdentificationNumber | String64 | 0..1 | IIN for payment cards |
| effectiveDate | TimeType | 0..1 | Date supplier became effective |

---

#### ServiceLocation
**Extends:** `WorkLocation`

Location of a customer's meter or service point.

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| accessMethod | String256 | 0..1 | Method to access location |
| needsInspection | xs:boolean | 0..1 | Inspection needed flag |
| siteAccessProblem | String256 | 0..1 | Access problem description |
| usagePoint | UsagePoint | 0..* | Associated usage points |

---

### 6.2 Device and Meter Types

#### EndDevice
**Extends:** `AssetContainer`

Asset that performs end device functions (metering, load control).

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| amrSystem | String256 | 0..1 | AMR system identifier |
| installCode | String256 | 0..1 | Installation code |
| isPan | xs:boolean | 0..1 | Part of PAN flag |
| isSmartInverter | xs:boolean | 0..1 | Smart inverter flag |
| isVirtual | xs:boolean | 0..1 | Virtual device flag |
| timeZoneOffset | Int16 | 0..1 | Timezone offset in minutes |

---

#### Meter
**Extends:** `EndDevice`

Physical meter device with associated multipliers.

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| formNumber | String256 | 0..1 | Meter form number |
| meterMultipliers | MeterMultiplier | 0..* | Meter multipliers |

---

### 6.3 Program and Billing Types

#### DemandResponseProgram
**Extends:** `IdentifiedObject`

Demand response program that customers can enroll in.

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| availabilityDate | DateTimeInterval | 0..1 | Availability period |
| programName | String256 | 0..1 | Program name |
| programDescription | String512 | 0..1 | Program description |
| enrollmentStatus | EnrollmentStatus | 0..1 | Enrollment status |
| capacityReservationLevel | SummaryMeasurement | 0..1 | Reserved capacity |
| DRProgramMandatoryLevel | SummaryMeasurement | 0..1 | Mandatory participation level |
| programDates | ProgramDate | 0..* | Program dates |

---

#### PricingStructure
**Extends:** `Document`

Pricing structure for services.

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| code | String32 | 0..1 | Pricing code |
| revenueKind | RevenueKind | 0..1 | Revenue type |
| taxExemption | xs:boolean | 0..1 | Tax exemption flag |
| dailyFloorUsage | Int48 | 0..1 | Daily floor usage |
| dailyCeilingUsage | Int48 | 0..1 | Daily ceiling usage |
| dailyEstimatedUsage | Int48 | 0..1 | Daily estimated usage |

---

#### Statement
**Extends:** `Document`

Billing statement for a customer account.

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| statementDate | TimeType | 0..1 | Statement date |
| dueDate | TimeType | 0..1 | Payment due date |
| amountDue | Int48 | 0..1 | Amount due in cents |
| previousBalance | Int48 | 0..1 | Previous balance |
| currentBalance | Int48 | 0..1 | Current balance |

---

#### ProgramDateIdMappings
**Extends:** `IdentifiedObject`

Container for program date ID mappings.

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| programDateIdMapping | ProgramDateIdMapping | 0..* | Individual mappings |

---

### 6.4 Time Configuration

#### TimeConfiguration
**Extends:** `IdentifiedObject`

Time zone and DST configuration (also known as LocalTimeParameters).

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| dstEndRule | DstRuleType | 1 | DST end rule (hex encoded) |
| dstOffset | Int48 | 1 | DST offset in seconds |
| dstStartRule | DstRuleType | 1 | DST start rule (hex encoded) |
| tzOffset | Int48 | 1 | Timezone offset in seconds |

---

### 6.5 Usage Point Types

#### UsagePoint
**Extends:** `IdentifiedObject`

Logical point on the network for measurement (reference to usage.xsd).

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| roleFlags | HexBinary16 | 0..1 | Role flags |
| serviceCategory | ServiceKind | 0..1 | Service category |
| status | UInt16 | 0..1 | Status |
| ServiceDeliveryPoint | ServiceDeliveryPoint | 0..1 | Service delivery point |

---

#### UsagePoints
**Type:** Complex (list container)

Container for multiple usage points.

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| usagePoint | UsagePoint | 0..* | List of usage points |

---

## 7. Complex Types (Supporting)

### 7.1 Base Types

#### Object
Base type for all ESPI objects.

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| extension | String512 | 0..1 | Extension data |

---

#### IdentifiedObject
**Extends:** `Object`

Base type for identified resources.

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| description | String512 | 0..1 | Description |

---

#### Document
**Extends:** `IdentifiedObject`

Parent type for document resources.

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| createdDateTime | TimeType | 0..1 | Creation timestamp |
| lastModifiedDateTime | TimeType | 0..1 | Last modification timestamp |

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
**Extends:** `IdentifiedObject`

Organization participating in the utility domain.

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| phone1 | TelephoneNumber | 0..1 | Primary phone |
| phone2 | TelephoneNumber | 0..1 | Secondary phone |
| streetAddress | StreetAddress | 0..1 | Street address |
| postalAddress | StreetAddress | 0..1 | Postal address |
| electronicAddress | ElectronicAddress | 0..1 | Electronic address |

---

#### OrganisationRole
**Extends:** `IdentifiedObject`

Role played by an organization.

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| organisation | Organisation | 0..1 | Associated organization |

---

### 7.3 Asset Types

#### Asset
**Extends:** `IdentifiedObject`

Physical asset owned by an organization.

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| utcNumber | String256 | 0..1 | UTC asset number |
| serialNumber | String256 | 0..1 | Serial number |
| lotNumber | String256 | 0..1 | Lot number |
| purchasePrice | Int48 | 0..1 | Purchase price in cents |
| lifecycle | LifecycleDate | 0..1 | Lifecycle dates |
| acceptanceTest | AcceptanceTest | 0..1 | Acceptance test data |

---

#### AssetContainer
**Extends:** `Asset`

Asset that contains other assets.

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| (inherits from Asset) | | | |

---

### 7.4 Location Types

#### Location
**Extends:** `IdentifiedObject`

Geographic location.

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| type | String256 | 0..1 | Location type |
| mainAddress | StreetAddress | 0..1 | Main address |
| phone1 | TelephoneNumber | 0..1 | Primary phone |
| phone2 | TelephoneNumber | 0..1 | Secondary phone |
| secondaryAddress | StreetAddress | 0..1 | Secondary address |
| electronicAddress | ElectronicAddress | 0..1 | Electronic address |
| geoInfoReference | String256 | 0..1 | GIS reference |
| direction | String256 | 0..1 | Directions |
| status | Status | 0..1 | Location status |
| positionPoints | PositionPoint | 0..* | Geographic coordinates |

---

#### WorkLocation
**Extends:** `Location`

Location for work purposes.

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| oneCallContact | String256 | 0..1 | One-call contact info |

---

#### PositionPoint
Geographic coordinate point.

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| xPosition | String32 | 0..1 | X coordinate (longitude) |
| yPosition | String32 | 0..1 | Y coordinate (latitude) |
| zPosition | String32 | 0..1 | Z coordinate (elevation) |

---

### 7.5 Address Types

#### StreetAddress
Postal street address.

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| streetDetail | StreetDetail | 0..1 | Street details |
| townDetail | TownDetail | 0..1 | Town details |
| poBox | String256 | 0..1 | PO Box number |
| postalCode | String256 | 0..1 | Postal code |
| status | Status | 0..1 | Address status |

---

#### StreetDetail
Street address details.

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| addressGeneral | String256 | 0..1 | General address |
| addressGeneral2 | String256 | 0..1 | Additional address |
| addressGeneral3 | String256 | 0..1 | Additional address |
| buildingName | String256 | 0..1 | Building name |
| code | String64 | 0..1 | Street code |
| name | String256 | 0..1 | Street name |
| number | String64 | 0..1 | Street number |
| prefix | String64 | 0..1 | Street prefix |
| suffix | String64 | 0..1 | Street suffix |
| suiteNumber | String64 | 0..1 | Suite number |
| type | String64 | 0..1 | Street type |
| withinTownLimits | xs:boolean | 0..1 | Within town limits flag |
| floorIdentification | String32 | 0..1 | Floor identifier |

---

#### TownDetail
Town/city address details.

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| code | String64 | 0..1 | Town code |
| country | String256 | 0..1 | Country |
| name | String256 | 0..1 | Town name |
| section | String256 | 0..1 | Town section |
| stateOrProvince | String256 | 0..1 | State or province |

---

### 7.6 Contact Types

#### TelephoneNumber
Telephone contact information.

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| areaCode | String32 | 0..1 | Area code |
| cityCode | String32 | 0..1 | City code |
| countryCode | String32 | 0..1 | Country code |
| extension | String32 | 0..1 | Extension |
| localNumber | String32 | 0..1 | Local number |
| dialOut | String32 | 0..1 | Dial-out prefix |
| ituPhone | String32 | 0..1 | ITU phone number |
| internationalPrefix | String32 | 0..1 | International prefix |

---

#### ElectronicAddress
Electronic contact information.

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| email1 | String256 | 0..1 | Primary email |
| email2 | String256 | 0..1 | Secondary email |
| lan | String256 | 0..1 | LAN address |
| mac | String256 | 0..1 | MAC address |
| radio | String256 | 0..1 | Radio address |
| userID | String256 | 0..1 | User ID |
| web | String256 | 0..1 | Web address |
| password | String256 | 0..1 | Password |

---

### 7.7 Other Supporting Types

#### DateTimeInterval
Time interval with start and end.

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| start | TimeType | 0..1 | Start time |
| end | TimeType | 0..1 | End time |
| duration | UInt32 | 0..1 | Duration in seconds |

---

#### Status
General status information.

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| value | String256 | 0..1 | Status value |
| dateTime | TimeType | 0..1 | Status timestamp |
| reason | String256 | 0..1 | Status reason |
| remark | String512 | 0..1 | Additional remarks |

---

#### Priority
Priority information.

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| justification | String512 | 0..1 | Priority justification |
| rank | UInt32 | 0..1 | Priority rank |
| type | String64 | 0..1 | Priority type |

---

#### MeterMultiplier
Meter multiplier values.

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| kind | MeterMultiplierKind | 0..1 | Multiplier type |
| value | xs:float | 0..1 | Multiplier value |

---

#### AcceptanceTest
Asset acceptance test data.

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| dateTime | TimeType | 0..1 | Test date/time |
| success | xs:boolean | 0..1 | Test success flag |
| type | String256 | 0..1 | Test type |

---

#### LifecycleDate
Asset lifecycle dates.

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| installationDate | TimeType | 0..1 | Installation date |
| manufacturedDate | TimeType | 0..1 | Manufacture date |
| purchaseDate | TimeType | 0..1 | Purchase date |
| receivedDate | TimeType | 0..1 | Received date |
| removalDate | TimeType | 0..1 | Removal date |
| retiredDate | TimeType | 0..1 | Retirement date |

---

#### SummaryMeasurement
Summary measurement value.

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| value | Int48 | 0..1 | Measurement value |
| powerOfTenMultiplier | UnitMultiplierKind | 0..1 | Power of 10 multiplier |
| uom | UnitSymbolKind | 0..1 | Unit of measure |

---

#### AccountNotification
Account notification record.

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| methodKind | NotificationMethodKind | 0..1 | Notification method |
| note | String512 | 0..1 | Notification content |
| time | TimeType | 0..1 | Notification time |
| customerNotificationKind | String256 | 0..1 | Customer notification type |

---

#### ProgramDate
Date associated with a program.

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| programDateKind | ProgramDateKind | 0..1 | Date type |
| date | TimeType | 0..1 | Date value |

---

#### ProgramDateIdMapping
Mapping of program date to identifier.

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| programDate | ProgramDate | 0..1 | Program date |
| id | String256 | 0..1 | Identifier |
| programDescription | String512 | 0..1 | Program description |

---

#### BatchItemInfo
Batch operation item information.

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| name | String256 | 0..1 | Item name |
| operation | CRUDOperation | 0..1 | CRUD operation |
| statusCode | StatusCode | 0..1 | Status code |
| statusReason | String512 | 0..1 | Status reason |

---

#### BatchListType
List of batch items.

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| resource | String512 | 0..* | Resource URIs |

---

#### StatementRef
Reference to a statement.

| Element | Type | Occurrence | Description |
|---------|------|------------|-------------|
| href | String512 | 0..1 | Statement reference URI |

---

## 8. Global Elements

The customer.xsd schema defines 45 global elements:

### Resource Elements
| Element | Type | Description |
|---------|------|-------------|
| Customer | Customer | Customer resource |
| CustomerAccount | CustomerAccount | Customer account resource |
| CustomerAgreement | CustomerAgreement | Customer agreement resource |
| EndDevice | EndDevice | End device resource |
| Meter | Meter | Meter device resource |
| ServiceLocation | ServiceLocation | Service location resource |
| ServiceSupplier | ServiceSupplier | Service supplier resource |
| Statement | Statement | Billing statement resource |
| ProgramDateIdMappings | ProgramDateIdMappings | Program date mappings resource |
| DemandResponseProgram | DemandResponseProgram | DR program resource |
| PricingStructure | PricingStructure | Pricing structure resource |
| UsagePoint | UsagePoint | Usage point resource |
| UsagePoints | UsagePoints | Usage points list |
| TimeConfiguration | TimeConfiguration | Time configuration |
| LocalTimeParameters | TimeConfiguration | Local time parameters (alias) |

### Supporting Type Elements
| Element | Type | Description |
|---------|------|-------------|
| AcceptanceTest | AcceptanceTest | Acceptance test data |
| AccountNotification | AccountNotification | Account notification |
| Agreement | Agreement | Agreement base type |
| Asset | Asset | Asset base type |
| AssetContainer | AssetContainer | Asset container |
| BatchItemInfo | BatchItemInfo | Batch item info |
| BatchList | BatchListType | Batch list |
| DateTimeInterval | DateTimeInterval | Time interval |
| Document | Document | Document base type |
| ElectronicAddress | ElectronicAddress | Electronic address |
| IdentifiedObject | IdentifiedObject | Identified object base |
| LifecycleDate | LifecycleDate | Lifecycle dates |
| Location | Location | Location base type |
| MeterMultiplier | MeterMultiplier | Meter multiplier |
| Object | Object | Object base type |
| Organisation | Organisation | Organisation type |
| OrganisationRole | OrganisationRole | Organisation role |
| PositionPoint | PositionPoint | Geographic point |
| Priority | Priority | Priority info |
| ProgramDate | ProgramDate | Program date |
| ProgramDateIdMapping | ProgramDateIdMapping | Program date mapping |
| Status | Status | Status info |
| StatementRef | StatementRef | Statement reference |
| StreetAddress | StreetAddress | Street address |
| StreetDetail | StreetDetail | Street details |
| SummaryMeasurement | SummaryMeasurement | Summary measurement |
| TelephoneNumber | TelephoneNumber | Phone number |
| TownDetail | TownDetail | Town details |
| WorkLocation | WorkLocation | Work location |

---

## Summary Statistics

| Category | Count |
|----------|-------|
| Basic Types (Primitives) | 8 |
| String Types | 5 |
| Hex Binary Types | 4 |
| Special Types | 2 |
| Enumeration Types | 16 |
| Complex Types (Resources) | 15 |
| Complex Types (Supporting) | 30 |
| Global Elements | 45 |

---

*Generated from NAESB REQ.21 ESPI Version 4.0.20231215 customer.xsd schema*
