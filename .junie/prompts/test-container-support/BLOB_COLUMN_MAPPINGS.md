# BLOB Column Mappings for Vendor-Specific Database Implementations

## Overview
This document outlines the vendor-specific column type mappings for BLOB columns that require different implementations across H2, MySQL, and PostgreSQL databases.

## Problematic Tables and Columns

### 1. time_configurations Table
**Location in MySQL migration**: Lines 228-254

**BLOB Columns:**
- `dst_end_rule` (line 246)
- `dst_start_rule` (line 248)

**Vendor-Specific Mappings:**
- **H2**: `BINARY` type
- **MySQL**: `BLOB` type (current implementation)
- **PostgreSQL**: `BYTEA` type

### 2. usage_points Table
**Location in MySQL migration**: Lines 337-407

**BLOB Columns:**
- `role_flags` (line 355)

**Vendor-Specific Mappings:**
- **H2**: `BINARY` type
- **MySQL**: `BLOB` type (current implementation)
- **PostgreSQL**: `BYTEA` type

## Related Tables (Also Need Vendor-Specific Implementation)

### 3. time_configuration_related_links Table
**Location in MySQL migration**: Lines 257-263
**Reason**: Foreign key relationship to time_configurations table

### 4. usage_point_related_links Table
**Location in MySQL migration**: Lines 410-416
**Reason**: Foreign key relationship to usage_points table

## JPA Entity Mappings
These BLOB columns map to the following JPA entity properties:

### TimeConfigurationEntity
- `byte[] dstEndRule` → `dst_end_rule` column
- `byte[] dstStartRule` → `dst_start_rule` column

### UsagePointEntity
- `byte[] roleFlags` → `role_flags` column

## Migration Strategy
1. **Base Migration**: Include all 60 other tables (vendor-neutral)
2. **Vendor-Specific Migrations**: Create separate V2 migrations for each database vendor containing only the 4 problematic tables
3. **Flyway Configuration**: Configure different migration paths per database vendor

## Database Vendor Compatibility Matrix

| Column | H2 | MySQL | PostgreSQL |
|--------|----|----|------------|
| `time_configurations.dst_end_rule` | BINARY | BLOB | BYTEA |
| `time_configurations.dst_start_rule` | BINARY | BLOB | BYTEA |
| `usage_points.role_flags` | BINARY | BLOB | BYTEA |

## Implementation Files Structure
```
openespi-common/src/main/resources/db/
├── migration/                           # Base migrations (vendor-neutral)
│   └── V1__Create_Base_Tables.sql      # 60 tables without BLOB issues
└── vendor/                             # Vendor-specific migrations
    ├── h2/
    │   └── V2__H2_Specific_Tables.sql  # 4 tables with BINARY columns
    ├── mysql/
    │   └── V2__MySQL_Specific_Tables.sql # 4 tables with BLOB columns
    └── postgres/
        └── V2__PostgreSQL_Specific_Tables.sql # 4 tables with BYTEA columns
```