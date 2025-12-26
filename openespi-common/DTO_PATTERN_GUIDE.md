# ESPI DTO Pattern Guide

## Purpose

This guide establishes the standard pattern for creating Data Transfer Object (DTO) classes for NAESB ESPI 4.0 resources to ensure proper XML marshalling/unmarshalling with Atom Protocol wrapper separation.

## Architecture Overview

ESPI uses a **two-layer architecture** for XML representation:

1. **Atom Wrapper Layer** (AtomEntryDto, AtomFeedDto)
   - Handles IdentifiedObject metadata: `published`, `updated`
   - Handles Atom links: `self`, `up`, `related`
   - Wraps the resource content

2. **Resource Layer** (Individual DTOs)
   - Contains ONLY XSD-defined resource fields
   - NO Atom metadata
   - NO link fields

### XML Structure Example

```xml
<entry xmlns="http://www.w3.org/2005/Atom">
  <id>urn:uuid:12345...</id>
  <title>Resource Title</title>
  <published>2025-01-01T00:00:00Z</published>     <!-- AtomEntryDto -->
  <updated>2025-01-01T00:00:00Z</updated>         <!-- AtomEntryDto -->
  <link rel="self" href="..." />                   <!-- AtomEntryDto -->
  <link rel="up" href="..." />                     <!-- AtomEntryDto -->
  <content type="application/xml">                 <!-- AtomContentDto -->
    <TimeConfiguration xmlns="http://naesb.org/espi">
      <dstEndRule>...</dstEndRule>                <!-- Resource DTO -->
      <dstOffset>...</dstOffset>
      <dstStartRule>...</dstStartRule>
      <tzOffset>...</tzOffset>
    </TimeConfiguration>
  </content>
</entry>
```

## Standard DTO Pattern

### Template Structure

```java
@XmlRootElement(name = "ResourceName", namespace = "http://naesb.org/espi")
@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlType(name = "ResourceName", namespace = "http://naesb.org/espi", propOrder = {
    "field1", "field2", "field3"  // XSD element sequence order
})
public record ResourceDto(

    @XmlTransient
    Long id,  // Database ID - never in XML

    String uuid,  // mRID - mapped in getter

    // XSD-defined fields (NO JAXB annotations on record parameters)
    FieldType field1,
    FieldType field2,
    FieldType field3

) {

    /**
     * Default constructor for JAXB.
     */
    public ResourceDto() {
        this(null, null, null, null, null);
    }

    // JAXB property accessors (in propOrder sequence)
    // REQUIRED: Records provide fieldName() but JAXB needs getFieldName()

    @XmlAttribute(name = "mRID")
    public String getUuid() {
        return uuid;
    }

    @XmlElement(name = "field1", namespace = "http://naesb.org/espi")
    public FieldType getField1() {
        return field1;
    }

    @XmlElement(name = "field2", namespace = "http://naesb.org/espi")
    public FieldType getField2() {
        return field2;
    }

    @XmlElement(name = "field3", namespace = "http://naesb.org/espi")
    public FieldType getField3() {
        return field3;
    }

    // Utility methods (optional)
}
```

## Key Rules

### Rule 1: @XmlAccessorType(PROPERTY) - REQUIRED

- **ALWAYS** use `@XmlAccessorType(XmlAccessType.PROPERTY)`
- JAXB accesses data via getter methods, NOT fields
- Allows mapping Java field names to different XML element names

**Why PROPERTY mode is required:**
- Java Records provide `fieldName()` accessors
- JAXB PROPERTY mode expects JavaBean-style `getFieldName()` getters
- These don't match, so explicit getters bridge the gap

### Rule 2: Explicit Getter Methods - REQUIRED

**Records alone are NOT sufficient for JAXB!**

```java
// Record provides:
dto.tzOffset()

// JAXB PROPERTY mode needs:
dto.getTzOffset()

// Solution: Explicit getter
public Long getTzOffset() {
    return tzOffset;
}
```

**All getter methods must:**
- Follow JavaBean naming: `getFieldName()` (or `isFieldName()` for boolean)
- Be public
- Have `@XmlElement` or `@XmlAttribute` annotation
- Return the record field value

### Rule 3: @XmlType propOrder

- **MUST** match XSD element sequence exactly
- Include all fields that will have @XmlElement annotations
- Order determines XML output order
- Verify against XSD `<xs:sequence>` elements

### Rule 4: Record Parameters

- Record parameters should have **NO JAXB annotations** (except @XmlTransient for id)
- JAXB annotations go on **getter methods only**
- This allows flexibility in XML element naming

### Rule 5: Namespace - CRITICAL

**Two different namespaces in ESPI:**

| Schema | Namespace | Usage |
|--------|-----------|-------|
| espi.xsd | `http://naesb.org/espi` | Usage domain (energy data) |
| customer.xsd | `http://naesb.org/espi/customer` | Customer domain (PII data) |

**IMPORTANT:** Customer domain DTOs MUST use the customer namespace!

```java
// Usage domain (espi.xsd)
@XmlRootElement(name = "TimeConfiguration", namespace = "http://naesb.org/espi")
@XmlType(name = "TimeConfiguration", namespace = "http://naesb.org/espi", propOrder = {...})

// Customer domain (customer.xsd)
@XmlRootElement(name = "Customer", namespace = "http://naesb.org/espi/customer")
@XmlType(name = "Customer", namespace = "http://naesb.org/espi/customer", propOrder = {...})
```

### Rule 6: Fields to EXCLUDE from DTOs

**Never include these IdentifiedObject fields in resource DTOs:**
- ❌ `published` (handled by AtomEntryDto)
- ❌ `updated` (handled by AtomEntryDto)
- ❌ `selfLink` (handled by AtomEntryDto.links)
- ❌ `upLink` (handled by AtomEntryDto.links)
- ❌ `relatedLinks` (handled by AtomEntryDto.links)
- ❌ `description` (unless explicitly in XSD as resource field, not IdentifiedObject.description)

**Why:** These are Atom Protocol metadata, not resource data. They belong in the AtomEntryDto wrapper.

### Rule 7: Fields to INCLUDE

**Always include:**
- ✅ `id` (Long, marked @XmlTransient - for internal database use)
- ✅ `uuid` (String, mapped to @XmlAttribute mRID via getter)
- ✅ All XSD-defined resource fields in correct sequence order

### Rule 8: XML Element Name Mapping

Use `@XmlElement(name = "xmlName")` to map Java names to XML names when they differ:

```java
// Java field name differs from XML element name
Long expiresIn

// Getter maps it correctly
@XmlElement(name = "expires_at", namespace = "http://naesb.org/espi")
public Long getExpiresIn() {
    return expiresIn;
}
```

### Rule 9: Required Fields

Mark required fields in getter annotation:

```java
@XmlElement(name = "scope", namespace = "http://naesb.org/espi", required = true)
public String getScope() {
    return scope;
}
```

## Pattern Examples

### Correct Pattern: AuthorizationDto (Usage Domain)

```java
@XmlRootElement(name = "Authorization", namespace = "http://naesb.org/espi")
@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlType(name = "Authorization", namespace = "http://naesb.org/espi", propOrder = {
    "authorizedPeriod", "publishedPeriod", "status", "expiresIn", "grantType",
    "scope", "tokenType", "error", "errorDescription", "errorUri",
    "resourceURI", "authorizationUri", "customerResourceURI"
})
public record AuthorizationDto(

    @XmlTransient
    String uuid,

    DateTimeIntervalDto authorizedPeriod,
    DateTimeIntervalDto publishedPeriod,
    Short status,
    Long expiresIn,
    String grantType,
    String scope,
    String tokenType,
    String error,
    String errorDescription,
    String errorUri,
    String resourceURI,
    String authorizationUri,
    String customerResourceURI,

    // OAuth2 fields not in XSD
    @XmlTransient String accessToken,
    @XmlTransient String refreshToken

) {

    public AuthorizationDto() {
        this(null, null, null, null, null, null, null, null, null, null,
             null, null, null, null, null, null);
    }

    // JAXB property accessors with annotations

    @XmlElement(name = "authorizedPeriod", namespace = "http://naesb.org/espi")
    public DateTimeIntervalDto getAuthorizedPeriod() { return authorizedPeriod; }

    @XmlElement(name = "publishedPeriod", namespace = "http://naesb.org/espi")
    public DateTimeIntervalDto getPublishedPeriod() { return publishedPeriod; }

    @XmlElement(name = "status", namespace = "http://naesb.org/espi", required = true)
    public Short getStatus() { return status; }

    @XmlElement(name = "expires_at", namespace = "http://naesb.org/espi", required = true)
    public Long getExpiresIn() { return expiresIn; }

    // ... other getters
}
```

### Incorrect Pattern: IntervalReadingDto (TO BE FIXED)

```java
// WRONG - Multiple issues
@XmlAccessorType(XmlAccessType.FIELD)  // ❌ Should be PROPERTY
@XmlType(propOrder = {
    "uuid", "published", "updated", "selfLink", "upLink", ...  // ❌ Includes Atom fields
})
public record IntervalReadingDto(

    @XmlElement(name = "published")  // ❌ Annotations on record params
    OffsetDateTime published,        // ❌ Should be in AtomEntryDto

    @XmlElement(name = "selfLink")   // ❌ Should be in AtomEntryDto
    LinkDto selfLink,

    @XmlElement(name = "value")
    Long value

) {
    // ❌ No explicit getters for JAXB
}
```

**Issues:**
1. Uses FIELD accessor type instead of PROPERTY
2. Includes Atom metadata fields (published, updated, links)
3. Has JAXB annotations on record parameters
4. Missing explicit getter methods

## Customer Domain Example Template

```java
@XmlRootElement(name = "Customer", namespace = "http://naesb.org/espi/customer")
@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlType(name = "Customer", namespace = "http://naesb.org/espi/customer", propOrder = {
    "field1", "field2"  // Match customer.xsd sequence
})
public record CustomerDto(

    @XmlTransient
    Long id,

    String uuid,

    FieldType field1,
    FieldType field2

) {

    public CustomerDto() {
        this(null, null, null, null);
    }

    @XmlAttribute(name = "mRID")
    public String getUuid() {
        return uuid;
    }

    @XmlElement(name = "field1", namespace = "http://naesb.org/espi/customer")
    public FieldType getField1() {
        return field1;
    }

    @XmlElement(name = "field2", namespace = "http://naesb.org/espi/customer")
    public FieldType getField2() {
        return field2;
    }
}
```

## Atom Wrapper Usage

When returning resources in Atom format:

```java
// 1. Create resource DTO (ONLY resource fields)
TimeConfigurationDto resource = new TimeConfigurationDto(
    null,  // id
    "550e8400-e29b-41d4-a716-446655440000",  // uuid
    new byte[]{...},  // dstEndRule
    3600L,            // dstOffset
    new byte[]{...},  // dstStartRule
    -28800L           // tzOffset (UTC-8)
);

// 2. Wrap in Atom Entry (adds IdentifiedObject metadata)
AtomEntryDto entry = new AtomEntryDto(
    "urn:uuid:" + resource.getUuid(),  // Atom id
    "LocalTimeParameters",              // Atom title
    OffsetDateTime.now(),               // published
    OffsetDateTime.now(),               // updated
    List.of(
        LinkDto.self("/espi/1_1/resource/LocalTimeParameters/" + resource.getUuid()),
        LinkDto.up("/espi/1_1/resource/LocalTimeParameters")
    ),
    new AtomContentDto("application/xml", resource)
);

// 3. Marshal to XML - Atom wrapper + resource content
```

## Issue #28 Context - Entities That Should NOT Extend IdentifiedObject

Per Issue #28, some entities currently extend IdentifiedObject but should extend Object:

| Entity | Current | Should Be | Implication |
|--------|---------|-----------|-------------|
| AggregatedNodeRefEntity | IdentifiedObject | Object | Embedded, no Atom wrapper |
| BatchListEntity | IdentifiedObject | BatchListType | Standalone message |
| IntervalReadingEntity | IdentifiedObject | Object | Embedded, no Atom wrapper |
| LineItemEntity | IdentifiedObject | Object | Embedded, no Atom wrapper |
| PnodeRefEntity | IdentifiedObject | Object | Embedded, no Atom wrapper |
| ReadingQualityEntity | IdentifiedObject | Object | Embedded, no Atom wrapper |
| ServiceDeliveryPointEntity | IdentifiedObject | Object | Embedded, no Atom wrapper |

**For entities that should NOT extend IdentifiedObject:**
- DTOs should be simpler (may not need uuid/mRID)
- Should NOT be wrapped in Atom entries
- Should be embedded within parent resource XML
- Still follow PROPERTY accessor pattern for consistency

## Verification Checklist

For each DTO created or updated, verify:

- [ ] Uses `@XmlAccessorType(XmlAccessType.PROPERTY)`
- [ ] Has `@XmlType` with correct propOrder matching XSD sequence
- [ ] Record parameters have NO JAXB annotations (except @XmlTransient on id)
- [ ] Has explicit JavaBean-style getter methods (`getFieldName()`)
- [ ] Getters have @XmlElement or @XmlAttribute annotations
- [ ] Does NOT include Atom metadata fields (published, updated, links)
- [ ] Includes uuid mapped to mRID attribute (for IdentifiedObject resources)
- [ ] Field order matches XSD element sequence exactly
- [ ] Uses correct namespace:
  - [ ] `http://naesb.org/espi` for usage domain (espi.xsd)
  - [ ] `http://naesb.org/espi/customer` for customer domain (customer.xsd)
- [ ] Has default no-arg constructor for JAXB
- [ ] Required fields marked with `required = true`

## References

- **Atom DTOs**: `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/dto/atom/`
  - AtomEntryDto.java - Entry wrapper with published, updated, links
  - AtomFeedDto.java - Feed wrapper for collections
  - AtomContentDto.java - Content wrapper using @XmlAnyElement
  - LinkDto.java - Atom link representation
- **Example DTO**: AuthorizationDto.java (correct pattern)
- **XSD Schemas**:
  - `openespi-common/src/main/resources/schema/ESPI_4.0/espi.xsd`
  - `openespi-common/src/main/resources/schema/ESPI_4.0/customer.xsd`
- **Issue #28**: Review Current Usage and Customer Entity Classes
- **Multi-Phase Plan**: MULTI_PHASE_SCHEMA_COMPLIANCE_PLAN.md

## Common Mistakes to Avoid

1. **Using FIELD accessor type** - Always use PROPERTY
2. **Forgetting explicit getters** - Records need JavaBean-style getters for JAXB
3. **Including Atom fields in DTO** - Let AtomEntryDto handle metadata
4. **Wrong namespace** - Check if resource is from espi.xsd or customer.xsd
5. **JAXB annotations on record params** - Put them on getters only
6. **Wrong propOrder** - Must match XSD <xs:sequence> exactly
7. **Missing default constructor** - JAXB requires no-arg constructor

## Updates

This guide should be updated whenever:
- New DTO patterns are discovered
- XSD schema changes
- Atom wrapper patterns evolve
- JAXB or Record usage patterns change

---

**Version**: 1.0
**Last Updated**: 2025-12-26
**Author**: Phase 1 Schema Compliance Review