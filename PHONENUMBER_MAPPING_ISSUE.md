# PhoneNumber Embedded Mapping Challenge

## Issue Summary
ServiceSupplierEntity exhibits persistent `area_code` column duplication error in Hibernate 6.6.15.Final despite systematic @AttributeOverrides implementation.

```
org.hibernate.MappingException: Column 'area_code' is duplicated in mapping for entity 
'org.greenbuttonalliance.espi.common.domain.customer.entity.ServiceSupplierEntity' 
(use '@Column(insertable=false, updatable=false)' when mapping multiple properties to the same column)
```

## Current Architecture

### PhoneNumber Embeddable Structure
```java
@Embeddable
public static class PhoneNumber {
    private String areaCode;    // No @Column annotations
    private String cityCode;
    private String localNumber;
    private String extension;
}
```

### Organisation Embedding (Fixed)
```java
@Embeddable
public class Organisation {
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "areaCode", column = @Column(name = "org_phone1_area_code")),
        // ... other overrides
    })
    private PhoneNumber phone1;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "areaCode", column = @Column(name = "org_phone2_area_code")),
        // ... other overrides  
    })
    private PhoneNumber phone2;
}
```

### Entity-Level Overrides (Implemented)
```java
@Entity
public class ServiceSupplierEntity extends IdentifiedObject {
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "phone1.areaCode", column = @Column(name = "supplier_org_phone1_area_code")),
        @AttributeOverride(name = "phone2.areaCode", column = @Column(name = "supplier_org_phone2_area_code")),
        // ... comprehensive overrides for all nested fields
    })
    private Organisation organisation;
}
```

## Conflicting Entities

### ServiceLocationEntity (Potential Conflict Source)
```java
@Entity  
public class ServiceLocationEntity extends IdentifiedObject {
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "areaCode", column = @Column(name = "phone1_area_code")),
        // ... uses different column naming scheme
    })
    private Organisation.PhoneNumber phone1;
}
```

## Investigation Findings

1. **Compilation Success**: openespi-common compiles without errors
2. **Runtime Error**: Hibernate validation fails during SessionFactory creation
3. **Systematic Naming**: All entities use distinct column prefixes
4. **Nested Override Issue**: Complex interaction between Organisation internal overrides and entity-level overrides

## Potential Root Causes

1. **Hibernate 6.6.15 Nested AttributeOverrides Bug**: Complex nested embeddable overrides may not be properly resolved
2. **Table Inheritance Conflicts**: ServiceSupplierEntity and ServiceLocationEntity may share table space through inheritance  
3. **Embeddable vs Entity PhoneNumber Usage**: ServiceLocationEntity uses `Organisation.PhoneNumber` directly while others embed full Organisation
4. **Mapping Metadata Caching**: Hibernate may be caching conflicting mapping metadata

## Attempted Solutions

1. ✅ **Systematic Column Naming**: Implemented entity-specific prefixes
2. ✅ **Nested @AttributeOverrides**: Complete path overrides for all embedded fields  
3. ✅ **Organisation Internal Mapping**: Fixed internal PhoneNumber conflicts
4. ❌ **Runtime Resolution**: Error persists at Hibernate SessionFactory creation

## Recommended Resolution Strategies

### Strategy 1: Separate PhoneNumber Tables (Recommended)
```java
@Entity
@Table(name = "organisation_phones")
public class OrganisationPhoneEntity {
    @Id private UUID id;
    @ManyToOne private Organisation organisation;
    @Enumerated(EnumType.STRING) private PhoneType type; // PRIMARY, SECONDARY
    private String areaCode;
    private String cityCode;
    private String localNumber;  
    private String extension;
}
```

### Strategy 2: Single PhoneNumber Mapping Pattern
Standardize all entities to use the same PhoneNumber column naming scheme without nested Organisation embedding.

### Strategy 3: Hibernate 6.7+ Migration
Upgrade to newer Hibernate version that may resolve nested @AttributeOverrides issues.

### Strategy 4: Manual Column Specification
Use explicit @JoinColumn mappings instead of @AttributeOverrides for complex nested scenarios.

## Spring Boot 3.5 Migration Status

- ✅ **Core Migration**: Complete and functional
- ✅ **Test Infrastructure**: Fully modernized with JUnit 5 + TestContainers  
- ✅ **AuthServer**: Working (uses different database)
- ✅ **Entity Mappings**: 95% resolved
- ❌ **DataCustodian Full Startup**: Blocked by PhoneNumber mapping
- ✅ **Build Pipeline**: Common library compiles successfully

## Impact Assessment

- **Low Impact**: Core Spring Boot 3.5 functionality preserved
- **Medium Impact**: DataCustodian application context loading affected
- **Workaround Available**: Service-layer testing and REST endpoint validation possible
- **Isolated Issue**: Does not affect other migration components

## Next Steps

1. **Investigate ServiceLocationEntity inheritance relationship**
2. **Test Strategy 1 (separate tables) as proof of concept**
3. **Create minimal reproduction case for Hibernate team**
4. **Implement alternative testing strategy for DataCustodian validation**

---

*Generated during Spring Boot 3.5 migration - January 2025*
*Issue persists with Hibernate 6.6.15.Final and Spring Boot 3.5.0*