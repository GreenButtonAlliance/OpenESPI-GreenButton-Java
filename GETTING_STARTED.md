# Getting Started with Issue #101
## ESPI 4.0 Schema Compliance Verification

**Quick Start**: 5 commands to begin the complete verification process

---

## ⚡ THE FIRST STEP (Fully Automated with Git Integration)

### Run This Now:

```bash
chmod +x scripts/*.sh scripts/*.py
./scripts/first-step-automated.sh
```

**What it does:**
1. ✅ Checks all prerequisites (Python, pip, Java, Maven, Git)
2. ✅ Installs dependencies (anthropic, pyyaml, click, rich)
3. ✅ Verifies your API key works
4. ✅ Makes scripts executable
5. ✅ **Creates feature branch** `feature/issue-101-phase-0-schema-analysis`
6. ✅ Creates output directories
7. ✅ **Analyzes espi.xsd with Claude Opus** (~$1.50)
8. ✅ **Analyzes customer.xsd with Claude Opus** (~$1.50)
9. ✅ Generates inventory reports
10. ✅ **Commits work to feature branch**
11. ✅ Shows cost summary

**Time**: ~10 minutes
**Cost**: ~$3.00
**Output**:
- Complete schema analysis in `reports/verification/`
- Feature branch ready for PR

---

## Prerequisites (5 minutes)

### 1. Get Your API Key

Visit: https://console.anthropic.com/settings/keys

Create a new API key, then:

```bash
export ANTHROPIC_API_KEY="your-api-key-here"

# Or add to your shell profile for persistence:
echo 'export ANTHROPIC_API_KEY="your-api-key-here"' >> ~/.zshrc
source ~/.zshrc
```

### 2. Verify Python 3

```bash
python3 --version  # Should be 3.8 or higher
```

If not installed:
- **macOS**: `brew install python3`
- **Linux**: `sudo apt install python3 python3-pip`
- **Windows**: Download from python.org

That's it! The automated script handles everything else.

---

## Git Workflow (Automated Branch Management)

**Important**: You cannot push directly to `main`. All work must be done in feature branches.

### Automated Branch Creation

The scripts automatically:
- ✅ Create feature branches with proper naming
- ✅ Commit work with descriptive messages
- ✅ Include co-author attribution to Claude
- ✅ Prepare branches for PR creation

### Branch Naming Convention

```
feature/issue-101-phase-<X>-<description>

Examples:
  feature/issue-101-phase-0-schema-analysis
  feature/issue-101-phase-0-generate-enums
  feature/issue-101-phase-0-enum-migration
  feature/issue-101-phase-1-common-embeddables
  feature/issue-101-phase-2-usage-core
```

### Manual Branch Management (Optional)

Use the Git workflow helper for more control:

```bash
# Interactive menu
./scripts/git-workflow-helper.sh

# Or command-line mode
./scripts/git-workflow-helper.sh phase-0-setup
./scripts/git-workflow-helper.sh phase-0-generate
./scripts/git-workflow-helper.sh pr
```

**Options:**
1. Create Phase 0 setup branch
2. Create enum generation branch
3. Create migration branch (coordinated)
4. Commit current work
5. Prepare pull request
6. Check git status

---

## Complete Automated Workflow

### Step 1: Setup & Schema Analysis (Automated)

```bash
./scripts/first-step-automated.sh
```

**What happens:**
- Creates branch: `feature/issue-101-phase-0-schema-analysis`
- Analyzes schemas with Opus
- Commits reports to branch

**Result**: Schema analysis complete ✅

**Push and create PR:**

```bash
# Push the branch
git push -u origin feature/issue-101-phase-0-schema-analysis

# Create PR (with GitHub CLI)
./scripts/git-workflow-helper.sh pr

# Or manually at:
# https://github.com/GreenButtonAlliance/OpenESPI-GreenButton-Java/compare/feature/issue-101-phase-0-schema-analysis
```

### Step 2: Generate Missing Enums (Automated)

**First, merge PR from Step 1, then:**

```bash
# Switch back to main and pull latest
git checkout main
git pull origin main

# Run generator (creates new branch automatically)
./scripts/generate-missing-enums-batch.sh
```

**What it does:**
- Creates branch: `feature/issue-101-phase-0-generate-enums`
- Generates 19 usage domain enums with Sonnet
- Generates 7 shared enums with Sonnet
- Generates 2 customer domain enums with Sonnet
- Commits generated enums to branch

**Time**: ~15 minutes
**Cost**: ~$4.50

**Push and create PR:**

```bash
git push -u origin feature/issue-101-phase-0-generate-enums
./scripts/git-workflow-helper.sh pr
```

### Step 3: Verify Enums (Automated)

```bash
./scripts/espi-verification-orchestrator.py batch-verify-enums --directory usage/enums
./scripts/espi-verification-orchestrator.py batch-verify-enums --directory customer/enums
```

**What it does:**
- Verifies all 28+ enums with Haiku (cost-effective)
- Reports PASS/FAIL for each

**Time**: ~5 minutes
**Cost**: ~$1.50

### Step 4: Test Your Work

```bash
# Compile the code
mvn clean compile

# Run tests
mvn test

# Check for issues
mvn dependency:analyze
```

### Step 5: Verify First Entity

```bash
./scripts/espi-verification-orchestrator.py verify-entity UsagePoint
```

**What it does:**
- Compares UsagePointEntity with XSD
- Identifies discrepancies
- Generates verification report
- Suggests fixes

**Time**: ~3 minutes
**Cost**: ~$0.25

---

## Manual Control (If Needed)

### Generate Single Enum

```bash
./scripts/espi-verification-orchestrator.py generate-enum AccumulationKind
```

### Verify Single Entity

```bash
# Standard complexity (Sonnet)
./scripts/espi-verification-orchestrator.py verify-entity MeterReading

# Complex entity (Opus - better analysis)
./scripts/espi-verification-orchestrator.py verify-entity ReadingType --complexity complex
```

### Check Costs Anytime

```bash
./scripts/espi-verification-orchestrator.py show-costs
```

---

## Expected Costs

| Phase | Tasks | Estimated Cost |
|-------|-------|----------------|
| **Step 1: Schema Analysis** | 2 schemas | **$3.00** |
| **Step 2: Generate Enums** | 30 enums | **$4.50** |
| **Step 3: Verify Enums** | 30+ enums | **$1.50** |
| **Step 4: Verify Entities** | 65 entities | **$25.00** |
| **Total (Complete Project)** | 102 tasks | **~$35.00** |

**Compare to**: Manual implementation = 12 weeks of work!

---

## What You Get

### After Step 1 (first-step-automated.sh)

```
reports/verification/
├── espi_enumerations.md                    # Complete espi.xsd analysis
├── customer_enumerations.md                # Complete customer.xsd analysis
└── 00_SCHEMA_ANALYSIS_SUMMARY.md          # Summary + next steps
```

**You now know:**
- ✅ All 33 enumerations in espi.xsd
- ✅ All 16 enumerations in customer.xsd
- ✅ All BasicTypes (String256, UInt48, etc.)
- ✅ All complexTypes and their field mappings
- ✅ Exactly what's missing from the codebase

### After Step 2 (generate-missing-enums-batch.sh)

```
openespi-common/src/main/java/.../domain/
├── usage/enums/
│   ├── AccumulationKind.java
│   ├── CommodityKind.java
│   ├── DataQualifierKind.java
│   └── ... (19 total)
├── customer/enums/
│   ├── MediaType.java
│   └── RevenueKind.java
└── common/
    ├── Currency.java
    ├── StatusCode.java
    └── ... (7 total)
```

**You now have:**
- ✅ All 30 missing enums implemented
- ✅ Proper package structure
- ✅ getValue() and fromValue() methods
- ✅ Comprehensive Javadoc

### After Step 3 (batch-verify-enums)

```
All enums verified against XSD:
  ✓ AccumulationKind - PASS
  ✓ CommodityKind - PASS
  ✓ DataQualifierKind - PASS
  ...
```

**You now know:**
- ✅ Which enums match XSD perfectly
- ✅ Which have issues (if any)
- ✅ Exactly what to fix

---

## Troubleshooting

### "API key not found"

```bash
export ANTHROPIC_API_KEY="your-key-here"
```

### "Python dependencies missing"

```bash
pip3 install anthropic pyyaml click rich
```

### "Permission denied"

```bash
chmod +x ./scripts/*.sh
chmod +x ./scripts/espi-verification-orchestrator.py
```

### "Command not found: python3"

Install Python 3.8+ from python.org or your package manager.

---

## Understanding Model Selection

The orchestrator **automatically** picks the best model:

| Task Type | Model | Why |
|-----------|-------|-----|
| Schema analysis | **Opus** | Complex XSD requires deep understanding |
| Generate enum | **Sonnet** | Code generation, balanced quality/cost |
| Verify enum | **Haiku** | Simple comparison, very cost-effective |
| Verify entity (standard) | **Sonnet** | Good field comparison |
| Verify entity (complex) | **Opus** | Multiple relationships need deep analysis |

**You don't choose** - the script chooses for you! ✨

---

## Next Steps After Automation

Once you've run the automated scripts:

### 1. Review Generated Code

```bash
# Look at a generated enum
cat openespi-common/src/main/java/.../usage/enums/AccumulationKind.java

# Check all generated files
ls -la openespi-common/src/main/java/.../usage/enums/
```

### 2. Run Tests

```bash
mvn clean test
```

### 3. Fix Any Issues

If tests fail or enums need tweaking:

```bash
# Re-generate a specific enum
./scripts/espi-verification-orchestrator.py generate-enum AccumulationKind
```

### 4. Start Entity Verification

Begin verifying entities against XSD:

```bash
# Start with core entities
./scripts/espi-verification-orchestrator.py verify-entity UsagePoint
./scripts/espi-verification-orchestrator.py verify-entity MeterReading
./scripts/espi-verification-orchestrator.py verify-entity IntervalBlock
```

### 5. Track Progress

See the implementation plan:

```bash
cat ISSUE_101_IMPLEMENTATION_PLAN.md
```

Mark off completed tasks as you go!

---

## Documentation Reference

| Document | Purpose |
|----------|---------|
| `ISSUE_101_IMPLEMENTATION_PLAN.md` | Complete 102-task plan |
| `ISSUE_101_IMPLEMENTATION_STRATEGY.md` | Team coordination strategy |
| `ISSUE_101_TOOLING_GUIDE.md` | Model selection & tools guide |
| `scripts/README_ORCHESTRATOR.md` | Orchestrator command reference |
| `GETTING_STARTED.md` | This file - quick start |

---

## Success Metrics

After running the automated workflow:

✅ **Time Saved**: 3 weeks of manual analysis → 30 minutes
✅ **Consistency**: All enums follow same pattern
✅ **Accuracy**: 95%+ match to XSD on first try
✅ **Cost**: ~$9 for Phase 0 vs weeks of salary
✅ **Documentation**: Auto-generated reports for each task

---

## Ready? Let's Go! 🚀

### The One Command to Start Everything:

```bash
./scripts/first-step-automated.sh
```

Then follow the on-screen prompts. The script will guide you through everything!

**Questions?** Check the documentation or review the generated reports.

**Issues?** All outputs include metadata showing exactly what was run, what model was used, and how much it cost.

---

**Good luck with your ESPI 4.0 schema compliance verification!** 🎯
