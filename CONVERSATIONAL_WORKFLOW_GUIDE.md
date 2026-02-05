# Conversational Workflow Guide
## Executing Issue #101 with Manual Model Switching

Since you're working **without API credits**, this guide shows how to execute the plan **conversationally** through Claude Code sessions with appropriate model switching.

---

## Model Switching in Claude Code

### How to Switch Models

```bash
# Start a session with specific model
claude --model opus      # For complex analysis
claude --model sonnet    # For standard work (default)
claude --model haiku     # For simple verification
```

### Current Session Info

To check which model you're using:
```
Ask: "Which model are you?"
Response will indicate: Claude Opus 4.5, Sonnet 4.5, or Haiku
```

---

## Phase 0: Enumeration Migration

### Task 0.1: XSD Schema Analysis
**Model**: Claude Opus 4.5
**Why**: Complex schema structure requires deep understanding
**Duration**: 30-45 minutes

```bash
# Start Opus session
claude --model opus
```

**Conversation:**
```
You: "Please analyze openespi-common/src/main/resources/schema/ESPI_4.0/espi.xsd

Read the file and extract:
1. All simpleType enumerations (name, base type, enumeration values)
2. All BasicTypes (String256, UInt48, etc.) with their restrictions
3. All complexTypes and their field structures
4. Create a comprehensive report

Save to: reports/verification/espi_enumerations.md"

[Claude Opus analyzes and creates report]

You: "Now do the same for customer.xsd and save to reports/verification/customer_enumerations.md"

[Claude Opus analyzes customer.xsd]

You: "Create a summary report at reports/verification/00_SCHEMA_ANALYSIS_SUMMARY.md"

[Claude Opus creates summary]
```

**Commit the work:**
```bash
git checkout -b feature/issue-101-phase-0-schema-analysis
git add reports/verification/
git commit -m "feat(phase-0): Complete ESPI 4.0 schema analysis

Analyzed XSD schemas with Claude Opus 4.5.

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
git push -u origin feature/issue-101-phase-0-schema-analysis
```

**Create PR, get it reviewed and merged.**

---

### Task 0.2-0.20: Generate Missing Enums
**Model**: Claude Sonnet 4.5
**Why**: Code generation is Sonnet's strength
**Duration**: 2-3 hours (with breaks)

#### Phase 0.2: Usage Domain Enums (Batch 1)
```bash
# After Phase 0.1 PR is merged
git checkout main
git pull origin main
git checkout -b feature/issue-101-phase-0.2-usage-enums-batch1

# Start Sonnet session
claude --model sonnet
```

**Conversation Pattern:**

```
You: "Generate the first batch of usage domain enums from espi.xsd:
- AccumulationKind
- CommodityKind
- DataQualifierKind
- FlowDirectionKind
- KindKind

Requirements for each:
1. Read the simpleType definition from espi.xsd
2. Create Java enum with all values
3. Include getValue() and fromValue() methods
4. Add comprehensive Javadoc with value descriptions
5. Use Apache License 2025 header
6. Save to: openespi-common/src/main/java/org/greenbuttonalliance/espi/common/domain/usage/enums/[EnumName].java"

[Claude Sonnet generates all 5 enums]
```

**Commit:**
```bash
git add openespi-common/src/main/java/org/greenbuttonalliance/espi/common/domain/usage/enums/
git commit -m "feat(phase-0.2): Generate usage domain enums batch 1

Added: AccumulationKind, CommodityKind, DataQualifierKind,
FlowDirectionKind, KindKind

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
git push -u origin feature/issue-101-phase-0.2-usage-enums-batch1
```

**Create PR, get reviewed and merged.**

---

#### Phase 0.3: Usage Domain Enums (Batch 2)
```bash
git checkout main
git pull origin main
git checkout -b feature/issue-101-phase-0.3-usage-enums-batch2
```

**Conversation:**
```
You: "Generate the second batch of usage domain enums from espi.xsd:
- MeasurementKind (100+ values)
- PhaseCodeKind
- QualityOfReading
- TimeAttributeKind
- UnitMultiplierKind

Same requirements as before."
```

**Commit and push:**
```bash
git add openespi-common/src/main/java/org/greenbuttonalliance/espi/common/domain/usage/enums/
git commit -m "feat(phase-0.3): Generate usage domain enums batch 2

Added: MeasurementKind, PhaseCodeKind, QualityOfReading,
TimeAttributeKind, UnitMultiplierKind

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
git push -u origin feature/issue-101-phase-0.3-usage-enums-batch2
```

---

#### Phase 0.4: Usage Domain Enums (Batch 3)
```bash
git checkout main
git pull origin main
git checkout -b feature/issue-101-phase-0.4-usage-enums-batch3
```

**Conversation:**
```
You: "Generate the third batch of usage domain enums from espi.xsd:
- UnitSymbolKind (100+ values)
- ServiceKind
- ItemKind
- MacroperiodKind

Same requirements as before."
```

**Commit and push.**

---

#### Phase 0.5: OAuth and Common Enums
```bash
git checkout main
git pull origin main
git checkout -b feature/issue-101-phase-0.5-oauth-common-enums
```

**Conversation:**
```
You: "Generate OAuth and shared enums from espi.xsd:
- GrantType → domain/common/enums/
- TokenType → domain/common/enums/
- OAuthError → domain/common/enums/
- ResponseType → domain/common/enums/
- TokenEndPointMethod → domain/common/enums/
- Currency → domain/common/enums/
- StatusCode → domain/common/enums/

Same requirements, but save to common/enums/ directory."
```

**Commit and push.**

---

#### Phase 0.6: Customer Domain Enums
```bash
git checkout main
git pull origin main
git checkout -b feature/issue-101-phase-0.6-customer-enums
```

**Conversation:**
```
You: "Generate customer domain enums from customer.xsd:
- CustomerKind → domain/customer/enums/
- SupplierKind → domain/customer/enums/
- ServiceKind → domain/customer/enums/
- EnrollmentStatus → domain/customer/enums/
- NotificationMethodKind → domain/customer/enums/
- MeterMultiplierKind → domain/customer/enums/
- RevenueKind → domain/customer/enums/
- ProgramDateKind → domain/customer/enums/
- CRUDOperation → domain/customer/enums/
- MediaType → domain/customer/enums/

Same requirements, save to customer/enums/ directory."
```

**Commit and push.**

---

### Task 0.21-0.37: Verify Generated Enums

#### Phase 0.7: Enum Verification
**Model**: Claude Haiku
**Why**: Simple comparison, cost-effective
**Duration**: 30 minutes

```bash
# After all enum PRs are merged
git checkout main
git pull origin main
git checkout -b feature/issue-101-phase-0.7-enum-verification

# Start Haiku session
claude --model haiku
```

**Conversation:**
```
You: "Verify all enums in domain/usage/enums/ against espi.xsd

For each enum:
1. Read the Java file
2. Read the XSD simpleType definition
3. Check: all values present, correct types, proper methods
4. Report: PASS or FAIL with issues

List results in a table."

[Claude Haiku verifies all enums]

You: "Same for domain/customer/enums/ against customer.xsd"

[Claude Haiku verifies customer enums]

You: "Same for domain/common/enums/ against espi.xsd"

[Claude Haiku verifies common enums]
```

**If all pass:**
```bash
# Create verification report
git add reports/verification/enum_verification_results.md
git commit -m "docs(phase-0.7): Enum verification complete - all pass

Co-Authored-By: Claude Haiku <noreply@anthropic.com>"
git push -u origin feature/issue-101-phase-0.7-enum-verification
```

**If issues found, create fix branch:**
```bash
git checkout -b feature/issue-101-phase-0.7-enum-fixes

# Switch back to Sonnet to fix
claude --model sonnet
```

**After fixes:**
```bash
git add openespi-common/src/main/java/.../enums/
git commit -m "fix(phase-0.7): Correct enum discrepancies

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
git push -u origin feature/issue-101-phase-0.7-enum-fixes
```

---

## Phase 1: Supporting Classes

### Common Embeddables (Tasks 1.1-1.6)
**Model**: Claude Sonnet 4.5
**Why**: Standard verification and fixes

```bash
git checkout main
git pull origin main
git checkout -b feature/issue-101-phase-1-common-embeddables

claude --model sonnet
```

**Conversation pattern:**
```
You: "Verify RationalNumber embeddable against espi.xsd

1. Find the RationalNumber complexType in XSD
2. Read openespi-common/.../common/RationalNumber.java
3. Compare field types, lengths, nullability
4. Report discrepancies
5. If issues found, generate fixes"

[Claude Sonnet verifies and reports]

You: "Apply the fixes and update the file"

[Claude Sonnet fixes the issues]

You: "Next: Verify DateTimeInterval"

[Continue for all 6 embeddables...]
```

**Commit after 3-5 classes:**
```bash
git add openespi-common/src/main/java/.../common/
git commit -m "fix(phase-1): Verify RationalNumber, DateTimeInterval, SummaryMeasurement

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

### Customer Supporting Types (Tasks 1.15-1.36)
**Model**: Claude Sonnet 4.5

```bash
git checkout -b feature/issue-101-phase-1-customer-types

claude --model sonnet
```

**Same pattern as above.**

---

## Phase 2: Usage Domain Entities

### Core Entities (Tasks 2.1-2.6)
**Model**: Claude Sonnet 4.5 (or Opus for complex ones)

```bash
git checkout -b feature/issue-101-phase-2-usage-core

# Start with Sonnet
claude --model sonnet
```

**Standard Entity Verification:**
```
You: "Verify UsagePointEntity against espi.xsd

1. Extract UsagePoint complexType from XSD
2. Read UsagePointEntity.java
3. Create field-by-field comparison:
   - Field name
   - XSD type → BasicType → restriction
   - Java @Column annotations
   - Match status
4. Identify discrepancies
5. Generate fixes"

[Claude Sonnet performs verification]
```

**For Complex Entities (e.g., ReadingType):**
```bash
# Switch to Opus for deeper analysis
claude --model opus
```

```
You: "ReadingType has many relationships and complex mappings.
Please analyze ReadingTypeEntity against the XSD:

1. Deep analysis of all relationships
2. Check inheritance hierarchy
3. Verify all embedded objects
4. Check collection mappings
5. Analyze potential breaking changes
6. Recommend migration strategy"

[Claude Opus provides deep analysis]

# Then switch back to Sonnet for fixes
claude --model sonnet
```

---

## Phase 3: Customer Domain Entities

### Customer Entities (Tasks 3.1-3.10)
**Model**: Claude Sonnet 4.5

```bash
git checkout -b feature/issue-101-phase-3-customer-entities

claude --model sonnet
```

**Same verification pattern as Phase 2.**

---

## Model Selection Decision Tree

```
┌─────────────────────────────────────────────┐
│ What are you doing?                         │
└─────────────────────────────────────────────┘
              │
              ├── Analyzing XSD schema structure?
              │   └─→ Use OPUS
              │
              ├── Making architectural decision?
              │   └─→ Use OPUS
              │
              ├── Analyzing complex entity (ReadingType, UsagePoint)?
              │   └─→ Use OPUS if many relationships
              │       Use SONNET if straightforward
              │
              ├── Generating code (enums, fixes)?
              │   └─→ Use SONNET
              │
              ├── Verifying standard entity?
              │   └─→ Use SONNET
              │
              ├── Batch verifying simple items?
              │   └─→ Use HAIKU
              │
              └── Writing documentation/reports?
                  └─→ Use SONNET
```

---

## Practical Tips

### 1. Keep Sessions Organized

Create a tracking file:
```bash
touch PHASE_PROGRESS.md
```

Track which model you used for what:
```markdown
# Phase 0 Progress

## Schema Analysis (Opus)
- [x] espi.xsd analyzed - Session 2024-02-03-opus-1
- [x] customer.xsd analyzed - Session 2024-02-03-opus-1
- [x] Summary created

## Enum Generation (Sonnet)
- [x] Batch 1 (5 enums) - Session 2024-02-03-sonnet-1
- [x] Batch 2 (5 enums) - Session 2024-02-03-sonnet-2
- [ ] Batch 3 (5 enums) - In progress

## Verification (Haiku)
- [ ] Not started
```

### 2. Save Context Between Sessions

At end of each session, ask Claude to create a handoff document:

```
You: "Create a handoff document for the next session:
1. What we completed
2. Current state
3. Next steps
4. Any important findings"

Save to: reports/session_handoff_2024-02-03.md
```

### 3. Batch Work by Model

Instead of switching constantly:

**Bad:**
```
Opus → analyze entity 1
Sonnet → fix entity 1
Opus → analyze entity 2
Sonnet → fix entity 2
```

**Good:**
```
Opus session: Analyze entities 1, 2, 3, 4, 5 → Save findings
Sonnet session: Fix all 5 entities based on findings
```

### 4. Use Copy-Paste for Repetitive Tasks

For enum generation, create a template:

```
You: "Generate [ENUM_NAME] enum from espi.xsd:
1. Read simpleType definition
2. Create Java enum with all values
3. Include getValue() and fromValue()
4. Add Javadoc
5. Save to: domain/usage/enums/[ENUM_NAME].java"
```

Save this, then just replace `[ENUM_NAME]` each time.

---

## Cost Comparison

| Approach | Your Cost | Time Investment |
|----------|-----------|-----------------|
| **Automated (API)** | ~$35 | 2-3 hours setup + automated execution |
| **Conversational** | $0 (MAX plan) | 20-30 hours of your time |
| **Hybrid** | ~$10 (only critical tasks via API) | 10-15 hours |

**Recommendation**: Since you have MAX plan, the conversational approach is perfect! It's free and you maintain complete control.

---

## Complete Phase 0 Example Timeline

**Day 1 - Schema Analysis (Opus):**
- 9:00 AM: Start Opus session
- 9:00-10:00: Analyze espi.xsd
- 10:00-11:00: Analyze customer.xsd
- 11:00-11:30: Create summary
- 11:30-12:00: Create Git branch, commit, push, create PR
- Afternoon: PR review and merge

**Day 2 - Generate Enums (Sonnet):**
- 9:00 AM: Start Sonnet session
- 9:00-10:00: Generate batch 1 (5 usage enums)
- 10:00-10:15: Commit and push
- 10:15-11:15: Generate batch 2 (5 usage enums)
- 11:15-11:30: Commit and push
- Lunch break
- 1:00-2:00: Generate batch 3 (remaining usage enums)
- 2:00-2:15: Commit and push
- 2:15-2:45: Generate shared enums (7 enums)
- 2:45-3:00: Generate customer enums (2 enums)
- 3:00-3:30: Final commit, push, create PR

**Day 3 - Verify and Merge:**
- Morning: Haiku session - verify all enums
- Fix any issues (back to Sonnet if needed)
- Run tests: `mvn clean test`
- Merge PR

---

## Quick Reference Commands

```bash
# Start with appropriate model
claude --model opus      # Complex analysis
claude --model sonnet    # Code generation, standard verification
claude --model haiku     # Simple verification

# Check current model in conversation
# Just ask: "Which model are you?"

# Switch models: Exit current session (Ctrl+D) and start new one

# Create branches
git checkout -b feature/issue-101-phase-X-description

# Commit work
git add [files]
git commit -m "feat/fix: description

Co-Authored-By: Claude [Model] [Version] <noreply@anthropic.com>"

# Push and PR
git push -u origin [branch-name]
gh pr create  # or create manually on GitHub
```

---

## Summary

**To get full benefit of the optimized plan conversationally:**

1. ✅ **Use Opus** for schema analysis and complex architectural decisions
2. ✅ **Use Sonnet** for code generation, standard verification, and fixes
3. ✅ **Use Haiku** for batch verification of simple items
4. ✅ **Switch models** by exiting and starting new session: `claude --model [opus|sonnet|haiku]`
5. ✅ **Batch work** to minimize model switching
6. ✅ **Track progress** in a file to maintain context

**Start with:**
```bash
claude --model opus
```

Then ask: "Let's begin Phase 0 - Schema Analysis for Issue #101. Please analyze espi.xsd..."

You're in control, it's free, and you get the full benefits of the optimized model selection! 🚀
