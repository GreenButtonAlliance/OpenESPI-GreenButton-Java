# Record to Lombok Class Conversion Plan

## Objective
Convert DTOs from Java records to Lombok-annotated classes to achieve JAXB compatibility while maintaining MapStruct mapper functionality.

## Pilot Conversion Scope

### Phase 1: IntervalBlock DTO Family
These DTOs are used in XmlDebugTest and form a complete test case:

1. **IntervalBlockDto** (parent)
   - Dependencies: DateTimeIntervalDto, List<IntervalReadingDto>

2. **IntervalReadingDto** (child)
   - Dependencies: DateTimeIntervalDto, List<ReadingQualityDto>

3. **DateTimeIntervalDto** (nested in both)
   - No dependencies (primitive types only)

4. **ReadingQualityDto** (nested in IntervalReading)
   - No dependencies (primitive types only)

### Phase 2: Customer DTO Family
These DTOs test customer domain with more complex nested objects:

1. **CustomerDto** (parent)
   - Dependencies: OrganisationDto, CustomerKind (enum)

2. **OrganisationDto** (nested)
   - Dependencies: TelephoneNumberDto, StreetAddressDto

3. **TelephoneNumberDto** (nested in Organisation)
   - No dependencies (primitive types only)

4. **StreetAddressDto** (nested in Organisation)
   - No dependencies (primitive types only)

**Total DTOs for Pilot:** 8 DTOs

## Conversion Pattern

### From Record:
```java
@XmlRootElement(name = "IntervalBlock", namespace = "http://naesb.org/espi")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "IntervalBlock", namespace = "http://naesb.org/espi", propOrder = {
    "interval", "intervalReadings"
})
public record IntervalBlockDto(

    @XmlTransient
    Long id,

    @XmlTransient
    String uuid,

    @XmlElement(name = "interval", namespace = "http://naesb.org/espi")
    DateTimeIntervalDto interval,

    @XmlElement(name = "IntervalReading", namespace = "http://naesb.org/espi")
    List<IntervalReadingDto> intervalReadings
) {
    // Record constructors and utility methods
}
```

### To Lombok Class:
```java
@XmlRootElement(name = "IntervalBlock", namespace = "http://naesb.org/espi")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "IntervalBlock", namespace = "http://naesb.org/espi", propOrder = {
    "interval", "intervalReadings"
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IntervalBlockDto {

    @XmlTransient
    private Long id;

    @XmlTransient
    private String uuid;

    @XmlElement(name = "interval", namespace = "http://naesb.org/espi")
    private DateTimeIntervalDto interval;

    @XmlElement(name = "IntervalReading", namespace = "http://naesb.org/espi")
    private List<IntervalReadingDto> intervalReadings;

    // Utility methods (if any) stay the same
}
```

## Conversion Steps

### Step 1: Change Declaration
- Replace `public record` with `public class`
- Add Lombok annotations: `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`

### Step 2: Convert Record Parameters to Fields
- Change record parameters in parentheses to private fields in class body
- Keep all JAXB annotations on fields

### Step 3: Handle Utility Methods
- Move any utility methods from record body to class body
- Methods that use `this.field` syntax will work unchanged
- Remove any custom record constructors (Lombok handles this)

### Step 4: Update Imports
- Add: `import lombok.Getter;`
- Add: `import lombok.Setter;`
- Add: `import lombok.NoArgsConstructor;`
- Add: `import lombok.AllArgsConstructor;`

## Testing Strategy

### XmlDebugTest Validation
The existing XmlDebugTest.java will validate:

1. **JAXB Marshalling Works**
   - No IllegalAnnotationsException errors
   - No "missing no-arg constructor" errors

2. **XML Structure Correct**
   - Proper namespace prefixes (espi:, cust:)
   - Correct element names and nesting
   - Proper attribute handling

3. **XML Content Correct**
   - Field values marshalled correctly
   - Null handling works (NON_EMPTY policy)
   - Collections marshalled correctly

4. **MapStruct Compatibility** (implicit)
   - If DTOs compile, MapStruct will work
   - Lombok runs before MapStruct in annotation processing

### Test Execution
```bash
# Run the XmlDebugTest
cd /Users/donal/Git/GreenButtonAlliance/OpenESPI-GreenButton-Java/openespi-common
mvn test -Dtest=XmlDebugTest
```

### Expected Test Output
```xml
<!-- IntervalBlock with proper espi: namespace -->
<entry xmlns="http://www.w3.org/2005/Atom">
  <id>urn:uuid:debug-test</id>
  <title>Debug Service</title>
  <content>
    <espi:IntervalBlock xmlns:espi="http://naesb.org/espi">
      <espi:interval>
        <espi:start>1634788800</espi:start>
        <espi:duration>3600</espi:duration>
      </espi:interval>
      <espi:IntervalReading>
        <espi:value>12345</espi:value>
      </espi:IntervalReading>
    </espi:IntervalBlock>
  </content>
</entry>

<!-- Customer with proper cust: namespace -->
<cust:Customer xmlns:cust="http://naesb.org/espi/customer">
  <cust:Organisation>
    <cust:name>ACME Utility</cust:name>
  </cust:Organisation>
  <cust:kind>residential</cust:kind>
</cust:Customer>
```

## Success Criteria

### Phase 1 Success (IntervalBlock):
- ✅ All 4 DTOs compile without errors
- ✅ XmlDebugTest passes all assertions
- ✅ XML output shows proper `espi:` namespace prefixes
- ✅ No JAXB IllegalAnnotationsException errors
- ✅ Null fields properly excluded from XML

### Phase 2 Success (Customer):
- ✅ All 4 DTOs compile without errors
- ✅ XmlDebugTest validates Customer marshalling
- ✅ XML output shows proper `cust:` namespace prefixes
- ✅ Nested objects (Organisation, TelephoneNumber, StreetAddress) marshal correctly

### Overall Success:
- ✅ Both IntervalBlock and Customer tests pass
- ✅ MapStruct mappers compile (run `mvn compile`)
- ✅ No regression in existing tests
- ✅ XML namespace prefixes match ESPI 4.0 specification

## Post-Pilot Actions

### If Pilot Succeeds:
1. Document final conversion pattern
2. Create conversion checklist for remaining ~32 DTOs
3. Proceed with domain-by-domain conversion:
   - Atom domain (AtomFeedDto, AtomEntryDto, LinkDto)
   - Usage domain (~20 DTOs)
   - Customer domain (remaining ~8 DTOs)
   - Shared/embedded DTOs (~5 DTOs)

### If Pilot Fails:
1. Document failure reason
2. Revisit Option B (MOXy JAXB) or alternative approaches
3. Consult with team on architectural decision

## File Locations

### DTOs to Convert (Pilot):
- `src/main/java/org/greenbuttonalliance/espi/common/dto/usage/IntervalBlockDto.java`
- `src/main/java/org/greenbuttonalliance/espi/common/dto/usage/IntervalReadingDto.java`
- `src/main/java/org/greenbuttonalliance/espi/common/dto/usage/DateTimeIntervalDto.java`
- `src/main/java/org/greenbuttonalliance/espi/common/dto/usage/ReadingQualityDto.java`
- `src/main/java/org/greenbuttonalliance/espi/common/dto/customer/CustomerDto.java`
- `src/main/java/org/greenbuttonalliance/espi/common/dto/customer/OrganisationDto.java`
- `src/main/java/org/greenbuttonalliance/espi/common/dto/customer/TelephoneNumberDto.java`
- `src/main/java/org/greenbuttonalliance/espi/common/dto/customer/StreetAddressDto.java`

### Test File:
- `src/test/java/org/greenbuttonalliance/espi/common/XmlDebugTest.java`

## Timeline Estimate

- **Phase 1 (IntervalBlock):** 45 minutes
  - Convert 4 DTOs: 30 minutes
  - Update test: 5 minutes
  - Run and validate: 10 minutes

- **Phase 2 (Customer):** 45 minutes
  - Convert 4 DTOs: 30 minutes
  - Add Customer test case: 10 minutes
  - Run and validate: 5 minutes

- **Documentation:** 15 minutes

**Total Pilot Time:** ~1.5-2 hours

---

**Created:** 2026-01-19
**Status:** READY TO IMPLEMENT
