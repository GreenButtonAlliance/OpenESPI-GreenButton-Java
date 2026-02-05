# Issue #101 Implementation Strategy
## Minimizing Team Impact During Schema Compliance Verification

**Goal**: Complete ESPI 4.0 schema compliance verification while minimizing disruption to active development on the main branch.

---

## Strategy Overview

### The Challenge

- **102 total tasks** across 4 phases
- **37 enumeration tasks** in Phase 0 that touch many files
- **File relocations** create merge conflict risks
- **Active team** continuing feature development
- **Long-lived branches** = integration hell

### The Solution: Incremental Delivery with Parallel Change Pattern

Use a **"Expand-Migrate-Contract"** approach with small, frequent merges to main.

---

## Recommended Approach: Phased Incremental Delivery

### Core Principles

1. **Small, frequent merges** - No branch lives longer than 3-5 days
2. **Backward compatibility** - Old code continues working during transition
3. **Feature flags** - Enable/disable new code paths
4. **Parallel changes** - Support both old and new simultaneously
5. **Clear ownership** - Explicit assignments to avoid conflicts
6. **Communication** - Daily standups on refactoring progress

---

## Phase 0: Enumeration Migration (Highest Risk)

This phase has the highest conflict potential due to file moves and renames.

### Option A: Incremental Parallel Change (RECOMMENDED)

**Timeline**: 3-4 weeks (slower but safer)

#### Week 1: Create New Structure (Expand)

**Branch**: `feature/enum-structure-setup`
**Duration**: 2-3 days
**Conflicts**: Minimal - only adding files

```
Tasks:
1. Create domain/usage/enums/ directory
2. Implement NEW enums only (don't move existing yet)
   - Priority: High-priority missing enums needed by entities
   - AccumulationKind, CommodityKind, DataQualifierKind, etc.
3. Add ServiceKind (NEW) alongside ServiceCategory (OLD)
4. Keep both versions working
```

**Merge to main**: After tests pass

#### Week 2: Implement Remaining New Enums

**Branch**: `feature/enum-implementations-batch-1`
**Duration**: 3-5 days
**Conflicts**: Minimal - only adding files

```
Tasks:
1. Implement remaining missing enums in correct locations
2. All new enums, no moves yet
3. Add comprehensive tests
```

**Merge to main**: After tests pass

#### Week 3: Parallel Migration (Migrate)

**Branch**: `feature/enum-usage-migration`
**Duration**: 3-5 days
**Conflicts**: Medium - changing entity imports

```
Tasks:
1. Update NEW entities to use new enum locations
2. Keep OLD entities using old locations (backward compatible)
3. Add deprecation warnings to old enum locations
4. No file deletions or moves yet
```

**Merge to main**: After tests pass, coordinate with team

#### Week 4: Final Cleanup (Contract)

**Branch**: `feature/enum-cleanup`
**Duration**: 2-3 days
**Conflicts**: High - deleting/moving files

**Prerequisites**:
- Coordinate with team - announce "enum migration freeze day"
- All team members merge their current work
- No new features touching enums for 24-48 hours

```
Tasks:
1. Move old enums to new locations
2. Update all remaining references
3. Delete deprecated locations
4. Remove ServiceCategory, keep only ServiceKind
```

**Merge to main**: Coordinated merge, immediate announcement

**Advantages:**
- ✅ Low conflict risk - most work is additive
- ✅ Backward compatible during migration
- ✅ Team can continue working with minimal disruption
- ✅ Easy to test incrementally
- ✅ Can abandon/rollback partially if needed

**Disadvantages:**
- ⏱️ Takes longer (3-4 weeks vs 1-2 weeks)
- 🔄 Temporary duplication of code

---

### Option B: Feature Branch with Frequent Rebasing (Higher Risk)

**Timeline**: 1-2 weeks (faster but riskier)

**Branch**: `feature/issue-101-phase-0-enums`
**Strategy**: Daily rebases from main

```
Day 1-2: Create structure + implement new enums
Day 3-4: Move existing enums
Day 5-7: Update all references
Day 8-9: Testing and fixes
Day 10: Coordinated merge to main
```

**Required:**
- Daily rebase from main (resolve conflicts daily, not at end)
- Clear team communication: "Major enum refactor in progress"
- Feature freeze for enum-touching code for final 2 days
- Dedicated "integration day" for final merge

**Advantages:**
- ⚡ Faster completion
- 🎯 Atomic change - everything updated at once

**Disadvantages:**
- ⚠️ High conflict risk
- 🚫 Requires team coordination/freeze
- 😰 Stressful final merge
- 🐛 Higher bug risk

---

### Option C: Hybrid Approach (BEST BALANCE)

Combine both strategies:

**Weeks 1-2: Expand (Additive Changes Only)**
- Create new directories
- Implement all new enums
- Add ServiceKind alongside ServiceCategory
- Merge to main frequently (every 2-3 days)

**Week 3: Coordinate Migration Window**
- Announce 1 week in advance: "Enum migration week - minimize enum changes"
- Create `feature/enum-migration` branch
- Perform moves and renames in one focused effort
- Daily rebases
- Coordinate final merge

**Week 4: Buffer/Cleanup**
- Fix any issues from migration
- Update documentation
- Verify all tests passing

---

## Phases 1-3: Supporting Classes and Entities (Lower Risk)

These phases have lower conflict potential because they modify existing files rather than moving them.

### Parallel Team Strategy

**Divide work by domain to minimize conflicts:**

#### Team Assignment

**Developer A: Common/Shared Types (Phase 1 - Part 1)**
- Branch: `feature/verify-common-embeddables`
- Tasks: 1.1-1.6 (RationalNumber, DateTimeInterval, etc.)
- Duration: 1 week
- Conflicts: Low

**Developer B: Customer Supporting Types (Phase 1 - Part 2)**
- Branch: `feature/verify-customer-types`
- Tasks: 1.15-1.36 (Customer embeddables, enums, base classes)
- Duration: 1.5 weeks
- Conflicts: Low (different domain)

**Developer C: Usage Domain Entities - Core (Phase 2 - Part 1)**
- Branch: `feature/verify-usage-core`
- Tasks: 2.1-2.6 (UsagePoint, MeterReading, IntervalBlock, ReadingType)
- Duration: 1.5 weeks
- Conflicts: Low (different domain)

**Developer A: Usage Domain Entities - Summaries (Phase 2 - Part 2)**
- Branch: `feature/verify-usage-summaries`
- Tasks: 2.7-2.13 (Summaries, config, customer resources)
- Duration: 1 week
- Conflicts: Low

**Developer B: Customer Domain Entities (Phase 3)**
- Branch: `feature/verify-customer-entities`
- Tasks: 3.1-3.10 (All customer entities)
- Duration: 1.5 weeks
- Conflicts: Low (different domain)

**Developer C: Usage Domain Entities - Misc (Phase 2 - Part 3)**
- Branch: `feature/verify-usage-misc`
- Tasks: 2.14-2.19 (Batch, references)
- Duration: 1 week
- Conflicts: Low

### Merge Cadence for Phases 1-3

**Recommended**: Merge every 3-5 entities verified

```
Example for Developer A (Common embeddables):
- Day 1-2: Verify RationalNumber, DateTimeInterval
- Day 2: Merge PR #1 to main
- Day 3-4: Verify SummaryMeasurement, LinkType
- Day 4: Merge PR #2 to main
- Day 5-6: Verify ReadingInterharmonic, BillingChargeSource
- Day 6: Merge PR #3 to main
```

**Benefits:**
- Small PRs are easier to review
- Conflicts are minimized
- Progress is visible
- Easy to rollback if needed
- Team can continue working on main

---

## Conflict Prevention Strategies

### 1. Clear Ownership and Communication

**Daily Standup Topics:**
- "I'm working on UsagePoint entity today"
- "I'll be modifying ReadingType enum mappings"
- "I need to update TimeConfiguration - anyone else touching it?"

**Use GitHub Issue Assignments:**
```
Issue #101: Parent tracking issue
├─ Task #101.1: @developer-a - Verify RationalNumber
├─ Task #101.2: @developer-b - Verify CustomerKind enum
├─ Task #101.3: @developer-c - Verify UsagePoint entity
```

### 2. Entity-Level Locking

**Claim entities before starting work:**

```markdown
# In ISSUE_101_IMPLEMENTATION_PLAN.md, add status column:

| Task | Entity | Assigned | Status | Branch |
|------|--------|----------|--------|--------|
| 2.1 | UsagePointEntity | @alice | In Progress | feature/verify-usagepoint |
| 2.2 | MeterReadingEntity | @bob | Not Started | - |
| 2.3 | IntervalBlockEntity | - | Available | - |
```

### 3. Rebase Daily (Not at End)

**For longer-lived branches:**

```bash
# Every morning, before starting work:
git checkout feature/my-verification-branch
git fetch origin
git rebase origin/main

# Resolve conflicts immediately while context is fresh
# Much easier than resolving 10 days of conflicts at once
```

### 4. Communicate Breaking Changes

**Use team chat/Slack:**

```
@team: I'm about to merge PR #145 which renames ServiceCategory to ServiceKind.
If you have any branches touching ServiceCategory, please:
1. Merge your work to main before EOD today, OR
2. Wait until tomorrow to rebase after my merge
```

### 5. Use Draft PRs for Visibility

**Create draft PRs early:**

```
Draft PR #145: [WIP] Verify UsagePoint entity compliance

Shows:
- What you're working on
- What files you're touching
- Prevents duplicate work
- Enables early feedback
```

---

## Recommended Timeline with Team Coordination

### Month 1: Phase 0 - Enumeration Migration

| Week | Focus | Merges | Team Impact |
|------|-------|--------|-------------|
| 1 | Create structure + new enums | 2-3 | Low - additive only |
| 2 | Implement remaining new enums | 2-3 | Low - additive only |
| 3 | **MIGRATION WEEK** - Move/rename | 1 big merge | **Medium** - coordinate |
| 4 | Buffer, testing, fixes | As needed | Low - stabilization |

**Team Communication:**
- Week 1: "Starting enum migration - new enums being added"
- Week 2: "More enums coming - avoid creating new enum dependencies"
- Week 3: **"MIGRATION WEEK - minimize enum changes, coordinate with @team"**
- Week 4: "Enum migration complete - update your branches"

### Month 2-3: Phases 1-3 - Parallel Verification

| Week | Developer A | Developer B | Developer C | Merges/Week |
|------|------------|------------|------------|-------------|
| 5 | Common embeddables | Customer types | Usage core | 6-9 |
| 6 | Common embeddables | Customer types | Usage core | 6-9 |
| 7 | Usage summaries | Customer entities | Usage misc | 6-9 |
| 8 | Usage summaries | Customer entities | Usage misc | 6-9 |

**Team Impact**: Low - different developers working on different domains

---

## Handling Conflicts When They Occur

### Conflict Prevention Checklist

Before starting work on an entity:

```bash
# 1. Check if anyone else is working on it
gh issue list --label "Issue-101" --assignee "@me"

# 2. Pull latest main
git checkout main
git pull origin main

# 3. Create feature branch
git checkout -b feature/verify-usage-point

# 4. Announce you're starting
# Post in team chat: "Starting UsagePoint verification"
```

### During Work

```bash
# Rebase frequently (daily recommended)
git fetch origin
git rebase origin/main

# If conflicts occur, resolve immediately
# Don't let them accumulate
```

### Before Merging

```bash
# Final rebase
git fetch origin
git rebase origin/main

# Run full test suite
mvn clean test

# Create PR
gh pr create --title "Verify UsagePoint entity schema compliance" \
             --body "Fixes #101 (Task 2.1)"
```

---

## Migration Windows for High-Risk Changes

Some changes require coordinated "migration windows" where team agrees to minimize conflicting work.

### Coordinated Migration Windows

**Use for:**
- Phase 0 final week (enum moves/renames)
- Renaming ServiceCategory → ServiceKind
- Moving files between packages
- Major refactorings

**Process:**

**1 Week Before:**
```
Announcement: "Enum migration window scheduled for Week 3, Mon-Wed.
Please avoid creating new enum dependencies this week."
```

**3 Days Before:**
```
Reminder: "Enum migration starts Monday.
Merge any enum-touching work by Friday EOD or wait until Thursday."
```

**Day Of:**
```
Active: "Enum migration in progress - avoid enum changes until Wednesday."
```

**Completion:**
```
Complete: "Enum migration merged! Please rebase your branches.
Migration guide: [link to document]"
```

---

## Testing Strategy During Migration

### Continuous Integration

**Every merge must:**
- ✅ Pass all unit tests
- ✅ Pass all integration tests
- ✅ Pass compilation (no broken imports)
- ✅ Meet code coverage thresholds

### Regression Testing

**After each phase:**
```bash
# Run full regression suite
mvn clean verify -Pintegration-tests

# Check for broken references
mvn dependency:analyze

# Verify no compilation warnings
mvn clean compile -Xlint:all
```

### Migration Validation

**After Phase 0 (enumerations):**
```bash
# Verify all old enum locations deleted
find . -path "*/domain/common/*Kind.java" -type f

# Verify new locations populated
ls -la openespi-common/src/main/java/.../domain/usage/enums/
ls -la openespi-common/src/main/java/.../domain/customer/enums/

# Grep for old imports (should be zero)
grep -r "domain.common.AmiBillingReadyKind" openespi-common/src/
```

---

## Rollback Plan

### If Things Go Wrong

**Small merge issues:**
- Fix forward - create hotfix PR
- Don't revert unless critical

**Major breakage:**

```bash
# Option 1: Revert the merge (immediate)
git revert <merge-commit-sha>
git push origin main

# Option 2: Fix forward (preferred if possible)
# Create emergency fix branch
git checkout -b hotfix/enum-migration-fix
# Fix the issue
# Create PR with [URGENT] tag
```

### Rollback Triggers

**Revert if:**
- ❌ Tests failing on main for > 2 hours
- ❌ Breaks production/demo environment
- ❌ Blocks multiple developers from working

**Fix forward if:**
- ⚠️ Minor test failures
- ⚠️ Documentation issues
- ⚠️ Non-critical bugs

---

## Team Workflow Example

### Scenario: 3 Developers, 8 Weeks

**Developer A (Alice):**
```
Week 1-2: Phase 0 support (new enums)
Week 3: Phase 0 migration (coordinate)
Week 4-5: Phase 1 - Common types
Week 6-7: Phase 2 - Usage summaries
Week 8: Buffer/review
```

**Developer B (Bob):**
```
Week 1-2: Phase 0 support (new enums)
Week 3: Phase 0 migration (coordinate)
Week 4-6: Phase 1 - Customer types
Week 7-8: Phase 3 - Customer entities
```

**Developer C (Carol):**
```
Week 1-2: Continue regular feature work
Week 3: Phase 0 migration (testing support)
Week 4-6: Phase 2 - Usage core entities
Week 7-8: Phase 2 - Usage misc entities
```

**Team Lead:**
```
Week 1-2: Review enum PRs, coordinate
Week 3: Manage migration window
Week 4-8: Review verification PRs, track progress
```

---

## Communication Templates

### Starting a Task

**Slack/Teams Message:**
```
🚀 Starting Task 2.1: Verify UsagePoint entity
📝 Branch: feature/verify-usage-point
⏱️ ETA: 2 days
🔗 Draft PR: #145

Let me know if anyone else is touching UsagePoint!
```

### Merging a Task

**Slack/Teams Message:**
```
✅ Merged: UsagePoint entity verification
🔗 PR #145: https://github.com/.../pull/145
📊 Status: 15/102 tasks complete
⚠️ Action needed: Rebase if you're working on:
   - ServiceCategory references
   - UsagePoint relationships
```

### Migration Window

**Email/Announcement:**
```
Subject: [ACTION REQUIRED] Enum Migration Window - Week 3

Team,

We're performing the enumeration migration (Phase 0) during Week 3 (Feb 10-14).

ACTIONS REQUIRED:
1. Merge any work touching enums by EOD Friday, Feb 7
2. Avoid creating new enum dependencies Feb 10-12
3. Rebase your branches on Feb 13 after migration completes

WHAT'S HAPPENING:
- Moving enums to domain/usage/enums/ and domain/customer/enums/
- Renaming ServiceCategory → ServiceKind
- Updating all entity references

QUESTIONS: Reply to this thread or ping @alice

Thanks for your cooperation!
```

---

## Success Metrics

### During Implementation

**Track weekly:**
- ✅ Tasks completed vs planned
- ⚠️ Merge conflicts encountered
- 🐛 Bugs introduced
- ⏱️ Time blocked by conflicts
- 📊 Test coverage

**Target Metrics:**
- < 2 hours/week blocked by conflicts per developer
- < 5% of merges require conflict resolution
- 100% test pass rate maintained
- Zero production incidents

### After Completion

**Verify:**
- ✅ All 102 tasks completed
- ✅ All tests passing
- ✅ Zero deprecation warnings
- ✅ Documentation updated
- ✅ Team velocity maintained (not slowed)

---

## Recommended Approach: Summary

### For This Team

**RECOMMENDED: Hybrid Approach**

**Phase 0 (Weeks 1-4):**
- Weeks 1-2: Incremental addition (Option A - Expand)
- Week 3: Coordinated migration window (Option B - Migrate)
- Week 4: Cleanup and stabilization (Option A - Contract)

**Phases 1-3 (Weeks 5-12):**
- Parallel work by domain
- Small, frequent merges (every 3-5 entities)
- Daily standups for coordination
- Clear ownership assignments

**Key Success Factors:**
1. ✅ Daily rebases (not weekly)
2. ✅ Small PRs (3-5 entities max)
3. ✅ Clear communication
4. ✅ Coordinated migration windows for risky changes
5. ✅ Entity-level ownership tracking
6. ✅ Continuous testing

**Result:**
- ✅ Minimal team disruption
- ✅ Continuous delivery to main
- ✅ Low conflict risk
- ✅ Easy rollback if needed
- ✅ Visible progress
- ✅ Maintained team velocity

---

## Tools and Automation

### GitHub Project Board

Create project board to track progress:

```
Columns:
- Backlog (102 tasks)
- In Progress (assigned, branch created)
- In Review (PR open)
- Done (merged to main)

Automation:
- Auto-move to "In Progress" when branch created
- Auto-move to "In Review" when PR opened
- Auto-move to "Done" when merged
```

### CI/CD Checks

**Required checks on every PR:**
```yaml
# .github/workflows/issue-101-verification.yml
name: Schema Compliance Verification

on: [pull_request]

jobs:
  verify:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Run tests
        run: mvn clean test
      - name: Check for old enum imports
        run: |
          if grep -r "domain.common.AmiBillingReadyKind" src/; then
            echo "Found deprecated enum imports"
            exit 1
          fi
      - name: Verify enum locations
        run: |
          # Custom script to verify enums in correct directories
          ./scripts/verify-enum-locations.sh
```

### Conflict Detection

**Pre-merge check:**
```bash
# scripts/check-conflicts.sh
#!/bin/bash

# Check if anyone else has modified same files
BRANCH=$(git rev-parse --abbrev-ref HEAD)
BASE="origin/main"

CHANGED_FILES=$(git diff --name-only $BASE..$BRANCH)

for file in $CHANGED_FILES; do
  RECENT_AUTHORS=$(git log $BASE..$BRANCH --format="%an" -- "$file" | sort -u)
  if [ $(echo "$RECENT_AUTHORS" | wc -l) -gt 1 ]; then
    echo "WARNING: Multiple authors modified $file"
    echo "$RECENT_AUTHORS"
  fi
done
```

---

## Conclusion

The **Hybrid Approach** balances speed, safety, and team productivity:

- **Additive changes first** - Low conflict risk
- **Coordinated migration window** - One-time disruption, well-communicated
- **Parallel verification work** - Maximum efficiency
- **Small, frequent merges** - Continuous integration
- **Clear communication** - No surprises

**Timeline**: 10-12 weeks with minimal team disruption

**Result**: ESPI 4.0 schema compliance achieved while maintaining team velocity and morale.
