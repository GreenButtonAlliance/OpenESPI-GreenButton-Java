# Phase 2: Comprehensive Mapper Analysis

**Date**: 2025-12-31
**Branch**: `feature/schema-compliance-phase-2-baseline`

---

## Executive Summary

**CRITICAL FINDING**: All 14 mappers use `@Mapping(target = "selfLink", ignore = true)` pattern.
- Links are NOT being mapped by MapStruct
- Links are handled elsewhere (likely DtoExportService)
- Refactoring impact is **removal only** - no logic changes needed

---

## All Mappers with selfLink/upLink (14 total, 64 occurrences)

| Mapper | Occurrences | Entity Extends IO? | DTO Has Links? | Needs Refactoring? |
|--------|-------------|-------------------|----------------|-------------------|
| **IntervalReadingMapper** | 6 | ✅ YES (wrong) | ✅ YES (wrong) | **YES - Remove links** |
| **ReadingQualityMapper** | 2 | ✅ YES (wrong) | ✅ YES (wrong) | **YES - Remove links** |
| **AggregatedNodeRefMapper** | 4 | ✅ YES (wrong) | ❌ NO (correct) | **YES - Entity only** |
| CustomerAccountMapper | 6 | ✅ YES (correct) | ✅ YES (correct) | NO - Keep as-is |
| CustomerAgreementMapper | 6 | ✅ YES (correct) | ✅ YES (correct) | NO - Keep as-is |
| CustomerMapper | 4 | ✅ YES (correct) | ✅ YES (correct) | NO - Keep as-is |
| ApplicationInformationMapper | 4 | ✅ YES (correct) | ✅ YES (correct) | NO - Keep as-is |
| AuthorizationMapper | 4 | ✅ YES (correct) | ✅ YES (correct) | NO - Keep as-is |
| ElectricPowerQualitySummaryMapper | 4 | ✅ YES (correct) | ✅ YES (correct) | NO - Keep as-is |
| IntervalBlockMapper | 6 | ✅ YES (correct) | ✅ YES (correct) | NO - Keep as-is |
| MeterReadingMapper | 4 | ✅ YES (correct) | ✅ YES (correct) | NO - Keep as-is |
| TimeConfigurationMapper | 4 | ✅ YES (correct) | ✅ YES (correct) | NO - Keep as-is |
| UsagePointMapper | 4 | ✅ YES (correct) | ✅ YES (correct) | NO - Keep as-is |
| UsageSummaryMapper | 6 | ✅ YES (correct) | ✅ YES (correct) | NO - Keep as-is |
| **TOTALS** | **64** | 14 entities | 11 DTOs | **3 mappers need changes** |

*IO = IdentifiedObject*

---

## Mapping Pattern Analysis

### Universal Pattern: ALL Mappers Ignore Links

**Every single mapper** uses this exact pattern:

```java
@Mapping(target = "relatedLinks", ignore = true)  // Links handled separately
@Mapping(target = "selfLink", ignore = true)
@Mapping(target = "upLink", ignore = true)
```

**This appears in**:
- `toDto()` method (entity → DTO mapping)
- `toEntity()` method (DTO → entity mapping)
- `updateEntity()` method (DTO → entity update)

### Why This Matters

1. **Links are NOT mapped by MapStruct** - they're populated somewhere else
2. **Removing fields won't break mappers** - they already ignore these fields
3. **Refactoring is simpler** - just remove the @Mapping annotations when fields don't exist

---

## Detailed Findings by Entity Category

### Category 1: Entities Needing Refactoring (3 mappers)

#### 1. IntervalReadingMapper (6 occurrences)

**Current State**:
- Entity: `IntervalReadingEntity extends IdentifiedObject` ❌ WRONG
- DTO: `IntervalReadingDto` has selfLink/upLink fields ❌ WRONG
- Mapper: Ignores links with `@Mapping(target = "selfLink", ignore = true)` ✅ CORRECT

**Refactoring Required**:
1. Remove `extends IdentifiedObject` from IntervalReadingEntity
2. Remove selfLink/upLink/relatedLinks fields from IntervalReadingDto
3. Remove these @Mapping annotations from IntervalReadingMapper:
   - Line 50: `@Mapping(target = "relatedLinks", ignore = true)`
   - Line 51: `@Mapping(target = "selfLink", ignore = true)`
   - Line 52: `@Mapping(target = "upLink", ignore = true)`
   - Line 76: `@Mapping(target = "upLink", ignore = true)`
   - Line 77: `@Mapping(target = "selfLink", ignore = true)`
   - Line 78: `@Mapping(target = "relatedLinks", ignore = true)`
   - Line 101: `@Mapping(target = "upLink", ignore = true)`
   - Line 102: `@Mapping(target = "selfLink", ignore = true)`
   - Line 103: `@Mapping(target = "relatedLinks", ignore = true)`

**Impact**: Low - mapper already ignores these fields

#### 2. ReadingQualityMapper (2 occurrences)

**Current State**:
- Entity: `ReadingQualityEntity extends IdentifiedObject` ❌ WRONG
- DTO: `ReadingQualityDto` has selfLink/upLink fields ❌ WRONG
- Mapper: Ignores links ✅ CORRECT

**Refactoring Required**:
1. Remove `extends IdentifiedObject` from ReadingQualityEntity
2. Remove selfLink/upLink/relatedLinks from ReadingQualityDto
3. Remove @Mapping ignore annotations from ReadingQualityMapper

**Impact**: Low - mapper already ignores these fields

#### 3. AggregatedNodeRefMapper (4 occurrences)

**Current State**:
- Entity: `AggregatedNodeRefEntity extends IdentifiedObject` ❌ WRONG
- DTO: `AggregatedNodeRefDto` - NO selfLink/upLink ✅ ALREADY CORRECT!
- Mapper: Ignores links ✅ CORRECT (bridges entity-DTO mismatch)

**Refactoring Required**:
1. Remove `extends IdentifiedObject` from AggregatedNodeRefEntity
2. Remove @Mapping ignore annotations from AggregatedNodeRefMapper
3. DTO already correct - no changes needed

**Impact**: Low - mapper already handles mismatch

**Note**: This entity is in a "partially compliant" state - DTO is correct, entity is wrong

---

### Category 2: Entities Already Correct (11 mappers)

These mappers handle entities that SHOULD extend IdentifiedObject and SHOULD have selfLink/upLink:

- CustomerAccountMapper (6)
- CustomerAgreementMapper (6)
- CustomerMapper (4)
- ApplicationInformationMapper (4)
- AuthorizationMapper (4)
- ElectricPowerQualitySummaryMapper (4)
- IntervalBlockMapper (6)
- MeterReadingMapper (4)
- TimeConfigurationMapper (4)
- UsagePointMapper (4)
- UsageSummaryMapper (6)

**Current State**: All correct - no changes needed

**Why They Ignore Links**: Even for entities that SHOULD have links, the mappers don't populate them. Links are likely set by a separate service (DtoExportService) when creating Atom feeds.

---

## Entities WITHOUT Mappers (3 entities)

These entities from our refactoring list have NO mapper files:

| Entity | Why No Mapper | Refactoring Impact |
|--------|---------------|-------------------|
| LineItem | Embedded in UsageSummary | Entity-only refactoring |
| BatchList | Special case, no DTO | Entity-only refactoring |
| PhoneNumber | Embedded in Customer | Entity-only refactoring |

**Impact**: These entities need entity refactoring only, no mapper changes

---

## Key Insights

### 1. Consistent Pattern Across All Mappers ✅
- Every mapper ignores selfLink, upLink, relatedLinks
- Comment: "Links handled separately"
- This is intentional design - links populated elsewhere

### 2. Links Populated by Separate Service 🔍
- MapStruct mappers don't touch links
- Likely handled by DtoExportService or similar
- Need to review DtoExportService next

### 3. Refactoring is Low Risk 🛡️
- Mappers already ignore these fields
- Removing fields = removing unnecessary @Mapping annotations
- No logic changes required in mappers
- MapStruct will recompile cleanly

### 4. Partially Compliant Entity Found 🔎
- AggregatedNodeRef: DTO correct, entity wrong
- This proves DTO-only compliance is already working
- Just need to fix the entity side

### 5. Total Mapper Changes Minimal ⚡
- Only 3 mappers need updates
- Changes are deletions only (remove @Mapping annotations)
- No new code to write

---

## Refactoring Checklist Summary

### Mappers Requiring Changes (3)

**IntervalReadingMapper**:
- [ ] Remove 3 @Mapping annotations from toDto() method
- [ ] Remove 3 @Mapping annotations from toEntity() method
- [ ] Remove 3 @Mapping annotations from updateEntity() method

**ReadingQualityMapper**:
- [ ] Remove @Mapping annotations for links (TBD - need to count)

**AggregatedNodeRefMapper**:
- [ ] Remove 3 @Mapping annotations from toEntity() method
- [ ] Remove 3 @Mapping annotations from updateEntity() method
- [ ] Note: toDto() might not need changes if DTO already lacks fields

### Next Steps

1. ✅ Mapper analysis complete
2. ⏭️ Review DtoExportService to understand link population
3. ⏭️ Review repository tests for the 6 entities
4. ⏭️ Create detailed per-entity refactoring checklists

---

## Conclusion

The mapper analysis reveals **excellent news**:
- All mappers already ignore selfLink/upLink
- Only 3 mappers need changes (deletion of @Mapping annotations)
- No complex mapping logic to refactor
- Low risk, high confidence refactoring path

The refactoring will be **simpler than anticipated** - primarily removing unnecessary code rather than rewriting logic.
