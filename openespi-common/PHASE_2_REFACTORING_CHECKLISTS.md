# Phase 2: Per-Entity Refactoring Checklists

**Date**: 2025-12-31
**Branch**: `feature/schema-compliance-phase-2-baseline`

---

## Summary

| Entity | Entity File | DTO File | Mapper File | Repository | Tests | Complexity |
|--------|------------|----------|-------------|------------|-------|------------|
| IntervalReading | ✅ | ✅ | ✅ | ❌ None | 1 file | **Medium** |
| ReadingQuality | ✅ | ✅ | ✅ | ❌ None | 0 files | **Low** |
| LineItem | ✅ | ❌ None | ❌ None | ✅ | 2 files | **Low** |
| BatchList | ✅ | ❌ None | ❌ None | ✅ | 1 file | **Low** |
| PhoneNumber | ✅ | ❌ None | ❌ None | ❌ None | 2 files | **Low** |
| AggregatedNodeRef | ✅ | ❌ Already OK | ✅ | ✅ | 1 file | **Low** |

---

## Entity 1: IntervalReading

**Complexity**: Medium (Entity + DTO + Mapper changes)
**Risk**: Low (no tests reference selfLink/upLink)

### Files to Modify

#### 1. Entity File
**File**: `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/domain/usage/IntervalReadingEntity.java`

**Changes**:
- [ ] Remove `extends IdentifiedObject` from class declaration
- [ ] Add direct JPA annotations (@Entity, @Table, etc.)
- [ ] Add `@Id` and `@GeneratedValue` for primary key (UUID id field)
- [ ] Remove any @AttributeOverride annotations for inherited fields
- [ ] Verify all existing fields remain (cost, value, timePeriod, readingQualities, etc.)

**Before**:
```java
public class IntervalReadingEntity extends IdentifiedObject {
```

**After**:
```java
@Entity
@Table(name = "interval_readings")
public class IntervalReadingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ... rest of fields
```

#### 2. DTO File
**File**: `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/dto/usage/IntervalReadingDto.java`

**Changes**:
- [ ] Remove selfLink field
- [ ] Remove upLink field
- [ ] Remove relatedLinks field
- [ ] Remove @XmlElement annotations for these fields
- [ ] Update @XmlType propOrder list (remove these 3 fields)
- [ ] Remove getter methods for these 3 fields
- [ ] Verify all ESPI-spec fields remain (cost, value, timePeriod, readingQualities, etc.)

#### 3. Mapper File
**File**: `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/mapper/usage/IntervalReadingMapper.java`

**Changes**:
- [ ] Remove `@Mapping(target = "selfLink", ignore = true)` from toDto() method (line 51)
- [ ] Remove `@Mapping(target = "upLink", ignore = true)` from toDto() method (line 52)
- [ ] Remove `@Mapping(target = "relatedLinks", ignore = true)` from toDto() method (line 50)
- [ ] Remove `@Mapping(target = "selfLink", ignore = true)` from toEntity() method (line 77)
- [ ] Remove `@Mapping(target = "upLink", ignore = true)` from toEntity() method (line 76)
- [ ] Remove `@Mapping(target = "relatedLinks", ignore = true)` from toEntity() method (line 78)
- [ ] Remove same 3 annotations from updateEntity() method (lines 101-103)
- [ ] Total: Remove 9 @Mapping annotations

#### 4. Repository Tests
**File**: None - IntervalReading has no repository (embedded entity)

**Test Files Using IntervalReading** (1):
- `IntervalBlockRepositoryTest.java` - May create IntervalReading objects
  - [ ] Review test data builders
  - [ ] Verify no selfLink/upLink assertions (already confirmed: 0 references)

---

## Entity 2: ReadingQuality

**Complexity**: Low (Entity + DTO + Mapper, no repository, no tests)
**Risk**: Very Low

### Files to Modify

#### 1. Entity File
**File**: `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/domain/usage/ReadingQualityEntity.java`

**Changes**:
- [ ] Remove `extends IdentifiedObject` from class declaration
- [ ] Add @Entity, @Table annotations
- [ ] Add @Id and @GeneratedValue for UUID id field
- [ ] Verify all ESPI fields remain (quality)

#### 2. DTO File
**File**: `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/dto/usage/ReadingQualityDto.java`

**Changes**:
- [ ] Remove selfLink, upLink, relatedLinks fields
- [ ] Remove @XmlElement annotations for these fields
- [ ] Update @XmlType propOrder
- [ ] Remove getter methods

#### 3. Mapper File
**File**: `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/mapper/usage/ReadingQualityMapper.java`

**Changes**:
- [ ] Remove @Mapping ignore annotations for selfLink, upLink, relatedLinks
- [ ] Likely 2-6 annotations to remove (need to count exact lines)

#### 4. Tests
**Files**: None - 0 test files reference ReadingQuality

---

## Entity 3: LineItem

**Complexity**: Low (Entity only, no DTO, no mapper)
**Risk**: Low

### Files to Modify

#### 1. Entity File
**File**: `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/domain/usage/LineItemEntity.java`

**Changes**:
- [ ] Remove `extends IdentifiedObject` from class declaration
- [ ] Add @Entity, @Table annotations (if not already present)
- [ ] Add @Id and @GeneratedValue for UUID id field
- [ ] Verify all ESPI fields remain

#### 2. DTO File
**File**: None - LineItem is embedded, no standalone DTO

**Impact**: None

#### 3. Mapper File
**File**: None - No LineItemMapper exists

**Impact**: None - LineItem is mapped as part of UsageSummaryMapper

**Check**:
- [ ] Review UsageSummaryMapper to ensure LineItem mapping is correct
- [ ] Verify LineItem is mapped as a collection field, not as IdentifiedObject

#### 4. Repository & Tests
**Repository**: `LineItemRepository.java` exists

**Test Files** (2):
- `LineItemRepositoryTest.java`
  - [ ] Review for selfLink/upLink usage (already confirmed: 0)
  - [ ] Verify tests still pass after entity changes
- `UsageSummaryRepositoryTest.java`
  - [ ] May use LineItem as embedded collection
  - [ ] Verify cascade operations work correctly

---

## Entity 4: BatchList

**Complexity**: Low (Entity only, no DTO, no mapper)
**Risk**: Low

### Files to Modify

#### 1. Entity File
**File**: `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/domain/usage/BatchListEntity.java`

**Changes**:
- [ ] Remove `extends IdentifiedObject` from class declaration
- [ ] Add @Entity, @Table annotations
- [ ] Add @Id and @GeneratedValue for UUID id field
- [ ] Verify @ElementCollection for batch_list_resources remains correct
- [ ] Verify all ESPI fields remain

**Note**: BatchListEntity uses @ElementCollection for `batch_list_resources` collection

#### 2. DTO File
**File**: None - No BatchListDto exists

**Impact**: None

#### 3. Mapper File
**File**: None - No BatchListMapper exists

**Impact**: None

#### 4. Repository & Tests
**Repository**: `BatchListRepository.java` exists

**Test Files** (1):
- `BatchListRepositoryTest.java`
  - [ ] Review for selfLink/upLink usage (already confirmed: 0)
  - [ ] Verify tests pass after entity changes
  - [ ] Check @ElementCollection tests still work

---

## Entity 5: PhoneNumber

**Complexity**: Low (Entity only, embedded in Customer)
**Risk**: Low

### Files to Modify

#### 1. Entity File
**File**: `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/domain/customer/entity/PhoneNumberEntity.java`

**Changes**:
- [ ] Remove `extends IdentifiedObject` from class declaration
- [ ] Add @Entity, @Table annotations
- [ ] Add @Id and @GeneratedValue for UUID id field
- [ ] Verify Customer relationship remains correct
- [ ] Verify all ESPI fields remain

#### 2. DTO File
**File**: None - PhoneNumber embedded in CustomerDto

**Impact**: None - PhoneNumber mapped as embedded collection within CustomerMapper

#### 3. Mapper File
**File**: None - No standalone PhoneNumberMapper

**Check**:
- [ ] Review CustomerMapper for PhoneNumber collection mapping
- [ ] Verify @ElementCollection mapping works correctly

#### 4. Tests
**No Repository** - PhoneNumber is embedded

**Test Files** (2):
- `ServiceLocationRepositoryTest.java` - May use PhoneNumber
  - [ ] Verify no selfLink/upLink usage
- `BaseRepositoryTest.java` - Base test class
  - [ ] Review if PhoneNumber is used in test data builders

---

## Entity 6: AggregatedNodeRef

**Complexity**: Low (Entity + Mapper only, DTO already correct!)
**Risk**: Very Low

### Files to Modify

#### 1. Entity File
**File**: `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/domain/usage/AggregatedNodeRefEntity.java`

**Changes**:
- [ ] Remove `extends IdentifiedObject` from class declaration
- [ ] Add @Entity, @Table annotations
- [ ] Add @Id and @GeneratedValue for UUID id field
- [ ] Verify PnodeRef relationship remains correct
- [ ] Verify all ESPI fields remain

#### 2. DTO File
**File**: `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/dto/usage/AggregatedNodeRefDto.java`

**Status**: ✅ **ALREADY CORRECT** - No selfLink/upLink fields present

**Changes**: ❌ None needed

#### 3. Mapper File
**File**: `openespi-common/src/main/java/org/greenbuttonalliance/espi/common/mapper/usage/AggregatedNodeRefMapper.java`

**Changes**:
- [ ] Remove `@Mapping(target = "selfLink", ignore = true)` from toEntity() method
- [ ] Remove `@Mapping(target = "upLink", ignore = true)` from toEntity() method
- [ ] Remove `@Mapping(target = "relatedLinks", ignore = true)` from toEntity() method
- [ ] Remove same 3 annotations from updateEntity() method
- [ ] Total: Remove 6 @Mapping annotations
- [ ] Note: toDto() may not need changes since DTO already lacks these fields

#### 4. Repository & Tests
**Repository**: `AggregatedNodeRefRepository.java` exists

**Test Files** (1):
- `AggregatedNodeRefRepositoryTest.java`
  - [ ] Review for selfLink/upLink usage (already confirmed: 0)
  - [ ] Verify tests pass after entity changes

---

## Common Tasks for All Entities

### Entity Changes (All 6)
- [ ] Remove `extends IdentifiedObject`
- [ ] Add @Entity annotation if missing
- [ ] Add @Table annotation with name
- [ ] Add UUID id field with @Id and @GeneratedValue
- [ ] Verify relationships (ManyToOne, OneToMany, etc.) remain intact
- [ ] Run `mvn clean compile` to verify no compilation errors

### DTO Changes (3: IntervalReading, ReadingQuality, StatementRef*)
- [ ] Remove selfLink field
- [ ] Remove upLink field
- [ ] Remove relatedLinks field
- [ ] Remove @XmlElement annotations
- [ ] Update @XmlType propOrder list
- [ ] Remove getter methods
- [ ] Run `mvn clean compile` to verify MapStruct generation

*Note: StatementRef not detailed above but follows same pattern

### Mapper Changes (3: IntervalReading, ReadingQuality, AggregatedNodeRef)
- [ ] Remove @Mapping(target = "selfLink", ignore = true) annotations
- [ ] Remove @Mapping(target = "upLink", ignore = true) annotations
- [ ] Remove @Mapping(target = "relatedLinks", ignore = true) annotations
- [ ] Verify MapStruct compiles cleanly

### Database Migration
- [ ] NO changes needed - tables already exist
- [ ] NO new migrations required
- [ ] related_links tables will remain (removed in separate phase)

### Testing
- [ ] Run full test suite: `mvn test`
- [ ] Verify baseline: 544/545 tests still passing
- [ ] Check specific entity repository tests
- [ ] Run integration tests with TestContainers

---

## Refactoring Order Recommendation

### Phase A: Entity-Only Refactoring (Low Risk)
**Entities with NO DTO/Mapper impact**:
1. **PhoneNumber** (simplest - embedded)
2. **LineItem** (simple - embedded in UsageSummary)
3. **BatchList** (has @ElementCollection to verify)

**Estimated Time**: 30-45 minutes total
**Risk**: Very Low - no DTO/Mapper dependencies

### Phase B: DTO Already Correct (Low Risk)
**Entity where DTO is already compliant**:
4. **AggregatedNodeRef** (DTO correct, just entity + mapper)

**Estimated Time**: 20-30 minutes
**Risk**: Very Low - DTO already done

### Phase C: Full Stack Refactoring (Medium Risk)
**Entities needing Entity + DTO + Mapper**:
5. **ReadingQuality** (simpler - no repository/tests)
6. **IntervalReading** (more complex - used in tests)

**Estimated Time**: 1-2 hours total
**Risk**: Low-Medium - most complete changes

---

## Verification Checklist

After each entity refactoring:
- [ ] `mvn clean compile` succeeds
- [ ] MapStruct generates mappers without warnings
- [ ] Entity repository tests pass
- [ ] Related parent entity tests pass
- [ ] Full test suite still at 544/545 passing

---

## Notes

- **No Flyway migrations needed** - tables exist, we're just removing inheritance
- **No related_links table removal yet** - that's Phase 3 (separate PR)
- **MapStruct will recompile** - verify generated mappers in target/generated-sources
- **Low test impact** - only 7 test files use these entities, 0 reference selfLink/upLink

---

## Success Criteria

✅ All 6 entities no longer extend IdentifiedObject
✅ 3 DTOs no longer have selfLink/upLink/relatedLinks fields
✅ 3 mappers no longer have @Mapping ignore annotations for links
✅ All 544 passing tests still pass
✅ MapStruct compilation succeeds
✅ No runtime errors in affected services
