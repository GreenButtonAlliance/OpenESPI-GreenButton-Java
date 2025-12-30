# PR #50 Impact Analysis: Spring Boot 4.0 + Java 25 Upgrade

**Date:** 2025-12-29
**PR:** #50 - "Issue 39 spring boot 4 and Java 25 upgrade"
**Status:** ⚠️ OPEN - CI/CD Failing
**Impact on Phase 1:** 🔴 **CRITICAL** - Blocks DTO decision

---

## Executive Summary

PR #50 attempts to upgrade the entire project to:
- **Java 25** (from Java 21)
- **Spring Boot 4.0.1** (from Spring Boot 3.5.0)
- **Jakarta EE 11** (from Jakarta EE 9+)

**Current Status:**
- ✅ Security scan passed
- ❌ Build & Test failed (openespi-common module)
- ❌ PR Validation failed
- ⏭️  SonarCloud Analysis skipped

This upgrade **significantly impacts** the JAXB vs Jackson XML decision for Phase 1 and the 26-phase DTO implementation plan.

---

## Build Failure Analysis

### Failed Module
```
[INFO] OpenESPI-Common 3.5.0-RC2 .......................... FAILURE [01:08 min]
[INFO] BUILD FAILURE
[ERROR] Failed to execute goal maven-surefire-plugin:3.5.4:test
```

**Root Cause:** Test failures in `openespi-common` module

**Impact:** Build stops at common module, preventing authserver/datacustodian/thirdparty modules from being tested.

---

## Key Changes in PR #50

### 1. Parent POM Structure Change

**Before (main branch):**
```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.5.0</version>
</parent>
```

**After (PR #50):**
```xml
<parent>
    <groupId>org.greenbuttonalliance.espi</groupId>
    <artifactId>openespi-parent</artifactId>
    <version>3.5.0</version>
</parent>
```

**Impact:** Centralized dependency management via new `openespi-parent` POM

###2. Java Version

**Change:** Java 21 → Java 25

**Impact on DTOs:**
- ✅ Records fully supported (Java 17+)
- ✅ Pattern matching enhanced
- ✅ Virtual threads available (if needed for performance)

### 3. Spring Boot Version

**Change:** Spring Boot 3.5.0 → Spring Boot 4.0.1

**Impact on DTOs:**
- 🔄 Test annotations relocated (see below)
- 🔄 Testing dependencies split into granular modules
- ⚠️ Unknown Jackson XML changes (need investigation)

### 4. Spring Boot 4.0 Testing Changes

**Annotation Deprecations:**
```java
// DEPRECATED in Spring Boot 4.0
@MockBean  → @MockitoBean
@SpyBean   → @MockitoSpyBean
```

**Package Relocations:**
```java
// OLD (Spring Boot 3.5)
org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest

// NEW (Spring Boot 4.0)
org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
```

**Dependency Splits:**
```xml
<!-- OLD: Single test dependency -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
</dependency>

<!-- NEW: Granular test dependencies -->
<dependency>
    <artifactId>spring-boot-starter-webmvc-test</artifactId>
</dependency>
<dependency>
    <artifactId>spring-boot-starter-data-jpa-test</artifactId>
</dependency>
<dependency>
    <artifactId>spring-boot-starter-validation-test</artifactId>
</dependency>
<dependency>
    <artifactId>spring-boot-starter-restclient-test</artifactId>
</dependency>
```

### 5. TestContainers Dependency Change

```xml
<!-- OLD -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
</dependency>

<!-- NEW -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers-junit-jupiter</artifactId>
</dependency>
```

---

## Impact on Phase 1 DTO Decision

### Critical Question: Jackson XML in Spring Boot 4.0?

**Unknown Factor:** Does Spring Boot 4.0 change Jackson XML behavior?

**What We Need to Know:**
1. Is `jackson-dataformat-xml` still the recommended dependency?
2. Have Jackson XML annotations changed?
3. Are there new Spring Boot 4.0 XML serialization features?
4. Does Spring Boot 4.0 favor Jackson over JAXB?

**Current Status:**
- ⚠️ Our Phase 1 prototypes built against Spring Boot 3.5.0
- ⚠️ If PR #50 merges, we'll need to verify both approaches work in Spring Boot 4.0

### Impact on `DTO_APPROACH_COMPARISON.md`

**Sections Requiring Updates:**

1. **Version References**
   - Current doc references Spring Boot 3.5/Java 21
   - Need to update to Spring Boot 4.0/Java 25

2. **Test Dependency Examples**
   - Examples show old `spring-boot-starter-test` dependency
   - Need to show new granular dependencies

3. **Jackson XML Testing**
   - Our `TimeConfigurationDtoJacksonTest` uses old testing structure
   - May need Spring Boot 4.0 test annotations

### Impact on `MULTI_PHASE_SCHEMA_COMPLIANCE_PLAN.md`

**Affected Sections:**

1. **Technology Stack** (lines 20-30)
   ```markdown
   - **Java**: 21 → 25
   - **Spring Boot**: 3.5.0 → 4.0.1
   - **Jakarta EE**: 9+ → 11
   ```

2. **Phase 1-26 Implementation Notes**
   - All test code needs Spring Boot 4.0 annotations
   - Mapper tests need updated `@DataJpaTest` imports
   - XML marshalling tests need updated `@WebMvcTest` imports (if used)

3. **Testing Requirements**
   - Add note about Spring Boot 4.0 test dependency splits
   - Update test examples to use new annotations

### Impact on `DTO_PATTERN_GUIDE.md`

**Sections Requiring Updates:**

1. **Testing Pattern Examples**
   - Update `@MockBean` → `@MockitoBean`
   - Show correct Spring Boot 4.0 test dependency structure

2. **JAXB vs Jackson XML Guidance**
   - Add Spring Boot 4.0 specific notes
   - Document any Spring Boot 4.0 XML serialization preferences

3. **Version-Specific Warnings**
   - Add callout for Spring Boot 3.x vs 4.x differences

---

## Recommendations

### Option 1: Wait for PR #50 to Merge (🔴 RECOMMENDED - DO NOT PROCEED)

**Rationale:**
- Spring Boot 4.0 may change Jackson XML behavior
- Our Phase 1 prototypes untested against Spring Boot 4.0
- Risk of rework if Jackson XML API changes
- Java 25 may have unknown impacts on records/JAXB

**Action Plan:**
1. ⏸️ **PAUSE Phase 1 DTO decision until PR #50 status is clear**
2. Monitor PR #50 for merge or closure
3. If PR #50 merges:
   - Re-test both JAXB and Jackson XML prototypes
   - Update comparison document for Spring Boot 4.0
   - Verify all test annotations work
4. If PR #50 closes:
   - Proceed with Phase 1 on Spring Boot 3.5/Java 21
   - Document future Spring Boot 4.0 migration considerations

### Option 2: Help Fix PR #50 (⚠️ HIGH RISK - SCOPE CREEP)

**Rationale:**
- PR #50 is currently failing in `openespi-common`
- Test failures likely related to Spring Boot 4.0 changes
- Fixing it would unblock DTO decision

**Risks:**
- Takes time away from Phase 1 work
- May uncover more Spring Boot 4.0 issues
- Not our original scope

**Action Plan:**
1. Investigate `openespi-common` test failures
2. Fix compatibility issues with Spring Boot 4.0
3. Re-run all Phase 1 prototypes
4. Update DTO comparison for Spring Boot 4.0

### Option 3: Proceed with Phase 1 on Current Main (⚠️ REWORK RISK)

**Rationale:**
- Main branch is stable (Spring Boot 3.5/Java 21)
- Can complete Phase 1 now
- Defer Spring Boot 4.0 migration

**Risks:**
- If PR #50 merges soon, we'll need to:
  - Re-test all Phase 1 code
  - Update documentation
  - Potentially refactor if Spring Boot 4.0 breaks anything

**Action Plan:**
1. Make DTO decision based on Spring Boot 3.5
2. Complete Phase 1 implementation
3. Add TODO: Verify Spring Boot 4.0 compatibility
4. When PR #50 merges, run full Phase 1 test suite again

---

## Decision Matrix

| Factor | Wait for PR #50 | Help Fix PR #50 | Proceed on Main |
|--------|----------------|-----------------|-----------------|
| **Risk of Rework** | ✅ Low | ✅ Low | ❌ High |
| **Time to Decision** | ❌ Unknown | ⚠️ Days | ✅ Immediate |
| **Technical Certainty** | ✅ High | ✅ High | ⚠️ Medium |
| **Scope Alignment** | ✅ In scope | ❌ Out of scope | ✅ In scope |
| **User Satisfaction** | ⚠️ Delay frustration | ⚠️ Scope creep | ⚠️ Potential rework |

---

## Specific Technical Unknowns

### 1. Jackson XML in Spring Boot 4.0

**Question:** Does Spring Boot 4.0 have native Jackson XML autoconfiguration?

**Investigation Needed:**
```java
// Check if this still works in Spring Boot 4.0
@Autowired
private XmlMapper xmlMapper; // Auto-configured?
```

### 2. JAXB in Spring Boot 4.0/Java 25

**Question:** Is Jakarta XML Binding fully compatible with Java 25?

**Investigation Needed:**
- Test JAXB context creation
- Verify marshaller/unmarshaller behavior
- Check for deprecations

### 3. MapStruct with Spring Boot 4.0

**Question:** Are there MapStruct version requirements for Spring Boot 4.0?

**Current Version:** MapStruct 1.6.0

**Investigation Needed:**
- Verify mapper generation works
- Check for Spring Boot 4.0 integration issues

---

## Next Steps

### Immediate Actions

1. **Consult with Team:**
   - Is PR #50 expected to merge soon?
   - Is someone actively fixing the test failures?
   - What is priority: Phase 1 completion vs Spring Boot 4.0 upgrade?

2. **If Waiting for PR #50:**
   - Document this blocker in Phase 1 status
   - Update timeline estimates
   - Consider working on other non-DTO Phase 1 tasks (if any)

3. **If Proceeding on Main:**
   - Add caveat to DTO comparison document
   - Note Spring Boot 4.0 revalidation required
   - Proceed with team decision

### Documentation Updates Required (When PR #50 Status is Clear)

**If PR #50 Merges:**
- [ ] Update `DTO_APPROACH_COMPARISON.md` with Spring Boot 4.0 references
- [ ] Update `MULTI_PHASE_SCHEMA_COMPLIANCE_PLAN.md` technology stack
- [ ] Update `DTO_PATTERN_GUIDE.md` with Spring Boot 4.0 test patterns
- [ ] Re-test `TimeConfigurationDto` (JAXB)
- [ ] Re-test `TimeConfigurationDtoJackson` (Jackson XML)
- [ ] Verify all 11 JAXB tests pass
- [ ] Verify all 10 Jackson XML tests pass
- [ ] Update todo list with Spring Boot 4.0 validation task

**If PR #50 Closes:**
- [ ] Add Spring Boot 4.0 migration note to DTO pattern guide
- [ ] Document known Spring Boot 3.5 → 4.0 breaking changes for DTOs
- [ ] Proceed with Phase 1 as planned

---

## Conclusion

**PR #50 creates a critical dependency for Phase 1 DTO decision.**

**Recommended Action:** ⏸️ **PAUSE and consult with team on PR #50 timeline.**

The Spring Boot 3.5 → 4.0 upgrade is significant enough that any DTO architectural decision made now could require validation and potential refactoring. Given that this affects all 26 phases of the schema compliance plan, it's prudent to wait for clarity on PR #50 before finalizing the JAXB vs Jackson XML decision.

**Key Question for Team:**
*"When is PR #50 expected to be resolved (merged or closed), and should we wait for it before completing Phase 1?"*

---

**Author:** Claude Sonnet 4.5
**Status:** Analysis Complete - Awaiting Team Direction
**Related Files:**
- `DTO_APPROACH_COMPARISON.md`
- `MULTI_PHASE_SCHEMA_COMPLIANCE_PLAN.md`
- `DTO_PATTERN_GUIDE.md`
- PR #50: https://github.com/GreenButtonAlliance/OpenESPI-GreenButton-Java/pull/50
