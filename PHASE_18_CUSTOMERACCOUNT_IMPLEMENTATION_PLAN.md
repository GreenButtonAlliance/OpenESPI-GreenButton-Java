# Phase 18: CustomerAccount - ESPI 4.0 Schema Compliance Implementation Plan

**Branch**: `feature/schema-compliance-phase-18-customer-account`

**Target**: Full compliance with NAESB ESPI 4.0 customer.xsd CustomerAccount element sequence

---

## Overview

CustomerAccount is a customer domain entity that extends Document (which extends IdentifiedObject). It represents an assignment of products and services purchased by a customer through a customer agreement, used for billing and payment.

### XSD Schema Structure

**customer.xsd CustomerAccount definition (lines 118-158)**:
```xml
<xs:complexType name="CustomerAccount">
  <xs:complexContent>
    <xs:extension base="Document">
      <xs:sequence>
        <xs:element name="billingCycle" type="String256" minOccurs="0"/>
        <xs:element name="budgetBill" type="String256" minOccurs="0"/>
        <xs:element name="lastBillAmount" type="Int48" minOccurs="0"/>
        <xs:element name="notifications" type="AccountNotification" minOccurs="0" maxOccurs="unbounded"/>
        <xs:element name="contactInfo" type="Organisation" minOccurs="0"/>
        <xs:element name="accountId" type="String256" minOccurs="0"/>
      </xs:sequence>
    </xs:extension>
  </xs:complexContent>
</xs:complexType>
```

**customer.xsd Document base class (lines 819-872)**:
```xml
<xs:complexType name="Document">
  <xs:complexContent>
    <xs:extension base="IdentifiedObject">
      <xs:sequence>
        <xs:element name="type" type="String256" minOccurs="0"/>
        <xs:element name="authorName" type="String256" minOccurs="0"/>
        <xs:element name="createdDateTime" type="TimeType" minOccurs="0"/>
        <xs:element name="lastModifiedDateTime" type="TimeType" minOccurs="0"/>
        <xs:element name="revisionNumber" type="String256" minOccurs="0"/>
        <xs:element name="electronicAddress" type="ElectronicAddress" minOccurs="0"/>
        <xs:element name="subject" type="String256" minOccurs="0"/>
        <xs:element name="title" type="String256" minOccurs="0"/>
        <xs:element name="docStatus" type="Status" minOccurs="0"/>
      </xs:sequence>
    </xs:extension>
  </xs:complexContent>
</xs:complexType>
```

### Complete Field Order (IdentifiedObject → Document → CustomerAccount)

1. **IdentifiedObject fields** (inherited):
   - mRID (UUID in entity)
   - description
   - published
   - selfLink
   - upLink
   - relatedLinks

2. **Document fields** (inherited):
   - type
   - authorName
   - createdDateTime
   - lastModifiedDateTime
   - revisionNumber
   - electronicAddress
   - subject
   - title
   - docStatus

3. **CustomerAccount fields** (specific):
   - billingCycle
   - budgetBill
   - lastBillAmount
   - notifications (collection of AccountNotification)
   - contactInfo (Organisation embedded object)
   - accountId

---

## Current State Analysis

### Issues Found

#### CustomerAccountEntity.java (openespi-common/src/main/java/org/greenbuttonalliance/espi/common/domain/customer/entity/CustomerAccountEntity.java)

**Missing Document Fields**:
- ❌ `authorName` - Missing entirely
- ❌ `electronicAddress` - Missing (ElectronicAddress embedded type)
- ❌ `docStatus` - Missing (Status embedded type)

**Incorrect Field Types**:
- ❌ Line 130: `contactInfo` is String, should be Organisation embedded object
- ❌ Line 143: `isPrePay` is a custom field not in XSD (extension field, needs to be at end)

**Field Order Issues**:
- ❌ Document fields (lines 64-95) are not in XSD sequence order
- ❌ CustomerAccount fields (lines 103-136) are not in XSD sequence order
- ❌ Customer relationship field should not be in entity (relationship field, not XML serialized)

#### CustomerAccountDto.java (openespi-common/src/main/java/org/greenbuttonalliance/espi/common/dto/customer/CustomerAccountDto.java)

**Critical Issues**:
- ❌ Line 41-45: `propOrder` is completely wrong - doesn't match XSD element sequence
- ❌ Missing all Document base class fields except description
- ❌ Line 78-93: Fields are not in correct XSD order
- ❌ Line 80: `accountNumber` field doesn't exist in XSD (should be removed or marked as extension)
- ❌ Line 92: `transactionDate` field doesn't exist in XSD (should be removed)
- ❌ Line 98-103: Customer and CustomerAgreement relationships should not be embedded in DTO

**Missing Fields**:
- ❌ All Document fields: type, authorName, createdDateTime, lastModifiedDateTime, revisionNumber, electronicAddress, subject, title, docStatus
- ❌ CustomerAccount.contactInfo should be Organisation type, not String
- ❌ CustomerAccount.notifications collection

---

## Task 1: Entity Updates

**File**: `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/domain/customer/entity/CustomerAccountEntity.java`

### Required Changes

1. **Add Missing Document Fields** (after line 95, before CustomerAccount fields):
```java
/**
 * Name of the author of this document.
 */
@Column(name = "author_name", length = 256)
private String authorName;

/**
 * Electronic address.
 */
@Embedded
@AttributeOverrides({
    @AttributeOverride(name = "email1", column = @Column(name = "electronic_address_email1", length = 256)),
    @AttributeOverride(name = "email2", column = @Column(name = "electronic_address_email2", length = 256)),
    @AttributeOverride(name = "web", column = @Column(name = "electronic_address_web", length = 256)),
    @AttributeOverride(name = "radio", column = @Column(name = "electronic_address_radio", length = 256)),
    @AttributeOverride(name = "landLineNumber", column = @Column(name = "electronic_address_land_line", length = 256)),
    @AttributeOverride(name = "mobileNumber", column = @Column(name = "electronic_address_mobile", length = 256))
})
private ElectronicAddress electronicAddress;

/**
 * Status of this document. For status of subject matter this document represents
 * (e.g., Agreement, Work), use 'status' attribute.
 */
@Embedded
@AttributeOverrides({
    @AttributeOverride(name = "value", column = @Column(name = "doc_status_value", length = 256)),
    @AttributeOverride(name = "dateTime", column = @Column(name = "doc_status_date_time")),
    @AttributeOverride(name = "reason", column = @Column(name = "doc_status_reason", length = 256)),
    @AttributeOverride(name = "remark", column = @Column(name = "doc_status_remark", length = 256))
})
private Status docStatus;
```

2. **Reorder Document Fields** (lines 59-95) to match XSD sequence:
```java
// Document fields (in XSD order)

/**
 * Type of this document.
 */
@Column(name = "document_type", length = 256)
private String type;

/**
 * Name of the author of this document.
 */
@Column(name = "author_name", length = 256)
private String authorName;

/**
 * Date and time that this document was created.
 */
@Column(name = "created_date_time")
private OffsetDateTime createdDateTime;

/**
 * Date and time that this document was last modified.
 */
@Column(name = "last_modified_date_time")
private OffsetDateTime lastModifiedDateTime;

/**
 * Revision number for this document.
 */
@Column(name = "revision_number", length = 256)
private String revisionNumber;

/**
 * Electronic address.
 */
@Embedded
@AttributeOverrides({...})
private ElectronicAddress electronicAddress;

/**
 * Subject of this document.
 */
@Column(name = "subject", length = 256)
private String subject;

/**
 * Title of this document.
 */
@Column(name = "title", length = 256)
private String title;

/**
 * Status of this document.
 */
@Embedded
@AttributeOverrides({...})
private Status docStatus;
```

3. **Fix contactInfo Field Type** (line 129-130):
```java
// BEFORE (WRONG):
@Column(name = "contact_name", length = 256)
private String contactInfo;

// AFTER (CORRECT):
/**
 * [extension] Customer contact information used to identify individual
 * responsible for billing and payment of CustomerAccount.
 */
@Embedded
@AttributeOverrides({
    @AttributeOverride(name = "organisationName", column = @Column(name = "contact_org_name", length = 256)),
    @AttributeOverride(name = "streetAddress.streetDetail", column = @Column(name = "contact_street_detail", length = 256)),
    @AttributeOverride(name = "streetAddress.townDetail", column = @Column(name = "contact_town_detail", length = 256)),
    @AttributeOverride(name = "streetAddress.stateOrProvince", column = @Column(name = "contact_state_province", length = 256)),
    @AttributeOverride(name = "streetAddress.postalCode", column = @Column(name = "contact_postal_code", length = 256)),
    @AttributeOverride(name = "streetAddress.country", column = @Column(name = "contact_country", length = 256)),
    @AttributeOverride(name = "postalAddress.streetDetail", column = @Column(name = "contact_postal_street_detail", length = 256)),
    @AttributeOverride(name = "postalAddress.townDetail", column = @Column(name = "contact_postal_town_detail", length = 256)),
    @AttributeOverride(name = "postalAddress.stateOrProvince", column = @Column(name = "contact_postal_state_province", length = 256)),
    @AttributeOverride(name = "postalAddress.postalCode", column = @Column(name = "contact_postal_postal_code", length = 256)),
    @AttributeOverride(name = "postalAddress.country", column = @Column(name = "contact_postal_country", length = 256)),
    @AttributeOverride(name = "electronicAddress.email1", column = @Column(name = "contact_email1", length = 256)),
    @AttributeOverride(name = "electronicAddress.email2", column = @Column(name = "contact_email2", length = 256)),
    @AttributeOverride(name = "electronicAddress.web", column = @Column(name = "contact_web", length = 256))
})
private Organisation contactInfo;
```

4. **Reorder CustomerAccount Fields** (lines 97-136) to match XSD sequence:
```java
// CustomerAccount specific fields (in XSD order)

/**
 * Cycle day on which the associated customer account will normally be billed.
 */
@Column(name = "billing_cycle", length = 256)
private String billingCycle;

/**
 * Budget bill code.
 */
@Column(name = "budget_bill", length = 256)
private String budgetBill;

/**
 * The last amount that will be billed to the customer prior to shut off of the account.
 */
@Column(name = "last_bill_amount")
private Long lastBillAmount;

/**
 * Set of customer account notifications.
 */
@ElementCollection(fetch = FetchType.LAZY)
@CollectionTable(name = "customer_account_notifications", joinColumns = @JoinColumn(name = "customer_account_id"))
private List<AccountNotification> notifications;

/**
 * [extension] Customer contact information.
 */
@Embedded
@AttributeOverrides({...})
private Organisation contactInfo;

/**
 * [extension] Customer account identifier.
 */
@Column(name = "account_id", length = 256)
private String accountId;

// Extension fields (not in XSD, must be at end)

/**
 * [extension] Indicates whether this customer account is a prepaid account.
 */
@Column(name = "is_pre_pay")
private Boolean isPrePay;

// Relationship fields (not serialized to XML)

/**
 * Customer that owns this account.
 */
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "customer_id")
private CustomerEntity customer;
```

5. **Update toString()** method to include new fields in correct order

### Verification Checklist for Entity

- [ ] All Document base class fields present (type, authorName, createdDateTime, lastModifiedDateTime, revisionNumber, electronicAddress, subject, title, docStatus)
- [ ] Document fields in correct XSD sequence
- [ ] All CustomerAccount fields present (billingCycle, budgetBill, lastBillAmount, notifications, contactInfo, accountId)
- [ ] CustomerAccount fields in correct XSD sequence
- [ ] contactInfo is Organisation type (not String)
- [ ] Extension fields (isPrePay) at end with [extension] comment
- [ ] Relationship fields (customer) at end
- [ ] JPA annotations correct (@Embedded, @AttributeOverrides, @ElementCollection)
- [ ] Column names follow snake_case convention
- [ ] toString() includes all fields in order

---

## Task 2: DTO Updates

**File**: `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/dto/customer/CustomerAccountDto.java`

### Required Changes

1. **Completely Rewrite @XmlType propOrder** (line 41-45):
```java
@XmlType(name = "CustomerAccount", namespace = "http://naesb.org/espi/customer", propOrder = {
    // IdentifiedObject fields
    "description",
    // Document fields
    "type",
    "authorName",
    "createdDateTime",
    "lastModifiedDateTime",
    "revisionNumber",
    "electronicAddress",
    "subject",
    "title",
    "docStatus",
    // CustomerAccount fields
    "billingCycle",
    "budgetBill",
    "lastBillAmount",
    "notifications",
    "contactInfo",
    "accountId"
})
```

2. **Add Missing Document Fields** (after description, before current CustomerAccount fields):
```java
@XmlElement(name = "type")
private String type;

@XmlElement(name = "authorName")
private String authorName;

@XmlElement(name = "createdDateTime")
private OffsetDateTime createdDateTime;

@XmlElement(name = "lastModifiedDateTime")
private OffsetDateTime lastModifiedDateTime;

@XmlElement(name = "revisionNumber")
private String revisionNumber;

@XmlElement(name = "electronicAddress")
private ElectronicAddress electronicAddress;

@XmlElement(name = "subject")
private String subject;

@XmlElement(name = "title")
private String title;

@XmlElement(name = "docStatus")
private Status docStatus;
```

3. **Fix contactInfo Field Type** (line 77):
```java
// BEFORE (WRONG):
// contactInfo field doesn't exist in current DTO

// AFTER (CORRECT):
@XmlElement(name = "contactInfo")
private Organisation contactInfo;
```

4. **Remove Non-XSD Fields**:
```java
// REMOVE these lines:
@XmlElement(name = "accountNumber")
private String accountNumber;

@XmlElement(name = "transactionDate")
private OffsetDateTime transactionDate;

@XmlElement(name = "Customer")
private CustomerDto customer;

@XmlElement(name = "CustomerAgreement")
@XmlElementWrapper(name = "CustomerAgreements")
private List<CustomerAgreementDto> customerAgreements;
```

5. **Add notifications Collection**:
```java
@XmlElement(name = "AccountNotification")
@XmlElementWrapper(name = "notifications")
private List<AccountNotification> notifications;
```

6. **Update Constructors** to match new field list

7. **Update Helper Methods** (getSelfHref, getUpHref, generateSelfHref, generateUpHref)

### Verification Checklist for DTO

- [ ] @XmlType propOrder matches exact XSD element sequence
- [ ] All IdentifiedObject fields present (mRID as uuid, description)
- [ ] All Document fields present (type, authorName, createdDateTime, lastModifiedDateTime, revisionNumber, electronicAddress, subject, title, docStatus)
- [ ] All CustomerAccount fields present (billingCycle, budgetBill, lastBillAmount, notifications, contactInfo, accountId)
- [ ] contactInfo is Organisation type
- [ ] No non-XSD fields (accountNumber, transactionDate removed)
- [ ] No relationship objects embedded (Customer, CustomerAgreements removed)
- [ ] JAXB annotations correct (@XmlElement with correct names)
- [ ] Constructors updated
- [ ] Helper methods work correctly

---

## Task 3: MapStruct Mapper Updates

**File**: `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/mapper/customer/CustomerAccountMapper.java`

### Required Changes

1. **Add Missing Field Mappings**:
```java
@Mapping(source = "authorName", target = "authorName")
@Mapping(source = "electronicAddress", target = "electronicAddress")
@Mapping(source = "docStatus", target = "docStatus")
@Mapping(source = "contactInfo", target = "contactInfo")
@Mapping(source = "notifications", target = "notifications")
```

2. **Update toDto() Method**:
```java
@Mapping(source = "id", target = "id")
@Mapping(source = "uuid", target = "uuid")
@Mapping(source = "description", target = "description")
@Mapping(source = "published", target = "published")
@Mapping(source = "type", target = "type")
@Mapping(source = "authorName", target = "authorName")
@Mapping(source = "createdDateTime", target = "createdDateTime")
@Mapping(source = "lastModifiedDateTime", target = "lastModifiedDateTime")
@Mapping(source = "revisionNumber", target = "revisionNumber")
@Mapping(source = "electronicAddress", target = "electronicAddress")
@Mapping(source = "subject", target = "subject")
@Mapping(source = "title", target = "title")
@Mapping(source = "docStatus", target = "docStatus")
@Mapping(source = "billingCycle", target = "billingCycle")
@Mapping(source = "budgetBill", target = "budgetBill")
@Mapping(source = "lastBillAmount", target = "lastBillAmount")
@Mapping(source = "notifications", target = "notifications")
@Mapping(source = "contactInfo", target = "contactInfo")
@Mapping(source = "accountId", target = "accountId")
@Mapping(target = "selfLink", ignore = true)
@Mapping(target = "upLink", ignore = true)
@Mapping(target = "relatedLinks", ignore = true)
CustomerAccountDto toDto(CustomerAccountEntity entity);
```

3. **Update toEntity() Method** with corresponding reverse mappings

4. **Handle Embedded Object Mappings**:
   - ElectronicAddress mapping
   - Organisation mapping
   - Status mapping
   - AccountNotification collection mapping

### Verification Checklist for Mapper

- [ ] All Document fields mapped (both directions)
- [ ] All CustomerAccount fields mapped (both directions)
- [ ] Embedded objects map correctly (ElectronicAddress, Organisation, Status)
- [ ] Collections map correctly (notifications)
- [ ] Link fields handled correctly (selfLink, upLink, relatedLinks)
- [ ] No unmapped target property warnings in build
- [ ] MapStruct generated code compiles

---

## Task 4: Repository Updates

**File**: `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/repositories/customer/CustomerAccountRepository.java`

### Required Changes

1. **Review Existing Queries**:
   - Keep ONLY queries using indexed fields
   - Remove any custom queries not required for tests

2. **Standard JpaRepository Methods** (should remain):
```java
public interface CustomerAccountRepository extends JpaRepository<CustomerAccountEntity, UUID> {
    // Standard methods: findById, findAll, save, delete, count, existsById

    // Custom queries ONLY if field is indexed
}
```

### Current Indexed Fields (from Flyway migration)
- `id` (primary key, UUID)
- `customer_id` (foreign key to customer)
- `account_id` (customer account identifier)
- `billing_cycle`

### Verification Checklist for Repository

- [ ] Extends JpaRepository<CustomerAccountEntity, UUID>
- [ ] Only indexed field queries present
- [ ] No complex queries that would be better in service layer
- [ ] All query methods have proper return types

---

## Task 5: Service Updates

**Files**:
- `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/service/customer/CustomerAccountService.java`
- `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/service/customer/impl/CustomerAccountServiceImpl.java`

### Required Changes

1. **Review Service Interface** (CustomerAccountService.java):
   - Basic CRUD operations
   - Any schema-compliant business logic
   - No complex queries better suited for repository

2. **Review Service Implementation** (CustomerAccountServiceImpl.java):
   - Field access order matches schema sequence
   - Validation uses schema constraints
   - Proper handling of embedded objects

3. **Add Validation Methods** (if needed):
```java
public void validateCustomerAccount(CustomerAccountEntity account) {
    // Validate Document fields
    // Validate CustomerAccount fields
    // Validate embedded objects (electronicAddress, contactInfo, docStatus)
    // Validate notifications collection
}
```

### Verification Checklist for Service

- [ ] Service methods work with new Document fields
- [ ] Embedded objects handled correctly in service logic
- [ ] Collections handled correctly (notifications)
- [ ] Validation aligns with XSD constraints
- [ ] All service tests pass

---

## Task 6: Flyway Migration Updates

**Primary File**: `openespi-common/src/main/resources/db/migration/V3__Create_additiional_Base_Tables.sql`

### Required Changes

1. **Add Missing Document Fields to customer_accounts Table**:
```sql
-- Add after title column (around line 135)
ALTER TABLE customer_accounts ADD COLUMN author_name VARCHAR(256);
ALTER TABLE customer_accounts ADD COLUMN electronic_address_email1 VARCHAR(256);
ALTER TABLE customer_accounts ADD COLUMN electronic_address_email2 VARCHAR(256);
ALTER TABLE customer_accounts ADD COLUMN electronic_address_web VARCHAR(256);
ALTER TABLE customer_accounts ADD COLUMN electronic_address_radio VARCHAR(256);
ALTER TABLE customer_accounts ADD COLUMN electronic_address_land_line VARCHAR(256);
ALTER TABLE customer_accounts ADD COLUMN electronic_address_mobile VARCHAR(256);
ALTER TABLE customer_accounts ADD COLUMN doc_status_value VARCHAR(256);
ALTER TABLE customer_accounts ADD COLUMN doc_status_date_time TIMESTAMP WITH TIME ZONE;
ALTER TABLE customer_accounts ADD COLUMN doc_status_reason VARCHAR(256);
ALTER TABLE customer_accounts ADD COLUMN doc_status_remark VARCHAR(256);
```

2. **Fix contactInfo Column Structure**:
```sql
-- Replace contact_name with Organisation embedded fields
ALTER TABLE customer_accounts DROP COLUMN contact_name;

ALTER TABLE customer_accounts ADD COLUMN contact_org_name VARCHAR(256);
ALTER TABLE customer_accounts ADD COLUMN contact_street_detail VARCHAR(256);
ALTER TABLE customer_accounts ADD COLUMN contact_town_detail VARCHAR(256);
ALTER TABLE customer_accounts ADD COLUMN contact_state_province VARCHAR(256);
ALTER TABLE customer_accounts ADD COLUMN contact_postal_code VARCHAR(256);
ALTER TABLE customer_accounts ADD COLUMN contact_country VARCHAR(256);
ALTER TABLE customer_accounts ADD COLUMN contact_postal_street_detail VARCHAR(256);
ALTER TABLE customer_accounts ADD COLUMN contact_postal_town_detail VARCHAR(256);
ALTER TABLE customer_accounts ADD COLUMN contact_postal_state_province VARCHAR(256);
ALTER TABLE customer_accounts ADD COLUMN contact_postal_postal_code VARCHAR(256);
ALTER TABLE customer_accounts ADD COLUMN contact_postal_country VARCHAR(256);
ALTER TABLE customer_accounts ADD COLUMN contact_email1 VARCHAR(256);
ALTER TABLE customer_accounts ADD COLUMN contact_email2 VARCHAR(256);
ALTER TABLE customer_accounts ADD COLUMN contact_web VARCHAR(256);
```

3. **Verify Column Order Matches XSD**:
```sql
-- Ensure columns are in this order (Document → CustomerAccount):
-- type, author_name, created_date_time, last_modified_date_time, revision_number,
-- electronic_address_*, subject, title, doc_status_*,
-- billing_cycle, budget_bill, last_bill_amount, (notifications in separate table),
-- contact_*, account_id, is_pre_pay
```

4. **Verify customer_account_notifications Table** exists and is correct

5. **Update Indexes** if needed:
```sql
CREATE INDEX IF NOT EXISTS idx_customer_account_billing_cycle ON customer_accounts (billing_cycle);
CREATE INDEX IF NOT EXISTS idx_customer_account_account_id ON customer_accounts (account_id);
```

### Verification Checklist for Migration

- [ ] All Document columns present
- [ ] All CustomerAccount columns present
- [ ] contactInfo as Organisation embedded (not String)
- [ ] Column order matches XSD element sequence
- [ ] customer_account_notifications table correct
- [ ] Indexes created on appropriate columns
- [ ] Migration runs without errors on H2, MySQL, PostgreSQL
- [ ] Column names follow snake_case convention

---

## Task 7: Testing

### 7.1 Unit Tests

**File**: `openespi-common/src/test/java/org/greenbuttonalliance/espi/common/repositories/customer/CustomerAccountRepositoryTest.java`

**Required Tests**:
1. Basic CRUD operations
2. Document field persistence
3. CustomerAccount field persistence
4. Embedded object persistence (ElectronicAddress, Organisation, Status)
5. AccountNotification collection persistence
6. Customer relationship
7. Validation constraints

### 7.2 DTO Marshalling Tests

**File**: `openespi-common/src/test/java/org/greenbuttonalliance/espi/common/dto/customer/CustomerAccountDtoMarshallingTest.java` (create new)

**Required Tests**:
```java
@Test
@DisplayName("Should marshal CustomerAccount with all fields populated")
void shouldMarshalCustomerAccountWithAllFields() {
    // Create CustomerAccountDto with:
    // - All Document fields
    // - All CustomerAccount fields
    // - All embedded objects (ElectronicAddress, Organisation, Status)
    // - notifications collection

    // Marshal to XML
    // Verify field order matches customer.xsd
}

@Test
@DisplayName("Should marshal CustomerAccount with minimal data")
void shouldMarshalCustomerAccountWithMinimalData() {
    // Create with only required fields
    // Verify optional fields are omitted (not null elements)
}

@Test
@DisplayName("Should verify CustomerAccount field order matches customer.xsd")
void shouldVerifyCustomerAccountFieldOrder() {
    // Create comprehensive CustomerAccountDto
    // Marshal to XML
    // Parse XML and verify element positions
    // Document fields before CustomerAccount fields
}

@Test
@DisplayName("Should use correct Customer namespace prefix (cust:)")
void shouldUseCorrectCustomerNamespace() {
    // Verify xmlns:cust="http://naesb.org/espi/customer"
    // Verify cust: prefix on all elements
}
```

### 7.3 Integration Tests

**Files**:
- `openespi-common/src/test/java/org/greenbuttonalliance/espi/common/repositories/integration/CustomerAccountMySQLIntegrationTest.java` (create new)
- `openespi-common/src/test/java/org/greenbuttonalliance/espi/common/repositories/integration/CustomerAccountPostgreSQLIntegrationTest.java` (create new)

**Required Tests**:
1. Full CRUD operations with real MySQL/PostgreSQL databases
2. Document field persistence verification
3. CustomerAccount field persistence verification
4. Embedded objects persistence (ElectronicAddress, Organisation, Status)
5. AccountNotification collection persistence
6. Customer relationship persistence
7. Bulk operations (saveAll, deleteAll)

### 7.4 Migration Verification Tests

**File**: `openespi-common/src/test/java/org/greenbuttonalliance/espi/common/MigrationVerificationTest.java`

**Add Tests**:
```java
@Test
@DisplayName("CustomerAccount entity with all embedded objects should work")
void customerAccountWithAllEmbeddedObjectsShouldWork() {
    // Test CustomerAccount with:
    // - All Document fields
    // - ElectronicAddress embedded
    // - Organisation embedded
    // - Status embedded
    // - AccountNotification collection
}

@Test
@DisplayName("CustomerAccount embedded objects should be null-safe")
void customerAccountEmbeddedObjectsShouldBeNullSafe() {
    // Test with null embedded objects
    // Should not throw exceptions
}
```

### 7.5 XSD Validation Tests

**File**: `openespi-common/src/test/java/org/greenbuttonalliance/espi/common/xsd/CustomerAccountXsdValidationTest.java` (create new)

**Required Tests**:
```java
@Test
@DisplayName("Should validate marshalled CustomerAccount XML against customer.xsd")
void shouldValidateMarshalledXmlAgainstXsd() {
    // Marshal CustomerAccountDto to XML
    // Validate against customer.xsd using SchemaFactory
    // Assert no validation errors
}
```

### Test Coverage Requirements

- [ ] CustomerAccountRepositoryTest: 100% coverage of CRUD operations
- [ ] CustomerAccountDtoMarshallingTest: All field orders verified
- [ ] CustomerAccountMySQLIntegrationTest: Full CRUD with MySQL
- [ ] CustomerAccountPostgreSQLIntegrationTest: Full CRUD with PostgreSQL
- [ ] MigrationVerificationTest: CustomerAccount cases added
- [ ] XSD validation passes for all test cases
- [ ] All embedded object tests pass
- [ ] All collection tests pass
- [ ] All 609+ tests passing (existing + new)

---

## Task 8: Commit, Push, PR

### Pre-Commit Checklist

- [ ] All 8 tasks completed
- [ ] All files modified are listed below
- [ ] Field order verified against customer.xsd
- [ ] All tests passing (mvn test)
- [ ] Integration tests passing with Docker (mvn verify -Pintegration-tests)
- [ ] No compilation warnings
- [ ] Code formatted consistently
- [ ] JavaDoc updated for new/modified fields

### Files to Commit

**Entity & Domain**:
- `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/domain/customer/entity/CustomerAccountEntity.java`

**DTO**:
- `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/dto/customer/CustomerAccountDto.java`

**Mapper**:
- `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/mapper/customer/CustomerAccountMapper.java`

**Repository**:
- `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/repositories/customer/CustomerAccountRepository.java`

**Service**:
- `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/service/customer/CustomerAccountService.java`
- `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/service/customer/impl/CustomerAccountServiceImpl.java`

**Flyway Migration**:
- `openespi-common/src/main/resources/db/migration/V3__Create_additiional_Base_Tables.sql`

**Tests**:
- `openespi-common/src/test/java/org/greenbuttonalliance/espi/common/repositories/customer/CustomerAccountRepositoryTest.java`
- `openespi-common/src/test/java/org/greenbuttonalliance/espi/common/dto/customer/CustomerAccountDtoMarshallingTest.java` (new)
- `openespi-common/src/test/java/org/greenbuttonalliance/espi/common/repositories/integration/CustomerAccountMySQLIntegrationTest.java` (new)
- `openespi-common/src/test/java/org/greenbuttonalliance/espi/common/repositories/integration/CustomerAccountPostgreSQLIntegrationTest.java` (new)
- `openespi-common/src/test/java/org/greenbuttonalliance/espi/common/xsd/CustomerAccountXsdValidationTest.java` (new)
- `openespi-common/src/test/java/org/greenbuttonalliance/espi/common/MigrationVerificationTest.java`

### Git Commands

```bash
# Create feature branch
git checkout main
git pull origin main
git checkout -b feature/schema-compliance-phase-18-customer-account

# Make all changes (Tasks 1-7)

# Stage all changes
git add openespi-common/src/main/java/org/greenbuttonalliance/espi/common/domain/customer/entity/CustomerAccountEntity.java
git add openespi-common/src/main/java/org/greenbuttonalliance/espi/common/dto/customer/CustomerAccountDto.java
git add openespi-common/src/main/java/org/greenbuttonalliance/espi/common/mapper/customer/CustomerAccountMapper.java
git add openespi-common/src/main/java/org/greenbuttonalliance/espi/common/repositories/customer/CustomerAccountRepository.java
git add openespi-common/src/main/java/org/greenbuttonalliance/espi/common/service/customer/CustomerAccountService.java
git add openespi-common/src/main/java/org/greenbuttonalliance/espi/common/service/customer/impl/CustomerAccountServiceImpl.java
git add openespi-common/src/main/resources/db/migration/V3__Create_additiional_Base_Tables.sql
git add openespi-common/src/test/java/org/greenbuttonalliance/espi/common/repositories/customer/CustomerAccountRepositoryTest.java
git add openespi-common/src/test/java/org/greenbuttonalliance/espi/common/dto/customer/CustomerAccountDtoMarshallingTest.java
git add openespi-common/src/test/java/org/greenbuttonalliance/espi/common/repositories/integration/CustomerAccountMySQLIntegrationTest.java
git add openespi-common/src/test/java/org/greenbuttonalliance/espi/common/repositories/integration/CustomerAccountPostgreSQLIntegrationTest.java
git add openespi-common/src/test/java/org/greenbuttonalliance/espi/common/xsd/CustomerAccountXsdValidationTest.java
git add openespi-common/src/test/java/org/greenbuttonalliance/espi/common/MigrationVerificationTest.java

# Verify staged files
git status

# Commit with detailed message
git commit -m "$(cat <<'EOF'
feat: ESPI 4.0 Schema Compliance - Phase 18: CustomerAccount

Comprehensive schema compliance updates for CustomerAccount entity and all
associated components to match NAESB ESPI 4.0 customer.xsd specification.

## Entity Changes (CustomerAccountEntity)
- Added missing Document base class fields: authorName, electronicAddress, docStatus
- Reordered Document fields to match XSD sequence
- Fixed contactInfo type from String to Organisation embedded object
- Reordered CustomerAccount fields to match XSD sequence
- Added proper @Embedded and @AttributeOverrides for all embedded objects

## DTO Changes (CustomerAccountDto)
- Complete rewrite of @XmlType propOrder to match XSD element sequence
- Added all missing Document fields (9 fields)
- Fixed contactInfo type to Organisation
- Added notifications collection
- Removed non-XSD fields (accountNumber, transactionDate)
- Removed embedded relationship objects (Customer, CustomerAgreements)

## Mapper Changes (CustomerAccountMapper)
- Added mappings for all Document fields
- Added mappings for embedded objects (ElectronicAddress, Organisation, Status)
- Added mapping for notifications collection
- Updated both toDto() and toEntity() methods

## Repository Updates (CustomerAccountRepository)
- Verified only indexed field queries present
- Maintained JpaRepository standard methods

## Service Updates
- Updated validation to handle new Document fields
- Updated business logic for embedded objects
- Verified field access order matches schema

## Flyway Migration Updates (V3__Create_additiional_Base_Tables.sql)
- Added missing Document columns (authorName, electronicAddress_*, docStatus_*)
- Fixed contactInfo from String to Organisation embedded columns
- Verified column order matches XSD element sequence
- Added appropriate indexes

## Testing
- Enhanced CustomerAccountRepositoryTest with embedded object tests
- Created CustomerAccountDtoMarshallingTest (3 tests) verifying field order
- Created CustomerAccountMySQLIntegrationTest (8 tests)
- Created CustomerAccountPostgreSQLIntegrationTest (8 tests)
- Created CustomerAccountXsdValidationTest for schema validation
- Enhanced MigrationVerificationTest with CustomerAccount cases
- All 650+ tests passing

## Schema Compliance
- ✅ Document base class field order matches customer.xsd
- ✅ CustomerAccount field order matches customer.xsd
- ✅ All embedded objects properly structured
- ✅ All collections properly mapped
- ✅ XML output validates against customer.xsd
- ✅ Namespace prefix (cust:) correctly applied

Related to Issue #28 Phase 18

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>
EOF
)"

# Push to remote
git push origin feature/schema-compliance-phase-18-customer-account

# Create PR using GitHub CLI
gh pr create \
  --title "feat: ESPI 4.0 Schema Compliance - Phase 18: CustomerAccount" \
  --body "$(cat <<'EOF'
## Summary
Comprehensive schema compliance updates for CustomerAccount entity to match NAESB ESPI 4.0 customer.xsd specification.

## Changes
- **Entity**: Added missing Document fields, fixed field types and order
- **DTO**: Complete rewrite to match XSD element sequence
- **Mapper**: Added mappings for all new fields and embedded objects
- **Repository**: Verified query compliance
- **Service**: Updated validation and business logic
- **Migration**: Added missing columns, fixed contactInfo structure
- **Tests**: 5 new test files, enhanced existing tests

## Schema Compliance
✅ All Document base class fields present and ordered correctly
✅ All CustomerAccount fields present and ordered correctly
✅ contactInfo properly embedded as Organisation
✅ All embedded objects structured correctly
✅ XML validates against customer.xsd

## Test Results
- All 650+ tests passing
- Integration tests verified with MySQL 8.0 and PostgreSQL 18
- XSD validation passes

## Checklist
- [x] All 8 phase tasks completed
- [x] Field order matches customer.xsd exactly
- [x] All tests passing
- [x] Integration tests passing
- [x] XSD validation passing
- [x] No compilation warnings
- [x] Documentation updated

Related to #28
EOF
)" \
  --base main
```

---

## Success Criteria

Phase 18 is complete when:

1. ✅ CustomerAccountEntity has all Document + CustomerAccount fields in XSD order
2. ✅ CustomerAccountDto has all fields in XSD propOrder
3. ✅ contactInfo is Organisation type (not String)
4. ✅ All embedded objects properly structured (ElectronicAddress, Organisation, Status)
5. ✅ CustomerAccountMapper handles all fields correctly
6. ✅ Flyway migration adds all missing columns
7. ✅ All unit tests pass
8. ✅ All integration tests pass (MySQL, PostgreSQL)
9. ✅ XSD validation passes
10. ✅ PR created and ready for review

---

## Dependencies

**Referenced By**:
- CustomerAgreement (Phase 21 - will reference CustomerAccount via Atom links)
- Statement (Phase 19 - will reference CustomerAccount via Atom links)
- Customer (Phase 20 - completed, references CustomerAccount via Atom links)

**This Phase Must Complete Before**:
- Phase 19: Statement (needs CustomerAccount relationship)
- Phase 21: CustomerAgreement (needs CustomerAccount relationship)

---

## Estimated Effort

- Task 1 (Entity): 2-3 hours
- Task 2 (DTO): 2-3 hours
- Task 3 (Mapper): 1-2 hours
- Task 4 (Repository): 30 minutes
- Task 5 (Service): 1 hour
- Task 6 (Migration): 1-2 hours
- Task 7 (Testing): 4-5 hours
- Task 8 (Commit/PR): 30 minutes

**Total**: 12-17 hours

---

## Notes

- CustomerAccount extends Document (not directly IdentifiedObject)
- Document has 9 fields that must all be present in entity and DTO
- contactInfo is Organisation embedded object (complex nested structure)
- ElectronicAddress embedded in Document.electronicAddress
- Status embedded in Document.docStatus
- AccountNotification is a separate @ElementCollection
- isPrePay is an extension field (not in XSD) - must be at end with [extension] comment
- Customer relationship is for JPA only - not serialized to XML