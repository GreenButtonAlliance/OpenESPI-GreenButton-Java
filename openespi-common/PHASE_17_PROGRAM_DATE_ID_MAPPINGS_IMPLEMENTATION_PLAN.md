# Phase 17: ProgramDateIdMappings - ESPI 4.0 Schema Compliance

## Overview
Implement full ESPI 4.0 schema compliance for ProgramDateIdMappings, including creating the nested ProgramDateIdMapping embeddable class and ProgramDateKind enum. This phase includes cleanup of the existing DTO which incorrectly includes IdentifiedObject fields directly in the DTO instead of relying on AtomEntryDto.

**CRITICAL PREREQUISITE**: Enable bidirectional Atom links infrastructure for CustomerAgreement, ProgramDateIdMappings, ServiceLocation, ServiceSupplier, EndDevice, and Meter entities to support ESPI 4.0 `<link rel="related">` requirements.

**Related Issue**: #28 - ESPI 4.0 Schema Compliance (Phase 17: ProgramDateIdMappings)

**IMPORTANT**: Issue #28 tracks the multi-phase ESPI 4.0 schema compliance effort. This plan implements Phase 17 only. **DO NOT close Issue #28** when Phase 17 is complete - additional phases (Phase 18+) remain to be implemented.

**Current State Issues**:
- ProgramDateIdMappingsEntity exists but has NO fields (only extends IdentifiedObject)
- ProgramDateIdMappingsDto has IdentifiedObject fields (id, uuid, published, updated, links) mixed into the DTO
- ProgramDateIdMapping embeddable class does NOT exist
- ProgramDateKind enum does NOT exist
- No mapper, repository, or service implementations
- Database table exists but doesn't match XSD structure
- **CRITICAL**: Related links tables exist in database but JPA @ElementCollection mappings are MISSING from all 6 entities (CustomerAgreement, ProgramDateIdMappings, ServiceLocation, ServiceSupplier, EndDevice, Meter)

**XSD Compliance Requirements**:
- ProgramDateIdMappings extends IdentifiedObject with ONE field: programDateIdMapping
- ProgramDateIdMapping extends Object (NOT IdentifiedObject) with 4 fields
- ProgramDateKind enum has 4 values defined in customer.xsd
- DTO must follow Phase 21 pattern: NO IdentifiedObject fields (handled by AtomEntryDto)
- Support Atom `<link rel="related">` for bidirectional relationships with CustomerAgreement
- Service layer queries ONLY by ID (no timestamp or other field queries)

## XSD Structure

**ProgramDateIdMappings extends IdentifiedObject** (customer.xsd lines 269-283):

### ProgramDateIdMappings Fields (1 field)
1. **programDateIdMapping** (ProgramDateIdMapping, optional) - Single customer energy efficiency program date mapping

### ProgramDateIdMapping Structure (customer.xsd lines 1223-1251)
ProgramDateIdMapping extends **Object** (NOT IdentifiedObject) - 4 fields:
- **programDateType** (ProgramDateKind enum) - Type of customer energy efficiency program date
- **code** (String64) - Code value (may be alphanumeric)
- **name** (String256) - Name associated with code
- **note** (String256, optional) - Optional description of code

### ProgramDateKind Enum (customer.xsd lines 1997-2030)
Per XSD, this enum has 4 values:
```xml
<xs:enumeration value="CUST_DR_PROGRAM_ENROLLMENT_DATE"/>           <!-- 0 -->
<xs:enumeration value="CUST_DR_PROGRAM_DE_ENROLLMENT_DATE"/>        <!-- 1 -->
<xs:enumeration value="CUST_DR_PROGRAM_TERM_DATE_REGARDLESS_FINANCIAL"/>  <!-- 2 -->
<xs:enumeration value="CUST_DR_PROGRAM_TERM_DATE_WITHOUT_FINANCIAL"/>     <!-- 3 -->
```

**Note**: The XSD uses `xs:union memberTypes="String64"` which means the enum can also accept custom string values not in the enumeration list.

### Architecture Decision
- **ProgramDateIdMappingsEntity**: Extends IdentifiedObject, has ONE field (programDateIdMapping) + relatedLinks collection
- **ProgramDateIdMapping**: @Embeddable class (extends Object conceptually, not IdentifiedObject)
- **ProgramDateIdMappingsDto**: JAXB DTO with NO IdentifiedObject fields (uses AtomEntryDto pattern from Phase 21)
- **ProgramDateIdMappingDto**: Nested DTO for the embedded object
- **ProgramDateKind**: Java enum with 4 values
- **Related Links**: Support Atom `<link rel="related">` bidirectional references (NAESB ESPI 4.0 standard)
- **Service Queries**: ID-based only (findById, findAll, deleteById)

## Tasks

### Phase A0: Enable Bidirectional Atom Links (PREREQUISITE)

**Context**: The NAESB ESPI 4.0 standard defines bidirectional relationships between entities using Atom `<link rel="related">` elements. While the database tables exist, the JPA entity mappings are missing. This includes relationships such as ServiceLocation ↔ EndDevice, ServiceLocation ↔ Meter, CustomerAgreement ↔ ProgramDateIdMappings, and CustomerAgreement ↔ ServiceLocation/ServiceSupplier.

**Database Infrastructure** (already exists in V3 migration):
- `customer_agreement_related_links` table ✓
- `program_date_id_mapping_related_links` table ✓
- `service_location_related_links` table ✓
- `service_supplier_related_links` table ✓
- `end_device_related_links` table ✓
- `meter_related_links` table ✓

**Missing JPA Mappings** (must be added):
All six entities lack the `@ElementCollection` field for relatedLinks.

#### Task A0.1: Add relatedLinks to CustomerAgreementEntity
**File**: `src/main/java/org/greenbuttonalliance/espi/common/domain/customer/entity/CustomerAgreementEntity.java`

**Add Field** (after existing fields, before equals/hashCode methods):
```java
/**
 * Atom related links for bidirectional references.
 * Per NAESB ESPI 4.0 standard, CustomerAgreement can have related links to:
 * - ProgramDateIdMappings (Demand Response program enrollment information)
 * - ServiceLocation (where service is delivered)
 * - ServiceSupplier (who supplies the service)
 *
 * Stored as href strings that will be wrapped in <link rel="related"> elements
 * during XML serialization by the service layer.
 */
@ElementCollection
@CollectionTable(name = "customer_agreement_related_links",
                 joinColumns = @JoinColumn(name = "customer_agreement_id"))
@Column(name = "related_links", length = 1024)
private List<String> relatedLinks;
```

**Update toString() method**: Add `relatedLinks` to the toString output.

#### Task A0.2: Add relatedLinks to ProgramDateIdMappingsEntity
**File**: `src/main/java/org/greenbuttonalliance/espi/common/domain/customer/entity/ProgramDateIdMappingsEntity.java`

**Add Field** (after programDateIdMapping field):
```java
/**
 * Atom related links for bidirectional references.
 * Per NAESB ESPI 4.0 standard, ProgramDateIdMappings can have related links to:
 * - CustomerAgreement (the agreement this program enrollment relates to)
 *
 * Stored as href strings that will be wrapped in <link rel="related"> elements
 * during XML serialization by the service layer.
 */
@ElementCollection
@CollectionTable(name = "program_date_id_mapping_related_links",
                 joinColumns = @JoinColumn(name = "program_date_id_mapping_id"))
@Column(name = "related_links", length = 1024)
private List<String> relatedLinks;
```

**Update toString() method**: Add `relatedLinks` to the toString output.

#### Task A0.3: Add relatedLinks to ServiceLocationEntity
**File**: `src/main/java/org/greenbuttonalliance/espi/common/domain/customer/entity/ServiceLocationEntity.java`

**Add Field** (after existing @ElementCollection for usagePointHrefs):
```java
/**
 * Atom related links for bidirectional references.
 * Per NAESB ESPI 4.0 standard, ServiceLocation can have related links to:
 * - CustomerAgreement (agreements for service at this location)
 * - UsagePoint (meters at this location - via usagePointHrefs already implemented)
 *
 * Stored as href strings that will be wrapped in <link rel="related"> elements
 * during XML serialization by the service layer.
 */
@ElementCollection
@CollectionTable(name = "service_location_related_links",
                 joinColumns = @JoinColumn(name = "service_location_id"))
@Column(name = "related_links", length = 1024)
private List<String> relatedLinks;
```

**Update toString() method**: Add `relatedLinks` to the toString output.

#### Task A0.4: Add relatedLinks to ServiceSupplierEntity
**File**: `src/main/java/org/greenbuttonalliance/espi/common/domain/customer/entity/ServiceSupplierEntity.java`

**Add Field** (after organisation field):
```java
/**
 * Atom related links for bidirectional references.
 * Per NAESB ESPI 4.0 standard, ServiceSupplier can have related links to:
 * - CustomerAgreement (agreements where this supplier provides service)
 * - Customer (customers this supplier serves)
 *
 * Stored as href strings that will be wrapped in <link rel="related"> elements
 * during XML serialization by the service layer.
 */
@ElementCollection
@CollectionTable(name = "service_supplier_related_links",
                 joinColumns = @JoinColumn(name = "service_supplier_id"))
@Column(name = "related_links", length = 1024)
private List<String> relatedLinks;
```

**Update toString() method**: Add `relatedLinks` to the toString output.

#### Task A0.5: Add relatedLinks to EndDeviceEntity
**File**: `src/main/java/org/greenbuttonalliance/espi/common/domain/customer/entity/EndDeviceEntity.java`

**Add Field** (after existing fields, before equals/hashCode methods):
```java
/**
 * Atom related links for bidirectional references.
 * Per NAESB ESPI 4.0 standard, EndDevice can have related links to:
 * - ServiceLocation (where this end device is located)
 * - UsagePoint (usage data associated with this end device)
 *
 * Stored as href strings that will be wrapped in <link rel="related"> elements
 * during XML serialization by the service layer.
 */
@ElementCollection
@CollectionTable(name = "end_device_related_links",
                 joinColumns = @JoinColumn(name = "end_device_id"))
@Column(name = "related_links", length = 1024)
private List<String> relatedLinks;
```

**Update toString() method**: Add `relatedLinks` to the toString output (in EndDeviceEntity, not in subclasses).

**Note**: MeterEntity extends EndDeviceEntity, so it will inherit this relatedLinks field automatically.

#### Task A0.6: Verify Meter inherits relatedLinks from EndDevice
**File**: `src/main/java/org/greenbuttonalliance/espi/common/domain/customer/entity/MeterEntity.java`

**Context**: MeterEntity extends EndDeviceEntity using `@Inheritance(strategy = InheritanceType.JOINED)`. The relatedLinks field will be inherited from EndDeviceEntity.

**Verification**:
- [ ] Confirm MeterEntity extends EndDeviceEntity
- [ ] Confirm EndDeviceEntity has @Inheritance(strategy = InheritanceType.JOINED)
- [ ] Confirm meter_related_links table exists in V3 migration (line 575)
- [ ] NO changes needed to MeterEntity (inherits relatedLinks from parent)

**Benefits**:
- ✅ Meter automatically gets relatedLinks via inheritance
- ✅ Consistent with JPA inheritance pattern
- ✅ Database table meter_related_links already exists

#### Task A0.7: Verification
**Verification Steps**:
- [ ] All 6 entities have @ElementCollection for relatedLinks (5 explicit + 1 inherited):
  - [ ] CustomerAgreementEntity (explicit)
  - [ ] ProgramDateIdMappingsEntity (explicit)
  - [ ] ServiceLocationEntity (explicit)
  - [ ] ServiceSupplierEntity (explicit)
  - [ ] EndDeviceEntity (explicit)
  - [ ] MeterEntity (inherited from EndDeviceEntity)
- [ ] All entities use correct table names (matching V3 migration)
- [ ] All entities have updated toString() methods
- [ ] Build succeeds: `mvn clean compile`
- [ ] No JPA mapping errors in logs

**Benefits**:
- ✅ Enables ESPI 4.0 compliant bidirectional Atom links across all customer entities
- ✅ Supports ServiceLocation ↔ EndDevice bidirectional relationships
- ✅ Supports ServiceLocation ↔ Meter bidirectional relationships
- ✅ Supports CustomerAgreement ↔ ProgramDateIdMappings/ServiceLocation/ServiceSupplier bidirectional relationships
- ✅ Service layer can populate `<link rel="related">` elements in XML
- ✅ Consistent pattern across all customer domain entities
- ✅ Uses existing database infrastructure (no migration changes needed)

---

### Phase A: Enum and Embeddable Creation

#### Task A1: Create ProgramDateKind Enum
**File**: `src/main/java/org/greenbuttonalliance/espi/common/domain/customer/enums/ProgramDateKind.java`

**Requirements**:
- Create new enum with 4 values from XSD
- Add JavaDoc for each value
- Use descriptive names matching XSD

**Implementation**:
```java
package org.greenbuttonalliance.espi.common.domain.customer.enums;

/**
 * Type of Demand Response program date based on ESPI 4.0 customer.xsd specification.
 *
 * Per customer.xsd lines 1997-2030.
 * Note: XSD uses union type, allowing both enumerated values and custom String64 values.
 *
 * Ordinal mapping:
 * 0 = CUST_DR_PROGRAM_ENROLLMENT_DATE
 * 1 = CUST_DR_PROGRAM_DE_ENROLLMENT_DATE
 * 2 = CUST_DR_PROGRAM_TERM_DATE_REGARDLESS_FINANCIAL
 * 3 = CUST_DR_PROGRAM_TERM_DATE_WITHOUT_FINANCIAL
 */
public enum ProgramDateKind {
    /**
     * Date customer enrolled in Demand Response program.
     * Ordinal: 0
     */
    CUST_DR_PROGRAM_ENROLLMENT_DATE,

    /**
     * Date customer terminated enrollment in Demand Response program.
     * Ordinal: 1
     */
    CUST_DR_PROGRAM_DE_ENROLLMENT_DATE,

    /**
     * Earliest date customer can terminate Demand Response enrollment, regardless of financial impact.
     * Ordinal: 2
     */
    CUST_DR_PROGRAM_TERM_DATE_REGARDLESS_FINANCIAL,

    /**
     * Earliest date customer can terminate Demand Response enrollment, without financial impact.
     * Ordinal: 3
     */
    CUST_DR_PROGRAM_TERM_DATE_WITHOUT_FINANCIAL
}
```

**Verification Checklist**:
- ✅ Location: `customer/enums` directory
- ✅ Sequence: Matches XSD exactly (ordinals 0-3)
- ✅ Values: Four enum constants matching XSD enumeration values
- ✅ JavaDoc: Comprehensive documentation for each value

#### Task A2: Create ProgramDateIdMapping Embeddable Class
**File**: `src/main/java/org/greenbuttonalliance/espi/common/domain/customer/common/ProgramDateIdMapping.java`

**Requirements**:
- @Embeddable annotation (not @Entity)
- Implements Serializable
- 4 fields matching XSD
- Column definitions with appropriate lengths
- Proper JavaDoc

**Implementation**:
```java
package org.greenbuttonalliance.espi.common.domain.customer.common;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.greenbuttonalliance.espi.common.domain.customer.enums.ProgramDateKind;

import java.io.Serializable;

/**
 * Embeddable class for single customer energy efficiency program date mapping.
 *
 * Per customer.xsd lines 1223-1251, ProgramDateIdMapping extends Object (NOT IdentifiedObject).
 * This is an embedded component, not a standalone entity.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProgramDateIdMapping implements Serializable {

    /**
     * Type of customer energy efficiency program date.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "program_date_type", length = 64)
    private ProgramDateKind programDateType;

    /**
     * Code value (may be alphanumeric).
     */
    @Column(name = "code", length = 64)
    private String code;

    /**
     * Name associated with code.
     */
    @Column(name = "name", length = 256)
    private String name;

    /**
     * Optional description of code.
     */
    @Column(name = "note", length = 256)
    private String note;

    @Override
    public String toString() {
        return "ProgramDateIdMapping{" +
                "programDateType=" + programDateType +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", note='" + note + '\'' +
                '}';
    }
}
```

### Phase B: Entity Updates

#### Task B1: Update ProgramDateIdMappingsEntity
**File**: `src/main/java/org/greenbuttonalliance/espi/common/domain/customer/entity/ProgramDateIdMappingsEntity.java`

**Current State**:
- Extends IdentifiedObject
- Has NO fields (completely empty)

**Changes Required**:
1. Add programDateIdMapping embedded field
2. Add relatedLinks collection (from Phase A0)
3. Update JavaDoc to reflect XSD structure
4. Add toString() method

**Updated Implementation**:
```java
package org.greenbuttonalliance.espi.common.domain.customer.entity;

import lombok.*;
import org.greenbuttonalliance.espi.common.domain.common.IdentifiedObject;
import org.greenbuttonalliance.espi.common.domain.customer.common.ProgramDateIdMapping;

import jakarta.persistence.*;
import java.util.List;

/**
 * Pure JPA/Hibernate entity for ProgramDateIdMappings without JAXB concerns.
 *
 * [extension] Collection of all customer Energy Efficiency programs.
 * Per customer.xsd lines 269-283, extends IdentifiedObject with one optional field.
 */
@Entity
@Table(name = "program_date_id_mappings")
@Getter
@Setter
@NoArgsConstructor
public class ProgramDateIdMappingsEntity extends IdentifiedObject {

    /**
     * [extension] Program date description.
     * Optional single customer energy efficiency program date mapping.
     */
    @Embedded
    private ProgramDateIdMapping programDateIdMapping;

    /**
     * Atom related links for bidirectional references.
     * Per NAESB ESPI 4.0 standard, ProgramDateIdMappings can have related links to:
     * - CustomerAgreement (the agreement this program enrollment relates to)
     *
     * Stored as href strings that will be wrapped in <link rel="related"> elements
     * during XML serialization by the service layer.
     */
    @ElementCollection
    @CollectionTable(name = "program_date_id_mapping_related_links",
                     joinColumns = @JoinColumn(name = "program_date_id_mapping_id"))
    @Column(name = "related_links", length = 1024)
    private List<String> relatedLinks;

    @Override
    public String toString() {
        return "ProgramDateIdMappingsEntity{" +
                "id=" + getId() +
                ", programDateIdMapping=" + programDateIdMapping +
                ", relatedLinks=" + relatedLinks +
                '}';
    }
}
```

### Phase C: DTO Implementation

#### Task C1: Create ProgramDateIdMappingDto
**File**: `src/main/java/org/greenbuttonalliance/espi/common/dto/customer/ProgramDateIdMappingDto.java`

**Requirements**:
- Use JAXB annotations for XML marshalling
- Include ONLY 4 fields from XSD
- NO IdentifiedObject fields
- Namespace: `http://naesb.org/espi/customer`

**Implementation**:
```java
package org.greenbuttonalliance.espi.common.dto.customer;

import jakarta.xml.bind.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.greenbuttonalliance.espi.common.domain.customer.enums.ProgramDateKind;

/**
 * ProgramDateIdMapping DTO class for JAXB XML marshalling/unmarshalling.
 *
 * Represents a single customer energy efficiency program date mapping.
 * Per customer.xsd lines 1223-1251, extends Object (NOT IdentifiedObject).
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ProgramDateIdMapping", namespace = "http://naesb.org/espi/customer", propOrder = {
    "programDateType", "code", "name", "note"
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProgramDateIdMappingDto {

    /**
     * Type of customer energy efficiency program date.
     */
    @XmlElement(name = "programDateType", namespace = "http://naesb.org/espi/customer")
    private ProgramDateKind programDateType;

    /**
     * Code value (may be alphanumeric).
     */
    @XmlElement(name = "code", namespace = "http://naesb.org/espi/customer", required = true)
    private String code;

    /**
     * Name associated with code.
     */
    @XmlElement(name = "name", namespace = "http://naesb.org/espi/customer", required = true)
    private String name;

    /**
     * Optional description of code.
     */
    @XmlElement(name = "note", namespace = "http://naesb.org/espi/customer")
    private String note;
}
```

#### Task C2: Update ProgramDateIdMappingsDto
**File**: `src/main/java/org/greenbuttonalliance/espi/common/dto/customer/ProgramDateIdMappingsDto.java`

**Current State**:
- Has IdentifiedObject fields (id, uuid, published, updated, selfLink, upLink, relatedLinks)
- Has extra fields (programId, programDate, mappingId, mappingType, isActive, customer)
- NOT XSD-compliant

**Changes Required**:
1. **REMOVE** all IdentifiedObject fields (id, uuid, published, updated, selfLink, upLink, relatedLinks)
2. **REMOVE** all non-XSD fields (programId, programDate, mappingId, mappingType, isActive, customer)
3. **ADD** ONLY the programDateIdMapping field from XSD
4. Update propOrder to match XSD element sequence
5. Update JavaDoc

**Corrected Implementation**:
```java
package org.greenbuttonalliance.espi.common.dto.customer;

import jakarta.xml.bind.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ProgramDateIdMappings DTO class for JAXB XML marshalling/unmarshalling.
 *
 * [extension] Collection of all customer Energy Efficiency programs.
 * Per customer.xsd lines 269-283, extends IdentifiedObject with one optional field.
 *
 * IMPORTANT: IdentifiedObject fields (id, published, updated, links) are handled by
 * AtomEntryDto wrapper, NOT included in this resource DTO. This follows the ESPI 4.0
 * pattern where Atom protocol metadata is separate from resource data.
 */
@XmlRootElement(name = "ProgramDateIdMappings", namespace = "http://naesb.org/espi/customer")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ProgramDateIdMappings", namespace = "http://naesb.org/espi/customer", propOrder = {
    "programDateIdMapping"
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProgramDateIdMappingsDto {

    /**
     * [extension] Program date description.
     * Optional single customer energy efficiency program date mapping.
     */
    @XmlElement(name = "programDateIdMapping", namespace = "http://naesb.org/espi/customer")
    private ProgramDateIdMappingDto programDateIdMapping;
}
```

### Phase D: Mapper Implementation

#### Task D1: Create ProgramDateIdMappingMapper
**File**: `src/main/java/org/greenbuttonalliance/espi/common/mapper/customer/ProgramDateIdMappingMapper.java`

**Requirements**:
- MapStruct interface
- Maps 4 fields between entity and DTO
- No IdentifiedObject fields (ProgramDateIdMapping extends Object, not IdentifiedObject)

**Implementation**:
```java
package org.greenbuttonalliance.espi.common.mapper.customer;

import org.greenbuttonalliance.espi.common.domain.customer.common.ProgramDateIdMapping;
import org.greenbuttonalliance.espi.common.dto.customer.ProgramDateIdMappingDto;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper for converting between ProgramDateIdMapping and ProgramDateIdMappingDto.
 *
 * Maps the 4 fields of ProgramDateIdMapping embeddable component.
 * This is NOT an IdentifiedObject, so there are no id/link fields to ignore.
 */
@Mapper(componentModel = "spring")
public interface ProgramDateIdMappingMapper {

    /**
     * Converts a ProgramDateIdMapping embeddable to a ProgramDateIdMappingDto.
     *
     * @param mapping the program date ID mapping embeddable
     * @return the program date ID mapping DTO
     */
    ProgramDateIdMappingDto toDto(ProgramDateIdMapping mapping);

    /**
     * Converts a ProgramDateIdMappingDto to a ProgramDateIdMapping embeddable.
     *
     * @param dto the program date ID mapping DTO
     * @return the program date ID mapping embeddable
     */
    ProgramDateIdMapping toEmbeddable(ProgramDateIdMappingDto dto);
}
```

#### Task D2: Create ProgramDateIdMappingsMapper
**File**: `src/main/java/org/greenbuttonalliance/espi/common/mapper/customer/ProgramDateIdMappingsMapper.java`

**Requirements**:
- MapStruct interface
- Maps ONLY 1 XSD field (programDateIdMapping)
- NO Atom field mappings (id, links, timestamps handled by service layer via AtomEntryDto)
- Uses ProgramDateIdMappingMapper for the nested object
- Does NOT map relatedLinks (managed by service layer)

**Implementation**:
```java
package org.greenbuttonalliance.espi.common.mapper.customer;

import org.greenbuttonalliance.espi.common.domain.customer.entity.ProgramDateIdMappingsEntity;
import org.greenbuttonalliance.espi.common.dto.customer.ProgramDateIdMappingsDto;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper for converting between ProgramDateIdMappingsEntity and ProgramDateIdMappingsDto.
 *
 * Maps only the programDateIdMapping field from customer.xsd.
 * IdentifiedObject fields (id, links, timestamps) and relatedLinks are NOT part of the DTO
 * and are handled separately by the service layer via AtomEntryDto.
 *
 * Handles the conversion between the JPA entity used for persistence and the DTO
 * used for JAXB XML marshalling in the Green Button API.
 */
@Mapper(componentModel = "spring", uses = {
    ProgramDateIdMappingMapper.class
})
public interface ProgramDateIdMappingsMapper {

    /**
     * Converts a ProgramDateIdMappingsEntity to a ProgramDateIdMappingsDto.
     * Maps only the programDateIdMapping field.
     *
     * @param entity the program date ID mappings entity
     * @return the program date ID mappings DTO
     */
    ProgramDateIdMappingsDto toDto(ProgramDateIdMappingsEntity entity);

    /**
     * Converts a ProgramDateIdMappingsDto to a ProgramDateIdMappingsEntity.
     * Maps only the programDateIdMapping field.
     *
     * @param dto the program date ID mappings DTO
     * @return the program date ID mappings entity (IdentifiedObject fields will be null)
     */
    ProgramDateIdMappingsEntity toEntity(ProgramDateIdMappingsDto dto);

    /**
     * Updates an existing ProgramDateIdMappingsEntity with data from a ProgramDateIdMappingsDto.
     * Updates only the programDateIdMapping field.
     *
     * @param dto the program date ID mappings DTO with updated data
     * @param entity the existing program date ID mappings entity to update
     */
    void updateEntityFromDto(ProgramDateIdMappingsDto dto, @MappingTarget ProgramDateIdMappingsEntity entity);
}
```

**Note**: The mapper does NOT need explicit `@Mapping(target = "...", ignore = true)` annotations because MapStruct automatically only maps fields that exist in both the source and target. Since ProgramDateIdMappingsDto has NO IdentifiedObject fields or relatedLinks, MapStruct won't try to map them.

### Phase E: Repository Implementation

#### Task E1: Create ProgramDateIdMappingsRepository
**File**: `src/main/java/org/greenbuttonalliance/espi/common/repositories/customer/ProgramDateIdMappingsRepository.java`

**Requirements**:
- Extends JpaRepository
- NO custom query methods (only JpaRepository defaults: findById, findAll, save, deleteById, count)
- Following Phase 17 guidance: Service layer queries ONLY by ID

**Implementation**:
```java
package org.greenbuttonalliance.espi.common.repositories.customer;

import org.greenbuttonalliance.espi.common.domain.customer.entity.ProgramDateIdMappingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository interface for ProgramDateIdMappingsEntity.
 *
 * Provides standard CRUD operations for program date ID mappings.
 * Following Phase 17 guidance: Service layer queries ONLY by ID.
 * All query methods are provided by JpaRepository.
 */
@Repository
public interface ProgramDateIdMappingsRepository extends JpaRepository<ProgramDateIdMappingsEntity, UUID> {
    // No custom methods - only JpaRepository defaults:
    // - Optional<T> findById(ID id)
    // - List<T> findAll()
    // - <S extends T> S save(S entity)
    // - void deleteById(ID id)
    // - long count()
}
```

### Phase F: Service Implementation

#### Task F1: Create ProgramDateIdMappingsService Interface
**File**: `src/main/java/org/greenbuttonalliance/espi/common/service/customer/ProgramDateIdMappingsService.java`

**Requirements**:
- Standard CRUD operations (ID-based only)
- No timestamp-based or field-based queries

**Implementation**:
```java
package org.greenbuttonalliance.espi.common.service.customer;

import org.greenbuttonalliance.espi.common.domain.customer.entity.ProgramDateIdMappingsEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for ProgramDateIdMappings operations.
 *
 * Provides business logic for managing program date ID mappings.
 * Following Phase 17 guidance: Queries ONLY by ID.
 */
public interface ProgramDateIdMappingsService {

    /**
     * Create a new program date ID mappings.
     *
     * @param entity the program date ID mappings entity to create
     * @return the created entity with generated ID
     */
    ProgramDateIdMappingsEntity create(ProgramDateIdMappingsEntity entity);

    /**
     * Find a program date ID mappings by ID.
     *
     * @param id the program date ID mappings ID
     * @return optional containing the entity if found
     */
    Optional<ProgramDateIdMappingsEntity> findById(UUID id);

    /**
     * Find all program date ID mappings.
     *
     * @return list of all program date ID mappings entities
     */
    List<ProgramDateIdMappingsEntity> findAll();

    /**
     * Update an existing program date ID mappings.
     *
     * @param entity the program date ID mappings entity with updated data
     * @return the updated entity
     */
    ProgramDateIdMappingsEntity update(ProgramDateIdMappingsEntity entity);

    /**
     * Delete a program date ID mappings by ID.
     *
     * @param id the program date ID mappings ID to delete
     */
    void deleteById(UUID id);

    /**
     * Count all program date ID mappings.
     *
     * @return total count of program date ID mappings
     */
    long count();
}
```

#### Task F2: Create ProgramDateIdMappingsServiceImpl
**File**: `src/main/java/org/greenbuttonalliance/espi/common/service/customer/impl/ProgramDateIdMappingsServiceImpl.java`

**Requirements**:
- Implements ProgramDateIdMappingsService
- Delegates to repository
- ID-based operations only

**Implementation**:
```java
package org.greenbuttonalliance.espi.common.service.customer.impl;

import lombok.RequiredArgsConstructor;
import org.greenbuttonalliance.espi.common.domain.customer.entity.ProgramDateIdMappingsEntity;
import org.greenbuttonalliance.espi.common.repositories.customer.ProgramDateIdMappingsRepository;
import org.greenbuttonalliance.espi.common.service.customer.ProgramDateIdMappingsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service implementation for ProgramDateIdMappings operations.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ProgramDateIdMappingsServiceImpl implements ProgramDateIdMappingsService {

    private final ProgramDateIdMappingsRepository repository;

    @Override
    public ProgramDateIdMappingsEntity create(ProgramDateIdMappingsEntity entity) {
        return repository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProgramDateIdMappingsEntity> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProgramDateIdMappingsEntity> findAll() {
        return repository.findAll();
    }

    @Override
    public ProgramDateIdMappingsEntity update(ProgramDateIdMappingsEntity entity) {
        return repository.save(entity);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return repository.count();
    }
}
```

### Phase G: Database Migration

#### Task G1: Update V3 Flyway Migration Script
**File**: `src/main/resources/db/migration/V3__Create_additiional_Base_Tables.sql`

**Current State**:
```sql
CREATE TABLE program_date_id_mappings
(
    id             CHAR(36) PRIMARY KEY,
    description    VARCHAR(255),
    created        TIMESTAMP NOT NULL,
    updated        TIMESTAMP NOT NULL,
    published      TIMESTAMP,
    up_link_rel    VARCHAR(255),
    up_link_href   VARCHAR(1024),
    up_link_type   VARCHAR(255),
    self_link_rel  VARCHAR(255),
    self_link_href VARCHAR(1024),
    self_link_type VARCHAR(255),

    -- Program date ID mapping specific fields
    program_date   BIGINT,
    program_id     VARCHAR(100)
);
```

**Issues**:
- Has fields `program_date` and `program_id` that don't exist in XSD
- Missing embedded ProgramDateIdMapping fields (program_date_type, code, name, note)

**Changes Required**:
1. **REMOVE** non-XSD fields: program_date, program_id
2. **ADD** embedded ProgramDateIdMapping fields with proper column names
3. **KEEP** IdentifiedObject fields (id, description, created, updated, published, link fields)
4. **KEEP** related_links table (already exists, just needs reference in comment)

**Updated CREATE TABLE Statement**:
```sql
-- ProgramDateIdMappings table
-- Per customer.xsd lines 269-283, extends IdentifiedObject
-- Contains one embedded ProgramDateIdMapping (lines 1223-1251)
CREATE TABLE program_date_id_mappings
(
    -- IdentifiedObject fields
    id             CHAR(36) PRIMARY KEY,
    description    VARCHAR(255),
    created        TIMESTAMP NOT NULL,
    updated        TIMESTAMP NOT NULL,
    published      TIMESTAMP,
    up_link_rel    VARCHAR(255),
    up_link_href   VARCHAR(1024),
    up_link_type   VARCHAR(255),
    self_link_rel  VARCHAR(255),
    self_link_href VARCHAR(1024),
    self_link_type VARCHAR(255),

    -- Embedded ProgramDateIdMapping fields (optional, minOccurs="0")
    program_date_type VARCHAR(64),   -- ProgramDateKind enum
    code              VARCHAR(64),   -- String64
    name              VARCHAR(256),  -- String256
    note              VARCHAR(256)   -- String256, optional
);

-- Indexes for IdentifiedObject timestamp fields
CREATE INDEX idx_program_date_id_mappings_created ON program_date_id_mappings (created);
CREATE INDEX idx_program_date_id_mappings_updated ON program_date_id_mappings (updated);

-- Related Links Table for Program Date ID Mappings (for Atom bidirectional links)
-- Note: Table already exists, managed by @ElementCollection
CREATE TABLE program_date_id_mapping_related_links
(
    program_date_id_mapping_id CHAR(36) NOT NULL,
    related_links              VARCHAR(1024),
    FOREIGN KEY (program_date_id_mapping_id) REFERENCES program_date_id_mappings (id) ON DELETE CASCADE
);

CREATE INDEX idx_program_date_id_mapping_related_links ON program_date_id_mapping_related_links (program_date_id_mapping_id);
```

### Phase H: DtoExportService Integration

#### Task H1: Update DtoExportServiceImpl
**File**: `src/main/java/org/greenbuttonalliance/espi/common/service/impl/DtoExportServiceImpl.java`

**Changes**:
1. Add ProgramDateIdMappingsMapper injection
2. Register ProgramDateIdMappingsDto.class in CustomerExportService JAXBContext
3. Ensure CustomerAtomEntryDto has @XmlElement for ProgramDateIdMappings

**CustomerAtomEntryDto.java Update**:
Verify/add ProgramDateIdMappings to the @XmlElements annotation:
```java
@XmlElements({
    // ... other customer domain elements ...
    @XmlElement(name = "ProgramDateIdMappings", type = ProgramDateIdMappingsDto.class, namespace = "http://naesb.org/espi/customer")
})
```

### Phase I: Testing

#### Task I1: Create ProgramDateIdMappingDtoTest
**File**: `src/test/java/org/greenbuttonalliance/espi/common/dto/customer/ProgramDateIdMappingDtoTest.java`

**Test Coverage** (8 tests):
1. shouldMarshalProgramDateIdMappingWithAllFields
2. shouldMarshalProgramDateIdMappingWithMinimalFields
3. shouldUnmarshalProgramDateIdMappingXml
4. shouldHandleNullFields
5. shouldVerifyXmlNamespaceAndElements
6. shouldVerifyFieldOrder
7. shouldHandleAllEnumValues
8. shouldSerializeEnumAsString

#### Task I2: Create ProgramDateIdMappingsDtoTest
**File**: `src/test/java/org/greenbuttonalliance/espi/common/dto/customer/ProgramDateIdMappingsDtoTest.java`

**Test Coverage** (8 tests):
1. shouldMarshalProgramDateIdMappingsWithEmbedded
2. shouldMarshalProgramDateIdMappingsWithoutEmbedded
3. shouldUnmarshalProgramDateIdMappingsXml
4. shouldHandleNullProgramDateIdMapping
5. shouldVerifyXmlNamespaceAndElements
6. shouldVerifyFieldOrder
7. shouldWrapInAtomEntry
8. shouldNotIncludeIdentifiedObjectFields

#### Task I3: Create ProgramDateIdMappingMapperTest
**File**: `src/test/java/org/greenbuttonalliance/espi/common/mapper/customer/ProgramDateIdMappingMapperTest.java`

**Test Coverage** (6 tests):
1. shouldMapToDtoWithAllFields
2. shouldMapToDtoWithNullFields
3. shouldMapToEmbeddableWithAllFields
4. shouldMapToEmbeddableWithNullFields
5. shouldHandleNullInput
6. shouldMapAllEnumValues

#### Task I4: Create ProgramDateIdMappingsMapperTest
**File**: `src/test/java/org/greenbuttonalliance/espi/common/mapper/customer/ProgramDateIdMappingsMapperTest.java`

**Test Coverage** (8 tests):
1. shouldMapToDtoWithEmbedded
2. shouldMapToDtoWithoutEmbedded
3. shouldMapToEntityWithEmbedded
4. shouldMapToEntityWithoutEmbedded
5. shouldUpdateEntityFromDto
6. shouldNotMapIdentifiedObjectFields
7. shouldHandleNullInput
8. shouldMapNestedObject

#### Task I5: Create ProgramDateIdMappingsRepositoryTest
**File**: `src/test/java/org/greenbuttonalliance/espi/common/repositories/customer/ProgramDateIdMappingsRepositoryTest.java`

**Test Coverage** (10 tests):
1. shouldSaveAndFindById
2. shouldFindAll
3. shouldUpdate
4. shouldDelete
5. shouldCount
6. shouldReturnEmptyOptionalWhenNotFound
7. shouldPersistEmbeddedFields
8. shouldPersistAllEnumValues
9. shouldHandleNullEmbeddedObject
10. shouldPersistRelatedLinks

#### Task I6: Create ProgramDateIdMappingsServiceTest
**File**: `src/test/java/org/greenbuttonalliance/espi/common/service/customer/ProgramDateIdMappingsServiceTest.java`

**Test Coverage** (8 tests):
1. shouldCreateProgramDateIdMappings
2. shouldFindById
3. shouldFindAll
4. shouldUpdate
5. shouldDeleteById
6. shouldCount
7. shouldHandleNonExistentId
8. shouldDelegateToRepository

## Testing Strategy

### Test Breakdown
- **DTO Tests (ProgramDateIdMappingDto)**: 8 tests
- **DTO Tests (ProgramDateIdMappingsDto)**: 8 tests
- **Mapper Tests (ProgramDateIdMappingMapper)**: 6 tests
- **Mapper Tests (ProgramDateIdMappingsMapper)**: 8 tests
- **Repository Tests**: 10 tests
- **Service Tests**: 8 tests

**Total New Tests**: ~48 tests

### Current Test Baseline
Based on Phase 21 completion: 760 tests

### Expected Test Count After Phase 17
**Target**: ~808 tests (760 + 48)

### Regression Testing
- All existing tests must pass
- Integration tests verify database schema updates
- XML marshalling validates against customer.xsd
- Related links collections work correctly

## Execution Checklist

### Pre-Implementation Review
- [ ] Review customer.xsd for ProgramDateIdMappings (lines 269-283)
- [ ] Review customer.xsd for ProgramDateIdMapping (lines 1223-1251)
- [ ] Review customer.xsd for ProgramDateKind enum (lines 1997-2030)
- [ ] Understand current database schema issues
- [ ] Understand current DTO cleanup requirements
- [ ] Understand NAESB ESPI 4.0 bidirectional link requirements

### Phase A0: Enable Bidirectional Atom Links (PREREQUISITE)
- [ ] Add relatedLinks @ElementCollection to CustomerAgreementEntity
- [ ] Add relatedLinks @ElementCollection to ProgramDateIdMappingsEntity
- [ ] Add relatedLinks @ElementCollection to ServiceLocationEntity
- [ ] Add relatedLinks @ElementCollection to ServiceSupplierEntity
- [ ] Update toString() methods in all 4 entities
- [ ] Verify build succeeds with no JPA errors

### Phase A: Enum and Embeddable Creation
- [ ] Create ProgramDateKind enum (4 values)
- [ ] Create ProgramDateIdMapping embeddable class (4 fields)

### Phase B: Entity Updates
- [ ] Update ProgramDateIdMappingsEntity (add programDateIdMapping field + relatedLinks)

### Phase C: DTO Implementation
- [ ] Create ProgramDateIdMappingDto (4 fields, NO IdentifiedObject fields)
- [ ] Update ProgramDateIdMappingsDto (remove IdentifiedObject fields, keep only 1 XSD field)

### Phase D: Mapper Implementation
- [ ] Create ProgramDateIdMappingMapper
- [ ] Create ProgramDateIdMappingsMapper (NO Atom field mappings)

### Phase E: Repository Implementation
- [ ] Create ProgramDateIdMappingsRepository (NO custom query methods)

### Phase F: Service Implementation
- [ ] Create ProgramDateIdMappingsService interface (ID-based queries only)
- [ ] Create ProgramDateIdMappingsServiceImpl

### Phase G: Database Migration
- [ ] Update V3 migration: remove non-XSD fields (program_date, program_id)
- [ ] Update V3 migration: add embedded fields (program_date_type, code, name, note)
- [ ] Verify indexes on created/updated fields
- [ ] Verify related_links table exists (no changes needed)

### Phase H: DtoExportService Integration
- [ ] Update DtoExportServiceImpl with ProgramDateIdMappingsMapper
- [ ] Update CustomerAtomEntryDto with ProgramDateIdMappings @XmlElement

### Phase I: Testing
- [ ] Create ProgramDateIdMappingDtoTest (8 tests)
- [ ] Create ProgramDateIdMappingsDtoTest (8 tests)
- [ ] Create ProgramDateIdMappingMapperTest (6 tests)
- [ ] Create ProgramDateIdMappingsMapperTest (8 tests)
- [ ] Create ProgramDateIdMappingsRepositoryTest (10 tests including relatedLinks)
- [ ] Create ProgramDateIdMappingsServiceTest (8 tests)

### Final Verification
- [ ] Run full test suite: `mvn clean test`
- [ ] Run integration tests: `mvn verify -Pintegration-tests`
- [ ] Verify test count: ~808 tests
- [ ] All tests passing on H2, MySQL, PostgreSQL
- [ ] XML marshalling validates against customer.xsd
- [ ] No IdentifiedObject fields in ProgramDateIdMappingsDto
- [ ] Related links work correctly in all 4 entities

### Documentation and Issue Tracking
- [ ] Update Issue #28 with Phase 17 completion status
- [ ] **DO NOT close Issue #28** - more phases remain (Phase 18+)
- [ ] Document ProgramDateIdMapping architecture (embeddable pattern)
- [ ] Document ProgramDateKind enum values
- [ ] Document bidirectional Atom links pattern
- [ ] Update CLAUDE.md if needed

## Success Criteria

1. ✅ ProgramDateKind enum created (4 values matching XSD)
2. ✅ ProgramDateIdMapping embeddable created (4 fields)
3. ✅ ProgramDateIdMappingsEntity updated (has programDateIdMapping field + relatedLinks)
4. ✅ ProgramDateIdMappingDto created (4 fields, NO IdentifiedObject fields)
5. ✅ ProgramDateIdMappingsDto cleaned up (ONLY programDateIdMapping field, NO IdentifiedObject fields)
6. ✅ ProgramDateIdMappingMapper implemented (simple 4-field mapping)
7. ✅ ProgramDateIdMappingsMapper implemented (simple 1-field mapping, NO Atom field ignores)
8. ✅ ProgramDateIdMappingsRepository created (NO custom query methods)
9. ✅ ProgramDateIdMappingsService and impl created (ID-based queries only)
10. ✅ V3 Flyway migration updated (correct embedded fields)
11. ✅ All 4 entities have relatedLinks @ElementCollection (CustomerAgreement, ProgramDateIdMappings, ServiceLocation, ServiceSupplier)
12. ✅ All 48 new tests passing
13. ✅ Total test count: ~808 tests
14. ✅ No test regressions
15. ✅ XML validates against customer.xsd
16. ✅ Build succeeds
17. ✅ Integration tests pass on all databases
18. ✅ DTO follows AtomEntryDto pattern (NO IdentifiedObject fields in resource DTO)
19. ✅ Bidirectional Atom links infrastructure enabled for ESPI 4.0 compliance
20. ✅ Service layer uses ONLY ID-based queries
21. ✅ Issue #28 updated with Phase 17 status (NOT closed - more phases remain)

## Benefits

### XSD Compliance
- ✅ ProgramDateIdMappings matches customer.xsd lines 269-283
- ✅ ProgramDateIdMapping matches customer.xsd lines 1223-1251
- ✅ ProgramDateKind matches customer.xsd lines 1997-2030
- ✅ DTO follows Phase 21 pattern (Atom metadata separated from resource data)

### ESPI 4.0 Standard Compliance
- ✅ Supports NAESB ESPI 4.0 bidirectional Atom links via `<link rel="related">`
- ✅ Consistent pattern across all customer domain entities
- ✅ Service layer can populate related links for CustomerAgreement relationships

### Type Safety
- ✅ Enum for program date types
- ✅ Compile-time checked relationships
- ✅ Standard JPA embeddable pattern

### Performance
- ✅ Single table (no JOINs for embedded object)
- ✅ Indexed timestamp fields for efficient queries
- ✅ Simpler queries (ID-based only)

### Architecture
- ✅ Clean separation: Atom protocol (AtomEntryDto) vs resource data (ProgramDateIdMappingsDto)
- ✅ Standard JPA embedded pattern for nested objects
- ✅ Consistent with Phase 21 ServiceSupplier implementation
- ✅ MapStruct for clean entity ↔ DTO conversion
- ✅ Simple mappers with NO unnecessary field ignores
- ✅ Reusable relatedLinks pattern across customer domain
- ✅ Minimal service layer (ID-based operations only)

## References

- **XSD**: `openespi-common/src/main/resources/schema/ESPI_4.0/customer.xsd`
  - ProgramDateIdMappings: lines 269-283
  - ProgramDateIdMapping: lines 1223-1251
  - ProgramDateKind: lines 1997-2030
- **NAESB ESPI 4.0 Standard**: Bidirectional relationships via Atom `<link rel="related">` elements
- **Entity**: `ProgramDateIdMappingsEntity.java`, `ProgramDateIdMapping.java`
- **Embeddable**: `ProgramDateIdMapping.java` (extends Object, not IdentifiedObject)
- **Pattern Reference**: Phase 21 ServiceSupplier implementation
- **Issue**: #28 (Phase 17: ProgramDateIdMappings)
