# ESPI 4.0 Schema Analysis Report

**Schema Version:** NAESB REQ.21 ESPI Version 4.0.20231213
**Published:** December 13, 2023
**Namespace:** `http://naesb.org/espi`

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

---

## 2. String Types

| Type Name | Base Type | Max Length | Description |
|-----------|-----------|------------|-------------|
| `String4` | `xs:string` | 4 | Character string of max length 4 |
| `String8` | `xs:string` | 8 | Character string of max length 8 |
| `String16` | `xs:string` | 16 | Character string of max length 16 |
| `String32` | `xs:string` | 32 | Character string of max length 32 |
| `String64` | `xs:string` | 64 | Character string of max length 64 |
| `String256` | `xs:string` | 256 | Character string of max length 256 |
| `String512` | `xs:string` | 512 | Character string of max length 512 [extension] |

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
| `DstRuleType` | `HexBinary32` | N/A | Bit-encoded DST rule (see detailed specification) |

---

## 5. Enumeration Types

### 5.1 OAuth Enumerations

#### TokenEndPointMethod
**Base:** `String32`

| Value | Description |
|-------|-------------|
| `client_secret_basic` | HTTP Basic authentication (RFC 6749 Section 2.3.1) |

#### GrantType
**Base:** `String32`

| Value | Description |
|-------|-------------|
| `authorization_code` | OAuth 2.0 Authorization Code Grant flow (RFC 6749 Section 4.1) |
| `client_credentials` | OAuth 2.0 Client Credentials Grant flow (RFC 6749 Section 4.4) |
| `refresh_token` | OAuth 2.0 Refresh Token flow (RFC 6749 Section 6) |

#### ResponseType
**Base:** `String32`

| Value | Description |
|-------|-------------|
| `code` | Request for authorization code (RFC 6749 Section 4.1.1) |

#### TokenType
**Base:** `String32`

| Value | Description |
|-------|-------------|
| `Bearer` | Bearer token (RFC6750 Section 1.2) |

#### OAuthError
**Base:** `String32`

| Value | Description |
|-------|-------------|
| `invalid_request` | Missing/unsupported parameter, malformed request |
| `invalid_client` | Client authentication failed |
| `invalid_grant` | Invalid/expired authorization code or refresh token |
| `unauthorized_client` | Client not authorized for grant type |
| `unsupported_grant_type` | Grant type not supported |
| `invalid_scope` | Invalid/unknown/malformed scope |
| `invalid_redirect_uri` | Invalid redirection URI |
| `invalid_client_metadata` | Invalid client metadata field |
| `invalid_client_id` | [DEPRECATED] Client authentication failed |
| `access_denied` | Resource owner denied request |
| `unsupported_response_type` | Response type not supported |
| `server_error` | Unexpected server error |
| `temporarily_unavailable` | Server temporarily unavailable |

---

### 5.2 ESPI Enumerations (Numeric)

#### ItemKind
**Base:** `UInt16` (union)

| Value | Name | Description |
|-------|------|-------------|
| 1 | Energy Generation Fee | A charge for generation of energy |
| 2 | Energy Delivery Fee | A charge for delivery of energy |
| 3 | Energy Usage Fee | A charge for electricity, natural gas, water consumption |
| 4 | Administrative Fee | A fee for administrative services |
| 5 | Tax | A local, state, or federal energy tax |
| 6 | Energy Generation Credit | A credit, discount or rebate for generation |
| 7 | Energy Delivery Credit | A credit, discount or rebate for delivery |
| 8 | Administrative Credit | A credit, discount or rebate for administrative services |
| 9 | Payment | A payment for previous billing |
| 10 | Information | An informational line item |

---

#### AccumulationKind
**Base:** `UInt16` (union)

| Value | Name | Description |
|-------|------|-------------|
| 0 | none | Not Applicable or implied by unit of measure |
| 1 | bulkQuantity | Bulk quantity from register (dial reading) |
| 2 | continuousCumulative | Sum of previous + present period values |
| 3 | cumulative | Sum of previous billing period values |
| 4 | deltaData | Difference between end and beginning of interval |
| 6 | indicating | Needle swing value (measured over hundreds of ms) |
| 9 | summation | Selective accumulation with respect to time |
| 10 | timeDelay | Computation with time delay characteristic |
| 12 | instantaneous | Measured over fastest period (milliseconds) |
| 13 | latchingQuantity | Time-independent cumulative that latches at max/min |
| 14 | boundedQuantity | Accumulation that stops at max/min bounds |

---

#### CommodityKind
**Base:** `UInt16` (union)

| Value | Name | Description |
|-------|------|-------------|
| 0 | none | Not Applicable |
| 1 | electricitySecondaryMetered | Secondary metered electricity |
| 2 | electricityPrimaryMetered | Primary metered electricity (with external VT/CT) |
| 3 | communication | Communication infrastructure measurement |
| 4 | air | Air |
| 5 | insulativeGas | Insulative Gas |
| 6 | insulativeOil | Insulative Oil |
| 7 | naturalGas | Natural Gas |
| 8 | propane | Propane C3H8 |
| 9 | potableWater | Drinkable water |
| 10 | steam | Steam (usually for heating) |
| 11 | wasteWater | Waste Water (Sewerage) |
| 12 | heatingFluid | Heating fluid (liquid form) |
| 13 | coolingFluid | Cooling fluid |
| 14 | nonpotableWater | Reclaimed/irrigation water |
| 15 | nox | Nitrous Oxides NOX |
| 16 | so2 | Sulfur Dioxide SO2 |
| 17 | ch4 | Methane CH4 |
| 18 | co2 | Carbon Dioxide CO2 |
| 19 | carbon | Carbon |
| 20 | hch | Hexachlorocyclohexane HCH |
| 21 | pfc | Perfluorocarbons PFC |
| 22 | sf6 | Sulfurhexafluoride SF6 |
| 23 | tvLicence | Television |
| 24 | internet | Internet service |
| 25 | refuse | Trash |
| 26 | electricityTransmissionMetered | Transmission metered electricity |

---

#### Currency
**Base:** `UInt16` (union) - ISO 4217 currency codes

| Value | Code | Description |
|-------|------|-------------|
| 0 | other | Another type of currency |
| 36 | AUD | Australian dollar |
| 124 | CAD | Canadian dollar |
| 156 | CNY | Chinese yuan renminbi |
| 208 | DKK | Danish crown |
| 356 | INR | India rupees |
| 392 | JPY | Japanese yen |
| 578 | NOK | Norwegian crown |
| 643 | RUB | Russian ruble |
| 752 | SEK | Swedish crown |
| 756 | CHF | Swiss francs |
| 826 | GBP | British pound |
| 840 | USD | US dollar |
| 978 | EUR | European euro |

---

#### DataQualifierKind
**Base:** `UInt16` (union)

| Value | Name | Description |
|-------|------|-------------|
| 0 | none | Not Applicable |
| 2 | average | Average value |
| 4 | excess | Amount over threshold |
| 5 | highThreshold | Programmed high threshold |
| 7 | lowThreshold | Programmed low threshold |
| 8 | maximum | Highest value observed |
| 9 | minimum | Smallest value observed |
| 11 | nominal | Nominal |
| 12 | normal | Normal |
| 16 | secondMaximum | Second highest value observed |
| 17 | secondMinimum | Second smallest value observed |
| 23 | thirdMaximum | Third highest value observed |
| 24 | fourthMaximum | Fourth highest value observed |
| 25 | fifthMaximum | Fifth highest value observed |
| 26 | sum | Accumulated sum |

---

#### FlowDirectionKind
**Base:** `UInt16` (union)

| Value | Name | Description |
|-------|------|-------------|
| 0 | none | Not Applicable (N/A) |
| 1 | forward | Delivered/Imported energy |
| 2 | lagging | Lagging (inductive loading) |
| 3 | leading | Leading (capacitive loading) |
| 4 | net | Forward - Reverse |
| 5 | q1plusQ2 | Reactive positive quadrants |
| 7 | q1plusQ3 | Quadrants 1 and 3 |
| 8 | q1plusQ4 | Quadrants 1 and 4 (forward active energy) |
| 9 | q1minusQ4 | Q1 minus Q4 |
| 10 | q2plusQ3 | Quadrants 2 and 3 (reverse active energy) |
| 11 | q2plusQ4 | Quadrants 2 and 4 |
| 12 | q2minusQ3 | Q2 minus Q3 |
| 13 | q3plusQ4 | Reactive negative quadrants |
| 14 | q3minusQ2 | Q3 minus Q2 |
| 15 | quadrant1 | Q1 only |
| 16 | quadrant2 | Q2 only |
| 17 | quadrant3 | Q3 only |
| 18 | quadrant4 | Q4 only |
| 19 | reverse | Reverse/Received/Exported energy |
| 20 | total | Forward + Reverse |
| 21 | totalByPhase | Total by phase (polyphase metering) |

---

#### MeasurementKind
**Base:** `UInt16` (union)

| Value | Name | Description |
|-------|------|-------------|
| 0 | none | Not Applicable |
| 2 | apparentPowerFactor | Apparent Power Factor |
| 3 | currency | Currency |
| 4 | current | Current |
| 5 | currentAngle | Current Angle |
| 6 | currentImbalance | Current Imbalance |
| 7 | date | Date |
| 8 | demand | Demand |
| 9 | distance | Distance |
| 10 | distortionVoltAmperes | Distortion Volt Amperes |
| 11 | energization | Energization |
| 12 | energy | Energy |
| 13 | energizationLoadSide | Energization Load Side |
| 14 | fan | Fan |
| 15 | frequency | Frequency |
| 16 | Funds | Funds (duplication of currency) |
| 17-28 | ieee1366* | Various IEEE 1366 reliability metrics |
| 31 | lineLosses | Line Losses |
| 32 | losses | Losses |
| 33 | negativeSequence | Negative Sequence |
| 34 | phasorPowerFactor | Phasor Power Factor |
| 35 | phasorReactivePower | Phasor Reactive Power |
| 36 | positiveSequence | Positive Sequence |
| 37 | power | Power |
| 38 | powerFactor | Power Factor |
| 40 | quantityPower | Quantity Power |
| 41 | sag | Sag (Voltage Dip) |
| 42 | swell | Swell |
| 43 | switchPosition | Switch Position |
| 44 | tapPosition | Tap Position |
| 45 | tariffRate | Tariff Rate |
| 46 | temperature | Temperature |
| 47 | totalHarmonicDistortion | Total Harmonic Distortion |
| 48 | transformerLosses | Transformer Losses |
| 49-53 | unipedeVoltageDip* | Unipede Voltage Dip ranges |
| 54 | voltage | Voltage |
| 55 | voltageAngle | Voltage Angle |
| 56 | voltageExcursion | Voltage Excursion |
| 57 | voltageImbalance | Voltage Imbalance |
| 58 | volume | Volume (fluid) |
| 59 | zeroFlowDuration | Zero Flow Duration |
| 60 | zeroSequence | Zero Sequence |
| 64 | distortionPowerFactor | Distortion Power Factor |
| 81 | frequencyExcursion | Frequency Excursion |
| 90-116 | *various* | Device/communication attributes |
| 117 | signaltoNoiseRatio | Signal to Noise Ratio |
| 118-155 | *various* | Alarms, events, billing items |

---

#### PhaseCodeKind
**Base:** `UInt16` (union)

| Value | Name | Description |
|-------|------|-------------|
| 0 | none | Not applicable to any phase |
| 16 | N | Neutral |
| 32 | C | Phase C |
| 33 | CN | Phase C to neutral |
| 40 | CAv | Phase C relative to Phase A voltage |
| 41 | ACN | Phases A, C and neutral |
| 64 | B | Phase B |
| 65 | BN | Phase B to neutral |
| 66 | BC | Phases B to C |
| 72 | BAv | Phase B relative to Phase A voltage |
| 96 | AC | Phases A and C |
| 97 | BCN | BC to neutral |
| 128 | A | Phase A |
| 129 | AN | Phase A to neutral |
| 132 | AB | Phases A to B |
| 136 | AtoAv | Phase A current relative to Phase A voltage |
| 193 | ABN | AB to Neutral |
| 224 | ABC | All phases |
| 225 | ABCN | ABC to Neutral |
| 256 | S2 | Phase S2 |
| 272 | S2N | Phase S2 to neutral |
| 512 | S1 | Phase S1 |
| 528 | S1N | Phase S1 to Neutral |
| 768 | S12 | Phase S1 to S2 |
| 769 | S12N | Phase S1, S2 to N |
| 784 | S12N | Phase S1, S2 to neutral |

---

#### UnitMultiplierKind
**Base:** `Int16` (union) - Power of ten multipliers

| Value | Symbol | Name | Multiplier |
|-------|--------|------|------------|
| -12 | p | pico | 10^-12 |
| -9 | n | nano | 10^-9 |
| -6 | micro | micro | 10^-6 |
| -3 | m | milli | 10^-3 |
| -2 | c | centi | 10^-2 |
| -1 | d | deci | 10^-1 |
| 0 | none | none | 1 |
| 1 | da | deca | 10^1 |
| 2 | h | hecto | 10^2 |
| 3 | k | kilo | 10^3 |
| 6 | M | mega | 10^6 |
| 9 | G | giga | 10^9 |
| 12 | T | tera | 10^12 |

---

#### QualityOfReading
**Base:** `UInt16` (union)

| Value | Name | Description |
|-------|------|-------------|
| 0 | valid | Data passed all validation checks or verified |
| 7 | manuallyEdited | Replaced or approved by human |
| 8 | estimatedUsingReferenceDay | Machine computed from historical data |
| 9 | estimatedUsingLinearInterpolation | Computed using linear interpolation |
| 10 | questionable | Failed one or more checks |
| 11 | derived | Calculated using logic or math |
| 12 | projected | Forecast of future readings |
| 13 | mixed | Mixed quality characteristics |
| 14 | raw | Not validated |
| 15 | normalizedForWeather | Adjusted for weather |
| 16 | other | Other characteristic |
| 17 | validated | Validated and possibly edited/estimated |
| 18 | verified | Failed validation but represents actual usage |
| 19 | revenueQuality | Valid and acceptable for billing |

---

#### ServiceKind
**Base:** `UInt16` (union)

| Value | Name | Description |
|-------|------|-------------|
| 0 | electricity | Electricity service |
| 1 | gas | Gas service |
| 2 | water | Water service |
| 3 | time | Time service |
| 4 | heat | Heat service |
| 5 | refuse | Refuse (waste) service |
| 6 | sewerage | Sewerage service |
| 7 | rates | Rates (tax, charge, toll, duty, tariff) |
| 8 | tvLicence | TV license service |
| 9 | internet | Internet service |

---

#### TimeAttributeKind
**Base:** `UInt16` (union)

| Value | Name | Description |
|-------|------|-------------|
| 0 | none | Not Applicable |
| 1 | tenMinute | 10-minute |
| 2 | fifteenMinute | 15-minute |
| 3 | oneMinute | 1-minute |
| 4 | twentyfourHour | 24-hour |
| 5 | thirtyMinute | 30-minute |
| 6 | fiveMinute | 5-minute |
| 7 | sixtyMinute | 60-minute |
| 10 | twoMinute | 2-minute |
| 14 | threeMinute | 3-minute |
| 15 | present | Within present period of time |
| 16 | previous | Previous monthly cycle |
| 31 | twentyMinute | 20-minute interval |
| 50-56 | fixedBlock* | Fixed block intervals (1-60 min) |
| 57-77 | rollingBlock* | Rolling block intervals with sub-intervals |

---

#### TimePeriodOfInterest
**Base:** `UInt16` (union)

| Value | Name | Description |
|-------|------|-------------|
| 0 | none | Not Applicable |
| 8 | billingPeriod | Billing period starting at midnight |
| 11 | daily | Daily period starting at midnight |
| 13 | monthly | Monthly period starting first day of month |
| 22 | seasonal | Season spanning multiple months |
| 24 | weekly | Weekly period starting at midnight |
| 32 | specifiedPeriod | Defined by TimePeriod element |

---

#### UnitSymbolKind
**Base:** `UInt16` (union) - **Over 100 enumeration values**

Key units include:

| Value | Symbol | Description |
|-------|--------|-------------|
| 0 | none | N/A |
| 2 | m | Length, meter |
| 3 | g | Mass, gram |
| 5 | A | Current, ampere |
| 6 | K | Temperature, Kelvin |
| 23 | degC | Degrees Celsius |
| 27 | sec | Time, seconds |
| 29 | V | Electric potential, Volt |
| 30 | ohm | Electric resistance, Ohm |
| 33 | Hz | Frequency, hertz |
| 38 | W | Real power, Watt |
| 42 | m3 | Volume, cubic meter |
| 61 | VA | Apparent power, Volt Ampere |
| 63 | VAr | Reactive power, Volt Ampere reactive |
| 65 | cosTheta | Power factor |
| 71 | VAh | Apparent energy, Volt Ampere hours |
| 72 | Wh | Real energy, Watt hours |
| 73 | VArh | Reactive energy, Volt Ampere reactive hours |
| 111 | count | Counter value |
| 119 | ft3 | Volume, cubic feet |
| 159 | min | Time, minute |
| 160 | h | Time, hour |
| 169 | therm | Energy, Therm |

*(See full schema for complete list of 100+ unit symbols)*

---

#### StatusCode
**Base:** `UInt16` (union) - HTTP-style status codes

| Value | Name |
|-------|------|
| 200 | Ok |
| 201 | Created |
| 202 | Accepted |
| 204 | No Content |
| 301 | Moved Permanently |
| 302 | Redirect |
| 304 | Not Modified |
| 400 | Bad Request |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |
| 405 | Method Not Allowed |
| 410 | Gone |
| 500 | Internal Server Error |

---

#### CRUDOperation
**Base:** `UInt16` (union)

| Value | Name |
|-------|------|
| 0 | Create |
| 1 | Read |
| 2 | Update |
| 3 | Delete |

---

#### DataCustodianApplicationStatus
**Base:** `UInt16` (union)

| Value | Name | Description |
|-------|------|-------------|
| 1 | Review | Under review |
| 2 | Production | Production (Live) |
| 3 | OnHold | On Hold |
| 4 | Revoked | Revoked |

---

#### ThirdPartyApplicatonStatus
**Base:** `UInt16` (union)

| Value | Name | Description |
|-------|------|-------------|
| 1 | Development | Development |
| 2 | ReviewTest | Review/Test |
| 3 | Production | Live |
| 4 | Retired | Removed |

---

#### ThirdPartyApplicationType
**Base:** `UInt16` (union)

| Value | Name | Description |
|-------|------|-------------|
| 1 | Web | Web application |
| 2 | Desktop | Desktop application |
| 3 | Mobile | Mobile device application |
| 4 | Device | Other device application |

---

#### ThirdPartyApplicationUse
**Base:** `UInt16` (union)

| Value | Name | Description |
|-------|------|-------------|
| 1 | EnergyManagement | Energy Management |
| 2 | Comparisons | Analytical |
| 3 | Government | Governmental |
| 4 | Academic | Academic |
| 5 | LawEnforcement | Law Enforcement |

---

#### AuthorizationStatus
**Base:** `UInt16` (union)

| Value | Name |
|-------|------|
| 0 | Revoked |
| 1 | Active |
| 2 | Denied |

---

#### ESPIServiceStatus
**Base:** `UInt16` (union)

| Value | Name | Description |
|-------|------|-------------|
| 0 | Unavailable | Unavailable |
| 1 | Normal | Normal; operational |

---

### 5.3 String-Based Enumerations

#### AmiBillingReadyKind
**Base:** `String32`

| Value | Description |
|-------|-------------|
| `amiCapable` | AMI capable meter without communications module |
| `amiDisabled` | AMI functionality disabled or not used |
| `billingApproved` | Operating AMI meter certified for billing |
| `enabled` | AMI capable meter with communications |
| `nonAmi` | Non-AMI capable meter |
| `nonMetered` | Not currently equipped with meter |
| `operable` | AMI meter functioning and communicating |

---

#### UsagePointConnectedKind
**Base:** `String32`

| Value | Description |
|-------|-------------|
| `connected` | Connected to network, able to receive/send commodity |
| `logicallyDisconnected` | Disconnected via meter disconnect function |
| `physicallyDisconnected` | Disconnected upstream of meter (field crew) |

---

#### ApnodeType
**Base:** `String8` - Aggregate Node Types (Pricing)

| Value | Description |
|-------|-------------|
| `AG` | Aggregated Generation |
| `CPZ` | Custom Price Zone |
| `DPZ` | Default Price Zone |
| `LAP` | Load Aggregation Point |
| `TH` | Trading Hub |
| `SYS` | System Zone |
| `CA` | Control Area |
| `DCA` | Designated Congestion Area |
| `GA` | Generic Aggregation |
| `GH` | Generic Hub |
| `EHV` | 500 kV (Extra High Voltage) |
| `ZN` | Zone |
| `INT` | Interface |
| `BUS` | Bus |

---

#### AnodeType
**Base:** `String8` - Aggregated Node Types

| Value | Description |
|-------|-------------|
| `SYS` | System Zone/Region |
| `RUC` | RUC Zone |
| `LFZ` | Load Forecast Zone |
| `REG` | Market Energy/Ancillary Service Region |
| `AGR` | Aggregate Generation Resource |
| `POD` | Point of Delivery |
| `ALR` | Aggregate Load Resource |
| `LTAC` | Load Transmission Access Charge Group |
| `ACA` | Adjacent Control Area |
| `ASR` | Aggregated System Resource |
| `ECA` | Embedded Control Area |

---

#### tOUorCPPorConsumptionTier
**Base:** `xs:string`

| Value | Description |
|-------|-------------|
| `tou` | Time of Use |
| `cpp` | Consumption Peak Period |
| `consumptiontier` | Consumption Tier |

---

#### ParticipationCategoryMPM
**Base:** `String4`

| Value | Description |
|-------|-------------|
| `Y` | Participates in both LMPM and SMPM |
| `N` | Not included in LMP price measures |
| `S` | Participates in SMPM price measures |
| `L` | Participates in LMPM price measures |

---

#### EnrollmentStatus
**Base:** `String32`

| Value | Description |
|-------|-------------|
| `unenrolled` | Currently NOT enrolled in Tariff Rider |
| `enrolled` | Currently enrolled in Tariff Rider |
| `enrolledPending` | Currently pending enrollment |

---

## 6. Complex Types (Resources)

### 6.1 ESPI Core Resources (extend IdentifiedObject)

| Type | Description | Key Fields |
|------|-------------|------------|
| `ApplicationInformation` | Third Party Application registration | dataCustodianId, client_id, client_secret, scope, grant_types |
| `Authorization` | OAuth authorization grant | authorizedPeriod, status, expires_at, scope, token_type |
| `IntervalBlock` | Time sequence of readings | interval (DateTimeInterval), IntervalReading[] |
| `MeterReading` | Set of values from meter | (extends IdentifiedObject only) |
| `ReadingType` | Characteristics of readings | accumulationBehaviour, commodity, currency, uom, phase, etc. |
| `UsagePoint` | Logical metering point | roleFlags, ServiceCategory, status, serviceDeliveryPoint, phaseCode |
| `ElectricPowerQualitySummary` | Power quality metrics | flickerPlt/Pst, harmonicVoltage, longInterruptions, etc. |
| `ElectricPowerUsageSummary` | [DEPRECATED] Usage summary | billingPeriod, billLastPeriod, various consumption metrics |
| `UsageSummary` | Usage for billing period | Same as above + tariffProfile, tariffRiderRefs |
| `TimeConfiguration` | Time/DST configuration | dstEndRule, dstOffset, dstStartRule, tzOffset |
| `ProgramIdMappings` | TOU/CPP/Tier code mappings | programIdMapping[] |

### 6.2 Supporting Complex Types (extend Object)

| Type | Description | Key Fields |
|------|-------------|------------|
| `Object` | Base superclass | extension[] |
| `IdentifiedObject` | Named object base | batchItemInfo |
| `DateTimeInterval` | Time interval | duration (UInt32), start (TimeType) |
| `IntervalReading` | Individual reading value | cost, ReadingQuality[], timePeriod, value, tou, cpp |
| `ReadingQuality` | Quality indicator | quality (QualityOfReading) |
| `ServiceCategory` | Service classification | kind (ServiceKind) |
| `SummaryMeasurement` | Aggregated measurement | powerOfTenMultiplier, timeStamp, uom, value |
| `BatchItemInfo` | Batch transaction info | name, operation, statusCode, statusReason |
| `ServiceDeliveryPoint` | Revenue UsagePoint info | name, tariffProfile, customerAgreement, tariffRiderRefs |
| `RationalNumber` | Fraction representation | numerator, denominator |
| `ReadingInterharmonic` | Harmonic/interharmonic | numerator, denominator |
| `LineItem` | Billing line item detail | amount, rounding, dateTime, note, itemKind, unitCost |
| `PnodeRefs` | Pricing node references | pnodeRef[] |
| `PnodeRef` | Single pricing node | apnodeType, ref, startEffectiveDate, endEffectiveDate |
| `AggregateNodeRefs` | Aggregate node references | aggregateNodeRef[] |
| `AggregateNodeRef` | Single aggregate node | anodeType, ref, pnodeRef[] |
| `TariffRiderRefs` | Tariff rider references | tariffRiderRef[] |
| `TariffRiderRef` | Single tariff rider | riderType, enrollmentStatus, effectiveDate |
| `BillingChargeSource` | Billing source info | agencyName |
| `ServiceStatus` | ESPI service status | currentStatus (ESPIServiceStatus) |
| `BatchListType` | Resource URI list | resources[] (xs:anyURI) |

---

## 7. Complex Types (Supporting)

### Inheritance Hierarchy

```
Object
├── IdentifiedObject
│   ├── ApplicationInformation
│   ├── Authorization
│   ├── IntervalBlock
│   ├── MeterReading
│   ├── ReadingType
│   ├── UsagePoint
│   ├── ElectricPowerQualitySummary
│   ├── ElectricPowerUsageSummary [DEPRECATED]
│   ├── UsageSummary
│   ├── TimeConfiguration
│   └── ProgramIdMappings
├── IntervalReading
├── ReadingQuality
├── ServiceCategory
├── SummaryMeasurement
├── BatchItemInfo
├── ServiceDeliveryPoint
├── RationalNumber
├── ReadingInterharmonic
├── LineItem
├── PnodeRefs
├── PnodeRef
├── AggregateNodeRefs
├── AggregateNodeRef
├── TariffRiderRefs
├── TariffRiderRef
├── BillingChargeSource
├── ServiceStatus
└── DateTimeInterval
```

---

## 8. Global Elements

The schema defines the following global elements that can appear at the root of XML documents:

| Element | Type |
|---------|------|
| `ApplicationInformation` | ApplicationInformation |
| `Authorization` | Authorization |
| `IntervalBlock` | IntervalBlock |
| `IntervalReading` | IntervalReading |
| `MeterReading` | MeterReading |
| `ReadingQuality` | ReadingQuality |
| `ReadingType` | ReadingType |
| `IdentifiedObject` | IdentifiedObject |
| `UsagePoint` | UsagePoint |
| `ElectricPowerQualitySummary` | ElectricPowerQualitySummary |
| `ElectricPowerUsageSummary` | ElectricPowerUsageSummary |
| `UsageSummary` | UsageSummary |
| `DateTimeInterval` | DateTimeInterval |
| `SummaryMeasurement` | SummaryMeasurement |
| `BatchItemInfo` | BatchItemInfo |
| `Object` | Object |
| `ServiceStatus` | ServiceStatus |
| `LocalTimeParameters` | TimeConfiguration |
| `ProgramIdMappings` | ProgramIdMappings |
| `BatchList` | BatchListType |

---

## Summary Statistics

| Category | Count |
|----------|-------|
| Basic/Primitive Types | 7 |
| String Types | 7 |
| Hex Binary Types | 4 |
| Special Types | 2 |
| Enumeration Types (Total) | 35 |
| - OAuth Enumerations | 5 |
| - Numeric Enumerations (UInt16) | 20 |
| - String-Based Enumerations | 10 |
| Complex Types (Resources) | 10 |
| Complex Types (Supporting) | 19 |
| Global Elements | 20 |

---

*Generated from ESPI 4.0 Schema (espi.xsd) - Version 4.0.20231213*
