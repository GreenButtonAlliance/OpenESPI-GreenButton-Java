# Jakarta JAXB and Java Records Incompatibility

## Issue Summary

Jakarta JAXB (GlassFish implementation) does NOT fully support Java records for XML marshalling/unmarshalling.

**Error Encountered:**
```
org.glassfish.jaxb.runtime.v2.runtime.IllegalAnnotationsException: 272 counts of IllegalAnnotationExceptions
JAXB annotation is placed on a method that is not a JAXB property
AtomEntryDto does not have a no-arg default constructor
```

## Root Cause Analysis

### Java Records vs JAXB Expectations

**Java Records (Java 14+):**
- Immutable data carriers
- All fields are implicitly `final`
- Compact canonical constructor only
- Accessor methods (not JavaBean getters): `field()` instead of `getField()`
- Cannot have additional constructors beyond canonical
- Class itself is implicitly `final`

**JAXB Requirements (JavaBeans pattern):**
- Mutable objects with default no-arg constructor
- Non-final fields with setters
- JavaBean-style getters: `getField()`
- Ability to instantiate empty object and populate via setters

### Specific Incompatibilities

1. **No Default Constructor:**
   ```java
   public record AtomEntryDto(String id, String title, ...) {}
   // JAXB Error: "does not have a no-arg default constructor"
   ```
   - Records only have canonical constructor
   - Cannot add no-arg constructor
   - Lombok's `@NoArgsConstructor` does NOT work on records

2. **Method Annotations Not Recognized:**
   ```java
   @XmlElement(name = "id", namespace = "...")
   String id

   // Record generates: public String id() { return id; }
   // JAXB expects: public String getId() { return id; }
   // Error: "JAXB annotation is placed on a method that is not a JAXB property"
   ```

3. **Immutability:**
   - Records have no setters
   - JAXB unmarshalling requires setters to populate fields
   - Cannot use `@XmlAccessType.FIELD` effectively with final fields

## Attempted Solutions That Don't Work

### ❌ Lombok @NoArgsConstructor
```java
@NoArgsConstructor  // Does NOT compile with records
public record AtomEntryDto(...) {}
```
**Error:** Lombok constructor annotations are not supported on records

### ❌ Manual No-Arg Constructor
```java
public record AtomEntryDto(...) {
    public AtomEntryDto() {  // COMPILE ERROR
        this(null, null, ...);
    }
}
```
**Error:** Records cannot have additional constructors beyond canonical/compact

### ❌ @XmlAccessType Configuration
```java
@XmlAccessorType(XmlAccessType.FIELD)
public record AtomEntryDto(...) {}
```
**Still Fails:** JAXB tries to access fields via reflection but records protect field access

## Valid Solutions

### Option A: Convert Records to Regular Classes (RECOMMENDED)

**Effort:** ~12-15 hours for 40+ DTOs

**Implementation:**
```java
// FROM (Record):
public record CustomerDto(
    String uuid,
    OrganisationDto organisation,
    CustomerKind kind
) {}

// TO (Class):
@XmlRootElement(name = "Customer", namespace = "http://naesb.org/espi/customer")
@XmlAccessorType(XmlAccessType.FIELD)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDto {

    @XmlTransient
    private String uuid;

    @XmlElement(name = "Organisation", namespace = "http://naesb.org/espi/customer")
    private OrganisationDto organisation;

    @XmlElement(name = "kind", namespace = "http://naesb.org/espi/customer")
    private CustomerKind kind;
}
```

**Benefits:**
- ✅ Full JAXB compatibility
- ✅ Proper namespace prefix support (`cust:`, `espi:`)
- ✅ Lombok reduces boilerplate
- ✅ Standard JavaBeans pattern

**Drawbacks:**
- ❌ Lose immutability
- ❌ More verbose (getters/setters visible in code)
- ❌ Larger codebase

### Option B: Use EclipseLink MOXy JAXB

**Effort:** ~2-4 hours

**Implementation:**
Replace GlassFish JAXB runtime with MOXy:
```xml
<dependency>
    <groupId>org.eclipse.persistence</groupId>
    <artifactId>org.eclipse.persistence.moxy</artifactId>
    <version>4.0.2</version>
</dependency>
```

**Status:** MOXy has EXPERIMENTAL record support but still has limitations with immutability.

**Risks:**
- ⚠️ MOXy record support incomplete
- ⚠️ Different JAXB implementation may have other issues
- ⚠️ Less commonly used than GlassFish JAXB

### Option C: Keep Jackson (NOT RECOMMENDED)

**Why Not Recommended:**
- Jackson ignores `@XmlNs` prefix declarations in package-info.java
- Auto-generates prefixes (`wstxns1`, `wstxns2`) instead of `espi:`, `cust:`
- No configuration option to override this behavior
- Fails ESPI 4.0 compliance for namespace prefixes

## Recommendation

**Convert all DTOs from records to Lombok-annotated classes (Option A).**

**Reasoning:**
1. Full Jakarta JAXB compliance
2. Proper namespace prefix support per ESPI 4.0 specification
3. Lombok mitigates boilerplate concerns
4. Standard JavaBeans pattern widely understood
5. MapStruct already works with Lombok classes (no changes needed)
6. Entities are already Lombok classes (consistent architecture)

## Impact Assessment

### Files Requiring Conversion (~40 DTOs)

**Atom Domain (2):**
- AtomEntryDto
- AtomFeedDto
- LinkDto

**Usage Domain (~20):**
- UsagePointDto
- MeterReadingDto
- IntervalBlockDto
- IntervalReadingDto
- ReadingTypeDto
- DateTimeIntervalDto
- ReadingQualityDto
- TimeConfigurationDto
- UsageSummaryDto
- ElectricPowerQualitySummaryDto
- ApplicationInformationDto
- AuthorizationDto
- SubscriptionDto
- BatchListDto
- LineItemDto
- SummaryMeasurementDto
- BillingChargeSourceDto
- TariffRiderRefDto
- TariffRiderRefsDto
- (+ others)

**Customer Domain (~10):**
- CustomerDto (already has namespace annotations)
- CustomerAccountDto
- CustomerAgreementDto
- ServiceLocationDto
- StatementDto
- MeterDto
- EndDeviceDto
- (+ nested records in CustomerDto)

**Shared DTOs (~5):**
- RationalNumberDto
- ReadingInterharmonicDto
- Various embedded records

### Migration Steps

1. **Create Lombok class templates** for common patterns
2. **Convert DTOs domain-by-domain** (atom → usage → customer)
3. **Update MapStruct mappers** (should work without changes)
4. **Run tests after each domain** (verify XML output)
5. **Validate namespace prefixes** in generated XML

### Estimated Timeline

- **Template creation:** 1 hour
- **Atom domain conversion:** 1 hour
- **Usage domain conversion:** 6 hours
- **Customer domain conversion:** 3 hours
- **Testing and validation:** 2 hours
- **TOTAL:** ~13 hours

## References

- [Java Records Specification (JEP 395)](https://openjdk.org/jeps/395)
- [Jakarta XML Binding Specification](https://jakarta.ee/specifications/xml-binding/4.0/)
- [Lombok Documentation - Constructor Annotations](https://projectlombok.org/features/constructor)
- [JAXB with Records Discussion (Stack Overflow)](https://stackoverflow.com/questions/66468393/jaxb-and-records)

---

**Document Created:** 2026-01-19
**Status:** BLOCKING - Requires architectural decision before proceeding