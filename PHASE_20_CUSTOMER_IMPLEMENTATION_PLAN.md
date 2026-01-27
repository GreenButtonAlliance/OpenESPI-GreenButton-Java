# Phase 20: Customer - ESPI 4.0 Schema Compliance Implementation Plan

## Overview
Ensure CustomerEntity and all related components strictly comply with ESPI 4.0 customer.xsd schema definition.

**Branch**: `feature/schema-compliance-phase-20-customer`

**Dependencies**:
- TimeConfiguration (via bidirectional Atom rel='related' links)
- Statement (via bidirectional Atom rel='related' links)
- CustomerAccount (via bidirectional Atom rel='related' links)

**Referenced By**: None

---

## Current State Analysis

### CustomerEntity.java - Current Structure
**Location**: `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/domain/customer/entity/CustomerEntity.java`

**Current Field Order**:
1. ✅ organisation (embedded Organisation) - From OrganisationRole
2. ✅ kind (CustomerKind enum)
3. ✅ specialNeed (String)
4. ✅ vip (Boolean)
5. ✅ pucNumber (String)
6. ✅ status (embedded Status)
7. ✅ priority (embedded Priority)
8. ✅ locale (String)
9. ✅ customerName (String)
10. customerAccounts (OneToMany relationship)
11. timeConfiguration (OneToOne relationship)
12. statements (OneToMany relationship)
13. phoneNumbers (OneToMany relationship)

### customer.xsd - Required Structure

**Customer extends OrganisationRole extends IdentifiedObject**

**XSD Element Sequence**:
1. IdentifiedObject fields (mRID, description)
2. **Organisation** (from OrganisationRole)
3. **kind** (CustomerKind enum)
4. **specialNeed** (String256)
5. **vip** (boolean)
6. **pucNumber** (String256)
7. **status** (Status)
8. **priority** (Priority)
9. **locale** (String256)
10. **customerName** (String256)

**Status Structure** (embedded):
- value (String256)
- dateTime (DateTimeInterval)
- reason (String256)

**Priority Structure** (embedded):
- value (Integer)
- rank (Integer)
- type (String256)

**Organisation Structure** (embedded):
- organisationName (String256)
- streetAddress (StreetAddress)
- postalAddress (StreetAddress)
- electronicAddress (ElectronicAddress)

### Compliance Assessment

✅ **COMPLIANT**: Field order matches customer.xsd sequence
✅ **COMPLIANT**: All required embedded classes present
✅ **COMPLIANT**: Extends IdentifiedObject (correct per XSD)
✅ **COMPLIANT**: Relationships defined (CustomerAccount, TimeConfiguration, Statement)

**Minor Issues to Address**:
1. ⚠️ Verify Organisation embedded field structure matches XSD exactly
2. ⚠️ Verify Status embedded class matches XSD (dateTime type)
3. ⚠️ Verify Priority embedded class matches XSD
4. ⚠️ Check Flyway migration column order matches XSD sequence
5. ⚠️ Review CustomerDto field order
6. ⚠️ Review CustomerMapper mappings
7. ⚠️ Review CustomerRepository for non-indexed queries
8. ⚠️ Add XML marshalling tests

---

## Implementation Tasks

### Task 1: Entity Updates (CustomerEntity.java)
**Status**: ✅ Mostly Complete - Verify Only

**Actions**:
1. ✅ Verify field order matches customer.xsd sequence (appears correct)
2. ⚠️ Verify Organisation embedded class structure:
   - Check streetAddress and postalAddress field mapping
   - Check electronicAddress field mapping
   - Verify column name prefixes are consistent
3. ⚠️ Verify Status embedded class:
   - Confirm dateTime uses OffsetDateTime (correct type)
   - Verify column names
4. ⚠️ Verify Priority embedded class:
   - Confirm all three fields present (value, rank, type)
   - Verify column names
5. ✅ Relationships look correct (CustomerAccount, TimeConfiguration, Statement)
6. ⚠️ Check phoneNumbers relationship - ensure it's handled correctly

**Files to Review**:
- `CustomerEntity.java`
- `Organisation.java` (if separate embeddable)
- `Status.java` (inner class)
- `Priority.java` (inner class)

---

### Task 2: DTO Updates (CustomerDto.java)
**Status**: ⚠️ Needs Review

**Actions**:
1. Read CustomerDto and verify field order matches customer.xsd
2. Ensure Organisation DTO structure matches XSD
3. Ensure Status DTO structure matches XSD
4. Ensure Priority DTO structure matches XSD
5. Verify JAXB annotations for XML marshalling
6. Ensure namespace is "http://naesb.org/espi/customer"

**Files to Review**:
- `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/dto/customer/CustomerDto.java`

---

### Task 3: MapStruct Mapper Updates (CustomerMapper.java)
**Status**: ⚠️ Needs Review

**Actions**:
1. Review CustomerMapper interface
2. Verify Entity-to-DTO conversion mappings
3. Verify DTO-to-Entity conversion mappings
4. Ensure embedded Organisation mapping is correct
5. Ensure embedded Status mapping is correct
6. Ensure embedded Priority mapping is correct
7. Handle relationship mappings (ignore or separate methods)
8. Remove any IdentifiedObject field mappings (handled by base)

**Files to Review**:
- `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/mapper/customer/CustomerMapper.java`

---

### Task 4: Repository Updates (CustomerRepository.java)
**Status**: ⚠️ Needs Review

**Actions**:
1. Review CustomerRepository interface
2. Keep ONLY queries on indexed fields:
   - id (primary key)
   - created, updated (likely indexed)
   - kind (likely indexed)
   - Any other explicitly indexed fields
3. Remove queries on non-indexed fields
4. Review test requirements and ensure indexed queries support them

**Files to Review**:
- `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/repositories/customer/CustomerRepository.java`

---

### Task 5: Service Updates
**Status**: ⚠️ Needs Review

**Actions**:
1. Review CustomerService interface
2. Review CustomerServiceImpl implementation
3. Verify service methods support schema-compliant operations
4. Ensure proper relationship handling (CustomerAccount, TimeConfiguration, Statement)
5. Check for any legacy patterns that need updating

**Files to Review**:
- `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/service/customer/CustomerService.java`
- `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/service/customer/CustomerServiceImpl.java` (if exists)

---

### Task 6: Flyway Migration Updates
**Status**: ⚠️ Needs Investigation

**Actions**:
1. Locate Customer table creation in Flyway migrations
2. Verify column order matches customer.xsd element sequence
3. Check Organisation embedded fields have correct column names
4. Check Status embedded fields have correct column names
5. Check Priority embedded fields have correct column names
6. Verify foreign key relationships (time_configuration_id)
7. Verify indexes on commonly queried fields

**Files to Locate**:
- `openespi-common/src/main/resources/db/migration/V*.sql` (Customer table)
- `openespi-common/src/main/resources/db/vendor/*/V*.sql` (vendor-specific)

---

### Task 7: Testing
**Status**: ⚠️ Needs Work

**Actions**:
1. **Unit Tests**:
   - Review CustomerRepositoryTest
   - Add tests for all indexed query methods
   - Test embedded Organisation fields
   - Test embedded Status fields
   - Test embedded Priority fields
   - Test relationship loading (CustomerAccount, TimeConfiguration, Statement)

2. **Integration Tests**:
   - Add TestContainers-based integration tests
   - Test full CRUD operations
   - Test relationship persistence

3. **XML Marshalling Tests**:
   - Add XML marshalling test for CustomerEntity → CustomerDto → XML
   - Add XML unmarshalling test for XML → CustomerDto → CustomerEntity
   - Validate generated XML against customer.xsd schema
   - Test embedded Organisation serialization
   - Test embedded Status serialization
   - Test embedded Priority serialization
   - Verify namespace is "http://naesb.org/espi/customer"

4. **Migration Tests**:
   - Use MigrationVerificationTest pattern
   - Verify Customer table structure matches XSD

**Files to Review/Create**:
- `openespi-common/src/test/java/org/greenbuttonalliance/espi/common/repositories/customer/CustomerRepositoryTest.java`
- Create: `CustomerIntegrationTest.java`
- Create: `CustomerXmlMarshallingTest.java`

---

### Task 8: Commit, Push, PR

**Actions**:
1. Create feature branch: `feature/schema-compliance-phase-20-customer`
2. Stage all changes
3. Commit with message:
   ```
   feat: Phase 20 - Customer ESPI 4.0 Schema Compliance

   Ensured CustomerEntity and related components comply with customer.xsd.

   Changes:
   - Verified CustomerEntity field order matches customer.xsd
   - Updated CustomerDto field order and JAXB annotations
   - Updated CustomerMapper mappings
   - Cleaned up CustomerRepository (removed non-indexed queries)
   - Verified/updated Flyway migrations
   - Added XML marshalling tests
   - Updated unit and integration tests

   Customer is now 100% ESPI 4.0 customer.xsd compliant.

   Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>
   ```
4. Push branch to remote
5. Create PR with comprehensive description
6. Wait for CI/CD checks to pass
7. Request review

---

## Estimated Sub-Phases

Based on complexity, Phase 20 can be broken into:

### Phase 20a: Entity and DTO Verification (Low Risk)
- Review and verify CustomerEntity field order
- Review and verify embedded classes (Organisation, Status, Priority)
- Review and verify CustomerDto structure
- Update JAXB annotations if needed

### Phase 20b: Mapper and Repository Cleanup (Medium Risk)
- Review and update CustomerMapper
- Clean up CustomerRepository (remove non-indexed queries)
- Update service layer if needed

### Phase 20c: Flyway Migration Review (Medium Risk)
- Locate and review Customer table migrations
- Verify column order matches XSD
- Add migration script if changes needed

### Phase 20d: Testing (Low Risk)
- Add/update unit tests
- Add integration tests
- Add XML marshalling/unmarshalling tests
- Verify schema validation

---

## Success Criteria

✅ CustomerEntity field order matches customer.xsd sequence exactly
✅ CustomerDto field order matches customer.xsd sequence exactly
✅ All embedded classes (Organisation, Status, Priority) match XSD
✅ CustomerMapper correctly maps all fields
✅ CustomerRepository contains only indexed field queries
✅ Flyway migration column order matches XSD
✅ All unit tests pass
✅ Integration tests pass with TestContainers
✅ XML marshalling tests validate against customer.xsd
✅ CI/CD pipeline passes all checks
✅ PR approved and merged

---

## Risk Assessment

**Low Risk**:
- Entity structure appears correct
- DTO and Mapper infrastructure exists
- Service layer exists

**Medium Risk**:
- Flyway migrations may need column reordering
- Repository may have non-indexed queries to remove
- XML marshalling tests need to be created

**High Risk**:
- None identified

---

## Dependencies and Blockers

**Dependencies**:
- TimeConfiguration must be schema-compliant (already done in earlier phases)
- Statement entity must exist (verify)
- CustomerAccount entity must exist (verify)

**Blockers**:
- None identified

---

## Next Steps After Phase 20

After completing Phase 20 (Customer), proceed to:
- **Phase 21: ServiceSupplier**
- **Phase 22: Asset**
- **Phase 23: ServiceLocation**
- **Phase 24: CustomerAgreement**

These phases will complete the customer.xsd schema compliance work.

---

## Notes

1. Customer is a PII (Personally Identifiable Information) entity in the customer.xsd namespace
2. Customer extends OrganisationRole which extends IdentifiedObject
3. Customer has multiple embedded complex types (Organisation, Status, Priority)
4. Customer has relationships to TimeConfiguration, Statement, and CustomerAccount
5. The entity appears well-structured and mostly compliant already
6. Main work will be verification, testing, and documentation

---

**Created**: 2026-01-16
**Phase**: 20
**Entity**: Customer
**Schema**: customer.xsd
**Status**: Ready for Implementation
