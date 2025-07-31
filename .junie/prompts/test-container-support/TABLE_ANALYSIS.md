# Database Table Analysis: Vendor-Neutral vs Vendor-Specific

## Overview
This document categorizes all 64 tables from the MySQL migration file into vendor-neutral and vendor-specific groups for the database migration refactoring.

**Total Tables**: 64
**Vendor-Neutral Tables**: 60 (can be included in base migration)
**Vendor-Specific Tables**: 4 (require separate vendor implementations)

---

## Vendor-Specific Tables (4 tables)
These tables contain BLOB columns or have foreign key relationships to tables with BLOB columns, requiring vendor-specific implementations.

### 1. time_configurations (Line 228)
- **Reason**: Contains BLOB columns (`dst_end_rule`, `dst_start_rule`)
- **BLOB Columns**: 2
- **Migration**: Requires vendor-specific V2 migration

### 2. time_configuration_related_links (Line 257)
- **Reason**: Foreign key relationship to `time_configurations` table
- **BLOB Columns**: 0 (but depends on vendor-specific table)
- **Migration**: Requires vendor-specific V2 migration

### 3. usage_points (Line 337)
- **Reason**: Contains BLOB column (`role_flags`)
- **BLOB Columns**: 1
- **Migration**: Requires vendor-specific V2 migration

### 4. usage_point_related_links (Line 410)
- **Reason**: Foreign key relationship to `usage_points` table
- **BLOB Columns**: 0 (but depends on vendor-specific table)
- **Migration**: Requires vendor-specific V2 migration

---

## Vendor-Neutral Tables (60 tables)
These tables can be included in the base migration as they don't contain vendor-specific column types.

### Core Application Tables
1. **identified_object_related_links** (Line 14)
2. **application_information** (Line 24)
3. **application_information_related_links** (Line 113)
4. **application_information_grant_types** (Line 122)
5. **application_information_scopes** (Line 131)

### Customer and Account Tables
6. **retail_customers** (Line 140)
7. **retail_customer_related_links** (Line 178)
8. **customers** (Line 1095)
9. **customer_related_links** (Line 1165)
10. **customer_accounts** (Line 1174)
11. **customer_account_related_links** (Line 1223)
12. **customer_account_notifications** (Line 1232)
13. **customer_agreements** (Line 1244)
14. **customer_agreement_related_links** (Line 1288)
15. **customer_agreement_future_status** (Line 1297)

### Service and Location Tables
16. **service_delivery_points** (Line 187)
17. **service_delivery_point_related_links** (Line 219)
18. **service_locations** (Line 1471)
19. **service_location_related_links** (Line 1532)
20. **service_suppliers** (Line 1541)
21. **service_supplier_related_links** (Line 1588)

### Authorization and Security Tables
22. **authorizations** (Line 266)
23. **authorization_related_links** (Line 328)

### Reading and Measurement Tables
24. **reading_types** (Line 419)
25. **reading_type_related_links** (Line 467)
26. **meter_readings** (Line 476)
27. **meter_reading_related_links** (Line 508)
28. **interval_blocks** (Line 517)
29. **interval_block_related_links** (Line 551)
30. **interval_readings** (Line 560)
31. **interval_reading_related_links** (Line 600)
32. **reading_qualities** (Line 609)
33. **reading_quality_related_links** (Line 642)

### Usage and Summary Tables
34. **usage_summaries** (Line 651)
35. **usage_summary_related_links** (Line 765)
36. **electric_power_quality_summaries** (Line 774)
37. **electric_power_quality_summary_related_links** (Line 823)

### Subscription Tables
38. **subscriptions** (Line 832)
39. **subscription_related_links** (Line 871)
40. **subscription_usage_points** (Line 880)

### Node Reference Tables
41. **pnode_refs** (Line 902)
42. **pnode_ref_related_links** (Line 939)
43. **aggregated_node_refs** (Line 948)
44. **aggregated_node_ref_related_links** (Line 988)

### Batch Processing Tables
45. **batch_lists** (Line 997)
46. **batch_list_related_links** (Line 1024)
47. **batch_list_resources** (Line 1033)

### Line Item and Statement Tables
48. **line_items** (Line 1043)
49. **line_item_related_links** (Line 1080)
50. **statements** (Line 1597)
51. **statement_related_links** (Line 1633)
52. **statement_refs** (Line 1642)
53. **statement_ref_related_links** (Line 1674)

### Device and Equipment Tables
54. **end_devices** (Line 1308)
55. **end_device_related_links** (Line 1364)
56. **meters** (Line 1373)

### Communication Tables
57. **phone_numbers** (Line 1385)
58. **phone_number_related_links** (Line 1424)

### Program and Mapping Tables
59. **program_date_id_mappings** (Line 1433)
60. **program_date_id_mapping_related_links** (Line 1462)

---

## Migration Strategy Summary

### Base Migration (V1__Create_Base_Tables.sql)
- **Tables**: 60 vendor-neutral tables
- **Location**: `openespi-common/src/main/resources/db/migration/`
- **Compatibility**: H2, MySQL, PostgreSQL

### Vendor-Specific Migrations (V2__*_Specific_Tables.sql)
- **Tables**: 4 vendor-specific tables
- **Locations**: 
  - `openespi-common/src/main/resources/db/vendor/h2/`
  - `openespi-common/src/main/resources/db/vendor/mysql/`
  - `openespi-common/src/main/resources/db/vendor/postgres/`
- **Column Type Variations**: BINARY (H2), BLOB (MySQL), BYTEA (PostgreSQL)

### Flyway Configuration
- **Base path**: `classpath:db/migration`
- **Vendor-specific paths**: `classpath:db/vendor/{vendor}`
- **Execution order**: V1 (base) → V2 (vendor-specific)

---

## Validation Checklist
- [x] All 64 tables categorized
- [x] BLOB column dependencies identified
- [x] Foreign key relationships analyzed
- [x] Migration strategy defined
- [x] File structure planned