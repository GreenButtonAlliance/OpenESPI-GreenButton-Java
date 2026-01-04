# Multi-Phase NAESB ESPI Schema Compliance Plan

## Overview

This plan reviews domain entities in `/domain/usage`, `/domain/customer`, and `/domain/common` to ensure compliance with NAESB ESPI 4.0 schema (espi.xsd and customer.xsd) element sequences.

---

## Technology Stack

**Baseline (Spring Boot 4.0 + Java 25):**
- **Java**: 25 (LTS - Zulu 25.28+85-CA)
- **Spring Boot**: 4.0.1
- **Jakarta EE**: 11
- **Maven**: 3.9+
- **MapStruct**: 1.6.3
- **Jackson 3 XML**: tools.jackson.dataformat:jackson-dataformat-xml:3.0.3
- **Jackson JAXB Module**: tools.jackson.module:jackson-module-jakarta-xmlbind-annotations:3.0.3
- **TestContainers**: 1.20.x

**Testing Framework:**
- **JUnit**: 5.x (JUnit Platform 6.0.1)
- **Mockito**: 5.x via @MockitoBean/@MockitoSpyBean (Spring Boot 4.0)
- **AssertJ**: 3.x
- **Test Dependencies**: Granular test starters:
  - `spring-boot-starter-webmvc-test`
  - `spring-boot-starter-data-jpa-test`
  - `spring-boot-starter-validation-test`
  - `spring-boot-starter-restclient-test`

**DTO Marshalling Approach:**
- **Jackson 3 (jackson-dataformat-xml)** with Jakarta XML Bind annotations - Selected for all 26 phases
- **Serialization Engine**: Jackson XmlMapper (jackson-dataformat-xml)
- **DTO Annotations**: Jakarta XML Bind (JAXB 3.0) annotations (jakarta.xml.bind.annotation.*)
- **Bridge Module**: jackson-module-jakarta-xmlbind-annotations (enables Jackson to process JAXB annotations)
- **Rationale**: Combines Jackson 3's modern features and performance with JAXB's standardized annotations, leveraging Spring Boot 4's managed dependencies

**IMPORTANT:** All 26 phases are implemented against the Spring Boot 4.0 + Java 25 baseline established by PR #50 (merged 2025-12-29).

---

**Excluded Entities** (already completed):
- ApplicationInformationEntity
- AuthorizationEntity

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
   - Update field JavaDoc to reference XSD documentation

2. **DTO Updates** (TimeConfigurationDto.java):
   - Match DTO field order to espi.xsd TimeConfiguration element sequence
   - Verify Jakarta XML Bind annotations for XML marshalling (jakarta.xml.bind.annotation.*)
     - `@XmlRootElement`, `@XmlElement`, `@XmlAttribute`, `@XmlType(propOrder)`
     - Jackson will process these annotations via jackson-module-jakarta-xmlbind-annotations
   - Add XSD constraint validation annotations

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

7. **Testing** (Spring Boot 4.0 Patterns):
   - **Unit Tests**: Update tests for TimeConfiguration entity, service, repository
     - Use `@MockitoBean` instead of deprecated `@MockBean`
     - Use `@MockitoSpyBean` instead of deprecated `@SpyBean`
     - Add `@AutoConfigureMockMvc` if using `@SpringBootTest` with web layer
   - **Integration Tests**: Add/update tests using TestContainers
     - TestContainers dependency: `org.testcontainers:testcontainers-junit-jupiter` (artifact ID changed in Spring Boot 4.0)
   - **XML Marshalling Tests**: Create Jackson 3 XML marshalling/unmarshalling tests with JAXB annotations
     - Use pure JUnit 5 (no Spring Boot test dependencies required)
     - Use Jackson XmlMapper for serialization (processes Jakarta XML Bind annotations)
     - Ensure jackson-module-jakarta-xmlbind-annotations is available
     - Verify element sequence matches espi.xsd
     - Test round-trip serialization (marshal → unmarshal → verify equality)
   - **XSD Validation**: Validate generated XML against espi.xsd using schema validation

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
   - Match DTO field order to espi.xsd ReadingType element sequence
   - Verify embedded RationalNumber and ReadingInterharmonic DTOs
   - Add XSD constraint validation annotations

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
   - Match DTO field order to espi.xsd IntervalReading element sequence
   - Verify embedded DateTimeInterval DTO

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
   - Match DTO field order to espi.xsd ReadingQuality element sequence

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
   - Match DTO field order to espi.xsd IntervalBlock element sequence
   - Verify IntervalReading collection DTO

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
   - Match DTO field order to espi.xsd MeterReading element sequence
   - Verify ReadingType reference (Atom link, not embedded)
   - Verify IntervalBlock references (Atom links, not embedded collection)

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
   - Match DTO field order to espi.xsd ServiceDeliveryPoint element sequence

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
   - Match DTO field order to espi.xsd PnodeRef element sequence

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
   - Match DTO field order to espi.xsd AggregatedNodeRef element sequence

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
   - Match DTO field order to espi.xsd LineItem element sequence

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
   - Match DTO field order to espi.xsd UsageSummary element sequence
   - Verify LineItem collection DTO

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
   - Match DTO field order to espi.xsd ElectricPowerQualitySummary element sequence

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
   - Match DTO field order to espi.xsd RetailCustomer element sequence

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
   - Match DTO field order to espi.xsd Subscription element sequence

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
   - Match DTO field order to espi.xsd BatchList element sequence

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
   - Match DTO field order to espi.xsd UsagePoint element sequence

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

2. **DTO Updates** (ProgramDateIdMappingsDto.java):
   - Match DTO field order to customer.xsd ProgramDateIdMappings element sequence
   - Verify Jakarta XML Bind annotations for XML marshalling (jakarta.xml.bind.annotation.*)
     - `@XmlRootElement`, `@XmlElement`, `@XmlAttribute`, `@XmlType(propOrder)`
     - Jackson will process these annotations via jackson-module-jakarta-xmlbind-annotations

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
   - Match DTO field order to customer.xsd CustomerAccount element sequence

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
   - Match DTO field order to customer.xsd Statement element sequence

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
   - Match DTO field order to customer.xsd Customer element sequence

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
   - Match DTO field order to customer.xsd ServiceSupplier element sequence

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
   - Match DTO field order to customer.xsd Asset element sequence

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
   - Match DTO field order to customer.xsd ServiceLocation element sequence
   - Verify UsagePoint href URL field

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
   - Match DTO field order to customer.xsd CustomerAgreement element sequence

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
   - Match DTO field order to customer.xsd EndDevice element sequence

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
   - Match DTO field order to customer.xsd Meter element sequence

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
2. DTO field order verification with Jakarta XML Bind (JAXB 3.0) annotations
3. Bidirectional MapStruct mapper updates (Entity-to-DTO and DTO-to-Entity)
4. Repository query simplification (index fields only)
5. Service method schema compliance review
6. Flyway migration script updates (original scripts, no new scripts)
7. Comprehensive testing including Jackson 3 XML marshalling/unmarshalling and XSD validation
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

**Test Requirements** (Spring Boot 4.0 + Java 25):
- **Unit Tests**: Entity, service, repository tests using:
  - `@MockitoBean` (NOT `@MockBean` - deprecated in Spring Boot 4.0)
  - `@MockitoSpyBean` (NOT `@SpyBean` - deprecated in Spring Boot 4.0)
  - `@AutoConfigureMockMvc` annotation required when using `@SpringBootTest` with web layer
- **Integration Tests**: TestContainers for MySQL, PostgreSQL, H2
  - Dependency: `org.testcontainers:testcontainers-junit-jupiter` (artifact ID changed from `junit-jupiter`)
- **XML Marshalling Tests**: Jackson 3 XML marshalling/unmarshalling tests for ALL 26 phases
  - Pure JUnit 5 tests (no Spring Boot test dependencies needed)
  - Use Jackson XmlMapper (from jackson-dataformat-xml)
  - DTOs use Jakarta XML Bind annotations (jakarta.xml.bind.annotation.*)
  - Requires jackson-module-jakarta-xmlbind-annotations module
  - Verify XSD element sequence compliance
  - Test round-trip serialization
- **XSD Schema Validation**:
  - Usage domain (Phases 1-16): Validate against `espi.xsd`
  - Customer domain (Phases 17-26): Validate against `customer.xsd`

**Spring Boot 4.0 Migration Notes:**
- Test annotation package relocations:
  - `@WebMvcTest`: `org.springframework.boot.webmvc.test.autoconfigure` (moved from `org.springframework.boot.test.autoconfigure.web.servlet`)
  - `@DataJpaTest`: `org.springframework.boot.data.jpa.test.autoconfigure` (moved from `org.springframework.boot.test.autoconfigure.orm.jpa`)
- Granular test dependencies replace single `spring-boot-starter-test`
- See `.junie/guidelines.md` for comprehensive Spring Boot 4.0 test migration guidance
