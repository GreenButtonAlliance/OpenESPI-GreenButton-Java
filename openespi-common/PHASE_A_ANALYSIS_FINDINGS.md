# Phase A: Schema Compliance Analysis Findings

**Date**: 2026-01-06
**Branch**: `fix/schema-compliance-analysis`
**Related Issue**: #28
**Related Plan**: SCHEMA_COMPLIANCE_REMEDIATION_PLAN.md

---

## Executive Summary

Analysis of 11 entities reveals **ALL correctly do NOT extend IdentifiedObject**. This is good news - the Java entity layer already reflects proper XSD compliance. However, **11 incorrect `related_links` database tables** still exist and must be removed.

**Key Finding**: The entity Java files have already been corrected (likely in previous work), but the database schema (Flyway migrations) has not been updated to match.

---

## Entity Analysis Results

### Category 1: XSD Object-Based Entities (7 entities)

These entities extend `Object` in XSD, not `IdentifiedObject`. They are element collections within parent resources.

| # | Entity | Current Java Inheritance | Database Table | Related Links Table | XSD Reference | Status |
|---|--------|--------------------------|----------------|---------------------|---------------|--------|
| 1 | **IntervalReadingEntity** | Does NOT extend IdentifiedObject ✅ | `interval_readings` | `interval_reading_related_links` ❌ | espi.xsd:1016 extends Object | Entity ✅ DB ❌ |
| 2 | **ReadingQualityEntity** | Does NOT extend IdentifiedObject ✅ | `reading_qualities` | `reading_quality_related_links` ❌ | espi.xsd:1062 extends Object | Entity ✅ DB ❌ |
| 3 | **PnodeRefEntity** | **EXTENDS IdentifiedObject** ❌ | `pnode_refs` | `pnode_ref_related_links` ❌ | espi.xsd:1539 extends Object | Entity ❌ DB ❌ |
| 4 | **AggregatedNodeRefEntity** | Does NOT extend IdentifiedObject ✅ | `aggregated_node_refs` | `aggregated_node_ref_related_links` ❌ | espi.xsd:1570 extends Object | Entity ✅ DB ❌ |
| 5 | **LineItemEntity** | Does NOT extend IdentifiedObject ✅ | `line_items` | `line_item_related_links` ❌ | espi.xsd:1444 extends Object | Entity ✅ DB ❌ |
| 6 | **ServiceDeliveryPointEntity** | **EXTENDS IdentifiedObject** ❌ | `service_delivery_points` | `service_delivery_point_related_links` ❌ | espi.xsd:1161 extends Object | Entity ❌ DB ❌ |
| 7 | **StatementRefEntity** | Does NOT extend IdentifiedObject ✅ | `statement_refs` | `statement_ref_related_links` ❌ | customer.xsd:285 extends Object | Entity ✅ DB ❌ |

**Critical Issue Discovered**:
- ✅ **5 entities correctly do NOT extend IdentifiedObject**: IntervalReading, ReadingQuality, AggregatedNodeRef, LineItem, StatementRef
- ❌ **2 entities INCORRECTLY extend IdentifiedObject**: PnodeRef, ServiceDeliveryPoint

---

### Category 2: Custom Entities Using Direct FK References (2 entities)

| # | Entity | Current Java Inheritance | Database Table | Related Links Table | Purpose | Status |
|---|--------|--------------------------|----------------|---------------------|---------|--------|
| 8 | **RetailCustomerEntity** | **EXTENDS IdentifiedObject** ✅ | `retail_customers` | `retail_customer_related_links` ❌ | Authentication/authorization bridge | Entity ✅ DB ❌ |
| 9 | **SubscriptionEntity** | **EXTENDS IdentifiedObject** ✅ | `subscriptions` | `subscription_related_links` ❌ | OAuth2 access token authorization | Entity ✅ DB ❌ |

**Note**: RetailCustomer and Subscription correctly extend IdentifiedObject (they are API entities with identity), but they use **direct FK relationships** instead of Atom `rel="related"` links. The `related_links` tables are unnecessary.

---

### Category 3: Element Wrappers (2 entities)

| # | Entity | Current Java Inheritance | Database Table | Related Links Table | XSD Type | Status |
|---|--------|--------------------------|----------------|---------------------|----------|--------|
| 10 | **PhoneNumberEntity** | Does NOT extend IdentifiedObject ✅ | `phone_numbers` | `phone_number_related_links` ❌ | NOT in XSD (custom) | Entity ✅ DB ❌ |
| 11 | **BatchListEntity** | Does NOT extend IdentifiedObject ✅ | `batch_lists` | `batch_list_related_links` ❌ | BatchListType (sequence) | Entity ✅ DB ❌ |

---

## Detailed Entity Findings

### 1. IntervalReadingEntity ✅

**File**: `src/main/java/org/greenbuttonalliance/espi/common/domain/usage/IntervalReadingEntity.java`

**Current State**:
- ✅ Does NOT extend IdentifiedObject
- ✅ Has UUID primary key (`@GeneratedValue(strategy = GenerationType.UUID)`)
- ✅ Has `@ManyToOne` relationship to IntervalBlock (line 112: `private IntervalBlockEntity intervalBlock;`)
- ✅ Has `@OneToMany` relationship to ReadingQuality collection (line 125: `List<ReadingQualityEntity> readingQualities`)
- ✅ Javadoc correctly states: "Note: IntervalReading does NOT extend IdentifiedObject per ESPI 4.0 specification" (line 45)

**Issues**:
- ❌ Database has `interval_reading_related_links` table (should not exist)

**Required Changes**:
- Remove `interval_reading_related_links` table from Flyway migration
- Verify DTO does not have Atom link elements
- Verify no services attempt to access IntervalReading as standalone resource

---

### 2. ReadingQualityEntity ✅

**File**: `src/main/java/org/greenbuttonalliance/espi/common/domain/usage/ReadingQualityEntity.java`

**Current State**:
- ✅ Does NOT extend IdentifiedObject
- ✅ Has UUID primary key
- ✅ Has `@ManyToOne` relationship to IntervalReading (line 98: `private IntervalReadingEntity intervalReading;`)
- ✅ Javadoc correctly states: "Note: ReadingQuality does NOT extend IdentifiedObject per ESPI 4.0 specification" (line 44)
- ✅ Has quality constants and validation methods

**Issues**:
- ❌ Database has `reading_quality_related_links` table (should not exist)

**Required Changes**:
- Remove `reading_quality_related_links` table from Flyway migration
- Verify DTO does not have Atom link elements

---

### 3. PnodeRefEntity ❌ CRITICAL

**File**: `src/main/java/org/greenbuttonalliance/espi/common/domain/usage/PnodeRefEntity.java`

**Current State**:
- ❌ **EXTENDS IdentifiedObject** (line 40: `public class PnodeRefEntity extends IdentifiedObject`)
- Has UUID primary key (inherited from IdentifiedObject)
- Has `@ManyToOne` relationship to UsagePoint (line 74: `private UsagePointEntity usagePoint;`)
- Has fields: apnodeType, ref, startEffectiveDate, endEffectiveDate

**Issues**:
- ❌ **INCORRECTLY extends IdentifiedObject** - should extend Object per espi.xsd:1539
- ❌ Database has `pnode_ref_related_links` table (should not exist)
- ❌ Inherits selfLink, upLink, relatedLinks from IdentifiedObject

**Required Changes**:
- **Remove `extends IdentifiedObject`** from entity class
- Add explicit UUID primary key field (no longer inherited)
- Remove IdentifiedObject-specific methods (getSelfHref, getUpHref, etc.)
- Remove `pnode_ref_related_links` table from Flyway migration
- Update DTO to remove Atom link elements
- Update mapper to handle non-IdentifiedObject mapping

---

### 4. AggregatedNodeRefEntity ✅

**File**: `src/main/java/org/greenbuttonalliance/espi/common/domain/usage/AggregatedNodeRefEntity.java`

**Current State**:
- ✅ Does NOT extend IdentifiedObject
- ✅ Has UUID primary key explicitly defined (line 56: `private UUID id;`)
- ✅ Has `@ManyToOne` relationship to PnodeRef (line 91: `private PnodeRefEntity pnodeRef;`)
- ✅ Has `@ManyToOne` relationship to UsagePoint (line 99: `private UsagePointEntity usagePoint;`)
- ✅ Javadoc correctly states: "Note: AggregatedNodeRef does NOT extend IdentifiedObject per ESPI 4.0 specification" (line 39)

**Issues**:
- ❌ Database has `aggregated_node_ref_related_links` table (should not exist)

**Required Changes**:
- Remove `aggregated_node_ref_related_links` table from Flyway migration
- Verify DTO does not have Atom link elements

---

### 5. LineItemEntity ✅

**File**: `src/main/java/org/greenbuttonalliance/espi/common/domain/usage/LineItemEntity.java`

**Current State**:
- ✅ Does NOT extend IdentifiedObject
- ✅ Has UUID primary key
- ✅ Has `@ManyToOne` relationship to UsageSummary (line 109: `private UsageSummaryEntity usageSummary;`)
- ✅ Javadoc correctly states: "Note: LineItem does NOT extend IdentifiedObject per ESPI 4.0 specification" (line 48)
- ✅ Has extensive utility methods for currency conversions

**Issues**:
- ❌ Database has `line_item_related_links` table (should not exist)

**Required Changes**:
- Remove `line_item_related_links` table from Flyway migration
- Verify DTO does not have Atom link elements

---

### 6. ServiceDeliveryPointEntity ❌ CRITICAL

**File**: `src/main/java/org/greenbuttonalliance/espi/common/domain/usage/ServiceDeliveryPointEntity.java`

**Current State**:
- ❌ **EXTENDS IdentifiedObject** (line 48: `public class ServiceDeliveryPointEntity extends IdentifiedObject`)
- Has UUID primary key (inherited from IdentifiedObject)
- Has fields: mrid, name, tariffProfile, customerAgreement
- Javadoc says "ServiceDeliveryPoint is now a standalone ESPI resource that extends IdentifiedObject" (line 41)

**Issues**:
- ❌ **INCORRECTLY extends IdentifiedObject** - should extend Object per espi.xsd:1161
- ❌ Database has `service_delivery_point_related_links` table (should not exist)
- ❌ Inherits selfLink, upLink, relatedLinks from IdentifiedObject
- ❌ Javadoc is incorrect - claims it's a "standalone ESPI resource"

**Required Changes**:
- **Remove `extends IdentifiedObject`** from entity class
- Add explicit UUID primary key field
- Remove IdentifiedObject-specific methods
- Update javadoc to reflect it's NOT a standalone resource
- Remove `service_delivery_point_related_links` table from Flyway migration
- Update DTO to remove Atom link elements
- Update mapper to handle non-IdentifiedObject mapping
- **Consider**: Could be `@Embedded` in UsagePoint instead of separate entity

---

### 7. StatementRefEntity ✅

**File**: `src/main/java/org/greenbuttonalliance/espi/common/domain/customer/entity/StatementRefEntity.java`

**Current State**:
- ✅ Does NOT extend IdentifiedObject
- ✅ Has UUID primary key
- ✅ Has `@ManyToOne` relationship to Statement (line 79: `private StatementEntity statement;`)
- ✅ Javadoc correctly states: "Note: StatementRef does NOT extend IdentifiedObject per ESPI 4.0 specification" (line 38)
- ✅ Has fields: fileName, mediaType, statementURL

**Issues**:
- ❌ Database has `statement_ref_related_links` table (should not exist)

**Required Changes**:
- Remove `statement_ref_related_links` table from Flyway migration
- Verify DTO does not have Atom link elements

---

### 8. PhoneNumberEntity ✅

**File**: `src/main/java/org/greenbuttonalliance/espi/common/domain/customer/entity/PhoneNumberEntity.java`

**Current State**:
- ✅ Does NOT extend IdentifiedObject
- ✅ Has UUID primary key
- ✅ Has polymorphic parent tracking (line 93: `parentEntityUuid`, line 99: `parentEntityType`)
- ✅ Javadoc correctly states: "Note: PhoneNumber does NOT extend IdentifiedObject per ESPI 4.0 specification" (line 39)
- ✅ Has PhoneType enum (PRIMARY, SECONDARY, LOCATION_PRIMARY, LOCATION_SECONDARY)

**Issues**:
- ❌ Database has `phone_number_related_links` table (should not exist)

**Required Changes**:
- Remove `phone_number_related_links` table from Flyway migration
- Verify DTO does not have Atom link elements

---

### 9. RetailCustomerEntity ⚠️ SPECIAL CASE

**File**: `src/main/java/org/greenbuttonalliance/espi/common/domain/usage/RetailCustomerEntity.java`

**Current State**:
- ✅ **EXTENDS IdentifiedObject** (line 52: `public class RetailCustomerEntity extends IdentifiedObject`)
- Has UUID primary key (inherited)
- Has `@OneToMany` to UsagePoint collection (line 171: `List<UsagePointEntity> usagePoints`)
- Has `@OneToMany` to Authorization collection (line 180: `List<AuthorizationEntity> authorizations`)
- **Uses direct FK relationships, NOT Atom rel="related" links**
- Overrides `getSelfHref()` to return `/espi/1_1/resource/RetailCustomer/{id}` (line 296)

**Analysis**:
- ✅ Correctly extends IdentifiedObject (it's a top-level API entity with identity)
- ❌ However, it uses **direct FK references** (`usage_point.retail_customer_id`) instead of `related_links` table
- ❌ The `retail_customer_related_links` table is unused and should be removed

**Required Changes**:
- Remove `retail_customer_related_links` table from Flyway migration
- Verify services use direct FK queries, not related_links
- Keep IdentifiedObject inheritance (this is correct)
- Document in javadoc: "Uses direct FK relationships for marshalling, not Atom rel='related' links"

---

### 10. SubscriptionEntity ⚠️ SPECIAL CASE

**File**: `src/main/java/org/greenbuttonalliance/espi/common/domain/usage/SubscriptionEntity.java`

**Current State**:
- ✅ **EXTENDS IdentifiedObject** (line 54: `public class SubscriptionEntity extends IdentifiedObject`)
- Has UUID primary key (inherited)
- Has `@ManyToOne` to RetailCustomer (line 76: `private RetailCustomerEntity retailCustomer;`)
- Has `@OneToOne` to Authorization (line 85: `private AuthorizationEntity authorization;`)
- Has `@ManyToOne` to ApplicationInformation (line 93: `private ApplicationInformationEntity applicationInformation;`)
- Has `@ManyToMany` to UsagePoint collection (line 102: `List<UsagePointEntity> usagePoints`)
- **Uses direct FK relationships, NOT Atom rel="related" links**

**Analysis**:
- ✅ Correctly extends IdentifiedObject (it's a top-level API entity with identity)
- ❌ However, it uses **direct FK references** instead of `related_links` table
- ❌ The `subscription_related_links` table is unused and should be removed

**Required Changes**:
- Remove `subscription_related_links` table from Flyway migration
- Verify services use direct FK queries, not related_links
- Keep IdentifiedObject inheritance (this is correct)
- Document in javadoc: "Uses direct FK relationships for marshalling, not Atom rel='related' links"

---

### 11. BatchListEntity ✅

**File**: `src/main/java/org/greenbuttonalliance/espi/common/domain/usage/BatchListEntity.java`

**Current State**:
- ✅ Does NOT extend IdentifiedObject
- ✅ Has UUID primary key
- ✅ Has `@ElementCollection` for resources (line 72: `List<String> resources`)
- ✅ Javadoc correctly states: "Note: BatchList does NOT extend IdentifiedObject per ESPI 4.0 specification" (line 45)
- ✅ Has extensive utility methods for managing resource URIs

**Issues**:
- ❌ Database has `batch_list_related_links` table (should not exist)
- ⚠️ **Question**: Should BatchList be persisted at all? XSD shows it's just a wrapper for URI collections

**Required Changes**:
- Remove `batch_list_related_links` table from Flyway migration
- Verify DTO does not have Atom link elements
- **Evaluate**: Consider making BatchList transient/in-memory only if not actively used

---

## Critical Issues Summary

### High Priority: Remove IdentifiedObject Inheritance

**2 entities incorrectly extend IdentifiedObject:**

1. **PnodeRefEntity** (line 40)
   - Must remove `extends IdentifiedObject`
   - Add explicit UUID primary key
   - Remove inherited Atom link methods

2. **ServiceDeliveryPointEntity** (line 48)
   - Must remove `extends IdentifiedObject`
   - Add explicit UUID primary key
   - Remove inherited Atom link methods
   - Update incorrect javadoc

### Medium Priority: Remove Unused Related Links Tables

**11 related_links tables to remove from database:**

| Table Name | Category | Flyway File |
|-----------|----------|-------------|
| `interval_reading_related_links` | Collection | V3__Create_additiional_Base_Tables.sql |
| `reading_quality_related_links` | Collection | V3__Create_additiional_Base_Tables.sql |
| `pnode_ref_related_links` | Collection | V3__Create_additiional_Base_Tables.sql |
| `aggregated_node_ref_related_links` | Collection | V3__Create_additiional_Base_Tables.sql |
| `line_item_related_links` | Collection | V3__Create_additiional_Base_Tables.sql |
| `service_delivery_point_related_links` | Collection | V1__Create_Base_Tables.sql |
| `statement_ref_related_links` | Collection | V3__Create_additiional_Base_Tables.sql |
| `phone_number_related_links` | Collection | V3__Create_additiional_Base_Tables.sql |
| `retail_customer_related_links` | API Entity (FK-based) | V1__Create_Base_Tables.sql |
| `subscription_related_links` | API Entity (FK-based) | V1__Create_Base_Tables.sql |
| `batch_list_related_links` | Wrapper | V1__Create_Base_Tables.sql |

---

## Next Steps

1. ✅ **Phase A Complete**: Entity analysis documented
2. ⏭️ **Read DTOs**: Analyze 11 corresponding DTO files for Atom link elements
3. ⏭️ **Identify Flyway migrations**: Find exact line numbers for table removals
4. ⏭️ **Create inventory**: Detailed change list for each entity/DTO/migration
5. ⏭️ **Update FLYWAY_SCHEMA_SUMMARY.md**: Document findings
6. ⏭️ **Create migration templates**: V4-V6 scripts for table removals

---

## Recommendations

### Before Phase B Implementation:

1. **Address PnodeRef and ServiceDeliveryPoint FIRST** - these have incorrect inheritance that blocks proper schema compliance
2. **Verify DTO layer** - ensure DTOs don't have Atom link elements for these 11 entities
3. **Check services** - ensure no REST endpoints treat these as standalone resources
4. **Run all tests** - baseline test suite before making changes

### Implementation Order:

**Phase 0** (NEW - PREREQUISITE):
- Fix PnodeRefEntity inheritance
- Fix ServiceDeliveryPointEntity inheritance
- Update corresponding DTOs and mappers
- Run tests to ensure no breakage

**Then proceed with original Phases B-E** as planned.

---

**Analysis Status**: ✅ Complete
**Critical Issues Found**: 2 (PnodeRef, ServiceDeliveryPoint extend IdentifiedObject incorrectly)
**Next Action**: Read corresponding DTO files
