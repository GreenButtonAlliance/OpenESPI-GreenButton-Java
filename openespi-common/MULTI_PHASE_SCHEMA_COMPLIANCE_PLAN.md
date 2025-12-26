# Multi-Phase NAESB ESPI Schema Compliance Plan

## Overview

This plan reviews domain entities in `/domain/usage`, `/domain/customer`, and `/domain/common` to ensure compliance with NAESB ESPI 4.0 schema (espi.xsd and customer.xsd) element sequences.

**OAuth2 Domain Entities** (already compliant with DTO_PATTERN_GUIDE.md):
- **ApplicationInformationEntity** - ApplicationInformationDto already follows DTO_PATTERN_GUIDE.md pattern:
  - Uses `@XmlAccessorType(XmlAccessType.PROPERTY)`
  - Has explicit JavaBean-style getters with @XmlElement annotations
  - Uses correct namespace: `http://naesb.org/espi`
  - propOrder matches XSD sequence (espi.xsd lines 62-246)
  - No review required
- **AuthorizationEntity** - AuthorizationDto already follows DTO_PATTERN_GUIDE.md pattern:
  - Uses `@XmlAccessorType(XmlAccessType.PROPERTY)`
  - Has explicit JavaBean-style getters with @XmlElement annotations
  - Uses correct namespace: `http://naesb.org/espi`
  - propOrder matches XSD sequence
  - No review required

**Processing Order**:
1. Usage domain entities (Phases 1-16)
2. Customer domain entities (Phases 17-26)

**Branch Strategy**:
- Each phase uses a separate feature branch for single entity and all associated files
- Branch naming: `feature/schema-compliance-phase-{number}-{entity-name}`
- Commit, push, and create PR for each phase when completed
- Merge PR before starting next phase

**Entity Ordering Principles**:
- Dependency order (parents before children)
- Progress from simple to complex entities
- Field order in entities must match XSD element sequence exactly

**Reference Documents**:
- **DTO_PATTERN_GUIDE.md** - Standard pattern for creating ESPI DTOs with JAXB
  - Atom wrapper separation pattern
  - Required @XmlAccessorType(PROPERTY) usage
  - Explicit getter method requirements for Records
  - Namespace guidelines (espi.xsd vs customer.xsd)
  - Verification checklist

---

## Usage Domain Entities (Phases 1-16)

### Phase 1: TimeConfiguration (LocalTimeParameters)
**Branch**: `feature/schema-compliance-phase-1-time-configuration`

**Dependencies**: None (standalone entity)
**Referenced By**: UsagePoint, Customer, ServiceLocation (via bidirectional Atom rel='related' links)

**Tasks**:
1. **Entity Updates** (TimeConfigurationEntity.java):
   - Review field order against espi.xsd TimeConfiguration element sequence
   - Verify JPA annotations match schema constraints
   - Ensure JAXB annotations align with XML element names and order
   - Update field JavaDoc to reference XSD documentation

2. **DTO Updates** (TimeConfigurationDto.java):
   - **Follow DTO_PATTERN_GUIDE.md** - Standard pattern for ESPI DTOs
   - Use `@XmlAccessorType(XmlAccessType.PROPERTY)` - REQUIRED
   - Create explicit JavaBean-style getters (Records need `getFieldName()` for JAXB)
   - Match DTO field order to XSD element sequence in propOrder
   - Use correct namespace: `http://naesb.org/espi` for usage domain
   - Exclude Atom metadata fields (published, updated, selfLink, upLink)
   - Include only XSD-defined resource fields
   - Add uuid mapped to mRID attribute via getter
   - **Verify against DTO_PATTERN_GUIDE.md checklist before proceeding**

3. **MapStruct Mapper Updates** (TimeConfigurationMapper.java):
   - Update Entity-to-DTO conversion mapping
   - Update DTO-to-Entity conversion mapping
   - Ensure field order preservation during mapping

4. **Repository Updates** (TimeConfigurationRepository.java):
   - Keep ONLY index field queries
   - Remove all non-index queries not required for tests

5. **Service Updates** (TimeConfigurationService.java, TimeConfigurationServiceImpl.java):
   - Review service methods for schema compliance
   - Update any field access order to match schema sequence

6. **Flyway Migration Updates**:
   - Update `db/migration/V1__Create_Base_Tables.sql` and `db/migration/V3__Create_additiional_Base_Tables.sql`
   - Verify column order matches espi.xsd element sequence
   - Update vendor-specific scripts if needed:
     - `db/vendor/h2/V2__H2_Specific_Tables.sql`
     - `db/vendor/mysql/V2__MySQL_Specific_Tables.sql`
     - `db/vendor/postgres/V2__PostgreSQL_Specific_Tables.sql`

7. **Testing**:
   - Update unit tests for TimeConfiguration entity, service, repository
   - Add/update integration tests using TestContainers
   - Add XML marshalling/unmarshalling tests to verify schema compliance
   - Validate generated XML against espi.xsd using XSD schema validation

8. **Commit, Push, PR**:
   - Stage all changes: `git add .`
   - Commit: `git commit -m "Phase 1: TimeConfiguration schema compliance"`
   - Push: `git push origin feature/schema-compliance-phase-1-time-configuration`
   - Create PR and merge when approved

---

### Phase 2: ReadingType
**Branch**: `feature/schema-compliance-phase-2-reading-type`

**Dependencies**: None (standalone entity with embedded RationalNumber and ReadingInterharmonic)
**Referenced By**: MeterReading (via Atom rel='related' links), IntervalBlock (indirectly via MeterReading)

**Tasks**:
1. **Entity Updates** (ReadingTypeEntity.java):
   - Review field order against espi.xsd ReadingType element sequence
   - Verify embedded RationalNumber and ReadingInterharmonic field order
   - Verify JPA @AttributeOverrides match schema constraints
   - Ensure JAXB annotations align with XML element names and order

2. **DTO Updates** (ReadingTypeDto.java):
   - **Follow DTO_PATTERN_GUIDE.md** - Standard pattern for ESPI DTOs
   - Use `@XmlAccessorType(XmlAccessType.PROPERTY)` - REQUIRED
   - Create explicit JavaBean-style getters (Records need `getFieldName()` for JAXB)
   - Match DTO field order to XSD element sequence in propOrder
   - Use correct namespace: `http://naesb.org/espi` for usage domain
   - Exclude Atom metadata fields (published, updated, selfLink, upLink)
   - Include only XSD-defined resource fields
   - Verify embedded RationalNumber and ReadingInterharmonic DTOs follow same pattern
   - Add uuid mapped to mRID attribute via getter
   - **Verify against DTO_PATTERN_GUIDE.md checklist before proceeding**

3. **MapStruct Mapper Updates** (ReadingTypeMapper.java):
   - Update Entity-to-DTO conversion mapping
   - Update DTO-to-Entity conversion mapping
   - Handle embedded object mappings

4. **Repository Updates** (ReadingTypeRepository.java):
   - Keep ONLY index field queries
   - Remove all non-index queries not required for tests

5. **Service Updates** (ReadingTypeService.java, ReadingTypeServiceImpl.java):
   - Review service methods for schema compliance
   - Update any field access order to match schema sequence

6. **Flyway Migration Updates**:
   - Update original Flyway scripts to match espi.xsd element sequence
   - Verify embedded object column order

7. **Testing**:
   - Update unit tests for ReadingType entity, service, repository
   - Add/update integration tests using TestContainers
   - Add XML marshalling/unmarshalling tests to verify schema compliance
   - Validate generated XML against espi.xsd

8. **Commit, Push, PR**

---

### Phase 3: IntervalReading
**Branch**: `feature/schema-compliance-phase-3-interval-reading`

**Dependencies**: None (embedded DateTimeInterval, owned by IntervalBlock)
**Referenced By**: IntervalBlock (via collection, parent owns child), ReadingQuality (via Atom rel='related' links)

**Tasks**:
1. **Entity Updates** (IntervalReadingEntity.java):
   - Review field order against espi.xsd IntervalReading element sequence
   - Verify embedded DateTimeInterval (timePeriod) field order
   - Verify JPA annotations match schema constraints

2. **DTO Updates** (IntervalReadingDto.java):
   - **Follow DTO_PATTERN_GUIDE.md** - Standard pattern for ESPI DTOs
   - Use `@XmlAccessorType(XmlAccessType.PROPERTY)` - REQUIRED
   - Create explicit JavaBean-style getters (Records need `getFieldName()` for JAXB)
   - Match DTO field order to XSD element sequence in propOrder
   - Use correct namespace: `http://naesb.org/espi` for usage domain
   - Exclude Atom metadata fields (published, updated, selfLink, upLink)
   - Include only XSD-defined resource fields
   - Verify embedded DateTimeInterval DTO follows same pattern
   - **Note**: IntervalReading may not extend IdentifiedObject per Issue #28
   - **Verify against DTO_PATTERN_GUIDE.md checklist before proceeding**

3. **MapStruct Mapper Updates** (IntervalReadingMapper.java):
   - Update Entity-to-DTO conversion mapping
   - Update DTO-to-Entity conversion mapping
   - Handle embedded DateTimeInterval mapping

4. **Repository Updates** (IntervalReadingRepository.java):
   - Keep ONLY index field queries
   - Remove all non-index queries not required for tests

5. **Service Updates** (IntervalReadingService.java, IntervalReadingServiceImpl.java):
   - Review service methods for schema compliance

6. **Flyway Migration Updates**:
   - Update original Flyway scripts to match espi.xsd element sequence

7. **Testing**:
   - Update unit tests for IntervalReading entity, service, repository
   - Add/update integration tests using TestContainers
   - Add XML marshalling/unmarshalling tests to verify schema compliance
   - Validate generated XML against espi.xsd

8. **Commit, Push, PR**

---

### Phase 4: ReadingQuality
**Branch**: `feature/schema-compliance-phase-4-reading-quality`

**Dependencies**: IntervalReading (via Atom rel='related' links)
**Referenced By**: None

**Tasks**:
1. **Entity Updates** (ReadingQualityEntity.java):
   - Review field order against espi.xsd ReadingQuality element sequence
   - Verify relationship to IntervalReading

2. **DTO Updates** (ReadingQualityDto.java):
   - **Follow DTO_PATTERN_GUIDE.md** - Standard pattern for ESPI DTOs
   - Use `@XmlAccessorType(XmlAccessType.PROPERTY)` - REQUIRED
   - Create explicit JavaBean-style getters (Records need `getFieldName()` for JAXB)
   - Match DTO field order to XSD element sequence in propOrder
   - Use correct namespace: `http://naesb.org/espi` for usage domain
   - Exclude Atom metadata fields (published, updated, selfLink, upLink)
   - Include only XSD-defined resource fields
   - **Note**: ReadingQuality may not extend IdentifiedObject per Issue #28
   - **Verify against DTO_PATTERN_GUIDE.md checklist before proceeding**

3. **MapStruct Mapper Updates** (ReadingQualityMapper.java):
   - Update Entity-to-DTO conversion mapping
   - Update DTO-to-Entity conversion mapping

4. **Repository Updates** (ReadingQualityRepository.java):
   - Keep ONLY index field queries
   - Remove all non-index queries not required for tests

5. **Service Updates** (ReadingQualityService.java, ReadingQualityServiceImpl.java):
   - Review service methods for schema compliance

6. **Flyway Migration Updates**:
   - Update original Flyway scripts to match espi.xsd element sequence

7. **Testing**:
   - Update unit tests for ReadingQuality entity, service, repository
   - Add/update integration tests using TestContainers
   - Add XML marshalling/unmarshalling tests to verify schema compliance
   - Validate generated XML against espi.xsd

8. **Commit, Push, PR**

---

### Phase 5: IntervalBlock
**Branch**: `feature/schema-compliance-phase-5-interval-block`

**Dependencies**: IntervalReading (contains collection), ReadingQuality (indirectly via IntervalReading)
**Referenced By**: MeterReading (via bidirectional Atom rel='related' links)

**Tasks**:
1. **Entity Updates** (IntervalBlockEntity.java):
   - Review field order against espi.xsd IntervalBlock element sequence
   - Verify embedded DateTimeInterval (interval) field order
   - Verify IntervalReading collection mapping and order
   - Verify @OneToMany cascade and orphanRemoval settings

2. **DTO Updates** (IntervalBlockDto.java):
   - **Follow DTO_PATTERN_GUIDE.md** - Standard pattern for ESPI DTOs
   - Use `@XmlAccessorType(XmlAccessType.PROPERTY)` - REQUIRED
   - Create explicit JavaBean-style getters (Records need `getFieldName()` for JAXB)
   - Match DTO field order to XSD element sequence in propOrder
   - Use correct namespace: `http://naesb.org/espi` for usage domain
   - Exclude Atom metadata fields (published, updated, selfLink, upLink)
   - Include only XSD-defined resource fields
   - Add uuid mapped to mRID attribute via getter
   - Verify embedded DateTimeInterval DTO follows same pattern
   - Verify IntervalReading collection DTO follows same pattern
   - **Verify against DTO_PATTERN_GUIDE.md checklist before proceeding**

3. **MapStruct Mapper Updates** (IntervalBlockMapper.java):
   - Update Entity-to-DTO conversion mapping
   - Update DTO-to-Entity conversion mapping
   - Handle IntervalReading collection mapping

4. **Repository Updates** (IntervalBlockRepository.java):
   - Keep ONLY index field queries
   - Remove all non-index queries not required for tests

5. **Service Updates** (IntervalBlockService.java, IntervalBlockServiceImpl.java):
   - Review service methods for schema compliance

6. **Flyway Migration Updates**:
   - Update original Flyway scripts to match espi.xsd element sequence

7. **Testing**:
   - Update unit tests for IntervalBlock entity, service, repository
   - Add/update integration tests using TestContainers
   - Add XML marshalling/unmarshalling tests to verify schema compliance
   - Validate generated XML against espi.xsd

8. **Commit, Push, PR**

---

### Phase 6: MeterReading
**Branch**: `feature/schema-compliance-phase-6-meter-reading`

**Dependencies**: ReadingType (via Atom rel='related' links), IntervalBlock (via bidirectional Atom rel='related' links)
**Referenced By**: UsagePoint (via bidirectional Atom rel='related' links)

**Note**: MeterReading has NO child elements, ONLY Atom links to IntervalBlock and ReadingType

**Tasks**:
1. **Entity Updates** (MeterReadingEntity.java):
   - Review field order against espi.xsd MeterReading element sequence
   - Verify @ManyToOne relationship to UsagePoint
   - Verify @OneToOne relationship to ReadingType (via Atom links, NOT embedded)
   - Verify relationship to IntervalBlock collection (via Atom links, NOT embedded)
   - Ensure NO embedded child elements exist

2. **DTO Updates** (MeterReadingDto.java):
   - **Follow DTO_PATTERN_GUIDE.md** - Standard pattern for ESPI DTOs
   - Use `@XmlAccessorType(XmlAccessType.PROPERTY)` - REQUIRED
   - Create explicit JavaBean-style getters (Records need `getFieldName()` for JAXB)
   - Match DTO field order to XSD element sequence in propOrder
   - Use correct namespace: `http://naesb.org/espi` for usage domain
   - Exclude Atom metadata fields (published, updated, selfLink, upLink)
   - Include only XSD-defined resource fields
   - Add uuid mapped to mRID attribute via getter
   - **CRITICAL**: NO child elements, ONLY Atom links to ReadingType and IntervalBlock
   - Verify ReadingType reference (Atom link via AtomEntryDto, NOT embedded)
   - Verify IntervalBlock references (Atom links via AtomEntryDto, NOT embedded collection)
   - **Verify against DTO_PATTERN_GUIDE.md checklist before proceeding**

3. **MapStruct Mapper Updates** (MeterReadingMapper.java):
   - Update Entity-to-DTO conversion mapping
   - Update DTO-to-Entity conversion mapping
   - Handle ReadingType and IntervalBlock reference mappings (NOT embedded objects)

4. **Repository Updates** (MeterReadingRepository.java):
   - Keep ONLY index field queries
   - Remove all non-index queries not required for tests

5. **Service Updates** (MeterReadingService.java, MeterReadingServiceImpl.java):
   - Review service methods for schema compliance

6. **Flyway Migration Updates**:
   - Update original Flyway scripts to match espi.xsd element sequence

7. **Testing**:
   - Update unit tests for MeterReading entity, service, repository
   - Add/update integration tests using TestContainers
   - Add XML marshalling/unmarshalling tests to verify schema compliance
   - Validate that XML output contains ONLY Atom links, NO embedded elements
   - Validate generated XML against espi.xsd

8. **Commit, Push, PR**

---

### Phase 7: ServiceDeliveryPoint
**Branch**: `feature/schema-compliance-phase-7-service-delivery-point`

**Dependencies**: None (standalone ESPI resource)
**Referenced By**: UsagePoint (via @OneToOne)

**Tasks**:
1. **Entity Updates** (ServiceDeliveryPointEntity.java):
   - Review field order against espi.xsd ServiceDeliveryPoint element sequence
   - Verify JPA annotations match schema constraints

2. **DTO Updates** (ServiceDeliveryPointDto.java):
   - **Follow DTO_PATTERN_GUIDE.md** - Standard pattern for ESPI DTOs
   - Use `@XmlAccessorType(XmlAccessType.PROPERTY)` - REQUIRED
   - Create explicit JavaBean-style getters (Records need `getFieldName()` for JAXB)
   - Match DTO field order to XSD element sequence in propOrder
   - Use correct namespace: `http://naesb.org/espi` for usage domain
   - Exclude Atom metadata fields (published, updated, selfLink, upLink)
   - Include only XSD-defined resource fields
   - **Note**: ServiceDeliveryPoint may not extend IdentifiedObject per Issue #28
   - **Verify against DTO_PATTERN_GUIDE.md checklist before proceeding**

3. **MapStruct Mapper Updates** (ServiceDeliveryPointMapper.java):
   - Update Entity-to-DTO conversion mapping
   - Update DTO-to-Entity conversion mapping

4. **Repository Updates** (ServiceDeliveryPointRepository.java):
   - Keep ONLY index field queries
   - Remove all non-index queries not required for tests

5. **Service Updates** (ServiceDeliveryPointService.java, ServiceDeliveryPointServiceImpl.java):
   - Review service methods for schema compliance

6. **Flyway Migration Updates**:
   - Update original Flyway scripts to match espi.xsd element sequence

7. **Testing**:
   - Update unit tests for ServiceDeliveryPoint entity, service, repository
   - Add/update integration tests using TestContainers
   - Add XML marshalling/unmarshalling tests to verify schema compliance
   - Validate generated XML against espi.xsd

8. **Commit, Push, PR**

---

### Phase 8: PnodeRef
**Branch**: `feature/schema-compliance-phase-8-pnode-ref`

**Dependencies**: None (embedded in UsagePoint)
**Referenced By**: UsagePoint (via collection)

**Tasks**:
1. **Entity Updates** (PnodeRefEntity.java):
   - Review field order against espi.xsd PnodeRef element sequence

2. **DTO Updates** (PnodeRefDto.java):
   - **Follow DTO_PATTERN_GUIDE.md** - Standard pattern for ESPI DTOs
   - Use `@XmlAccessorType(XmlAccessType.PROPERTY)` - REQUIRED
   - Create explicit JavaBean-style getters (Records need `getFieldName()` for JAXB)
   - Match DTO field order to XSD element sequence in propOrder
   - Use correct namespace: `http://naesb.org/espi` for usage domain
   - Exclude Atom metadata fields (published, updated, selfLink, upLink)
   - Include only XSD-defined resource fields
   - **Note**: PnodeRef may not extend IdentifiedObject per Issue #28 (embedded in UsagePoint)
   - **Verify against DTO_PATTERN_GUIDE.md checklist before proceeding**

3. **MapStruct Mapper Updates** (PnodeRefMapper.java):
   - Update Entity-to-DTO conversion mapping
   - Update DTO-to-Entity conversion mapping

4. **Repository Updates** (PnodeRefRepository.java):
   - Keep ONLY index field queries
   - Remove all non-index queries not required for tests

5. **Service Updates** (PnodeRefService.java, PnodeRefServiceImpl.java):
   - Review service methods for schema compliance

6. **Flyway Migration Updates**:
   - Update original Flyway scripts to match espi.xsd element sequence

7. **Testing**:
   - Update unit tests for PnodeRef entity, service, repository
   - Add/update integration tests using TestContainers
   - Add XML marshalling/unmarshalling tests to verify schema compliance
   - Validate generated XML against espi.xsd

8. **Commit, Push, PR**

---

### Phase 9: AggregatedNodeRef
**Branch**: `feature/schema-compliance-phase-9-aggregated-node-ref`

**Dependencies**: None (embedded in UsagePoint)
**Referenced By**: UsagePoint (via collection)

**Tasks**:
1. **Entity Updates** (AggregatedNodeRefEntity.java):
   - Review field order against espi.xsd AggregatedNodeRef element sequence

2. **DTO Updates** (AggregatedNodeRefDto.java):
   - **Follow DTO_PATTERN_GUIDE.md** - Standard pattern for ESPI DTOs
   - Use `@XmlAccessorType(XmlAccessType.PROPERTY)` - REQUIRED
   - Create explicit JavaBean-style getters (Records need `getFieldName()` for JAXB)
   - Match DTO field order to XSD element sequence in propOrder
   - Use correct namespace: `http://naesb.org/espi` for usage domain
   - Exclude Atom metadata fields (published, updated, selfLink, upLink)
   - Include only XSD-defined resource fields
   - **Note**: AggregatedNodeRef may not extend IdentifiedObject per Issue #28 (embedded in UsagePoint)
   - **Verify against DTO_PATTERN_GUIDE.md checklist before proceeding**

3. **MapStruct Mapper Updates** (AggregatedNodeRefMapper.java):
   - Update Entity-to-DTO conversion mapping
   - Update DTO-to-Entity conversion mapping

4. **Repository Updates** (AggregatedNodeRefRepository.java):
   - Keep ONLY index field queries
   - Remove all non-index queries not required for tests

5. **Service Updates** (AggregatedNodeRefService.java, AggregatedNodeRefServiceImpl.java):
   - Review service methods for schema compliance

6. **Flyway Migration Updates**:
   - Update original Flyway scripts to match espi.xsd element sequence

7. **Testing**:
   - Update unit tests for AggregatedNodeRef entity, service, repository
   - Add/update integration tests using TestContainers
   - Add XML marshalling/unmarshalling tests to verify schema compliance
   - Validate generated XML against espi.xsd

8. **Commit, Push, PR**

---

### Phase 10: LineItem
**Branch**: `feature/schema-compliance-phase-10-line-item`

**Dependencies**: None (embedded child of UsageSummary)
**Referenced By**: UsageSummary (via costAdditionalDetailLastPeriod embedded collection)

**Tasks**:
1. **Entity Updates** (LineItemEntity.java):
   - Review field order against espi.xsd LineItem element sequence
   - Verify embedded relationship to UsageSummary

2. **DTO Updates** (LineItemDto.java):
   - **Follow DTO_PATTERN_GUIDE.md** - Standard pattern for ESPI DTOs
   - Use `@XmlAccessorType(XmlAccessType.PROPERTY)` - REQUIRED
   - Create explicit JavaBean-style getters (Records need `getFieldName()` for JAXB)
   - Match DTO field order to XSD element sequence in propOrder
   - Use correct namespace: `http://naesb.org/espi` for usage domain
   - Exclude Atom metadata fields (published, updated, selfLink, upLink)
   - Include only XSD-defined resource fields
   - **Note**: LineItem may not extend IdentifiedObject per Issue #28 (embedded child of UsageSummary)
   - **Verify against DTO_PATTERN_GUIDE.md checklist before proceeding**

3. **MapStruct Mapper Updates** (LineItemMapper.java):
   - Update Entity-to-DTO conversion mapping
   - Update DTO-to-Entity conversion mapping

4. **Repository Updates** (LineItemRepository.java):
   - Keep ONLY index field queries
   - Remove all non-index queries not required for tests

5. **Service Updates** (LineItemService.java, LineItemServiceImpl.java):
   - Review service methods for schema compliance

6. **Flyway Migration Updates**:
   - Update original Flyway scripts to match espi.xsd element sequence

7. **Testing**:
   - Update unit tests for LineItem entity, service, repository
   - Add/update integration tests using TestContainers
   - Add XML marshalling/unmarshalling tests to verify schema compliance
   - Validate generated XML against espi.xsd

8. **Commit, Push, PR**

---

### Phase 11: UsageSummary
**Branch**: `feature/schema-compliance-phase-11-usage-summary`

**Dependencies**: LineItem (embedded child via costAdditionalDetailLastPeriod)
**Referenced By**: UsagePoint (via bidirectional Atom rel='related' links)

**Tasks**:
1. **Entity Updates** (UsageSummaryEntity.java):
   - Review field order against espi.xsd UsageSummary element sequence
   - Verify embedded DateTimeInterval fields (billingPeriod, etc.)
   - Verify embedded SummaryMeasurement fields
   - Verify LineItem collection (costAdditionalDetailLastPeriod)

2. **DTO Updates** (UsageSummaryDto.java):
   - **Follow DTO_PATTERN_GUIDE.md** - Standard pattern for ESPI DTOs
   - Use `@XmlAccessorType(XmlAccessType.PROPERTY)` - REQUIRED
   - Create explicit JavaBean-style getters (Records need `getFieldName()` for JAXB)
   - Match DTO field order to XSD element sequence in propOrder
   - Use correct namespace: `http://naesb.org/espi` for usage domain
   - Exclude Atom metadata fields (published, updated, selfLink, upLink)
   - Include only XSD-defined resource fields
   - Add uuid mapped to mRID attribute via getter
   - Verify embedded DateTimeInterval and SummaryMeasurement DTOs follow same pattern
   - Verify LineItem collection DTO follows same pattern
   - **Verify against DTO_PATTERN_GUIDE.md checklist before proceeding**

3. **MapStruct Mapper Updates** (UsageSummaryMapper.java):
   - Update Entity-to-DTO conversion mapping
   - Update DTO-to-Entity conversion mapping
   - Handle LineItem collection mapping

4. **Repository Updates** (UsageSummaryRepository.java):
   - Keep ONLY index field queries
   - Remove all non-index queries not required for tests

5. **Service Updates** (UsageSummaryService.java, UsageSummaryServiceImpl.java):
   - Review service methods for schema compliance

6. **Flyway Migration Updates**:
   - Update original Flyway scripts to match espi.xsd element sequence

7. **Testing**:
   - Update unit tests for UsageSummary entity, service, repository
   - Add/update integration tests using TestContainers
   - Add XML marshalling/unmarshalling tests to verify schema compliance
   - Validate generated XML against espi.xsd

8. **Commit, Push, PR**

---

### Phase 12: ElectricPowerQualitySummary
**Branch**: `feature/schema-compliance-phase-12-electric-power-quality-summary`

**Dependencies**: None (standalone summary entity)
**Referenced By**: UsagePoint (via bidirectional Atom rel='related' links)

**Tasks**:
1. **Entity Updates** (ElectricPowerQualitySummaryEntity.java):
   - Review field order against espi.xsd ElectricPowerQualitySummary element sequence
   - Verify embedded DateTimeInterval (summaryInterval)
   - Verify embedded SummaryMeasurement fields

2. **DTO Updates** (ElectricPowerQualitySummaryDto.java):
   - **Follow DTO_PATTERN_GUIDE.md** - Standard pattern for ESPI DTOs
   - Use `@XmlAccessorType(XmlAccessType.PROPERTY)` - REQUIRED
   - Create explicit JavaBean-style getters (Records need `getFieldName()` for JAXB)
   - Match DTO field order to XSD element sequence in propOrder
   - Use correct namespace: `http://naesb.org/espi` for usage domain
   - Exclude Atom metadata fields (published, updated, selfLink, upLink)
   - Include only XSD-defined resource fields
   - Add uuid mapped to mRID attribute via getter
   - Verify embedded DateTimeInterval and SummaryMeasurement DTOs follow same pattern
   - **Verify against DTO_PATTERN_GUIDE.md checklist before proceeding**

3. **MapStruct Mapper Updates** (ElectricPowerQualitySummaryMapper.java):
   - Update Entity-to-DTO conversion mapping
   - Update DTO-to-Entity conversion mapping

4. **Repository Updates** (ElectricPowerQualitySummaryRepository.java):
   - Keep ONLY index field queries
   - Remove all non-index queries not required for tests

5. **Service Updates** (ElectricPowerQualitySummaryService.java, ElectricPowerQualitySummaryServiceImpl.java):
   - Review service methods for schema compliance

6. **Flyway Migration Updates**:
   - Update original Flyway scripts to match espi.xsd element sequence

7. **Testing**:
   - Update unit tests for ElectricPowerQualitySummary entity, service, repository
   - Add/update integration tests using TestContainers
   - Add XML marshalling/unmarshalling tests to verify schema compliance
   - Validate generated XML against espi.xsd

8. **Commit, Push, PR**

---

### Phase 13: RetailCustomer
**Branch**: `feature/schema-compliance-phase-13-retail-customer`

**Dependencies**: None (standalone entity)
**Referenced By**: UsagePoint (via @ManyToOne), Subscription (via @ManyToOne)

**Tasks**:
1. **Entity Updates** (RetailCustomerEntity.java):
   - Review field order against espi.xsd RetailCustomer element sequence
   - Verify relationships to UsagePoint collection
   - Verify relationships to Subscription collection

2. **DTO Updates** (RetailCustomerDto.java):
   - **Follow DTO_PATTERN_GUIDE.md** - Standard pattern for ESPI DTOs
   - Use `@XmlAccessorType(XmlAccessType.PROPERTY)` - REQUIRED
   - Create explicit JavaBean-style getters (Records need `getFieldName()` for JAXB)
   - Match DTO field order to XSD element sequence in propOrder
   - Use correct namespace: `http://naesb.org/espi` for usage domain
   - Exclude Atom metadata fields (published, updated, selfLink, upLink)
   - Include only XSD-defined resource fields
   - Add uuid mapped to mRID attribute via getter
   - **Verify against DTO_PATTERN_GUIDE.md checklist before proceeding**

3. **MapStruct Mapper Updates** (RetailCustomerMapper.java):
   - Update Entity-to-DTO conversion mapping
   - Update DTO-to-Entity conversion mapping

4. **Repository Updates** (RetailCustomerRepository.java):
   - Keep ONLY index field queries
   - Remove all non-index queries not required for tests

5. **Service Updates** (RetailCustomerService.java, RetailCustomerServiceImpl.java):
   - Review service methods for schema compliance

6. **Flyway Migration Updates**:
   - Update original Flyway scripts to match espi.xsd element sequence

7. **Testing**:
   - Update unit tests for RetailCustomer entity, service, repository
   - Add/update integration tests using TestContainers
   - Add XML marshalling/unmarshalling tests to verify schema compliance
   - Validate generated XML against espi.xsd

8. **Commit, Push, PR**

---

### Phase 14: Subscription
**Branch**: `feature/schema-compliance-phase-14-subscription`

**Dependencies**: RetailCustomer (via @ManyToOne)
**Referenced By**: UsagePoint (via @ManyToMany and @OneToOne)

**Tasks**:
1. **Entity Updates** (SubscriptionEntity.java):
   - Review field order against espi.xsd Subscription element sequence
   - Verify relationship to RetailCustomer
   - Verify @ManyToMany relationship to UsagePoint collection
   - Verify @OneToOne relationship to single UsagePoint

2. **DTO Updates** (SubscriptionDto.java):
   - **Follow DTO_PATTERN_GUIDE.md** - Standard pattern for ESPI DTOs
   - Use `@XmlAccessorType(XmlAccessType.PROPERTY)` - REQUIRED
   - Create explicit JavaBean-style getters (Records need `getFieldName()` for JAXB)
   - Match DTO field order to XSD element sequence in propOrder
   - Use correct namespace: `http://naesb.org/espi` for usage domain
   - Exclude Atom metadata fields (published, updated, selfLink, upLink)
   - Include only XSD-defined resource fields
   - Add uuid mapped to mRID attribute via getter
   - **Verify against DTO_PATTERN_GUIDE.md checklist before proceeding**

3. **MapStruct Mapper Updates** (SubscriptionMapper.java):
   - Update Entity-to-DTO conversion mapping
   - Update DTO-to-Entity conversion mapping

4. **Repository Updates** (SubscriptionRepository.java):
   - Keep ONLY index field queries
   - Remove all non-index queries not required for tests

5. **Service Updates** (SubscriptionService.java, SubscriptionServiceImpl.java):
   - Review service methods for schema compliance

6. **Flyway Migration Updates**:
   - Update original Flyway scripts to match espi.xsd element sequence

7. **Testing**:
   - Update unit tests for Subscription entity, service, repository
   - Add/update integration tests using TestContainers
   - Add XML marshalling/unmarshalling tests to verify schema compliance
   - Validate generated XML against espi.xsd

8. **Commit, Push, PR**

---

### Phase 15: BatchList
**Branch**: `feature/schema-compliance-phase-15-batch-list`

**Dependencies**: None (standalone entity)
**Referenced By**: None

**Tasks**:
1. **Entity Updates** (BatchListEntity.java):
   - Review field order against espi.xsd BatchList element sequence

2. **DTO Updates** (BatchListDto.java):
   - **Follow DTO_PATTERN_GUIDE.md** - Standard pattern for ESPI DTOs
   - Use `@XmlAccessorType(XmlAccessType.PROPERTY)` - REQUIRED
   - Create explicit JavaBean-style getters (Records need `getFieldName()` for JAXB)
   - Match DTO field order to XSD element sequence in propOrder
   - Use correct namespace: `http://naesb.org/espi` for usage domain
   - **Note**: BatchList may not extend IdentifiedObject per Issue #28 (standalone message type)
   - Include only XSD-defined resource fields
   - **Verify against DTO_PATTERN_GUIDE.md checklist before proceeding**

3. **MapStruct Mapper Updates** (BatchListMapper.java):
   - Update Entity-to-DTO conversion mapping
   - Update DTO-to-Entity conversion mapping

4. **Repository Updates** (BatchListRepository.java):
   - Keep ONLY index field queries
   - Remove all non-index queries not required for tests

5. **Service Updates** (BatchListService.java, BatchListServiceImpl.java):
   - Review service methods for schema compliance

6. **Flyway Migration Updates**:
   - Update original Flyway scripts to match espi.xsd element sequence

7. **Testing**:
   - Update unit tests for BatchList entity, service, repository
   - Add/update integration tests using TestContainers
   - Add XML marshalling/unmarshalling tests to verify schema compliance
   - Validate generated XML against espi.xsd

8. **Commit, Push, PR**

---

### Phase 16: UsagePoint
**Branch**: `feature/schema-compliance-phase-16-usage-point`

**Dependencies**: ServiceDeliveryPoint, PnodeRef, AggregatedNodeRef, TimeConfiguration (via Atom links), MeterReading (via Atom links), UsageSummary (via Atom links), ElectricPowerQualitySummary (via Atom links), RetailCustomer (via @ManyToOne), Subscription (via @ManyToMany and @OneToOne)
**Referenced By**: ServiceLocation (cross-stream href reference from customer.xsd PII stream)

**Tasks**:
1. **Entity Updates** (UsagePointEntity.java):
   - Review field order against espi.xsd UsagePoint element sequence
   - Verify embedded SummaryMeasurement fields (estimatedLoad, nominalServiceVoltage, ratedCurrent, ratedPower)
   - Verify @OneToOne relationship to ServiceDeliveryPoint
   - Verify @ManyToOne relationship to RetailCustomer
   - Verify @ManyToOne relationship to TimeConfiguration (via Atom links)
   - Verify @OneToMany relationships: MeterReading, UsageSummary, ElectricPowerQualitySummary, PnodeRef, AggregatedNodeRef collections
   - Verify @ManyToMany relationship to Subscription collection
   - Verify @OneToOne relationship to single Subscription

2. **DTO Updates** (UsagePointDto.java):
   - **Follow DTO_PATTERN_GUIDE.md** - Standard pattern for ESPI DTOs
   - Use `@XmlAccessorType(XmlAccessType.PROPERTY)` - REQUIRED
   - Create explicit JavaBean-style getters (Records need `getFieldName()` for JAXB)
   - Match DTO field order to XSD element sequence in propOrder
   - Use correct namespace: `http://naesb.org/espi` for usage domain
   - Exclude Atom metadata fields (published, updated, selfLink, upLink)
   - Include only XSD-defined resource fields
   - Add uuid mapped to mRID attribute via getter
   - Verify embedded SummaryMeasurement DTOs follow same pattern
   - Verify PnodeRef and AggregatedNodeRef collection DTOs follow same pattern
   - **CRITICAL**: Most complex entity - verify all embedded and relationship patterns carefully
   - **Verify against DTO_PATTERN_GUIDE.md checklist before proceeding**

3. **MapStruct Mapper Updates** (UsagePointMapper.java):
   - Update Entity-to-DTO conversion mapping
   - Update DTO-to-Entity conversion mapping
   - Handle all embedded and related entity mappings

4. **Repository Updates** (UsagePointRepository.java):
   - Keep ONLY index field queries
   - Remove all non-index queries not required for tests

5. **Service Updates** (UsagePointService.java, UsagePointServiceImpl.java):
   - Review service methods for schema compliance

6. **Flyway Migration Updates**:
   - Update original Flyway scripts to match espi.xsd element sequence

7. **Testing**:
   - Update unit tests for UsagePoint entity, service, repository
   - Add/update integration tests using TestContainers
   - Add XML marshalling/unmarshalling tests to verify schema compliance
   - Validate generated XML against espi.xsd

8. **Commit, Push, PR**

---

## Customer Domain Entities (Phases 17-26)

### Phase 17: ProgramDateIdMappings
**Branch**: `feature/schema-compliance-phase-17-program-date-id-mappings`

**Dependencies**: None (standalone entity)
**Referenced By**: CustomerAgreement (via bidirectional Atom rel='related' links)

**Tasks**:
1. **Entity Updates** (ProgramDateIdMappingsEntity.java):
   - Review field order against customer.xsd ProgramDateIdMappings element sequence
   - Verify JPA annotations match schema constraints
   - Verify JAXB annotations align with XML element names and order

2. **DTO Updates** (ProgramDateIdMappingsDto.java):
   - **Follow DTO_PATTERN_GUIDE.md** - Standard pattern for ESPI DTOs
   - Use `@XmlAccessorType(XmlAccessType.PROPERTY)` - REQUIRED
   - Create explicit JavaBean-style getters (Records need `getFieldName()` for JAXB)
   - Match DTO field order to XSD element sequence in propOrder
   - **Use correct namespace: `http://naesb.org/espi/customer` for customer domain**
   - Exclude Atom metadata fields (published, updated, selfLink, upLink)
   - Include only XSD-defined resource fields
   - Add uuid mapped to mRID attribute via getter
   - **Verify against DTO_PATTERN_GUIDE.md checklist before proceeding**

3. **MapStruct Mapper Updates** (ProgramDateIdMappingsMapper.java):
   - Update Entity-to-DTO conversion mapping
   - Update DTO-to-Entity conversion mapping

4. **Repository Updates** (ProgramDateIdMappingsRepository.java):
   - Keep ONLY index field queries
   - Remove all non-index queries not required for tests

5. **Service Updates** (ProgramDateIdMappingsService.java, ProgramDateIdMappingsServiceImpl.java):
   - Review service methods for schema compliance

6. **Flyway Migration Updates**:
   - Update original Flyway scripts to match customer.xsd element sequence

7. **Testing**:
   - Update unit tests for ProgramDateIdMappings entity, service, repository
   - Add/update integration tests using TestContainers
   - Add XML marshalling/unmarshalling tests to verify schema compliance
   - Validate generated XML against customer.xsd

8. **Commit, Push, PR**

---

### Phase 18: CustomerAccount
**Branch**: `feature/schema-compliance-phase-18-customer-account`

**Dependencies**: None (standalone entity)
**Referenced By**: CustomerAgreement (via bidirectional Atom rel='related' links), Statement (via bidirectional Atom rel='related' links), Customer (via bidirectional Atom rel='related' links)

**Tasks**:
1. **Entity Updates** (CustomerAccountEntity.java):
   - Review field order against customer.xsd CustomerAccount element sequence
   - Verify embedded AccountNotification field order

2. **DTO Updates** (CustomerAccountDto.java):
   - **Follow DTO_PATTERN_GUIDE.md** - Standard pattern for ESPI DTOs
   - Use `@XmlAccessorType(XmlAccessType.PROPERTY)` - REQUIRED
   - Create explicit JavaBean-style getters (Records need `getFieldName()` for JAXB)
   - Match DTO field order to XSD element sequence in propOrder
   - **Use correct namespace: `http://naesb.org/espi/customer` for customer domain**
   - Exclude Atom metadata fields (published, updated, selfLink, upLink)
   - Include only XSD-defined resource fields
   - Add uuid mapped to mRID attribute via getter
   - Verify embedded AccountNotification DTO follows same pattern
   - **Verify against DTO_PATTERN_GUIDE.md checklist before proceeding**

3. **MapStruct Mapper Updates** (CustomerAccountMapper.java):
   - Update Entity-to-DTO conversion mapping
   - Update DTO-to-Entity conversion mapping

4. **Repository Updates** (CustomerAccountRepository.java):
   - Keep ONLY index field queries
   - Remove all non-index queries not required for tests

5. **Service Updates** (CustomerAccountService.java, CustomerAccountServiceImpl.java):
   - Review service methods for schema compliance

6. **Flyway Migration Updates**:
   - Update original Flyway scripts to match customer.xsd element sequence

7. **Testing**:
   - Update unit tests for CustomerAccount entity, service, repository
   - Add/update integration tests using TestContainers
   - Add XML marshalling/unmarshalling tests to verify schema compliance
   - Validate generated XML against customer.xsd

8. **Commit, Push, PR**

---

### Phase 19: Statement
**Branch**: `feature/schema-compliance-phase-19-statement`

**Dependencies**: None (standalone entity)
**Referenced By**: Customer (via bidirectional Atom rel='related' links), CustomerAccount (via bidirectional Atom rel='related' links)

**Tasks**:
1. **Entity Updates** (StatementEntity.java):
   - Review field order against customer.xsd Statement element sequence
   - Verify embedded DateTimeInterval (billingPeriod) field order
   - Verify embedded SummaryMeasurement fields

2. **DTO Updates** (StatementDto.java):
   - **Follow DTO_PATTERN_GUIDE.md** - Standard pattern for ESPI DTOs
   - Use `@XmlAccessorType(XmlAccessType.PROPERTY)` - REQUIRED
   - Create explicit JavaBean-style getters (Records need `getFieldName()` for JAXB)
   - Match DTO field order to XSD element sequence in propOrder
   - **Use correct namespace: `http://naesb.org/espi/customer` for customer domain**
   - Exclude Atom metadata fields (published, updated, selfLink, upLink)
   - Include only XSD-defined resource fields
   - Add uuid mapped to mRID attribute via getter
   - Verify embedded DateTimeInterval and SummaryMeasurement DTOs follow same pattern
   - **Verify against DTO_PATTERN_GUIDE.md checklist before proceeding**

3. **MapStruct Mapper Updates** (StatementMapper.java):
   - Update Entity-to-DTO conversion mapping
   - Update DTO-to-Entity conversion mapping

4. **Repository Updates** (StatementRepository.java):
   - Keep ONLY index field queries
   - Remove all non-index queries not required for tests

5. **Service Updates** (StatementService.java, StatementServiceImpl.java):
   - Review service methods for schema compliance

6. **Flyway Migration Updates**:
   - Update original Flyway scripts to match customer.xsd element sequence

7. **Testing**:
   - Update unit tests for Statement entity, service, repository
   - Add/update integration tests using TestContainers
   - Add XML marshalling/unmarshalling tests to verify schema compliance
   - Validate generated XML against customer.xsd

8. **Commit, Push, PR**

---

### Phase 20: Customer
**Branch**: `feature/schema-compliance-phase-20-customer`

**Dependencies**: TimeConfiguration (via bidirectional Atom rel='related' links), Statement (via bidirectional Atom rel='related' links), CustomerAccount (via bidirectional Atom rel='related' links)
**Referenced By**: None

**Tasks**:
1. **Entity Updates** (CustomerEntity.java):
   - Review field order against customer.xsd Customer element sequence
   - Verify embedded Organisation fields (organisation, residence)
   - Verify embedded Status, Priority fields
   - Verify relationships to TimeConfiguration, Statement, CustomerAccount

2. **DTO Updates** (CustomerDto.java):
   - **Follow DTO_PATTERN_GUIDE.md** - Standard pattern for ESPI DTOs
   - Use `@XmlAccessorType(XmlAccessType.PROPERTY)` - REQUIRED
   - Create explicit JavaBean-style getters (Records need `getFieldName()` for JAXB)
   - Match DTO field order to XSD element sequence in propOrder
   - **Use correct namespace: `http://naesb.org/espi/customer` for customer domain**
   - Exclude Atom metadata fields (published, updated, selfLink, upLink)
   - Include only XSD-defined resource fields
   - Add uuid mapped to mRID attribute via getter
   - Verify embedded Organisation, Status, Priority DTOs follow same pattern
   - **Verify against DTO_PATTERN_GUIDE.md checklist before proceeding**

3. **MapStruct Mapper Updates** (CustomerMapper.java):
   - Update Entity-to-DTO conversion mapping
   - Update DTO-to-Entity conversion mapping

4. **Repository Updates** (CustomerRepository.java):
   - Keep ONLY index field queries
   - Remove all non-index queries not required for tests

5. **Service Updates** (CustomerService.java, CustomerServiceImpl.java):
   - Review service methods for schema compliance

6. **Flyway Migration Updates**:
   - Update original Flyway scripts to match customer.xsd element sequence

7. **Testing**:
   - Update unit tests for Customer entity, service, repository
   - Add/update integration tests using TestContainers
   - Add XML marshalling/unmarshalling tests to verify schema compliance
   - Validate generated XML against customer.xsd

8. **Commit, Push, PR**

---

### Phase 21: ServiceSupplier
**Branch**: `feature/schema-compliance-phase-21-service-supplier`

**Dependencies**: None (standalone entity)
**Referenced By**: CustomerAgreement (via bidirectional Atom rel='related' links)

**Tasks**:
1. **Entity Updates** (ServiceSupplierEntity.java):
   - Review field order against customer.xsd ServiceSupplier element sequence
   - Verify embedded Organisation fields

2. **DTO Updates** (ServiceSupplierDto.java):
   - **Follow DTO_PATTERN_GUIDE.md** - Standard pattern for ESPI DTOs
   - Use `@XmlAccessorType(XmlAccessType.PROPERTY)` - REQUIRED
   - Create explicit JavaBean-style getters (Records need `getFieldName()` for JAXB)
   - Match DTO field order to XSD element sequence in propOrder
   - **Use correct namespace: `http://naesb.org/espi/customer` for customer domain**
   - Exclude Atom metadata fields (published, updated, selfLink, upLink)
   - Include only XSD-defined resource fields
   - Add uuid mapped to mRID attribute via getter
   - Verify embedded Organisation DTOs follow same pattern
   - **Verify against DTO_PATTERN_GUIDE.md checklist before proceeding**

3. **MapStruct Mapper Updates** (ServiceSupplierMapper.java):
   - Update Entity-to-DTO conversion mapping
   - Update DTO-to-Entity conversion mapping

4. **Repository Updates** (ServiceSupplierRepository.java):
   - Keep ONLY index field queries
   - Remove all non-index queries not required for tests

5. **Service Updates** (ServiceSupplierService.java, ServiceSupplierServiceImpl.java):
   - Review service methods for schema compliance

6. **Flyway Migration Updates**:
   - Update original Flyway scripts to match customer.xsd element sequence

7. **Testing**:
   - Update unit tests for ServiceSupplier entity, service, repository
   - Add/update integration tests using TestContainers
   - Add XML marshalling/unmarshalling tests to verify schema compliance
   - Validate generated XML against customer.xsd

8. **Commit, Push, PR**

---

### Phase 22: Asset
**Branch**: `feature/schema-compliance-phase-22-asset`

**Dependencies**: None (base class for EndDevice)
**Referenced By**: EndDevice (subclass)

**Tasks**:
1. **Entity Updates** (AssetEntity.java):
   - Review field order against customer.xsd Asset element sequence
   - Verify embedded Status field order
   - Verify JPA @Inheritance and @DiscriminatorColumn annotations

2. **DTO Updates** (AssetDto.java):
   - **Follow DTO_PATTERN_GUIDE.md** - Standard pattern for ESPI DTOs
   - Use `@XmlAccessorType(XmlAccessType.PROPERTY)` - REQUIRED
   - Create explicit JavaBean-style getters (Records need `getFieldName()` for JAXB)
   - Match DTO field order to XSD element sequence in propOrder
   - **Use correct namespace: `http://naesb.org/espi/customer` for customer domain**
   - Exclude Atom metadata fields (published, updated, selfLink, upLink)
   - Include only XSD-defined resource fields
   - Add uuid mapped to mRID attribute via getter
   - Verify embedded Status DTO follows same pattern
   - **Verify against DTO_PATTERN_GUIDE.md checklist before proceeding**

3. **MapStruct Mapper Updates** (AssetMapper.java):
   - Update Entity-to-DTO conversion mapping
   - Update DTO-to-Entity conversion mapping

4. **Repository Updates** (AssetRepository.java):
   - Keep ONLY index field queries
   - Remove all non-index queries not required for tests

5. **Service Updates** (AssetService.java, AssetServiceImpl.java):
   - Review service methods for schema compliance

6. **Flyway Migration Updates**:
   - Update original Flyway scripts to match customer.xsd element sequence

7. **Testing**:
   - Update unit tests for Asset entity, service, repository
   - Add/update integration tests using TestContainers
   - Add XML marshalling/unmarshalling tests to verify schema compliance
   - Validate generated XML against customer.xsd

8. **Commit, Push, PR**

---

### Phase 23: ServiceLocation
**Branch**: `feature/schema-compliance-phase-23-service-location`

**Dependencies**: TimeConfiguration (via bidirectional Atom rel='related' links), UsagePoint (cross-stream reference via href URL from espi.xsd), EndDevice (via bidirectional Atom rel='related' links)
**Referenced By**: CustomerAgreement (via bidirectional Atom rel='related' links), EndDevice (via bidirectional Atom rel='related' links)

**Note**: ServiceLocation exists in customer.xsd PII data stream, UsagePoint exists in espi.xsd non-PII data stream. ServiceLocation stores UsagePoint's rel="self" href URL directly, NOT via Atom link element.

**Tasks**:
1. **Entity Updates** (ServiceLocationEntity.java):
   - Review field order against customer.xsd ServiceLocation element sequence
   - Verify embedded StreetAddress fields (mainAddress, secondaryAddress)
   - Verify embedded ElectronicAddress field
   - Verify embedded Status field
   - Verify relationship to TimeConfiguration (via Atom links)
   - Verify UsagePoint href URL storage (String field, NOT Atom link)
   - Verify relationship to EndDevice (via Atom links)

2. **DTO Updates** (ServiceLocationDto.java):
   - **Follow DTO_PATTERN_GUIDE.md** - Standard pattern for ESPI DTOs
   - Use `@XmlAccessorType(XmlAccessType.PROPERTY)` - REQUIRED
   - Create explicit JavaBean-style getters (Records need `getFieldName()` for JAXB)
   - Match DTO field order to XSD element sequence in propOrder
   - **Use correct namespace: `http://naesb.org/espi/customer` for customer domain**
   - Exclude Atom metadata fields (published, updated, selfLink, upLink)
   - Include only XSD-defined resource fields
   - Add uuid mapped to mRID attribute via getter
   - Verify embedded StreetAddress, ElectronicAddress, Status DTOs follow same pattern
   - **CRITICAL**: UsagePoint reference is href URL string (XSD field), NOT Atom link
   - **Verify against DTO_PATTERN_GUIDE.md checklist before proceeding**

3. **MapStruct Mapper Updates** (ServiceLocationMapper.java):
   - Update Entity-to-DTO conversion mapping
   - Update DTO-to-Entity conversion mapping

4. **Repository Updates** (ServiceLocationRepository.java):
   - Keep ONLY index field queries
   - Remove all non-index queries not required for tests

5. **Service Updates** (ServiceLocationService.java, ServiceLocationServiceImpl.java):
   - Review service methods for schema compliance

6. **Flyway Migration Updates**:
   - Update original Flyway scripts to match customer.xsd element sequence

7. **Testing**:
   - Update unit tests for ServiceLocation entity, service, repository
   - Add/update integration tests using TestContainers
   - Add XML marshalling/unmarshalling tests to verify schema compliance
   - Validate that UsagePoint reference is href URL string, NOT Atom link
   - Validate generated XML against customer.xsd

8. **Commit, Push, PR**

---

### Phase 24: CustomerAgreement
**Branch**: `feature/schema-compliance-phase-24-customer-agreement`

**Dependencies**: CustomerAccount (via bidirectional Atom rel='related' links), ServiceLocation (via bidirectional Atom rel='related' links), ServiceSupplier (via bidirectional Atom rel='related' links), ProgramDateIdMappings (via bidirectional Atom rel='related' links)
**Referenced By**: ServiceSupplier (via bidirectional Atom rel='related' links), ServiceLocation (via bidirectional Atom rel='related' links), ProgramDateIdMappings (via bidirectional Atom rel='related' links)

**Tasks**:
1. **Entity Updates** (CustomerAgreementEntity.java):
   - Review field order against customer.xsd CustomerAgreement element sequence
   - Verify embedded DateTimeInterval (validityInterval) field order
   - Verify relationships to CustomerAccount, ServiceLocation, ServiceSupplier, ProgramDateIdMappings

2. **DTO Updates** (CustomerAgreementDto.java):
   - **Follow DTO_PATTERN_GUIDE.md** - Standard pattern for ESPI DTOs
   - Use `@XmlAccessorType(XmlAccessType.PROPERTY)` - REQUIRED
   - Create explicit JavaBean-style getters (Records need `getFieldName()` for JAXB)
   - Match DTO field order to XSD element sequence in propOrder
   - **Use correct namespace: `http://naesb.org/espi/customer` for customer domain**
   - Exclude Atom metadata fields (published, updated, selfLink, upLink)
   - Include only XSD-defined resource fields
   - Add uuid mapped to mRID attribute via getter
   - Verify embedded DateTimeInterval DTO follows same pattern
   - **Verify against DTO_PATTERN_GUIDE.md checklist before proceeding**

3. **MapStruct Mapper Updates** (CustomerAgreementMapper.java):
   - Update Entity-to-DTO conversion mapping
   - Update DTO-to-Entity conversion mapping

4. **Repository Updates** (CustomerAgreementRepository.java):
   - Keep ONLY index field queries
   - Remove all non-index queries not required for tests

5. **Service Updates** (CustomerAgreementService.java, CustomerAgreementServiceImpl.java):
   - Review service methods for schema compliance

6. **Flyway Migration Updates**:
   - Update original Flyway scripts to match customer.xsd element sequence

7. **Testing**:
   - Update unit tests for CustomerAgreement entity, service, repository
   - Add/update integration tests using TestContainers
   - Add XML marshalling/unmarshalling tests to verify schema compliance
   - Validate generated XML against customer.xsd

8. **Commit, Push, PR**

---

### Phase 25: EndDevice
**Branch**: `feature/schema-compliance-phase-25-end-device`

**Dependencies**: Asset (base class), ServiceLocation (via bidirectional Atom rel='related' links)
**Referenced By**: Meter (subclass), ServiceLocation (via bidirectional Atom rel='related' links)

**Note**: EndDevice and ServiceLocation have bidirectional Atom rel='related' links. Since ServiceLocation is processed in Phase 23, the EndDevice → ServiceLocation link will be added in this phase.

**Tasks**:
1. **Entity Updates** (EndDeviceEntity.java):
   - Review field order against customer.xsd EndDevice element sequence
   - Verify inheritance from AssetEntity
   - Verify embedded Status, Organisation.ElectronicAddress fields
   - Verify relationship to ServiceLocation (via Atom links)

2. **DTO Updates** (EndDeviceDto.java):
   - **Follow DTO_PATTERN_GUIDE.md** - Standard pattern for ESPI DTOs
   - Use `@XmlAccessorType(XmlAccessType.PROPERTY)` - REQUIRED
   - Create explicit JavaBean-style getters (Records need `getFieldName()` for JAXB)
   - Match DTO field order to XSD element sequence in propOrder
   - **Use correct namespace: `http://naesb.org/espi/customer` for customer domain**
   - Exclude Atom metadata fields (published, updated, selfLink, upLink)
   - Include only XSD-defined resource fields
   - Add uuid mapped to mRID attribute via getter
   - Verify embedded Status and Organisation.ElectronicAddress DTOs follow same pattern
   - Verify inheritance from AssetDto
   - **Verify against DTO_PATTERN_GUIDE.md checklist before proceeding**

3. **MapStruct Mapper Updates** (EndDeviceMapper.java):
   - Update Entity-to-DTO conversion mapping
   - Update DTO-to-Entity conversion mapping

4. **Repository Updates** (EndDeviceRepository.java):
   - Keep ONLY index field queries
   - Remove all non-index queries not required for tests

5. **Service Updates** (EndDeviceService.java, EndDeviceServiceImpl.java):
   - Review service methods for schema compliance

6. **Flyway Migration Updates**:
   - Update original Flyway scripts to match customer.xsd element sequence

7. **Testing**:
   - Update unit tests for EndDevice entity, service, repository
   - Add/update integration tests using TestContainers
   - Add XML marshalling/unmarshalling tests to verify schema compliance
   - Validate bidirectional Atom link with ServiceLocation
   - Validate generated XML against customer.xsd

8. **Commit, Push, PR**

---

### Phase 26: Meter
**Branch**: `feature/schema-compliance-phase-26-meter`

**Dependencies**: EndDevice (base class), ServiceLocation (inherited from EndDevice via Atom links)
**Referenced By**: None

**Tasks**:
1. **Entity Updates** (MeterEntity.java):
   - Review field order against customer.xsd Meter element sequence
   - Verify inheritance from EndDeviceEntity
   - Verify Meter-specific fields (formNumber, intervalLength)

2. **DTO Updates** (MeterDto.java):
   - **Follow DTO_PATTERN_GUIDE.md** - Standard pattern for ESPI DTOs
   - Use `@XmlAccessorType(XmlAccessType.PROPERTY)` - REQUIRED
   - Create explicit JavaBean-style getters (Records need `getFieldName()` for JAXB)
   - Match DTO field order to XSD element sequence in propOrder
   - **Use correct namespace: `http://naesb.org/espi/customer` for customer domain**
   - Exclude Atom metadata fields (published, updated, selfLink, upLink)
   - Include only XSD-defined resource fields
   - Add uuid mapped to mRID attribute via getter
   - Verify inheritance from EndDeviceDto
   - **Verify against DTO_PATTERN_GUIDE.md checklist before proceeding**

3. **MapStruct Mapper Updates** (MeterMapper.java):
   - Update Entity-to-DTO conversion mapping
   - Update DTO-to-Entity conversion mapping

4. **Repository Updates** (MeterRepository.java):
   - Keep ONLY index field queries
   - Remove all non-index queries not required for tests

5. **Service Updates** (MeterService.java, MeterServiceImpl.java):
   - Review service methods for schema compliance

6. **Flyway Migration Updates**:
   - Update original Flyway scripts to match customer.xsd element sequence

7. **Testing**:
   - Update unit tests for Meter entity, service, repository
   - Add/update integration tests using TestContainers
   - Add XML marshalling/unmarshalling tests to verify schema compliance
   - Validate generated XML against customer.xsd

8. **Commit, Push, PR**

---

## Summary

This 26-phase plan ensures comprehensive schema compliance review for all NAESB ESPI 4.0 domain entities and associated classes. Each phase focuses on a single entity and includes:

1. Entity field order verification against XSD schema
2. DTO field order verification and JAXB annotations
3. Bidirectional MapStruct mapper updates (Entity-to-DTO and DTO-to-Entity)
4. Repository query simplification (index fields only)
5. Service method schema compliance review
6. Flyway migration script updates (original scripts, no new scripts)
7. Comprehensive testing including XML marshalling/unmarshalling and XSD validation
8. Git workflow (branch, commit, push, PR merge)

**Processing Order**:
- Usage domain first (Phases 1-16)
- Customer domain second (Phases 17-26)
- Dependency order (parents before children)
- Simple to complex entities within each dependency level

**Key Notes**:
- MeterReading has NO child elements, ONLY Atom links
- ServiceLocation (customer.xsd) references UsagePoint (espi.xsd) via href URL string, NOT Atom link (cross-stream reference)
- UsageSummary contains LineItem collection (costAdditionalDetailLastPeriod)
- EndDevice and ServiceLocation have bidirectional Atom rel='related' links
- CustomerAgreement has bidirectional Atom links with ServiceSupplier, ServiceLocation, and ProgramDateIdMappings
- Statement has bidirectional Atom links with both Customer and CustomerAccount
- Customer has bidirectional Atom links with TimeConfiguration, Statement, and CustomerAccount

**Git Branch Naming Pattern**:
`feature/schema-compliance-phase-{number}-{entity-name}`

**Test Requirements**:
- Unit tests for entity, service, repository
- Integration tests using TestContainers (MySQL, PostgreSQL, H2)
- XML marshalling/unmarshalling tests for ALL phases
- XSD schema validation (espi.xsd for usage domain, customer.xsd for customer domain)
