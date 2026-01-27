# Phase 24: CustomerAgreement ESPI 4.0 Schema Compliance Implementation Plan

## Overview

Implement complete CustomerAgreement ESPI 4.0 schema compliance following the successful Phase 18 pattern. CustomerAgreement extends Agreement which extends Document, similar to how CustomerAccount extends Document.

**Branch**: `feature/schema-compliance-phase-24-customer-agreement`
**Dependencies**: CustomerAccount, ServiceLocation, ServiceSupplier, ProgramDateIdMappings (via Atom rel='related' links)
**Referenced By**: ServiceSupplier, ServiceLocation, ProgramDateIdMappings (via bidirectional Atom rel='related' links)

## Atom Protocol Architecture (CRITICAL UNDERSTANDING)

### Separation of Concerns

**Atom Wrapper (AtomFeedDto & CustomerAtomEntryDto)**:
- Contains metadata: id, title, published, updated
- Contains navigation: links (self, up, related)
- Handles relationships via `<link rel="related" href="..."/>`
- Wraps ESPI resource in `<atom:entry><content>` element

**ESPI Resource DTO (CustomerAgreementDto)**:
- Contains ONLY customer.xsd schema-defined fields
- NO Atom fields (published, updated, id, links)
- NO IdentifiedObject fields (description is mapped to AtomEntryDto.title)
- NO embedded relationship DTOs (use Atom links instead)

**Correct XML Structure**:
```xml
<atom:feed xmlns:atom="http://www.w3.org/2005/Atom" xmlns:cust="http://naesb.org/espi/customer">
  <atom:entry>
    <atom:id>urn:uuid:xxx</atom:id>              <!-- CustomerAtomEntryDto.id -->
    <atom:title>Agreement Title</atom:title>     <!-- CustomerAtomEntryDto.title -->
    <atom:published>2025-01-25T...</atom:published>  <!-- CustomerAtomEntryDto.published -->
    <atom:updated>2025-01-25T...</atom:updated>      <!-- CustomerAtomEntryDto.updated -->
    <atom:link rel="self" href=".../CustomerAgreement/123"/>      <!-- CustomerAtomEntryDto.links -->
    <atom:link rel="up" href=".../CustomerAgreement"/>
    <atom:link rel="related" href=".../CustomerAccount/456"/>     <!-- NOT embedded DTO! -->

    <cust:CustomerAgreement>                     <!-- CustomerAgreementDto content -->
      <!-- ONLY customer.xsd fields here, NO Atom fields -->
      <cust:type>CONTRACT</cust:type>
      <cust:authorName>John Doe</cust:authorName>
      <cust:createdDateTime>2025-01-01T...</cust:createdDateTime>
      ...
      <cust:agreementId>AGR-123</cust:agreementId>
    </cust:CustomerAgreement>
  </atom:entry>
</atom:feed>
```

**Reference Implementation**: Phase 18 CustomerAccountDto correctly implements this pattern.

## Schema Analysis

### Inheritance Chain
```
CustomerAgreement extends Agreement extends Document extends IdentifiedObject
```

### Field Ordering (per customer.xsd)

**Document fields** (lines 819-872):
1. type (String256)
2. authorName (String256)
3. createdDateTime (TimeType)
4. lastModifiedDateTime (TimeType)
5. revisionNumber (String256)
6. electronicAddress (ElectronicAddress)
7. subject (String256)
8. title (String256)
9. docStatus (Status)

**Agreement fields** (lines 622-660):
10. signDate (TimeType)
11. validityInterval (DateTimeInterval)

**CustomerAgreement fields** (lines 159-260):
12. loadMgmt (String256) - optional
13. isPrePay (boolean) - optional
14. shutOffDateTime (TimeType) - optional
15. DemandResponseProgram (collection) - optional - TODO: Future implementation
16. PricingStructures (collection) - optional - TODO: Future implementation
17. currency (Currency/String3) - optional
18. futureStatus (Status collection) - optional, [extension]
19. agreementId (String256) - optional, [extension]

## Current State Analysis

### ✅ Existing Files
- **CustomerAgreementEntity.java**: Exists, needs Document field additions and reordering
- **CustomerAgreementDto.java**: Exists, needs complete rewrite with correct field ordering
- **CustomerAgreementMapper.java**: Exists, needs updates for new fields

### ❌ Missing Files
- **CustomerAgreementRepository.java**: Does not exist, needs creation
- **CustomerAgreementService.java**: Does not exist, needs creation
- **CustomerAgreementServiceImpl.java**: Does not exist, needs creation
- **CustomerAgreementDtoTest.java**: Does not exist, needs creation
- **CustomerAgreementRepositoryTest.java**: Does not exist, needs creation

## Issues Identified

### 1. Entity Field Order Issues
**Current order in CustomerAgreementEntity.java**:
```java
// Document fields (lines 50-86)
createdDateTime, lastModifiedDateTime, revisionNumber, subject, title, type

// Agreement fields (lines 88-100)
signDate, validityInterval

// CustomerAgreement fields (lines 102-158)
loadMgmt, isPrePay, shutOffDateTime, currency, futureStatus, agreementId
```

**Problems**:
- ❌ Missing Document fields: type first, authorName, electronicAddress, docStatus
- ❌ Wrong field order: type should be FIRST, not last in Document section
- ❌ Missing embedded Status for docStatus
- ❌ Missing Organisation.ElectronicAddress for electronicAddress
- ❌ futureStatus uses CustomerEntity.Status instead of Status embeddable
- ⚠️ @ElementCollection with @AttributeOverrides wrapper (should apply directly per java:S1710)

**Correct order per XSD**:
```java
// Document fields (1-9)
type, authorName, createdDateTime, lastModifiedDateTime, revisionNumber,
electronicAddress, subject, title, docStatus

// Agreement fields (10-11)
signDate, validityInterval

// CustomerAgreement fields (12-19)
loadMgmt, isPrePay, shutOffDateTime, currency, futureStatus, agreementId
```

### 2. DTO Field Order Issues - ATOM FIELDS MUST BE EXCLUDED

**IMPORTANT**: Atom protocol fields (published, updated, id, title, links) are handled by AtomFeedDto, AtomEntryDto (specifically CustomerAtomEntryDto), and LinkDto. These fields MUST NOT appear in CustomerAgreementDto.

**Current propOrder in CustomerAgreementDto.java** (line 41-45):
```java
"published", "updated", "selfLink", "upLink", "relatedLinks",
"description", "signDate", "validityInterval", "customerAccount",
"serviceLocations", "statements"
```

**CRITICAL Problems**:
- ❌ **Atom Fields Present** (WRONG!): published, updated, selfLink, upLink, relatedLinks
  - These are handled by CustomerAtomEntryDto and AtomFeedDto, NOT CustomerAgreementDto
- ❌ **IdentifiedObject Fields Present** (WRONG!): description (id handled by AtomEntryDto.id, description by AtomEntryDto.title)
- ❌ **Relationship DTOs Present** (WRONG!): customerAccount, serviceLocations, statements
  - Relationships handled via Atom `<link rel="related">` in AtomEntryDto.links, NOT embedded DTOs
- ❌ **Missing ALL Document Fields**: type, authorName, createdDateTime, lastModifiedDateTime, revisionNumber, electronicAddress, subject, title, docStatus
- ❌ **Missing ALL CustomerAgreement Fields**: loadMgmt, isPrePay, shutOffDateTime, currency, futureStatus, agreementId
- ❌ **Wrong Type**: validityInterval is String instead of DateTimeIntervalDto

**Correct propOrder per XSD** (ONLY customer.xsd fields, NO Atom fields):
```java
"type", "authorName", "createdDateTime", "lastModifiedDateTime", "revisionNumber",
"electronicAddress", "subject", "title", "docStatus",
"signDate", "validityInterval",
"loadMgmt", "isPrePay", "shutOffDateTime", "currency", "futureStatus", "agreementId"
```

**Reference: Phase 18 CustomerAccountDto** (correctly excludes Atom fields):
- ✅ Contains ONLY 15 XSD-defined fields (9 Document + 6 CustomerAccount)
- ✅ NO Atom fields (published, updated, id, links)
- ✅ NO IdentifiedObject fields (description)
- ✅ NO embedded relationship DTOs

### 3. Mapper Issues
**CustomerAgreementMapper.java** needs updates for:
- All 9 Document field mappings
- All 2 Agreement field mappings
- All 6 CustomerAgreement field mappings (excluding TODO collections)
- Status embeddable mapping (reuse StatusMapper from Phase 18)
- ElectronicAddress embeddable mapping (reuse ElectronicAddressMapper from Phase 18)
- DateTimeInterval mapping

### 4. Missing Infrastructure
- No Repository interface/implementation
- No Service interface/implementation
- No DTO tests for XML marshalling
- No Repository tests for CRUD operations

## Implementation Tasks

### Task 1: Entity Updates (CustomerAgreementEntity.java)

**1.1 Add Missing Document Fields**
```java
// Add these fields at the top in correct Document field order
@Column(name = "document_type", length = 256)
private String type;  // Move from line 85 to after class declaration

@Column(name = "author_name", length = 256)
private String authorName;  // NEW FIELD

// createdDateTime (already exists)
// lastModifiedDateTime (already exists)
// revisionNumber (already exists)

@Embedded
private Organisation.ElectronicAddress electronicAddress;  // NEW FIELD

// subject (already exists)
// title (already exists)

@Embedded
private Status docStatus;  // NEW FIELD (replace futureStatus CustomerEntity.Status usage)
```

**1.2 Reorder All Fields to Match XSD**
Reorder sections:
1. Document fields (9 fields total)
2. Agreement fields (2 fields: signDate, validityInterval)
3. CustomerAgreement fields (6 fields, excluding TODO collections)

**1.3 Fix futureStatus Field**
```java
// BEFORE:
@ElementCollection
@CollectionTable(name = "customer_agreement_future_status", joinColumns = @JoinColumn(name = "customer_agreement_id"))
@AttributeOverrides({...})
private List<CustomerEntity.Status> futureStatus;

// AFTER:
@ElementCollection(fetch = FetchType.LAZY)
@CollectionTable(name = "customer_agreement_future_status", joinColumns = @JoinColumn(name = "customer_agreement_id"))
@AttributeOverride(name = "value", column = @Column(name = "status_value"))
@AttributeOverride(name = "dateTime", column = @Column(name = "status_date_time"))
@AttributeOverride(name = "reason", column = @Column(name = "status_reason"))
private List<Status> futureStatus;
```

**1.4 Apply @AttributeOverride Directly Without Wrapper**
Per java:S1710, apply annotations directly on class level for upLink/selfLink:
```java
@Entity
@Table(name = "customer_agreements")
@AttributeOverride(name = "upLink.rel", column = @Column(name = "customer_agreement_up_link_rel"))
@AttributeOverride(name = "upLink.href", column = @Column(name = "customer_agreement_up_link_href"))
@AttributeOverride(name = "upLink.type", column = @Column(name = "customer_agreement_up_link_type"))
@AttributeOverride(name = "selfLink.rel", column = @Column(name = "customer_agreement_self_link_rel"))
@AttributeOverride(name = "selfLink.href", column = @Column(name = "customer_agreement_self_link_href"))
@AttributeOverride(name = "selfLink.type", column = @Column(name = "customer_agreement_self_link_type"))
```

**1.5 Update equals/hashCode for Pattern Matching**
```java
Class<?> oEffectiveClass = o instanceof HibernateProxy hibernateProxy ?
    hibernateProxy.getHibernateLazyInitializer().getPersistentClass() : o.getClass();
```

### Task 2: DTO Updates (CustomerAgreementDto.java)

**2.1 Remove ALL Atom Fields and Relationship DTOs**
Delete these fields (handled by AtomEntryDto/LinkDto):
- ❌ id (Long) - replaced by uuid (String)
- ❌ published (OffsetDateTime) - handled by CustomerAtomEntryDto
- ❌ updated (OffsetDateTime) - handled by CustomerAtomEntryDto
- ❌ relatedLinks (List<LinkDto>) - handled by CustomerAtomEntryDto.links
- ❌ selfLink (LinkDto) - handled by CustomerAtomEntryDto.links
- ❌ upLink (LinkDto) - handled by CustomerAtomEntryDto.links
- ❌ description (String) - handled by CustomerAtomEntryDto.title
- ❌ customerAccount (CustomerAccountDto) - handled by Atom `<link rel="related">`
- ❌ serviceLocations (List<ServiceLocationDto>) - handled by Atom `<link rel="related">`
- ❌ statements (List<StatementDto>) - handled by Atom `<link rel="related">`
- ❌ Helper methods (getSelfHref, getUpHref, generateSelfHref, generateUpHref)

**2.2 Complete Rewrite with ONLY XSD-Defined Fields**
```java
@XmlRootElement(name = "CustomerAgreement", namespace = "http://naesb.org/espi/customer")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CustomerAgreement", namespace = "http://naesb.org/espi/customer", propOrder = {
    "type", "authorName", "createdDateTime", "lastModifiedDateTime", "revisionNumber",
    "electronicAddress", "subject", "title", "docStatus",
    "signDate", "validityInterval",
    "loadMgmt", "isPrePay", "shutOffDateTime", "currency", "futureStatus", "agreementId"
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerAgreementDto {

    // UUID for internal mapping (NOT marshalled to XML - handled by AtomEntryDto.id)
    @XmlTransient
    private String uuid;

    // Document fields (9)
    @XmlElement(name = "type", namespace = "http://naesb.org/espi/customer")
    private String type;

    @XmlElement(name = "authorName", namespace = "http://naesb.org/espi/customer")
    private String authorName;

    @XmlElement(name = "createdDateTime", namespace = "http://naesb.org/espi/customer")
    private OffsetDateTime createdDateTime;

    @XmlElement(name = "lastModifiedDateTime", namespace = "http://naesb.org/espi/customer")
    private OffsetDateTime lastModifiedDateTime;

    @XmlElement(name = "revisionNumber", namespace = "http://naesb.org/espi/customer")
    private String revisionNumber;

    @XmlElement(name = "electronicAddress", namespace = "http://naesb.org/espi/customer")
    private CustomerDto.ElectronicAddressDto electronicAddress;

    @XmlElement(name = "subject", namespace = "http://naesb.org/espi/customer")
    private String subject;

    @XmlElement(name = "title", namespace = "http://naesb.org/espi/customer")
    private String title;

    @XmlElement(name = "docStatus", namespace = "http://naesb.org/espi/customer")
    private StatusDto docStatus;

    // Agreement fields (2)
    @XmlElement(name = "signDate", namespace = "http://naesb.org/espi/customer")
    private OffsetDateTime signDate;

    @XmlElement(name = "validityInterval", namespace = "http://naesb.org/espi/customer")
    private DateTimeIntervalDto validityInterval;

    // CustomerAgreement fields (6)
    @XmlElement(name = "loadMgmt", namespace = "http://naesb.org/espi/customer")
    private String loadMgmt;

    @XmlElement(name = "isPrePay", namespace = "http://naesb.org/espi/customer")
    private Boolean isPrePay;

    @XmlElement(name = "shutOffDateTime", namespace = "http://naesb.org/espi/customer")
    private OffsetDateTime shutOffDateTime;

    @XmlElement(name = "currency", namespace = "http://naesb.org/espi/customer")
    private String currency;

    @XmlElement(name = "futureStatus", namespace = "http://naesb.org/espi/customer")
    @XmlElementWrapper(name = "futureStatus", namespace = "http://naesb.org/espi/customer")
    private List<StatusDto> futureStatus;

    @XmlElement(name = "agreementId", namespace = "http://naesb.org/espi/customer")
    private String agreementId;
}
```

**NOTE**: Reuse existing nested DTOs from Phase 18:
- `CustomerAccountDto.StatusDto` for docStatus and futureStatus fields
- `CustomerDto.ElectronicAddressDto` for electronicAddress field
- Create `DateTimeIntervalDto` if not already available

### Task 3: Mapper Updates (CustomerAgreementMapper.java)

**3.1 Add All Field Mappings**
```java
@Mapper(componentModel = "spring", uses = {
    StatusMapper.class,
    ElectronicAddressMapper.class,
    DateTimeIntervalMapper.class
})
public interface CustomerAgreementMapper {

    @Mapping(target = "uuid", source = "id")

    // Document fields (9)
    @Mapping(target = "type", source = "type")
    @Mapping(target = "authorName", source = "authorName")
    @Mapping(target = "createdDateTime", source = "createdDateTime")
    @Mapping(target = "lastModifiedDateTime", source = "lastModifiedDateTime")
    @Mapping(target = "revisionNumber", source = "revisionNumber")
    @Mapping(target = "electronicAddress", source = "electronicAddress")
    @Mapping(target = "subject", source = "subject")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "docStatus", source = "docStatus")

    // Agreement fields (2)
    @Mapping(target = "signDate", source = "signDate")
    @Mapping(target = "validityInterval", source = "validityInterval")

    // CustomerAgreement fields (6)
    @Mapping(target = "loadMgmt", source = "loadMgmt")
    @Mapping(target = "isPrePay", source = "isPrePay")
    @Mapping(target = "shutOffDateTime", source = "shutOffDateTime")
    @Mapping(target = "currency", source = "currency")
    @Mapping(target = "futureStatus", source = "futureStatus")
    @Mapping(target = "agreementId", source = "agreementId")

    CustomerAgreementDto toDto(CustomerAgreementEntity entity);

    @InheritInverseConfiguration
    CustomerAgreementEntity toEntity(CustomerAgreementDto dto);
}
```

**3.2 Create DateTimeIntervalMapper** (if missing)
```java
@Mapper(componentModel = "spring")
public interface DateTimeIntervalMapper {
    DateTimeIntervalDto toDto(DateTimeInterval entity);
    DateTimeInterval toEntity(DateTimeIntervalDto dto);
}
```

### Task 4: Repository Creation (CustomerAgreementRepository.java)

**4.1 Create Repository Interface**
```java
package org.greenbuttonalliance.espi.common.repositories.customer;

import org.greenbuttonalliance.espi.common.domain.customer.entity.CustomerAgreementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Spring Data JPA repository for CustomerAgreementEntity.
 * Phase 24: CustomerAgreement schema compliance.
 *
 * Only uses inherited JpaRepository methods to avoid H2 keyword conflicts.
 */
@Repository
public interface CustomerAgreementRepository extends JpaRepository<CustomerAgreementEntity, UUID> {
    // Use only inherited methods: findById, findAll, save, delete, count, existsById
    // No custom query methods to avoid H2 keyword conflicts
}
```

### Task 5: Service Layer Creation

**5.1 Create Service Interface**
```java
package org.greenbuttonalliance.espi.common.service.customer;

import org.greenbuttonalliance.espi.common.domain.customer.entity.CustomerAgreementEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for CustomerAgreement entity.
 * Phase 24: CustomerAgreement schema compliance.
 */
public interface CustomerAgreementService {

    /**
     * Save a customer agreement.
     */
    CustomerAgreementEntity save(CustomerAgreementEntity agreement);

    /**
     * Find customer agreement by ID.
     */
    Optional<CustomerAgreementEntity> findById(UUID id);

    /**
     * Find all customer agreements.
     */
    List<CustomerAgreementEntity> findAll();

    /**
     * Delete customer agreement by ID.
     */
    void deleteById(UUID id);

    /**
     * Check if customer agreement exists.
     */
    boolean existsById(UUID id);

    /**
     * Count all customer agreements.
     */
    long count();
}
```

**5.2 Create Service Implementation**
```java
package org.greenbuttonalliance.espi.common.service.customer.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.greenbuttonalliance.espi.common.domain.customer.entity.CustomerAgreementEntity;
import org.greenbuttonalliance.espi.common.repositories.customer.CustomerAgreementRepository;
import org.greenbuttonalliance.espi.common.service.EspiIdGeneratorService;
import org.greenbuttonalliance.espi.common.service.customer.CustomerAgreementService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service implementation for CustomerAgreement entity.
 * Phase 24: CustomerAgreement schema compliance.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerAgreementServiceImpl implements CustomerAgreementService {

    private static final String NAMESPACE = "ESPI-CUSTOMER-AGREEMENT";

    private final CustomerAgreementRepository repository;
    private final EspiIdGeneratorService idGenerator;

    @Override
    @Transactional
    public CustomerAgreementEntity save(CustomerAgreementEntity agreement) {
        if (agreement.getId() == null) {
            String seed = agreement.getAgreementId() != null ?
                agreement.getAgreementId() : UUID.randomUUID().toString();
            UUID deterministicId = idGenerator.generateV5UUID(NAMESPACE, seed);
            agreement.setId(deterministicId);
            log.debug("Generated UUID v5 for CustomerAgreement: {}", deterministicId);
        }
        return repository.save(agreement);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CustomerAgreementEntity> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerAgreementEntity> findAll() {
        return repository.findAll();
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(UUID id) {
        return repository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return repository.count();
    }
}
```

### Task 6: Flyway Migration Updates

**6.1 Update V3__Create_additiional_Base_Tables.sql**

Add missing Document fields to customer_agreements table:
```sql
-- Add missing Document fields
ALTER TABLE customer_agreements ADD COLUMN IF NOT EXISTS document_type VARCHAR(256);
ALTER TABLE customer_agreements ADD COLUMN IF NOT EXISTS author_name VARCHAR(256);
ALTER TABLE customer_agreements ADD COLUMN IF NOT EXISTS status_value VARCHAR(256);
ALTER TABLE customer_agreements ADD COLUMN IF NOT EXISTS status_date_time TIMESTAMP WITH TIME ZONE;
ALTER TABLE customer_agreements ADD COLUMN IF NOT EXISTS status_reason VARCHAR(512);

-- Add ElectronicAddress embedded fields
ALTER TABLE customer_agreements ADD COLUMN IF NOT EXISTS email1 VARCHAR(256);
ALTER TABLE customer_agreements ADD COLUMN IF NOT EXISTS email2 VARCHAR(256);
ALTER TABLE customer_agreements ADD COLUMN IF NOT EXISTS web VARCHAR(256);
ALTER TABLE customer_agreements ADD COLUMN IF NOT EXISTS radio VARCHAR(256);

-- Reorder columns to match XSD (PostgreSQL/MySQL - comment out for H2)
-- Document fields: type, author_name, created_date_time, last_modified_date_time,
--                  revision_number, email1, email2, web, radio, subject, title,
--                  status_value, status_date_time, status_reason
-- Agreement fields: sign_date, validity_interval_start, validity_interval_duration
-- CustomerAgreement fields: load_mgmt, is_pre_pay, shut_off_date_time, currency, agreement_id
```

### Task 7: Testing

**7.1 Create CustomerAgreementDtoTest.java**

Follow Phase 18 CustomerAccountDtoTest pattern:
```java
@DisplayName("CustomerAgreementDto XML Marshalling Tests")
class CustomerAgreementDtoTest {

    private DtoExportServiceImpl dtoExportService;

    @BeforeEach
    void setUp() {
        EspiIdGeneratorService espiIdGeneratorService = new EspiIdGeneratorService();
        dtoExportService = new DtoExportServiceImpl(null, null, espiIdGeneratorService);
    }

    @Test
    @DisplayName("Should export CustomerAgreement with complete Document/Agreement fields")
    void shouldExportCustomerAgreementWithCompleteFields() {
        // Test all 17 fields marshal correctly
        // Verify field order matches customer.xsd
    }

    @Test
    @DisplayName("Should verify CustomerAgreement field order matches customer.xsd")
    void shouldVerifyCustomerAgreementFieldOrder() {
        // Assert Document fields order (lines 819-872)
        // Assert Agreement fields order
        // Assert CustomerAgreement fields order (lines 159-260)
    }

    @Test
    @DisplayName("Should use correct customer namespace")
    void shouldUseCorrectCustomerNamespace() {
        // Verify cust: namespace prefix usage
        // Verify http://naesb.org/espi/customer namespace
    }
}
```

**7.2 Create CustomerAgreementRepositoryTest.java**

Follow Phase 18 CustomerAccountRepositoryTest pattern with 21+ tests:
```java
@DisplayName("CustomerAgreement Repository Tests")
class CustomerAgreementRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private CustomerAgreementRepository customerAgreementRepository;

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperationsTest {
        // 7 tests: save, retrieve, update, delete, findAll, exists, count
    }

    @Nested
    @DisplayName("Document Field Persistence")
    class DocumentFieldPersistenceTest {
        // 3 tests: All Document fields, electronicAddress, docStatus
    }

    @Nested
    @DisplayName("Agreement Field Persistence")
    class AgreementFieldPersistenceTest {
        // 2 tests: signDate, validityInterval
    }

    @Nested
    @DisplayName("CustomerAgreement Field Persistence")
    class CustomerAgreementFieldPersistenceTest {
        // 3 tests: All CustomerAgreement fields, futureStatus collection, null optional fields
    }

    @Nested
    @DisplayName("Base Class Functionality")
    class BaseClassTest {
        // 5 tests: IdentifiedObject inheritance, timestamps, unique IDs, equals/hashCode, toString
    }
}
```

**7.3 Service and Mapper Tests**
- Create CustomerAgreementServiceTest
- Create CustomerAgreementMapperTest (if needed)

### Task 8: Code Quality (SonarQube Compliance)

Ensure zero violations by following Phase 18 patterns:

**8.1 Avoid Common Violations**
- ✅ No IOException throws on test methods
- ✅ Chain multiple assertions using fluent API
- ✅ Apply @AttributeOverride directly without wrapper
- ✅ Make all embeddable objects Serializable
- ✅ Use instanceof with pattern variables
- ✅ Remove unused variables
- ✅ No Thread.sleep() in tests
- ✅ No empty catch blocks
- ✅ Use hasSameHashCodeAs() for hashCode assertions
- ✅ Remove all commented-out code

### Task 9: Commit, Push, PR

**9.1 Git Workflow**
```bash
# Create feature branch
git checkout -b feature/schema-compliance-phase-24-customer-agreement

# Stage all changes
git add openespi-common/src/main/java/org/greenbuttonalliance/espi/common/domain/customer/entity/CustomerAgreementEntity.java
git add openespi-common/src/main/java/org/greenbuttonalliance/espi/common/dto/customer/CustomerAgreementDto.java
git add openespi-common/src/main/java/org/greenbuttonalliance/espi/common/mapper/customer/CustomerAgreementMapper.java
git add openespi-common/src/main/java/org/greenbuttonalliance/espi/common/mapper/customer/DateTimeIntervalMapper.java  # if new
git add openespi-common/src/main/java/org/greenbuttonalliance/espi/common/repositories/customer/CustomerAgreementRepository.java
git add openespi-common/src/main/java/org/greenbuttonalliance/espi/common/service/customer/CustomerAgreementService.java
git add openespi-common/src/main/java/org/greenbuttonalliance/espi/common/service/customer/impl/CustomerAgreementServiceImpl.java
git add openespi-common/src/main/resources/db/migration/V3__Create_additiional_Base_Tables.sql
git add openespi-common/src/test/java/org/greenbuttonalliance/espi/common/dto/customer/CustomerAgreementDtoTest.java
git add openespi-common/src/test/java/org/greenbuttonalliance/espi/common/repositories/customer/CustomerAgreementRepositoryTest.java

# Commit with detailed message
git commit -m "feat: ESPI 4.0 Schema Compliance - Phase 24: CustomerAgreement Implementation"

# Push and create PR
git push -u origin feature/schema-compliance-phase-24-customer-agreement
gh pr create --base main --title "feat: Phase 24 - CustomerAgreement ESPI 4.0 Compliance" --body "..."
```

**9.2 Update Issue #28**
```bash
gh issue comment 28 --body "Phase 24: CustomerAgreement implementation completed. PR #XX ready for review."
```

## Expected File Changes Summary

| File | Change Type | Description |
|------|-------------|-------------|
| CustomerAgreementEntity.java | MODIFY | Add 3 Document fields, reorder all fields, fix futureStatus type, add @AttributeOverride |
| CustomerAgreementDto.java | REWRITE | **Remove ALL Atom fields**, remove relationship DTOs, add 17 XSD fields, correct propOrder |
| CustomerAgreementMapper.java | MODIFY | Add 17 field mappings, use StatusMapper/ElectronicAddressMapper |
| DateTimeIntervalMapper.java | CREATE | New mapper for DateTimeInterval ↔ DateTimeIntervalDto |
| CustomerAgreementRepository.java | CREATE | New JpaRepository interface |
| CustomerAgreementService.java | CREATE | New service interface |
| CustomerAgreementServiceImpl.java | CREATE | New service implementation with UUID v5 generation |
| V3__Create_additiional_Base_Tables.sql | MODIFY | Add 8 new columns for Document fields |
| CustomerAgreementDtoTest.java | CREATE | 3+ XML marshalling tests |
| CustomerAgreementRepositoryTest.java | CREATE | 21+ repository tests |

**Total**: 10 files (3 modified, 7 created)

## Success Criteria

✅ All 609+ existing tests pass
✅ All new CustomerAgreement tests pass (24+ new tests)
✅ Zero SonarQube violations
✅ Integration tests pass (H2, MySQL, PostgreSQL)
✅ XML marshalling validates against customer.xsd
✅ Field ordering matches customer.xsd exactly
✅ All Document base class fields implemented
✅ All Agreement base class fields implemented
✅ All CustomerAgreement specific fields implemented
✅ Repository, service, and mapper layers complete
✅ CI/CD pipeline passes all checks

## References

- **Phase 18 Implementation**: CustomerAccount (successful pattern to follow)
- **customer.xsd**: Lines 159-260 (CustomerAgreement), 622-660 (Agreement), 819-872 (Document)
- **Issue #28**: Phase 24 task list
- **CLAUDE.md**: Project conventions and patterns

## Notes

1. **🚨 CRITICAL: NO ATOM FIELDS IN DTO** 🚨
   - CustomerAgreementDto must NOT contain: published, updated, id, title, links, description
   - These are handled by CustomerAtomEntryDto and AtomFeedDto
   - Current DTO incorrectly has these fields - they MUST be removed
   - CustomerAccountDto from Phase 18 shows correct pattern (ONLY XSD fields)

2. **Reuse Existing Mappers**: StatusMapper and ElectronicAddressMapper from Phase 18

3. **Serializable Requirement**: All embedded classes must implement Serializable

4. **UUID v5 Generation**: Use namespace "ESPI-CUSTOMER-AGREEMENT" with agreementId seed

5. **TODO Collections**: DemandResponseProgram and PricingStructures commented out - future implementation

6. **Atom Links**: Relationships to CustomerAccount/ServiceLocation/ServiceSupplier via Atom `<link rel="related">` links (NOT embedded DTOs)

7. **futureStatus**: Element collection of Status objects (not CustomerEntity.Status)

8. **Follow Phase 18 Pattern**: Use CustomerAccount implementation as template for consistency

---

**Plan Created**: 2026-01-25
**Target Completion**: Phase 24 implementation
**Next Phase**: Phase 25 - EndDevice
