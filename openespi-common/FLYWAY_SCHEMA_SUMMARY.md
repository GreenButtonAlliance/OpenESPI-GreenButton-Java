# Flyway Database Schema Summary

## Migration Strategy Overview

The OpenESPI database schema uses a **three-phase migration strategy**:

1. **V1**: Vendor-neutral base tables (no BLOB/BYTEA/BINARY columns)
2. **V2**: Vendor-specific tables (MySQL BLOB, PostgreSQL BYTEA, H2 BINARY)
3. **V3**: Additional vendor-neutral tables (dependent on V1 and V2)

---

## V1: Vendor-Neutral Base Tables (15 core tables)

**File**: `db/migration/V1__Create_Base_Tables.sql`
**Compatible with**: H2, MySQL, PostgreSQL

### Core Entity Tables

| Table | Purpose | Foreign Keys | Related Links Table |
|-------|---------|--------------|---------------------|
| `application_information` | OAuth2 application registration | None | `application_information_related_links` |
| `retail_customers` | Customer accounts (usage schema) | None | `retail_customer_related_links` |
| `service_delivery_points` | Physical delivery locations | None | `service_delivery_point_related_links` |
| `reading_types` | Measurement metadata | None | `reading_type_related_links` |
| `subscriptions` | Third-party data subscriptions | `application_information_id`, `retail_customer_id` | `subscription_related_links` |
| `authorizations` | OAuth2 access grants | `application_information_id`, `retail_customer_id`, `subscription_id` | `authorization_related_links` |
| `batch_lists` | Batch operation tracking | None | `batch_list_related_links` |

### Collection Tables

| Table | Parent Table | Purpose |
|-------|--------------|---------|
| `application_information_grant_types` | `application_information` | OAuth2 grant types |
| `application_information_scopes` | `application_information` | OAuth2 scopes |
| `batch_list_resources` | `batch_lists` | Batch resource URIs |

### Generic Related Links Table

| Table | Purpose |
|-------|---------|
| `identified_object_related_links` | Generic related links for all IdentifiedObject entities |

**Note**: This table is NOT entity-specific. It stores `identified_object_id` and can reference any entity extending `IdentifiedObject`.

---

## V2: Vendor-Specific Tables (BLOB/BYTEA/BINARY Columns)

Three vendor-specific migration files create identical logical schemas with different binary column types:

| File | Database | Binary Type |
|------|----------|-------------|
| `db/vendor/mysql/V2__MySQL_Specific_Tables.sql` | MySQL 8.0+ | `BLOB` |
| `db/vendor/postgres/V2__PostgreSQL_Specific_Tables.sql` | PostgreSQL 12+ | `BYTEA` |
| `db/vendor/h2/V2__H2_Specific_Tables.sql` | H2 | `BINARY` |

### Core Entity Tables

| Table | Purpose | Binary Columns | Foreign Keys | Related Links Table |
|-------|---------|----------------|--------------|---------------------|
| `time_configurations` | Timezone and DST parameters | `dst_end_rule`, `dst_start_rule` | None | `time_configuration_related_links` |
| `usage_points` | Energy metering points | `role_flags` | `retail_customer_id`, `service_delivery_point_id`, `local_time_parameters_id` | `usage_point_related_links` |

### Key Relationships

```
time_configurations (BLOB/BYTEA/BINARY)
    ↓ (local_time_parameters_id)
usage_points (BLOB/BYTEA/BINARY)
    ↓ (retail_customer_id)
retail_customers (V1)
    ↓ (service_delivery_point_id)
service_delivery_points (V1)
```

**Important**: `time_configurations.local_time_parameters_id` references `time_configurations.id` (self-reference for timezone inheritance).

---

## V3: Additional Vendor-Neutral Tables (50+ tables)

**File**: `db/migration/V3__Create_additiional_Base_Tables.sql`
**Compatible with**: H2, MySQL, PostgreSQL (depends on V2 tables existing)

### Usage Schema Tables (ESPI usage.xsd)

#### Time-Series Data Hierarchy

```
usage_points (V2)
    ↓
meter_readings
    ↓
interval_blocks
    ↓
interval_readings
    ↓
reading_qualities
```

| Table | Purpose | Foreign Keys | Related Links Table |
|-------|---------|--------------|---------------------|
| `meter_readings` | Reading collections | `usage_point_id`, `reading_type_id` | `meter_reading_related_links` |
| `interval_blocks` | Time-series blocks | `meter_reading_id` | `interval_block_related_links` |
| `interval_readings` | Individual interval readings | `interval_block_id` | `interval_reading_related_links` |
| `reading_qualities` | Reading quality indicators | `interval_reading_id` | `reading_quality_related_links` |
| `usage_summaries` | Aggregated usage summaries | `usage_point_id` | `usage_summary_related_links` |
| `electric_power_quality_summaries` | Power quality metrics | `usage_point_id` | `electric_power_quality_summary_related_links` |
| `line_items` | Billing line items | `usage_summary_id` | `line_item_related_links` |

#### Pricing Node References

| Table | Purpose | Foreign Keys | Related Links Table |
|-------|---------|--------------|---------------------|
| `pnode_refs` | Pricing node references | `usage_point_id` | `pnode_ref_related_links` |
| `aggregated_node_refs` | Aggregated pricing nodes | `pnode_ref_id`, `usage_point_id` | `aggregated_node_ref_related_links` |

#### Many-to-Many Relationships

| Table | Purpose | Foreign Keys |
|-------|---------|--------------|
| `subscription_usage_points` | Subscription-UsagePoint join table | `subscription_id`, `usage_point_id` |

### Customer Schema Tables (ESPI customer.xsd)

| Table | Purpose | Foreign Keys | Related Links Table |
|-------|---------|--------------|---------------------|
| `customers` | Customer information | `time_configuration_id`, `retail_customer_href` (ATOM link) | None |
| `customer_agreements` | Customer-supplier agreements | None | `customer_agreement_related_links` |
| `customer_accounts` | Billing accounts | `customer_id` | None |
| `service_suppliers` | Utility companies | None | `service_supplier_related_links` |
| `service_locations` | Physical service locations | None | `service_location_related_links` |
| `end_devices` | Meter devices | None | `end_device_related_links` |
| `meters` | Specialized meter devices | `id` (joined inheritance from `end_devices`) | None |
| `phone_numbers` | Phone number entities | Polymorphic: `parent_entity_uuid`, `parent_entity_type` | `phone_number_related_links` |
| `statements` | Billing statements | `customer_id` | `statement_related_links` |
| `statement_refs` | Statement file references | `statement_id` | `statement_ref_related_links` |
| `program_date_id_mappings` | Program date mappings | None | `program_date_id_mapping_related_links` |

#### Customer Schema Collection Tables

| Table | Parent Table | Purpose |
|-------|--------------|---------|
| `customer_agreement_future_status` | `customer_agreements` | Future status collection |
| `customer_account_notifications` | `customer_accounts` | Notification collection |

### V3 Foreign Key Additions (ALTER TABLE)

Two foreign key constraints are added to V2 tables:

```sql
ALTER TABLE authorizations ADD CONSTRAINT fk_authorization_subscription
    FOREIGN KEY (subscription_id) REFERENCES subscriptions (id) ON DELETE SET NULL;

ALTER TABLE usage_points ADD CONSTRAINT fk_usage_point_subscription
    FOREIGN KEY (subscription_id) REFERENCES subscriptions (id) ON DELETE SET NULL;
```

---

## "Related Links" Tables - Deep Dive

### Purpose

**Related links** tables implement the ESPI Atom `<link rel="related">` pattern. Each ESPI resource can have multiple related resource links for navigation between entities.

### Pattern

Every entity that extends `IdentifiedObject` has:
- **Self Link**: `self_link_rel`, `self_link_href`, `self_link_type` (stored in main entity table)
- **Up Link**: `up_link_rel`, `up_link_href`, `up_link_type` (stored in main entity table)
- **Related Links**: Multiple links stored in a separate `*_related_links` table

### Schema Pattern

```sql
CREATE TABLE {entity}_related_links (
    {entity}_id CHAR(36) NOT NULL,
    related_links VARCHAR(1024),
    FOREIGN KEY ({entity}_id) REFERENCES {entity} (id) ON DELETE CASCADE
);
```

### All Related Links Tables (26 total)

| Related Links Table | Parent Entity Table | Schema |
|---------------------|---------------------|--------|
| `identified_object_related_links` | ANY `IdentifiedObject` (generic) | Usage/Customer |
| `application_information_related_links` | `application_information` | Usage |
| `retail_customer_related_links` | `retail_customers` | Usage |
| `service_delivery_point_related_links` | `service_delivery_points` | Usage |
| `reading_type_related_links` | `reading_types` | Usage |
| `subscription_related_links` | `subscriptions` | Usage |
| `authorization_related_links` | `authorizations` | Usage |
| `batch_list_related_links` | `batch_lists` | Usage |
| `time_configuration_related_links` | `time_configurations` | Usage |
| `usage_point_related_links` | `usage_points` | Usage |
| `meter_reading_related_links` | `meter_readings` | Usage |
| `interval_block_related_links` | `interval_blocks` | Usage |
| `interval_reading_related_links` | `interval_readings` | Usage |
| `reading_quality_related_links` | `reading_qualities` | Usage |
| `usage_summary_related_links` | `usage_summaries` | Usage |
| `pnode_ref_related_links` | `pnode_refs` | Usage |
| `aggregated_node_ref_related_links` | `aggregated_node_refs` | Usage |
| `line_item_related_links` | `line_items` | Usage |
| `customer_agreement_related_links` | `customer_agreements` | Customer |
| `service_supplier_related_links` | `service_suppliers` | Customer |
| `service_location_related_links` | `service_locations` | Customer |
| `end_device_related_links` | `end_devices` | Customer |
| `phone_number_related_links` | `phone_numbers` | Customer |
| `statement_related_links` | `statements` | Customer |
| `statement_ref_related_links` | `statement_refs` | Customer |
| `program_date_id_mapping_related_links` | `program_date_id_mappings` | Customer |

### Related Links vs IdentifiedObject Related Links

**Question**: Why both entity-specific and generic related links tables?

**Answer**:
- **Entity-specific tables** (`{entity}_related_links`): Designed for typical 1:N relationships where each entity has 0-N related links
- **Generic table** (`identified_object_related_links`): Fallback/generic storage for polymorphic scenarios or entities without specific related links tables

**Note**: Most entities use entity-specific related links tables. The generic `identified_object_related_links` may be legacy or used for dynamic/runtime relationships.

---

## Key Cross-Schema Relationships

### Usage Schema → Customer Schema

| Usage Table | Customer Table | Link Type | Notes |
|-------------|----------------|-----------|-------|
| `customers` | `time_configurations` | FK: `time_configuration_id` | Customer timezone settings |
| `retail_customers` | `customers` | ATOM href: `retail_customer_href` | **NO foreign key** - uses ATOM link pattern |

**Critical**: The `retail_customers` (usage.xsd) and `customers` (customer.xsd) schemas are loosely coupled via ATOM href references, NOT foreign keys.

### Subscription Relationships (Added in V3)

```sql
-- V3 adds these foreign keys to V2 tables
authorizations.subscription_id → subscriptions.id
usage_points.subscription_id → subscriptions.id
```

---

## Entity Inheritance Patterns

### Joined Table Inheritance

| Parent Table | Child Table | Pattern | Notes |
|--------------|-------------|---------|-------|
| `end_devices` | `meters` | `meters.id` → `end_devices.id` | Meter is specialized EndDevice |

### Polymorphic Relationships

| Table | Pattern | Target Entities |
|-------|---------|-----------------|
| `phone_numbers` | `parent_entity_uuid` + `parent_entity_type` | Customer, ServiceSupplier, etc. |

---

## Embedded Objects Pattern

Many tables use **embedded object columns** instead of separate tables:

### Examples

| Table | Embedded Object | Columns Pattern |
|-------|-----------------|-----------------|
| `usage_points` | `SummaryMeasurement` (4 types) | `{type}_multiplier`, `{type}_timestamp`, `{type}_uom`, `{type}_value`, `{type}_reading_type_ref` |
| `usage_summaries` | `SummaryMeasurement` (10 types) | Same pattern as above |
| `usage_summaries` | `DateTimeInterval` (2 types) | `{type}_start`, `{type}_duration` |
| `customers` | `Organisation` | `customer_organisation_name`, `customer_street_detail`, etc. |
| `customers` | `Status` | `status_value`, `status_date_time`, `status_reason` |
| `service_suppliers` | `Organisation` | `supplier_organisation_name`, `supplier_street_detail`, etc. |

**Rationale**: ESPI XSD defines these as complex types embedded within parent elements, not separate resources with URIs.

---

## Complete Table Count

| Migration | Core Tables | Related Links Tables (Correct) | Related Links Tables (To Remove) | Collection Tables | Total |
|-----------|-------------|-------------------------------|----------------------------------|-------------------|-------|
| V1 | 7 | 4 | 3 | 4 | 18 |
| V2 (vendor-specific) | 2 | 2 | 0 | 0 | 4 |
| V3 | 29 | 10 | 8 | 2 | 50 |
| **Grand Total** | **38** | **16** | **11** | **6** | **72** |

**Note**: After schema compliance remediation, 11 incorrect related_links tables will be removed (entities extend Object, not IdentifiedObject).

---

## Object vs IdentifiedObject: Schema Compliance

### ESPI XSD Type Hierarchy

```
Object (base class)
    │
    ├── IdentifiedObject (adds mRID, Atom links)
    │       │
    │       ├── [CONCRETE ESPI RESOURCES - espi.xsd]
    │       ├── ApplicationInformation ✅
    │       ├── Authorization ✅
    │       ├── ReadingType ✅
    │       ├── IntervalBlock ✅
    │       ├── MeterReading ✅
    │       ├── UsagePoint ✅
    │       ├── UsageSummary ✅
    │       ├── ElectricPowerQualitySummary ✅
    │       ├── TimeConfiguration ✅
    │       │
    │       ├── [ABSTRACT BASE CLASSES - customer.xsd]
    │       ├── Document (abstract) ⚠️ No entity
    │       ├── Asset (abstract) ⚠️ No entity
    │       ├── Location (abstract) ⚠️ No entity
    │       ├── OrganisationRole (abstract) ⚠️ No entity
    │       │
    │       ├── [CONCRETE ESPI RESOURCES - customer.xsd]
    │       ├── ProgramDateIdMappings ✅
    │       ├── Statement ✅
    │       ├── CustomerAccount ✅ (extends Document)
    │       ├── CustomerAgreement ✅ (extends Agreement → Document)
    │       ├── Customer ✅ (extends OrganisationRole)
    │       ├── ServiceSupplier ✅ (extends OrganisationRole)
    │       ├── ServiceLocation ✅ (extends WorkLocation → Location)
    │       ├── EndDevice ✅ (extends AssetContainer → Asset)
    │       └── Meter ✅ (extends EndDevice - uses end_device_related_links)
    │
    ├── [OBJECT-BASED (NO RELATED LINKS)]
    ├── IntervalReading ❌
    ├── ReadingQuality ❌
    ├── ServiceDeliveryPoint ❌
    ├── PnodeRef ❌
    ├── AggregateNodeRef ❌
    ├── LineItem ❌
    ├── StatementRef ❌
    └── [Embedded: DateTimeInterval, SummaryMeasurement, etc.]
```

### Correct Related Links Tables (15 existing + 3 missing = 18 total)

**ESPI Resources - espi.xsd (9 tables)**:
1. `application_information_related_links` ✅
2. `authorization_related_links` ✅
3. `reading_type_related_links` ✅
4. `interval_block_related_links` ✅
5. `meter_reading_related_links` ✅
6. `usage_point_related_links` ✅
7. `usage_summary_related_links` ✅
8. `electric_power_quality_summary_related_links` ✅
9. `time_configuration_related_links` ✅

**ESPI Resources - customer.xsd (6 existing + 3 missing = 9 total)**:
10. `program_date_id_mapping_related_links` ✅
11. `statement_related_links` ✅
12. `customer_agreement_related_links` ✅
13. `customer_related_links` ⚠️ **MISSING - needs creation**
14. `service_supplier_related_links` ✅
15. `service_location_related_links` ✅
16. `end_device_related_links` ✅
17. `meter_related_links` ⚠️ **MISSING - needs creation**
18. `customer_account_related_links` ⚠️ **MISSING - needs creation**

**Note**: RetailCustomer and Subscription extend IdentifiedObject but use direct foreign key references instead of Atom rel="related" links, so they do NOT require related_links tables.

### Incorrect Related Links Tables (To Remove: 11)

| ❌ Table | Entity | Reason |
|---------|--------|--------|
| `interval_reading_related_links` | IntervalReading | Extends Object, not IdentifiedObject |
| `reading_quality_related_links` | ReadingQuality | Extends Object, not IdentifiedObject |
| `service_delivery_point_related_links` | ServiceDeliveryPoint | Extends Object, not IdentifiedObject |
| `pnode_ref_related_links` | PnodeRef | Extends Object, not IdentifiedObject |
| `aggregated_node_ref_related_links` | AggregateNodeRef | Extends Object, not IdentifiedObject |
| `line_item_related_links` | LineItem | Extends Object, not IdentifiedObject |
| `statement_ref_related_links` | StatementRef | Extends Object, not IdentifiedObject |
| `phone_number_related_links` | PhoneNumber | Not in XSD (custom addition) |
| `batch_list_related_links` | BatchList | Element wrapper (BatchListType) |
| `retail_customer_related_links` | RetailCustomer | Uses direct FK references, not Atom rel="related" |
| `subscription_related_links` | Subscription | Uses direct FK references, not Atom rel="related" |

**Note**: RetailCustomer and Subscription extend IdentifiedObject but don't use Atom feed patterns - they use direct foreign key relationships for data marshalling.

**Remediation Plan**: See `SCHEMA_COMPLIANCE_REMEDIATION_PLAN.md`

---

## Migration Execution Order

```
V1__Create_Base_Tables.sql (all databases)
    ↓
[Vendor-specific V2 - ONE of the following:]
    → V2__MySQL_Specific_Tables.sql (MySQL)
    → V2__PostgreSQL_Specific_Tables.sql (PostgreSQL)
    → V2__H2_Specific_Tables.sql (H2)
    ↓
V3__Create_additiional_Base_Tables.sql (all databases)
```

**Critical**: V3 depends on both V1 AND V2 tables existing. The `usage_points` table from V2 is referenced by many V3 tables.

---

## Phase 1 TimeConfiguration Lesson Learned

**Issue**: Attempted to create `V4__Create_Time_Configurations.sql`

**Root Cause**: `time_configurations` table already exists in V2 (MySQL/PostgreSQL/H2)

**Resolution**: Deleted V4 migration - table already exists with correct schema

**Best Practice**: Always check V1, V2, and V3 migrations BEFORE creating new migrations for existing entities
