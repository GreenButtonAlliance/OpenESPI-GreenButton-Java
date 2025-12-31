# Schema Compliance Audit Results

**Date**: 2025-12-30
**Auditor**: Senior Spring Developer Analysis
**Scope**: related_links tables and IdentifiedObject inheritance compliance

---

## Executive Summary

✅ **All 11 incorrect related_links tables EXIST and need removal**
⚠️ **4 missing related_links tables need creation**
❌ **11 entities incorrectly extend IdentifiedObject**

---

## 1. Incorrect Related Links Tables Found (11 total - ALL EXIST)

### V3__Create_additiional_Base_Tables.sql (7 tables)

| # | Table | Line | Entity | Issue |
|---|-------|------|--------|-------|
| 1 | `interval_reading_related_links` | 122 | IntervalReading | Extends Object, not IdentifiedObject |
| 2 | `reading_quality_related_links` | 163 | ReadingQuality | Extends Object, not IdentifiedObject |
| 3 | `pnode_ref_related_links` | 355 | PnodeRef | Extends Object, not IdentifiedObject |
| 4 | `aggregated_node_ref_related_links` | 403 | AggregatedNodeRef | Extends Object, not IdentifiedObject |
| 5 | `line_item_related_links` | 746 | LineItem | Extends Object, not IdentifiedObject |
| 6 | `phone_number_related_links` | 804 | PhoneNumber | Not in XSD (custom polymorphic) |
| 7 | `statement_ref_related_links` | 1042 | StatementRef | Extends Object, not IdentifiedObject |

### V1__Create_Base_Tables.sql (4 tables)

| # | Table | Line | Entity | Issue |
|---|-------|------|--------|-------|
| 8 | `retail_customer_related_links` | 175 | RetailCustomer | Uses direct FK, not Atom rel="related" |
| 9 | `service_delivery_point_related_links` | 213 | ServiceDeliveryPoint | Extends Object, not IdentifiedObject |
| 10 | `subscription_related_links` | 378 | Subscription | Uses direct FK, not Atom rel="related" |
| 11 | `batch_list_related_links` | 411 | BatchList | Element wrapper, not IdentifiedObject |

---

## 2. Missing Related Links Tables (4 total - NEED CREATION)

### Required for ESPI Compliance

| # | Missing Table | Entity | Parent XSD | Status |
|---|--------------|--------|------------|--------|
| 1 | `customer_related_links` | Customer | customer.xsd:67 → OrganisationRole | ⚠️ **NOT FOUND** |
| 2 | `meter_related_links` | Meter | customer.xsd:243 → EndDevice | ⚠️ **NOT FOUND** |
| 3 | `customer_account_related_links` | CustomerAccount | customer.xsd:118 → Document | ⚠️ **NOT FOUND** |
| 4 | `electric_power_quality_summary_related_links` | ElectricPowerQualitySummary | espi.xsd:614 | ⚠️ **NOT FOUND** |

**Note**: Meter currently shares `end_device_related_links` but should have its own table per ESPI compliance.

---

## 3. Correct Related Links Tables (15 exist)

### V1__Create_Base_Tables.sql (3 tables)
- ✅ `application_information_related_links` (line 111)
- ✅ `authorization_related_links` (line 279)
- ✅ `reading_type_related_links` (line 333)

### V2 Vendor-Specific (2 tables - all 3 vendors)
- ✅ `time_configuration_related_links` (MySQL: line 59, PostgreSQL: line 59, H2: line 60)
- ✅ `usage_point_related_links` (MySQL: line 138, PostgreSQL: line 139, H2: line 113)

### V3__Create_additiional_Base_Tables.sql (10 tables)
- ✅ `meter_reading_related_links` (line 31)
- ✅ `interval_block_related_links` (line 73)
- ✅ `usage_summary_related_links` (line 288)
- ✅ `customer_agreement_related_links` (line 521)
- ✅ `end_device_related_links` (line 703)
- ✅ `program_date_id_mapping_related_links` (line 839)
- ✅ `service_location_related_links` (line 908)
- ✅ `service_supplier_related_links` (line 963)
- ✅ `statement_related_links` (line 1005)

---

## 4. Entity Class Inheritance Issues (11 entities)

### Entities That Should NOT Extend IdentifiedObject (9)

**Found in**: `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/domain/`

| Entity Class | Current | Should Be | Action Required |
|--------------|---------|-----------|-----------------|
| `IntervalReadingEntity.java` | extends IdentifiedObject | extends Object | Remove IdentifiedObject, keep as @Entity |
| `ReadingQualityEntity.java` | extends IdentifiedObject | extends Object | Remove IdentifiedObject, keep as @Entity |
| `PnodeRefEntity.java` | extends IdentifiedObject | extends Object | Remove IdentifiedObject, keep as @Entity |
| `AggregatedNodeRefEntity.java` | extends IdentifiedObject | extends Object | Remove IdentifiedObject, keep as @Entity |
| `LineItemEntity.java` | extends IdentifiedObject | extends Object | Remove IdentifiedObject, keep as @Entity |
| `ServiceDeliveryPointEntity.java` | extends IdentifiedObject | extends Object | Remove IdentifiedObject, keep as @Entity |
| `StatementRefEntity.java` | extends IdentifiedObject | extends Object | Remove IdentifiedObject, keep as @Entity |
| `PhoneNumberEntity.java` | extends IdentifiedObject | No inheritance | Remove IdentifiedObject, custom polymorphic |
| `BatchListEntity.java` | extends IdentifiedObject | No inheritance | Remove IdentifiedObject, wrapper type |

### Special Case Entities (2)

| Entity Class | Current | Correct | Related Links? | Action Required |
|--------------|---------|---------|----------------|-----------------|
| `RetailCustomerEntity.java` | extends IdentifiedObject | ✅ Correct | ❌ NO | Remove related_links table only |
| `SubscriptionEntity.java` | extends IdentifiedObject | ✅ Correct | ❌ NO | Remove related_links table only |

**Note**: RetailCustomer and Subscription correctly extend IdentifiedObject but use direct FK references (not Atom rel="related" links), so they don't need related_links tables.

---

## 5. Additional Findings

### Unexpected Table
- `identified_object_related_links` (V1__Create_Base_Tables.sql:21)
  - **Purpose**: Unclear - may be abstract base class table
  - **Action**: Investigate usage before removal

---

## 6. Remediation Priority

### Phase 1: Low-Risk Additions (RECOMMENDED FIRST)
**Add 4 missing related_links tables to existing V3 Flyway script** - No breaking changes

Since the system has not been deployed, add tables directly to V3__Create_additiional_Base_Tables.sql:

```sql
-- Add to V3__Create_additiional_Base_Tables.sql (after respective entity tables)
CREATE TABLE customer_related_links (...);  -- Add after customers table
CREATE TABLE meter_related_links (...);  -- Add after meters table
CREATE TABLE customer_account_related_links (...);  -- Add after customer_accounts table
CREATE TABLE electric_power_quality_summary_related_links (...);  -- Add after electric_power_quality_summaries table
```

**Affected Tests**:
- CustomerRepositoryTest.java
- MeterRepositoryTest.java
- CustomerAccountRepositoryTest.java
- ElectricPowerQualitySummaryRepositoryTest.java

### Phase 2: High-Risk Removals (DO SECOND)
**Remove 11 incorrect related_links tables** - Breaking changes

Follow SCHEMA_COMPLIANCE_REMEDIATION_PLAN.md for systematic removal:
1. Update Entity classes (remove IdentifiedObject inheritance where incorrect)
2. Update DTOs (remove Atom link elements)
3. Update Mappers
4. Remove related_links table definitions from V1 and V3 Flyway scripts
5. Update affected repository tests
6. Run full test suite with TestContainers

---

## 7. Risk Assessment

| Risk Level | Count | Description |
|------------|-------|-------------|
| 🟢 **LOW** | 4 | Creating missing tables (additive changes) |
| 🟡 **MEDIUM** | 2 | Removing RetailCustomer/Subscription related_links (special case) |
| 🔴 **HIGH** | 9 | Removing Object-based entity related_links (requires entity refactoring) |

---

## 8. Testing Strategy

### Per-Phase Validation
```bash
# After each change
mvn clean test -pl openespi-common
mvn verify -pl openespi-common -Pintegration-tests
```

### Database Validation
```bash
# Verify tables created/removed correctly
mvn flyway:info -pl openespi-common
mvn flyway:clean -pl openespi-common  # WARNING: Destroys data
mvn flyway:migrate -pl openespi-common
```

---

## 9. Recommended Implementation Order

### Phase 0: Preparation (5 minutes)
```bash
# Create feature branch
git checkout -b feature/schema-compliance-related-links-cleanup
```

### Phase 1: Add Missing Related Links Tables (30 minutes)
**Goal**: Add 4 missing related_links tables to V3 Flyway script

**Steps**:
1. **Open**: `V3__Create_additiional_Base_Tables.sql`
2. **Locate entity tables and add related_links after each**:
   - Find `customers` table → Add `customer_related_links` table after it
   - Find `meters` table → Add `meter_related_links` table after it
   - Find `customer_accounts` table → Add `customer_account_related_links` table after it
   - Find `electric_power_quality_summaries` table → Add `electric_power_quality_summary_related_links` table after it

**Table Structure Template**:
```sql
CREATE TABLE [entity]_related_links
(
    id               BINARY(16)   NOT NULL,
    rel              VARCHAR(255),
    href             VARCHAR(255),
    [entity]_id      BINARY(16),
    PRIMARY KEY (id),
    FOREIGN KEY ([entity]_id) REFERENCES [entity_table](id) ON DELETE CASCADE
);
```

**Testing**:
```bash
# Test Flyway migration
mvn flyway:clean flyway:migrate -pl openespi-common -Pdev-mysql

# Run affected repository tests
mvn test -pl openespi-common -Dtest=CustomerRepositoryTest
mvn test -pl openespi-common -Dtest=MeterRepositoryTest
mvn test -pl openespi-common -Dtest=CustomerAccountRepositoryTest
mvn test -pl openespi-common -Dtest=ElectricPowerQualitySummaryRepositoryTest

# Run full test suite
mvn test -pl openespi-common
```

**Commit**:
```bash
git add openespi-common/src/main/resources/db/migration/V3__Create_additiional_Base_Tables.sql
git commit -m "feat: add 4 missing related_links tables to V3 migration

- Add customer_related_links for Customer entity
- Add meter_related_links for Meter entity
- Add customer_account_related_links for CustomerAccount entity
- Add electric_power_quality_summary_related_links for ElectricPowerQualitySummary entity

Related to ESPI 4.0 schema compliance - all extend IdentifiedObject"
```

---

### Phase 2: Code Review & Baseline (2-3 hours)
**Goal**: Review all code that uses the 11 entities to understand full impact before removal

**A. Repository Tests to Review** (11 affected by entity refactoring):
```bash
# Tests for entities that will lose IdentifiedObject inheritance
openespi-common/src/test/java/org/greenbuttonalliance/espi/common/repositories/usage/
  - IntervalBlockRepositoryTest.java (IntervalReading is child)
  - LineItemRepositoryTest.java ⚠️ Entity will change
  - PnodeRefRepositoryTest.java ⚠️ Entity will change
  - AggregatedNodeRefRepositoryTest.java ⚠️ Entity will change
  - ServiceDeliveryPointRepositoryTest.java ⚠️ Entity will change
  - BatchListRepositoryTest.java ⚠️ Entity will change
  - RetailCustomerRepositoryTest.java (special case)
  - SubscriptionRepositoryTest.java (special case)

openespi-common/src/test/java/org/greenbuttonalliance/espi/common/repositories/customer/
  - StatementRepositoryTest.java (StatementRef is child)
```

**Migration Tests** (will validate schema changes):
```bash
openespi-common/src/test/java/org/greenbuttonalliance/espi/common/migration/
  - DataCustodianApplicationH2Test.java ✅ Run after each phase
  - DataCustodianApplicationMysqlTest.java ✅ Run after each phase
  - DataCustodianApplicationPostgresTest.java ✅ Run after each phase
  - MigrationVerificationTest.java ✅ Critical validation
```

**Integration Tests** (will catch relationship issues):
```bash
openespi-common/src/test/java/org/greenbuttonalliance/espi/common/repositories/integration/
  - ComplexRelationshipIntegrationTest.java ⚠️ May need updates
  - ComplexRelationshipMySQLIntegrationTest.java ⚠️ May need updates
  - ComplexRelationshipPostgreSQLIntegrationTest.java ⚠️ May need updates
```

**B. Service Implementation Classes to Review**:
```bash
# Services that use entities losing IdentifiedObject inheritance
openespi-common/src/main/java/org/greenbuttonalliance/espi/common/service/impl/
  - IntervalBlockServiceImpl.java ⚠️ Uses IntervalReading
  - MeterReadingServiceImpl.java ⚠️ Uses IntervalReading
  - UsageSummaryServiceImpl.java ⚠️ Uses LineItem
  - UsagePointServiceImpl.java ⚠️ Uses PnodeRef, AggregatedNodeRef, ServiceDeliveryPoint
  - BatchListServiceImpl.java ⚠️ Uses BatchList
  - RetailCustomerServiceImpl.java (special case - keep IdentifiedObject)
  - SubscriptionServiceImpl.java (special case - keep IdentifiedObject)

# Search for usage of selfLink/upLink in services
grep -r "selfLink\|upLink" openespi-common/src/main/java/org/greenbuttonalliance/espi/common/service/
```

**C. Controller Classes to Review**:
```bash
# Controllers that expose these entities via REST API
openespi-datacustodian/src/main/java/org/greenbuttonalliance/espi/datacustodian/web/
  - Check for any references to selfLink/upLink in request/response handling
  - Verify Atom feed generation doesn't rely on these fields

# Many controllers are currently .disabled - verify which are active
find openespi-datacustodian/src/main/java -name "*Controller.java" -o -name "*Controller.java.disabled"
```

**D. XML Marshalling/Unmarshalling Code to Review**:
```bash
# DTO classes with JAXB annotations
openespi-common/src/main/java/org/greenbuttonalliance/espi/common/dto/
  - IntervalReadingDto.java ⚠️ Remove selfLink/upLink XML elements
  - ReadingQualityDto.java ⚠️ Remove selfLink/upLink XML elements
  - LineItemDto.java ⚠️ Remove selfLink/upLink XML elements
  - PnodeRefDto.java ⚠️ Remove selfLink/upLink XML elements
  - AggregatedNodeRefDto.java ⚠️ Remove selfLink/upLink XML elements
  - ServiceDeliveryPointDto.java ⚠️ Remove selfLink/upLink XML elements
  - BatchListDto.java ⚠️ Remove selfLink/upLink XML elements
  - StatementRefDto.java ⚠️ Remove selfLink/upLink XML elements
  - PhoneNumberDto.java ⚠️ Remove selfLink/upLink XML elements (if exists)

# DtoExportService - handles entity to DTO conversion and XML export
openespi-common/src/main/java/org/greenbuttonalliance/espi/common/service/impl/DtoExportServiceImpl.java
  - Review exportEntity() methods
  - Verify Atom feed generation logic
  - Check if selfLink/upLink are used in XML output

# Mappers (MapStruct)
openespi-common/src/main/java/org/greenbuttonalliance/espi/common/mapper/
  - IntervalReadingMapper.java ⚠️ Remove link mappings
  - ReadingQualityMapper.java ⚠️ Remove link mappings
  - LineItemMapper.java ⚠️ Remove link mappings
  - PnodeRefMapper.java ⚠️ Remove link mappings
  - AggregatedNodeRefMapper.java ⚠️ Remove link mappings
  - ServiceDeliveryPointMapper.java ⚠️ Remove link mappings
  - BatchListMapper.java ⚠️ Remove link mappings
  - StatementRefMapper.java ⚠️ Remove link mappings

# XML Marshalling Tests
grep -r "marshal\|unmarshal" openespi-common/src/test/java/
```

**Action Items**:
1. Run baseline test suite and document current pass/fail state
2. Review all 37 test classes for selfLink/upLink assertions
3. Review service implementations for selfLink/upLink usage
4. Review controller classes for Atom link dependencies
5. Review DTO classes and mappers for link field mappings
6. Review DtoExportService XML marshalling logic
7. Document all code locations requiring updates per entity
8. Create entity-specific refactoring checklists based on findings

**Baseline Test Run**:
```bash
# Capture current state
mvn test -pl openespi-common > test-baseline-before-refactor.log 2>&1
mvn verify -pl openespi-common -Pintegration-tests >> test-baseline-before-refactor.log 2>&1
```

---

### Phase 3: Remove Special Case Tables (1 hour)
**Goal**: Remove RetailCustomer and Subscription related_links tables

**V1 Changes**:
```bash
# Edit V1__Create_Base_Tables.sql
# Remove lines ~175: retail_customer_related_links table
# Remove lines ~378: subscription_related_links table
```

**No Entity Changes Required** (they correctly extend IdentifiedObject)

**Testing**:
```bash
mvn test -Dtest=RetailCustomerRepositoryTest
mvn test -Dtest=SubscriptionRepositoryTest
mvn test -pl openespi-common
```

**Commit**:
```bash
git add openespi-common/src/main/resources/db/migration/V1__Create_Base_Tables.sql
git commit -m "refactor: remove retail_customer and subscription related_links tables

These entities use direct FK references instead of Atom rel='related' links.
RetailCustomer and Subscription still extend IdentifiedObject but don't
require related_links tables per ESPI 4.0 compliance."
```

---

### Phase 4-13: Systematic Entity Refactoring (2-3 hours each)
**Goal**: Remove 9 incorrect related_links tables and refactor entities

**Per-Entity Checklist**:
- [ ] Update Entity class (remove IdentifiedObject inheritance)
- [ ] Update DTO (remove Atom link elements)
- [ ] Update Mapper (remove link mappings)
- [ ] Remove related_links table from Flyway script (V1 or V3)
- [ ] Update repository test (remove selfLink/upLink assertions)
- [ ] Run entity-specific tests
- [ ] Run migration tests (H2, MySQL, PostgreSQL)
- [ ] Commit changes

**Entity Processing Order** (by risk level):

**4. PhoneNumber** (V3:804) - Lowest risk, custom entity
**5. BatchList** (V1:411) - Low risk, wrapper type
**6. StatementRef** (V3:1042) - Low risk, extends Object
**7. ServiceDeliveryPoint** (V1:213) - Medium risk, referenced by UsagePoint
**8. LineItem** (V3:746) - Medium risk, child of UsageSummary
**9. PnodeRef** (V3:355) - Medium risk, child of UsagePoint
**10. AggregatedNodeRef** (V3:403) - Medium risk, child of UsagePoint
**11. ReadingQuality** (V3:163) - Higher risk, child of IntervalReading
**12. IntervalReading** (V3:122) - Highest risk, child of IntervalBlock with ReadingQuality

**Example for Phase 4 (PhoneNumber)**:
```bash
# 1. Update Entity
# openespi-common/src/main/java/.../customer/entity/PhoneNumberEntity.java
# Change: public class PhoneNumberEntity extends IdentifiedObject
# To:     public class PhoneNumberEntity

# 2. Update DTO (if exists)
# Remove selfLink, upLink fields

# 3. Update Mapper (if exists)
# Remove link mapping methods

# 4. Update Flyway
# V3__Create_additiional_Base_Tables.sql
# Remove lines ~804: phone_number_related_links table

# 5. Update Tests
# Search for PhoneNumber test assertions on selfLink/upLink

# 6. Test
mvn flyway:clean flyway:migrate -pl openespi-common
mvn test -pl openespi-common
mvn verify -pl openespi-common -Pintegration-tests

# 7. Commit
git commit -m "refactor(PhoneNumber): remove IdentifiedObject inheritance

PhoneNumber is a custom polymorphic collection not in ESPI XSD.
Removed related_links table and IdentifiedObject base class.

- Updated PhoneNumberEntity to remove IdentifiedObject
- Removed phone_number_related_links table from V3 migration
- Updated tests to remove selfLink/upLink assertions"
```

---

### Phase 14: Final Validation (30 minutes)
**Goal**: Comprehensive testing across all databases

```bash
# Clean rebuild
mvn clean install -pl openespi-common

# Test all databases with TestContainers
mvn verify -pl openespi-common -Pintegration-tests

# Run migration verification
mvn test -Dtest=MigrationVerificationTest

# Verify table counts
mvn flyway:info -pl openespi-common
# Should show:
# - 18 correct related_links tables (15 existing + 4 new - 1 identified_object_related_links)
# - 0 incorrect related_links tables
```

---

### Phase 15: Documentation Update (15 minutes)
**Goal**: Update all documentation to reflect changes

**Files to Update**:
- ✅ FLYWAY_SCHEMA_SUMMARY.md (mark as implemented)
- ✅ MULTI_PHASE_SCHEMA_COMPLIANCE_PLAN.md (update status)
- ✅ SCHEMA_COMPLIANCE_REMEDIATION_PLAN.md (mark phases complete)
- ✅ MULTI_PHASE_PLAN_UPDATES.md (add implementation notes)

---

### Phase 16: Create Pull Request (10 minutes)
```bash
# Push feature branch
git push origin feature/schema-compliance-related-links-cleanup

# Create PR with description
gh pr create \
  --title "ESPI 4.0 Schema Compliance: related_links Table Cleanup" \
  --body "$(cat <<'EOF'
## Summary
Implements ESPI 4.0 schema compliance by:
- Adding 4 missing related_links tables
- Removing 11 incorrect related_links tables
- Refactoring 11 entities to correct inheritance hierarchy

## Changes
- ✅ Added customer_related_links, meter_related_links, customer_account_related_links, electric_power_quality_summary_related_links
- ✅ Removed 11 incorrect related_links tables (see SCHEMA_COMPLIANCE_AUDIT_RESULTS.md)
- ✅ Refactored 9 entities from IdentifiedObject to Object inheritance
- ✅ Updated 2 special case entities (RetailCustomer, Subscription)
- ✅ All tests passing (37 test classes validated)
- ✅ Migration tests passing for H2, MySQL, PostgreSQL

## Test Results
\`\`\`
Tests run: [X], Failures: 0, Errors: 0, Skipped: 0
\`\`\`

## Documentation
- SCHEMA_COMPLIANCE_AUDIT_RESULTS.md
- SCHEMA_COMPLIANCE_REMEDIATION_PLAN.md
- FLYWAY_SCHEMA_SUMMARY.md
- MULTI_PHASE_SCHEMA_COMPLIANCE_PLAN.md

## Breaking Changes
None - system not yet deployed

Closes #[issue-number]
EOF
)"
```

---

## Estimated Timeline

| Phase | Duration | Complexity |
|-------|----------|------------|
| 0. Preparation | 5 min | ⚪ Trivial |
| 1. Add Missing Tables | 30 min | 🟢 Low |
| 2. Code Review & Baseline | 2-3 hours | 🟡 Medium |
| 3. Special Cases | 1 hour | 🟢 Low |
| 4-13. Entity Refactoring (Pilot: 2 entities) | 4-6 hours | 🔴 High |
| 4-13. Entity Refactoring (Remaining: 7 entities) | 14-21 hours | 🔴 High |
| 14. Final Validation | 30 min | 🟡 Medium |
| 15. Documentation | 15 min | 🟢 Low |
| 16. Pull Request | 10 min | ⚪ Trivial |

**Total Estimated Time**: 22-32 hours (3-4 working days)
**Note**: Pilot-first approach allows validation before scaling to remaining entities

---

**Next Action**: Create feature branch and begin Phase 1 (add missing tables)
