# Phase 20: DTO Record-to-Class Conversion Cleanup Plan

## Current Status (2026-01-20)

**Build Status:** ✅ Compiles successfully
**Test Status:** ❌ XmlDebugTest fails with 75 JAXB IllegalAnnotationExceptions
**Completion:** ~70-75%

## Problem Summary

Many DTOs were converted from records to Lombok classes but still have **explicit getter methods with `@XmlElement` annotations**. Combined with:
- `@XmlAccessorType(XmlAccessType.FIELD)`
- `@XmlElement` annotations on private fields
- Lombok `@Getter` annotation

This creates **duplicate properties** that JAXB detects as errors like:
```
Class has two properties of the same name "fieldName"
  at public Type Dto.getFieldName()  // Explicit getter
  at private Type Dto.fieldName      // Private field
```

## Root Cause

The Python batch conversion script moved `@XmlElement` from getters to fields but **did NOT remove the explicit getter methods**. These must be manually removed.

## Cleanup Tasks

### Task 1: Identify All Affected DTOs

Run test to extract all duplicate property errors:
```bash
mvn test -Dtest=XmlDebugTest 2>&1 | \
  grep "Class has two properties of the same name" | \
  grep -oP 'org\.greenbuttonalliance\.espi\.common\.dto\.\S+(?=\.)' | \
  sort -u > affected_dtos.txt
```

### Task 2: Fix Each DTO (Pattern)

For each affected DTO, apply this pattern:

**BEFORE (Incorrect - has duplicates):**
```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
public class ExampleDto {

    @XmlElement(name = "fieldName")
    private String fieldName;  // ← Field with annotation

    // ❌ REMOVE THIS - Lombok @Getter generates it automatically
    @XmlElement(name = "fieldName")
    public String getFieldName() {
        return fieldName;
    }
}
```

**AFTER (Correct - no duplicates):**
```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
public class ExampleDto {

    /**
     * Description of field.
     */
    @XmlElement(name = "fieldName")
    private String fieldName;  // ← Lombok generates getter automatically

    // ✅ Keep only custom business logic methods
    public boolean hasFieldName() {
        return fieldName != null;
    }
}
```

### Task 3: Verify @XmlTransient Fields

Ensure all internal fields not in XSD have `@XmlTransient`:
- `uuid` (internal identifier)
- `id` (database ID)
- `description` (if not in propOrder)
- Any foreign key fields

**Pattern:**
```java
@XmlTransient
private String uuid;

@XmlTransient
private Long id;
```

### Task 4: Systematic Execution Plan

#### Phase A: Fix Known High-Priority DTOs

Based on test errors, these DTOs definitely need fixing:

1. **LineItemDto** - Has duplicate: amount, rounding, dateTime, note, measurement, itemKind, unitCost, itemPeriod
2. **UsageSummaryDto** - Likely has duplicates (needs verification)
3. **ServiceDeliveryPointDto** - Likely has duplicates (needs verification)

#### Phase B: Scan All Usage Domain DTOs

Check each file in `dto/usage/`:
```bash
for file in src/main/java/org/greenbuttonalliance/espi/common/dto/usage/*.java; do
  echo "=== $(basename $file) ==="
  grep -A 1 "@XmlElement" "$file" | grep "public.*get" || echo "OK - no explicit getters"
done
```

**Files to check:**
- [ ] ApplicationInformationDto
- [ ] AuthorizationDto
- [ ] BatchListDto
- [ ] DateTimeIntervalDto
- [ ] ElectricPowerQualitySummaryDto
- [ ] IntervalBlockDto
- [ ] IntervalReadingDto
- [ ] LineItemDto ⚠️ **CONFIRMED NEEDS FIXING**
- [ ] MeterReadingDto
- [ ] ReadingQualityDto
- [ ] ReadingTypeDto
- [ ] ServiceDeliveryPointDto ⚠️ **NEEDS VERIFICATION**
- [ ] SubscriptionDto
- [ ] TimeConfigurationDto
- [ ] UsagePointDto
- [ ] UsageSummaryDto ⚠️ **NEEDS VERIFICATION**
- [ ] AggregatedNodeRefDto ✅ **FIXED**
- [ ] AggregatedNodeRefsDto
- [ ] PnodeRefDto ✅ **FIXED**
- [ ] PnodeRefsDto
- [ ] TariffRiderRefDto
- [ ] TariffRiderRefsDto

#### Phase C: Check Common/Utility DTOs

Files in `dto/`:
- [ ] BillingChargeSourceDto
- [ ] SummaryMeasurementDto
- [ ] RationalNumberDto
- [ ] ReadingInterharmonicDto

#### Phase D: Check Atom DTOs

Files in `dto/atom/`:
- [ ] AtomEntryDto
- [ ] AtomFeedDto
- [ ] LinkDto

#### Phase E: Verify Customer Domain

Files in `dto/customer/` (should already be clean):
- [ ] CustomerAccountDto
- [ ] CustomerAgreementDto
- [ ] CustomerDto
- [ ] EndDeviceDto
- [ ] MeterDto
- [ ] ServiceLocationDto
- [ ] StatementDto
- [ ] ProgramDateIdMappingsDto

## Verification Steps

### Step 1: Compile Check
```bash
mvn clean compile -DskipTests
```
Expected: ✅ BUILD SUCCESS with only MapStruct warnings

### Step 2: Test Execution
```bash
mvn test -Dtest=XmlDebugTest
```
Expected: ✅ All 4 tests pass, no IllegalAnnotationExceptions

### Step 3: Full Test Suite
```bash
mvn test
```
Expected: All tests pass

### Step 4: XML Output Validation

Create test to verify namespace prefixes:
```java
@Test
void verifyNamespacePrefixes() {
    String xml = marshalToXml(dto);
    assertThat(xml).contains("espi:");  // Usage namespace
    assertThat(xml).contains("cust:");  // Customer namespace
    assertThat(xml).doesNotContain("wstxns"); // No auto-generated prefixes
}
```

## Risk Assessment

**LOW RISK:**
- Removing explicit getters is safe - Lombok generates them
- Pattern is repetitive and mechanical
- Each DTO can be fixed independently
- Rollback is easy (git checkout)

**POTENTIAL ISSUES:**
1. **Custom getter logic** - Some getters may have business logic (e.g., `getPnodeRef()` in AggregatedNodeRefDto returns empty list if null). Keep these.
2. **Defensive copying** - Byte array getters that clone arrays must be kept
3. **Computed properties** - Getters that compute values (e.g., `getScaledValue()`) must be kept

**MITIGATION:**
- Only remove getters that simply return the field
- Keep any getter with custom logic
- Test after each domain is fixed

## Estimated Effort

- **Phase A (High-Priority DTOs):** 30 minutes
- **Phase B (Usage Domain Scan):** 2 hours
- **Phase C (Common/Utility):** 30 minutes
- **Phase D (Atom DTOs):** 30 minutes
- **Phase E (Customer Verification):** 30 minutes
- **Testing & Validation:** 1 hour

**TOTAL:** ~5 hours

## Success Criteria

- [ ] All 75 IllegalAnnotationExceptions resolved
- [ ] XmlDebugTest passes all 4 tests
- [ ] Full test suite passes
- [ ] XML output contains correct namespace prefixes (`espi:`, `cust:`)
- [ ] No `wstxns` auto-generated prefixes in output
- [ ] Build succeeds with only expected MapStruct warnings

## Next Actions

1. **Review this plan** - Approve approach
2. **Execute Phase A** - Fix confirmed broken DTOs
3. **Run test** - Verify progress (should drop from 75 errors)
4. **Execute Phases B-E** - Systematic cleanup
5. **Final validation** - Namespace prefix verification
6. **Update documentation** - Mark JAXB_RECORDS_INCOMPATIBILITY.md as RESOLVED

---

**Plan Created:** 2026-01-20
**Estimated Completion:** 2026-01-20 (same day if approved)
**Status:** READY FOR REVIEW
