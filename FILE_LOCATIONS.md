# File Locations Guide
## All Generated Files for Issue #101

This document shows the location of all files created for the ESPI 4.0 schema compliance verification project.

---

## 📋 Documentation Files (Project Root)

Located in: `/Users/donal/Git/GreenButtonAlliance/OpenESPI-GreenButton-Java/`

```
OpenESPI-GreenButton-Java/
├── GETTING_STARTED.md                      ⭐ START HERE
├── ISSUE_101_IMPLEMENTATION_PLAN.md        📋 Complete 102-task plan
├── ISSUE_101_IMPLEMENTATION_STRATEGY.md    👥 Team coordination strategy
├── ISSUE_101_TOOLING_GUIDE.md             🛠️ Tools and Claude models guide
└── FILE_LOCATIONS.md                       📍 This file
```

### File Purposes:

| File | Size | Purpose | When to Use |
|------|------|---------|-------------|
| **GETTING_STARTED.md** | ~15 KB | Quick start guide | Read this FIRST |
| **ISSUE_101_IMPLEMENTATION_PLAN.md** | ~50 KB | Complete implementation plan with all 102 tasks | Reference for task details |
| **ISSUE_101_IMPLEMENTATION_STRATEGY.md** | ~35 KB | Team coordination and branching strategy | Before starting team work |
| **ISSUE_101_TOOLING_GUIDE.md** | ~40 KB | Claude model selection and tool recommendations | When choosing tools/models |
| **FILE_LOCATIONS.md** | ~10 KB | This file - shows where everything is | Finding files |

---

## 🔧 Scripts Directory

Located in: `/Users/donal/Git/GreenButtonAlliance/OpenESPI-GreenButton-Java/scripts/`

```
scripts/
├── espi-verification-orchestrator.py       🤖 Main automation script
├── verification-config.yaml                ⚙️ Configuration file
├── README_ORCHESTRATOR.md                  📖 Orchestrator documentation
├── first-step-automated.sh                 ⚡ Step 1: Setup & analysis
├── generate-missing-enums-batch.sh         📝 Step 2: Generate enums
├── run-phase-0-example.sh                  🎯 Example workflow demo
└── git-workflow-helper.sh                  🌿 Git branch management
```

### Script Details:

| File | Lines | Language | Executable | Purpose |
|------|-------|----------|------------|---------|
| **espi-verification-orchestrator.py** | ~800 | Python | ✅ Yes | Main automation engine with model switching |
| **verification-config.yaml** | ~100 | YAML | - | Task configuration and cost limits |
| **README_ORCHESTRATOR.md** | ~700 | Markdown | - | Complete orchestrator usage guide |
| **first-step-automated.sh** | ~250 | Bash | ✅ Yes | Automated setup and schema analysis |
| **generate-missing-enums-batch.sh** | ~200 | Bash | ✅ Yes | Batch generate all 30 missing enums |
| **run-phase-0-example.sh** | ~100 | Bash | ✅ Yes | Demo showing model switching |
| **git-workflow-helper.sh** | ~600 | Bash | ✅ Yes | Branch creation and PR management |

---

## 📊 Output Directories (Created by Scripts)

These directories are created when you run the scripts:

### Reports Directory

```
reports/
└── verification/                           📊 All verification outputs
    ├── 00_SCHEMA_ANALYSIS_SUMMARY.md      📋 Master summary
    ├── espi_enumerations.md               📄 espi.xsd analysis (Opus)
    ├── espi_enumerations.json             💰 Cost/token metadata
    ├── customer_enumerations.md           📄 customer.xsd analysis (Opus)
    ├── customer_enumerations.json         💰 Cost/token metadata
    ├── UsagePoint_verification.md         📄 Entity verification (future)
    ├── UsagePoint_report.md               📄 Formatted report (future)
    └── ...                                 (more files as you verify)
```

**Created by**: `first-step-automated.sh` (Step 7-8)

### Generated Code Directory

```
openespi-common/src/main/java/org/greenbuttonalliance/espi/common/domain/
├── usage/
│   └── enums/                              📁 New directory (Phase 0)
│       ├── .gitkeep                        📌 Ensures directory tracked
│       ├── AccumulationKind.java           ☕ Generated enum
│       ├── CommodityKind.java              ☕ Generated enum
│       ├── DataQualifierKind.java          ☕ Generated enum
│       ├── FlowDirectionKind.java          ☕ Generated enum
│       ├── MeasurementKind.java            ☕ Generated enum
│       └── ...                             (19 total)
│
├── customer/
│   └── enums/                              📁 Existing directory
│       ├── CustomerKind.java               ✅ Already exists
│       ├── MediaType.java                  ☕ New (generated)
│       └── RevenueKind.java                ☕ New (generated)
│
└── common/                                  📁 Shared enums
    ├── ServiceKind.java                     ☕ New (renamed from ServiceCategory)
    ├── Currency.java                        ☕ New (generated)
    ├── StatusCode.java                      ☕ New (generated)
    ├── UnitMultiplierKind.java              ☕ New (generated)
    └── ...                                  (7 total shared)
```

**Created by**: `generate-missing-enums-batch.sh` (Step 2)

---

## 🗂️ Complete File Tree

Here's the complete structure showing ALL generated files:

```
/Users/donal/Git/GreenButtonAlliance/OpenESPI-GreenButton-Java/
│
├── 📄 GETTING_STARTED.md                    ⭐ START HERE
├── 📄 ISSUE_101_IMPLEMENTATION_PLAN.md
├── 📄 ISSUE_101_IMPLEMENTATION_STRATEGY.md
├── 📄 ISSUE_101_TOOLING_GUIDE.md
├── 📄 FILE_LOCATIONS.md                     📍 This file
│
├── 📁 scripts/
│   ├── 🐍 espi-verification-orchestrator.py
│   ├── ⚙️ verification-config.yaml
│   ├── 📖 README_ORCHESTRATOR.md
│   ├── 🔧 first-step-automated.sh
│   ├── 🔧 generate-missing-enums-batch.sh
│   ├── 🔧 run-phase-0-example.sh
│   └── 🔧 git-workflow-helper.sh
│
├── 📁 reports/                              (Created by scripts)
│   └── 📁 verification/
│       ├── 00_SCHEMA_ANALYSIS_SUMMARY.md
│       ├── espi_enumerations.md
│       ├── espi_enumerations.json
│       ├── customer_enumerations.md
│       └── customer_enumerations.json
│
└── 📁 openespi-common/
    └── 📁 src/main/java/.../domain/
        ├── 📁 usage/
        │   └── 📁 enums/                    (Created by scripts)
        │       ├── .gitkeep
        │       ├── AccumulationKind.java
        │       ├── CommodityKind.java
        │       └── ... (19 total)
        │
        ├── 📁 customer/
        │   └── 📁 enums/
        │       ├── MediaType.java           (New)
        │       └── RevenueKind.java         (New)
        │
        └── 📁 common/
            ├── Currency.java                (New)
            ├── StatusCode.java              (New)
            └── ... (7 total shared)
```

---

## 📦 Files by Category

### 1. Documentation (Read These)

```bash
# Main documentation
cat GETTING_STARTED.md              # Quick start
cat ISSUE_101_IMPLEMENTATION_PLAN.md # Full plan
cat ISSUE_101_IMPLEMENTATION_STRATEGY.md # Team strategy
cat ISSUE_101_TOOLING_GUIDE.md      # Tools guide

# Script documentation
cat scripts/README_ORCHESTRATOR.md  # Orchestrator guide
```

### 2. Executable Scripts (Run These)

```bash
# Make executable first (if not already)
chmod +x scripts/*.sh scripts/*.py

# Then run
./scripts/first-step-automated.sh           # Step 1: Setup
./scripts/generate-missing-enums-batch.sh   # Step 2: Generate
./scripts/git-workflow-helper.sh            # Git management
./scripts/run-phase-0-example.sh            # Example demo

# Orchestrator commands
./scripts/espi-verification-orchestrator.py --help
./scripts/espi-verification-orchestrator.py analyze-schema
./scripts/espi-verification-orchestrator.py verify-entity UsagePoint
```

### 3. Configuration Files (Edit These)

```bash
# Main config
vim scripts/verification-config.yaml

# Edit to:
# - Change cost limits
# - Override model selection
# - Customize task lists
```

### 4. Generated Reports (Review These)

```bash
# Schema analysis
cat reports/verification/espi_enumerations.md
cat reports/verification/customer_enumerations.md

# Summary
cat reports/verification/00_SCHEMA_ANALYSIS_SUMMARY.md

# Metadata (JSON)
cat reports/verification/espi_enumerations.json
```

### 5. Generated Code (Verify These)

```bash
# New usage enums
ls -la openespi-common/src/main/java/.../usage/enums/

# Example enum
cat openespi-common/src/main/java/.../usage/enums/AccumulationKind.java

# New customer enums
ls -la openespi-common/src/main/java/.../customer/enums/

# Shared enums
ls -la openespi-common/src/main/java/.../common/
```

---

## 🔍 Finding Files Quickly

### Using Terminal

```bash
# Find all generated markdown files
find . -name "ISSUE_101*.md" -o -name "GETTING_STARTED.md"

# Find all scripts
find scripts/ -name "*.sh" -o -name "*.py"

# Find all generated enums
find openespi-common/src/main/java -path "*/enums/*.java" -newer /tmp

# Find verification reports
find reports/verification/ -name "*.md"
```

### Using Git

```bash
# See what's new (not committed)
git status

# See all files in feature branch
git ls-tree -r --name-only feature/issue-101-phase-0-schema-analysis

# See files changed in last commit
git show --name-only
```

---

## 📏 File Sizes

| Category | Files | Total Size |
|----------|-------|------------|
| Documentation | 5 | ~150 KB |
| Scripts | 7 | ~50 KB |
| Reports (after analysis) | ~5 | ~500 KB |
| Generated Code (30 enums) | ~30 | ~60 KB |
| **Total** | **~47** | **~760 KB** |

---

## 🎯 Quick Access Commands

Save these aliases:

```bash
# Add to ~/.bashrc or ~/.zshrc

# Project root
alias cd-espi='cd /Users/donal/Git/GreenButtonAlliance/OpenESPI-GreenButton-Java'

# Documentation
alias espi-docs='cd-espi && ls -la *.md'

# Scripts
alias espi-scripts='cd-espi && ls -la scripts/'

# Reports
alias espi-reports='cd-espi && ls -la reports/verification/'

# Generated code
alias espi-enums='cd-espi && find openespi-common -path "*/enums/*.java"'
```

Then use:

```bash
cd-espi           # Jump to project
espi-docs         # List all docs
espi-scripts      # List all scripts
espi-reports      # List all reports
espi-enums        # List all enum files
```

---

## 📋 Checklist: What Files Exist Now

Before running any scripts:

```
✅ GETTING_STARTED.md
✅ ISSUE_101_IMPLEMENTATION_PLAN.md
✅ ISSUE_101_IMPLEMENTATION_STRATEGY.md
✅ ISSUE_101_TOOLING_GUIDE.md
✅ FILE_LOCATIONS.md
✅ scripts/espi-verification-orchestrator.py
✅ scripts/verification-config.yaml
✅ scripts/README_ORCHESTRATOR.md
✅ scripts/first-step-automated.sh
✅ scripts/generate-missing-enums-batch.sh
✅ scripts/run-phase-0-example.sh
✅ scripts/git-workflow-helper.sh
```

After running `first-step-automated.sh`:

```
✅ reports/verification/
✅ reports/verification/00_SCHEMA_ANALYSIS_SUMMARY.md
✅ reports/verification/espi_enumerations.md
✅ reports/verification/customer_enumerations.md
✅ openespi-common/.../usage/enums/.gitkeep
```

After running `generate-missing-enums-batch.sh`:

```
✅ openespi-common/.../usage/enums/AccumulationKind.java
✅ openespi-common/.../usage/enums/CommodityKind.java
... (19 total usage enums)
✅ openespi-common/.../customer/enums/MediaType.java
✅ openespi-common/.../customer/enums/RevenueKind.java
✅ openespi-common/.../common/Currency.java
... (7 total shared enums)
```

---

## 🚀 Next Steps

1. **Read the docs** (in order):
   ```bash
   cat GETTING_STARTED.md              # 1. Start here
   cat ISSUE_101_IMPLEMENTATION_PLAN.md # 2. See full plan
   cat scripts/README_ORCHESTRATOR.md  # 3. Learn tools
   ```

2. **Run the first script**:
   ```bash
   ./scripts/first-step-automated.sh
   ```

3. **Check what was created**:
   ```bash
   ls -la reports/verification/
   ```

4. **Review the output**:
   ```bash
   cat reports/verification/00_SCHEMA_ANALYSIS_SUMMARY.md
   ```

---

## 💡 Tips

**Find files modified today:**
```bash
find . -type f -mtime 0 -name "*.md" -o -name "*.java" -o -name "*.sh"
```

**Search for specific content:**
```bash
grep -r "AccumulationKind" openespi-common/src/
grep -r "Claude Opus" reports/verification/
```

**Count generated files:**
```bash
find openespi-common -path "*/enums/*.java" | wc -l
```

**Show file tree:**
```bash
tree -L 3 scripts/
tree -L 5 openespi-common/src/main/java/.../domain/
```

---

**Everything is in place and ready to go!** 🎉
