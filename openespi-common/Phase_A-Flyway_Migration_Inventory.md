# Phase A: Flyway Migration Inventory

**Date**: 2026-01-06
**Branch**: `fix/schema-compliance-analysis`
**Related**: PHASE_A_ANALYSIS_FINDINGS.md, Phase_A-DTO_Analysis.md

---

## Executive Summary

Identified **11 incorrect `related_links` tables** across 2 Flyway migration files that must be removed:
- **V1__Create_Base_Tables.sql**: 4 tables (retail_customer, service_delivery_point, subscription, batch_list)
- **V3__Create_additiional_Base_Tables.sql**: 7 tables (interval_reading, reading_quality, pnode_ref, aggregated_node_ref, line_item, phone_number, statement_ref)

Each table removal includes:
- DROP TABLE statement
- DROP INDEX statement

**Total SQL statements to remove**: 22 (11 CREATE TABLE + 11 CREATE INDEX)

---

## Migration Files Overview

### V1__Create_Base_Tables.sql

**Location**: `src/main/resources/db/migration/V1__Create_Base_Tables.sql`

**Tables to Remove**: 4
- retail_customer_related_links
- service_delivery_point_related_links
- subscription_related_links
- batch_list_related_links

---

### V3__Create_additiional_Base_Tables.sql

**Location**: `src/main/resources/db/migration/V3__Create_additiional_Base_Tables.sql`

**Tables to Remove**: 7
- interval_reading_related_links
- reading_quality_related_links
- pnode_ref_related_links
- aggregated_node_ref_related_links
- line_item_related_links
- phone_number_related_links
- statement_ref_related_links

---

## Detailed Removal Inventory

### V1 Migration - Table 1: retail_customer_related_links

**Lines**: 175-182

**SQL to Remove**:
```sql
CREATE TABLE retail_customer_related_links
(
    retail_customer_id CHAR(36) NOT NULL,
    related_links      VARCHAR(1024),
    FOREIGN KEY (retail_customer_id) REFERENCES retail_customers (id) ON DELETE CASCADE
);

CREATE INDEX idx_retail_customer_related_links ON retail_customer_related_links (retail_customer_id);
```

**Rationale**: RetailCustomer uses direct FK relationships, not Atom rel="related" links.

---

### V1 Migration - Table 2: service_delivery_point_related_links

**Lines**: 213-220

**SQL to Remove**:
```sql
CREATE TABLE service_delivery_point_related_links
(
    service_delivery_point_id CHAR(36) NOT NULL,
    related_links             VARCHAR(1024),
    FOREIGN KEY (service_delivery_point_id) REFERENCES service_delivery_points (id) ON DELETE CASCADE
);

CREATE INDEX idx_sdp_related_links ON service_delivery_point_related_links (service_delivery_point_id);
```

**Rationale**: ServiceDeliveryPoint extends Object in espi.xsd:1161, not IdentifiedObject. It's an embedded element, not a standalone resource.

---

### V1 Migration - Table 3: subscription_related_links

**Lines**: 378-385

**SQL to Remove**:
```sql
CREATE TABLE subscription_related_links
(
    subscription_id CHAR(36) NOT NULL,
    related_links   VARCHAR(1024),
    FOREIGN KEY (subscription_id) REFERENCES subscriptions (id) ON DELETE CASCADE
);

CREATE INDEX idx_subscription_related_links ON subscription_related_links (subscription_id);
```

**Rationale**: Subscription uses direct FK relationships for OAuth2 authorization, not Atom rel="related" links.

---

### V1 Migration - Table 4: batch_list_related_links

**Lines**: 411-418

**SQL to Remove**:
```sql
CREATE TABLE batch_list_related_links
(
    batch_list_id CHAR(36) NOT NULL,
    related_links VARCHAR(1024),
    FOREIGN KEY (batch_list_id) REFERENCES batch_lists (id) ON DELETE CASCADE
);

CREATE INDEX idx_batch_list_related_links ON batch_list_related_links (batch_list_id);
```

**Rationale**: BatchListType extends Object in espi.xsd:1432 - it's a sequence wrapper, not an IdentifiedObject.

---

### V3 Migration - Table 1: interval_reading_related_links

**Lines**: 122-130

**SQL to Remove**:
```sql
CREATE TABLE interval_reading_related_links
(
    interval_reading_id CHAR(36) NOT NULL,
    related_links       VARCHAR(1024),
    FOREIGN KEY (interval_reading_id) REFERENCES interval_readings (id) ON DELETE CASCADE
);

-- Indexes for interval_reading_related_links table
CREATE INDEX idx_interval_reading_related_links ON interval_reading_related_links (interval_reading_id);
```

**Rationale**: IntervalReading extends Object in espi.xsd:1016, not IdentifiedObject. It's a child element collection of IntervalBlock.

---

### V3 Migration - Table 2: reading_quality_related_links

**Lines**: 163-171

**SQL to Remove**:
```sql
CREATE TABLE reading_quality_related_links
(
    reading_quality_id CHAR(36) NOT NULL,
    related_links      VARCHAR(1024),
    FOREIGN KEY (reading_quality_id) REFERENCES reading_qualities (id) ON DELETE CASCADE
);

-- Indexes for reading_quality_related_links table
CREATE INDEX idx_reading_quality_related_links ON reading_quality_related_links (reading_quality_id);
```

**Rationale**: ReadingQuality extends Object in espi.xsd:1062, not IdentifiedObject. It's a child element collection of IntervalReading.

---

### V3 Migration - Table 3: pnode_ref_related_links

**Lines**: 355-363

**SQL to Remove**:
```sql
CREATE TABLE pnode_ref_related_links
(
    pnode_ref_id  CHAR(36) NOT NULL,
    related_links VARCHAR(1024),
    FOREIGN KEY (pnode_ref_id) REFERENCES pnode_refs (id) ON DELETE CASCADE
);

-- Indexes for pnode_ref_related_links table
CREATE INDEX idx_pnode_ref_related_links ON pnode_ref_related_links (pnode_ref_id);
```

**Rationale**: PnodeRef extends Object in espi.xsd:1539, not IdentifiedObject. It's a reference element collection within UsagePoint.

---

### V3 Migration - Table 4: aggregated_node_ref_related_links

**Lines**: 403-411

**SQL to Remove**:
```sql
CREATE TABLE aggregated_node_ref_related_links
(
    aggregated_node_ref_id CHAR(36) NOT NULL,
    related_links          VARCHAR(1024),
    FOREIGN KEY (aggregated_node_ref_id) REFERENCES aggregated_node_refs (id) ON DELETE CASCADE
);

-- Indexes for aggregated_node_ref_related_links table
CREATE INDEX idx_aggregated_node_ref_related_links ON aggregated_node_ref_related_links (aggregated_node_ref_id);
```

**Rationale**: AggregatedNodeRef extends Object in espi.xsd:1570, not IdentifiedObject. It's a reference element collection within UsagePoint.

---

### V3 Migration - Table 5: line_item_related_links

**Lines**: 775-782

**SQL to Remove**:
```sql
CREATE TABLE line_item_related_links
(
    line_item_id  CHAR(36) NOT NULL,
    related_links VARCHAR(1024),
    FOREIGN KEY (line_item_id) REFERENCES line_items (id) ON DELETE CASCADE
);

CREATE INDEX idx_line_item_related_links ON line_item_related_links (line_item_id);
```

**Rationale**: LineItem extends Object in espi.xsd:1444, not IdentifiedObject. It's a child element collection of UsageSummary (costAdditionalDetailLastPeriod).

---

### V3 Migration - Table 6: phone_number_related_links

**Lines**: 843-850

**SQL to Remove**:
```sql
CREATE TABLE phone_number_related_links
(
    phone_number_id CHAR(36) NOT NULL,
    related_links   VARCHAR(1024),
    FOREIGN KEY (phone_number_id) REFERENCES phone_numbers (id) ON DELETE CASCADE
);

CREATE INDEX idx_phone_number_related_links ON phone_number_related_links (phone_number_id);
```

**Rationale**: PhoneNumber is not in ESPI XSD - it's a custom addition. Polymorphic ownership suggests it's a value object collection.

---

### V3 Migration - Table 7: statement_ref_related_links

**Lines**: 1081-1088

**SQL to Remove**:
```sql
CREATE TABLE statement_ref_related_links
(
    statement_ref_id CHAR(36) NOT NULL,
    related_links    VARCHAR(1024),
    FOREIGN KEY (statement_ref_id) REFERENCES statement_refs (id) ON DELETE CASCADE
);

CREATE INDEX idx_statement_ref_related_links ON statement_ref_related_links (statement_ref_id);
```

**Rationale**: StatementRef extends Object in customer.xsd:285, not IdentifiedObject. It's a document reference collection within Statement.

---

## Implementation Strategy

### Option 1: Update Existing Migrations (Development Only)

**When to Use**: Project is in development, database has not been deployed to production.

**Approach**:
1. Directly remove the CREATE TABLE and CREATE INDEX statements from V1 and V3 migration files
2. Add comments explaining why tables were removed (XSD compliance)
3. Reset Flyway baseline if needed for local development

**Pros**:
- Cleaner migration history
- Fewer migration files
- Easier to understand final schema

**Cons**:
- Cannot be used if any environment has already run V1/V3 migrations
- Breaks Flyway checksum validation

---

### Option 2: Create New DROP TABLE Migrations (Production Safe)

**When to Use**: Database migrations have been deployed to any environment (dev, test, production).

**Approach**:
1. Keep V1 and V3 migrations unchanged
2. Create new migration files:
   - `V4__Remove_Collection_Related_Links.sql` (7 tables from V3)
   - `V5__Remove_API_Entity_Related_Links.sql` (2 tables from V1: retail_customer, subscription)
   - `V6__Remove_Special_Case_Related_Links.sql` (2 tables from V1: service_delivery_point, batch_list)
3. Use `DROP TABLE IF EXISTS` for safety

**Pros**:
- Production safe
- Maintains Flyway checksum integrity
- Clear migration history
- Reversible (can create undo migrations)

**Cons**:
- More migration files
- Migration files remain with incorrect schema definitions

---

## Recommended Approach

**Recommendation**: Use **Option 2 (New DROP TABLE Migrations)** for the following reasons:

1. **Safety**: Ensures compatibility with any environments that have already run V1/V3
2. **Audit Trail**: Clear documentation of schema evolution
3. **Reversibility**: Can create undo migrations if needed
4. **Best Practice**: Follows Flyway recommended patterns

---

## New Migration Scripts

### V4__Remove_Collection_Related_Links.sql

**Tables**: 7 (collection-based entities)

```sql
-- ============================================================================
-- V4__Remove_Collection_Related_Links.sql
--
-- Removes related_links tables for entities extending Object, not IdentifiedObject
-- per ESPI 4.0 XSD schema compliance (Issue #28)
--
-- Rationale: Entities extending Object in ESPI XSD are element collections
-- within parent resources, not standalone IdentifiedObject resources.
-- Atom related links (rel="related") only apply to IdentifiedObject resources.
--
-- Category 1: XSD Object-Based Entities (7 tables)
-- ============================================================================

-- Remove IntervalReading related links (espi.xsd:1016 extends Object)
DROP TABLE IF EXISTS interval_reading_related_links;

-- Remove ReadingQuality related links (espi.xsd:1062 extends Object)
DROP TABLE IF EXISTS reading_quality_related_links;

-- Remove PnodeRef related links (espi.xsd:1539 extends Object)
DROP TABLE IF EXISTS pnode_ref_related_links;

-- Remove AggregatedNodeRef related links (espi.xsd:1570 extends Object)
DROP TABLE IF EXISTS aggregated_node_ref_related_links;

-- Remove LineItem related links (espi.xsd:1444 extends Object)
DROP TABLE IF EXISTS line_item_related_links;

-- Remove PhoneNumber related links (custom addition, not in XSD)
DROP TABLE IF EXISTS phone_number_related_links;

-- Remove StatementRef related links (customer.xsd:285 extends Object)
DROP TABLE IF EXISTS statement_ref_related_links;
```

---

### V5__Remove_API_Entity_Related_Links.sql

**Tables**: 2 (API entities with direct FK relationships)

```sql
-- ============================================================================
-- V5__Remove_API_Entity_Related_Links.sql
--
-- Removes related_links tables for API entities that use direct FK relationships
-- instead of Atom rel="related" links (Issue #28)
--
-- Rationale: RetailCustomer and Subscription extend IdentifiedObject
-- (they are top-level entities with identity), but they use direct foreign key
-- references for relationships instead of Atom related links tables.
--
-- Category 2: Custom Entities Using Direct FK References (2 tables)
-- ============================================================================

-- Remove RetailCustomer related links
-- Uses direct FK: usage_points.retail_customer_id, authorizations.retail_customer_id
DROP TABLE IF EXISTS retail_customer_related_links;

-- Remove Subscription related links
-- Uses direct FK: subscription.retail_customer_id, subscription.authorization_id, etc.
DROP TABLE IF EXISTS subscription_related_links;
```

---

### V6__Remove_Special_Case_Related_Links.sql

**Tables**: 2 (special cases - embedded/wrapper types)

```sql
-- ============================================================================
-- V6__Remove_Special_Case_Related_Links.sql
--
-- Removes related_links tables for special case entities (Issue #28)
--
-- Rationale:
-- - ServiceDeliveryPoint: Extends Object in espi.xsd:1161, not IdentifiedObject.
--   It's an embedded element within UsagePoint, not a standalone resource.
-- - BatchList: BatchListType in espi.xsd:1432 is a sequence wrapper, not IdentifiedObject.
--   It's a transient collection for batch operations.
--
-- Category 3: Element Wrappers (2 tables)
-- ============================================================================

-- Remove ServiceDeliveryPoint related links (espi.xsd:1161 extends Object)
DROP TABLE IF EXISTS service_delivery_point_related_links;

-- Remove BatchList related links (espi.xsd:1432 BatchListType - sequence wrapper)
DROP TABLE IF EXISTS batch_list_related_links;
```

---

## Verification Queries

After running migrations, verify tables are removed:

```sql
-- Check for any remaining incorrect related_links tables
SELECT table_name
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN (
    'interval_reading_related_links',
    'reading_quality_related_links',
    'pnode_ref_related_links',
    'aggregated_node_ref_related_links',
    'line_item_related_links',
    'service_delivery_point_related_links',
    'statement_ref_related_links',
    'phone_number_related_links',
    'retail_customer_related_links',
    'subscription_related_links',
    'batch_list_related_links'
  );

-- Should return 0 rows after successful migration
```

---

## Testing Strategy

### Unit Tests
- Verify entity persistence works without related_links tables
- Verify repository methods function correctly
- Verify cascade deletes work properly

### Integration Tests (TestContainers)
1. **MySQL Tests**: Run migrations V1-V6, verify schema correctness
2. **PostgreSQL Tests**: Run migrations V1-V6, verify schema correctness
3. **H2 Tests**: Run migrations V1-V6, verify schema correctness

### Flyway Migration Tests
- Verify migrations V4-V6 are idempotent (can run multiple times)
- Verify Flyway checksum validation passes
- Verify migration order is correct
- Verify rollback scenarios (if undo migrations created)

---

## Rollback Plan

If migrations V4-V6 cause issues:

### Immediate Rollback (Before Production Deployment)
1. Create undo migrations:
   - `U4__Restore_Collection_Related_Links.sql`
   - `U5__Restore_API_Entity_Related_Links.sql`
   - `U6__Restore_Special_Case_Related_Links.sql`
2. Run undo migrations to restore tables
3. Investigate root cause

### Production Rollback (After Deployment)
1. **DO NOT** run undo migrations in production without testing
2. Deploy hotfix with undo migrations to staging first
3. Verify application functions correctly with restored tables
4. Schedule maintenance window for production rollback

---

## Summary

**Migration Files Identified**: 2
- V1__Create_Base_Tables.sql (4 tables to remove)
- V3__Create_additiional_Base_Tables.sql (7 tables to remove)

**New Migration Files Required**: 3
- V4__Remove_Collection_Related_Links.sql (7 tables)
- V5__Remove_API_Entity_Related_Links.sql (2 tables)
- V6__Remove_Special_Case_Related_Links.sql (2 tables)

**Total SQL Statements to Remove**: 22
- 11 CREATE TABLE statements
- 11 CREATE INDEX statements

**Recommended Approach**: Create new DROP TABLE migrations (V4-V6) instead of modifying existing V1/V3 migrations.

**Next Steps**:
1. Create V4-V6 migration script files
2. Test migrations with TestContainers (MySQL, PostgreSQL, H2)
3. Update FLYWAY_SCHEMA_SUMMARY.md with changes
4. Document in MULTI_PHASE_SCHEMA_COMPLIANCE_PLAN.md

---

**Analysis Status**: ✅ Complete
**Migration Files Created**: Ready for Phase B implementation
**Testing**: Pending Phase B
