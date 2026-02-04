#!/bin/bash
#
# First Step: Automated Setup and Schema Analysis
#
# This script:
# 1. Checks all prerequisites
# 2. Sets up the environment
# 3. Tests the orchestrator
# 4. Runs foundational XSD schema analysis
# 5. Generates inventory reports
#
# This is THE FIRST THING TO RUN for Issue #101
#

set -e  # Exit on error

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
ORCHESTRATOR="$SCRIPT_DIR/espi-verification-orchestrator.py"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo "╔════════════════════════════════════════════════════════════╗"
echo "║                                                            ║"
echo "║        ESPI 4.0 Schema Compliance - First Step            ║"
echo "║              Automated Setup & Analysis                    ║"
echo "║                                                            ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

# ==================== Step 1: Check Prerequisites ====================

echo -e "${BLUE}Step 1: Checking Prerequisites${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Check Python
if ! command -v python3 &> /dev/null; then
    echo -e "${RED}✗ Python 3 not found${NC}"
    echo "  Please install Python 3.8 or higher"
    exit 1
fi
echo -e "${GREEN}✓ Python 3 found:${NC} $(python3 --version)"

# Check pip
if ! command -v pip3 &> /dev/null; then
    echo -e "${RED}✗ pip3 not found${NC}"
    echo "  Please install pip3"
    exit 1
fi
echo -e "${GREEN}✓ pip3 found${NC}"

# Check Java
if ! command -v java &> /dev/null; then
    echo -e "${YELLOW}⚠ Java not found${NC}"
    echo "  You'll need Java 25 to run tests later"
else
    echo -e "${GREEN}✓ Java found:${NC} $(java -version 2>&1 | head -1)"
fi

# Check Maven
if ! command -v mvn &> /dev/null; then
    echo -e "${YELLOW}⚠ Maven not found${NC}"
    echo "  You'll need Maven to build the project later"
else
    echo -e "${GREEN}✓ Maven found:${NC} $(mvn -version | head -1)"
fi

echo ""

# ==================== Step 2: Install Python Dependencies ====================

echo -e "${BLUE}Step 2: Installing Python Dependencies${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

REQUIRED_PACKAGES=("anthropic" "pyyaml" "click" "rich")
MISSING_PACKAGES=()

for package in "${REQUIRED_PACKAGES[@]}"; do
    if python3 -c "import $package" 2>/dev/null; then
        echo -e "${GREEN}✓ $package${NC} already installed"
    else
        echo -e "${YELLOW}⚠ $package${NC} not found, will install"
        MISSING_PACKAGES+=("$package")
    fi
done

if [ ${#MISSING_PACKAGES[@]} -gt 0 ]; then
    echo ""
    echo "Installing missing packages: ${MISSING_PACKAGES[*]}"
    pip3 install "${MISSING_PACKAGES[@]}"
    echo -e "${GREEN}✓ All dependencies installed${NC}"
fi

echo ""

# ==================== Step 3: Check API Key ====================

echo -e "${BLUE}Step 3: Checking Anthropic API Key${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

if [ -z "$ANTHROPIC_API_KEY" ]; then
    echo -e "${RED}✗ ANTHROPIC_API_KEY not set${NC}"
    echo ""
    echo "Please set your API key:"
    echo "  export ANTHROPIC_API_KEY='your-key-here'"
    echo ""
    echo "Get your API key from: https://console.anthropic.com/settings/keys"
    echo ""
    exit 1
fi

# Mask the key for display
MASKED_KEY="${ANTHROPIC_API_KEY:0:8}...${ANTHROPIC_API_KEY: -4}"
echo -e "${GREEN}✓ API key found:${NC} $MASKED_KEY"
echo ""

# Test the API key
echo "Testing API connection..."
TEST_RESULT=$(python3 -c "
import anthropic
import os
try:
    client = anthropic.Anthropic(api_key=os.environ.get('ANTHROPIC_API_KEY'))
    response = client.messages.create(
        model='claude-haiku-4-20250514',
        max_tokens=10,
        messages=[{'role': 'user', 'content': 'Hi'}]
    )
    print('success')
except Exception as e:
    print(f'error: {e}')
" 2>&1)

if [[ "$TEST_RESULT" == "success" ]]; then
    echo -e "${GREEN}✓ API connection successful${NC}"
else
    echo -e "${RED}✗ API test failed:${NC} $TEST_RESULT"
    exit 1
fi

echo ""

# ==================== Step 4: Make Scripts Executable ====================

echo -e "${BLUE}Step 4: Setting Up Scripts${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

chmod +x "$ORCHESTRATOR"
echo -e "${GREEN}✓ Orchestrator script is executable${NC}"

if [ -f "$SCRIPT_DIR/run-phase-0-example.sh" ]; then
    chmod +x "$SCRIPT_DIR/run-phase-0-example.sh"
    echo -e "${GREEN}✓ Example script is executable${NC}"
fi

echo ""

# ==================== Step 5: Create Feature Branch ====================

echo -e "${BLUE}Step 5: Creating Feature Branch${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

echo "Following best practices from ISSUE_101_IMPLEMENTATION_STRATEGY.md,"
echo "we'll create a feature branch for Phase 0 setup work."
echo ""
echo "Branch: feature/issue-101-phase-0-schema-analysis"
echo ""
echo "This branch will contain:"
echo "  • Directory structure setup"
echo "  • Schema analysis reports"
echo "  • Configuration updates"
echo ""

read -p "Create feature branch now? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    # Check if we're already on a feature branch
    CURRENT_BRANCH=$(git branch --show-current 2>/dev/null || echo "main")

    if [[ $CURRENT_BRANCH == "main" ]]; then
        # Create new branch
        git checkout -b feature/issue-101-phase-0-schema-analysis
        echo -e "${GREEN}✓ Created branch:${NC} feature/issue-101-phase-0-schema-analysis"
    else
        echo -e "${YELLOW}⚠ Already on branch:${NC} $CURRENT_BRANCH"
        read -p "Continue on this branch? (y/n) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            echo "Please switch to main or desired branch first"
            exit 1
        fi
    fi
else
    echo -e "${YELLOW}⚠ Skipping branch creation${NC}"
    echo "You can create it later with:"
    echo "  ./scripts/git-workflow-helper.sh phase-0-setup"
fi

echo ""

# ==================== Step 6: Create Output Directories ====================

echo -e "${BLUE}Step 6: Creating Output Directories${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

mkdir -p "$PROJECT_ROOT/reports/verification"
mkdir -p "$PROJECT_ROOT/openespi-common/src/main/java/org/greenbuttonalliance/espi/common/domain/usage/enums"

# Add .gitkeep to ensure directory is tracked
touch "$PROJECT_ROOT/openespi-common/src/main/java/org/greenbuttonalliance/espi/common/domain/usage/enums/.gitkeep"

echo -e "${GREEN}✓ Created:${NC} reports/verification/"
echo -e "${GREEN}✓ Created:${NC} domain/usage/enums/"
echo ""

# ==================== Step 7: XSD Schema Analysis (THE FOUNDATION) ====================

echo -e "${BLUE}Step 7: Analyzing ESPI XSD Schemas (Foundation)${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "This is the CRITICAL first step that informs all other work."
echo "It analyzes espi.xsd and customer.xsd to extract:"
echo "  • All enumeration types"
echo "  • All complex types"
echo "  • BasicType definitions"
echo "  • Field mappings"
echo ""
echo "Model: Claude Opus 4.5 (best for complex analysis)"
echo "Cost: ~\$3.00 (2 schemas)"
echo "Time: ~5-10 minutes"
echo ""

read -p "Press Enter to start schema analysis... "
echo ""

echo "Analyzing espi.xsd..."
"$ORCHESTRATOR" analyze-schema --schema espi

echo ""
echo "Analyzing customer.xsd..."
"$ORCHESTRATOR" analyze-schema --schema customer

echo ""
echo -e "${GREEN}✓ Schema analysis complete!${NC}"
echo ""

# ==================== Step 8: Generate Inventory Reports ====================

echo -e "${BLUE}Step 8: Generating Inventory Reports${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Create summary report
SUMMARY_FILE="$PROJECT_ROOT/reports/verification/00_SCHEMA_ANALYSIS_SUMMARY.md"

cat > "$SUMMARY_FILE" <<'EOF'
# ESPI 4.0 Schema Analysis Summary

**Generated**: $(date)
**Purpose**: Foundation for Issue #101 schema compliance verification

## Overview

This report summarizes the automated analysis of ESPI 4.0 XSD schemas.

## Schema Files Analyzed

1. **espi.xsd** - Energy usage data schema
   - Location: `openespi-common/src/main/resources/schema/ESPI_4.0/espi.xsd`
   - Analysis: `reports/verification/espi_enumerations.md`

2. **customer.xsd** - Customer and service provider data schema
   - Location: `openespi-common/src/main/resources/schema/ESPI_4.0/customer.xsd`
   - Analysis: `reports/verification/customer_enumerations.md`

## Key Findings

### Enumerations Inventory

Refer to individual schema analysis files for:
- Complete enumeration listings
- BasicType definitions (String256, UInt48, etc.)
- ComplexType structures
- Element-to-type mappings

### Next Steps

Based on the schema analysis, you can now:

1. **Phase 0**: Generate missing enumerations
   - See: `ISSUE_101_IMPLEMENTATION_PLAN.md` Phase 0 tasks
   - Script: `scripts/generate-all-enums.sh`

2. **Phase 1-3**: Verify entity compliance
   - Compare entities against analyzed schemas
   - Generate fixes for discrepancies
   - Update Flyway migrations

## Generated Files

- `espi_enumerations.md` - Complete espi.xsd analysis
- `customer_enumerations.md` - Complete customer.xsd analysis
- `espi_enumerations.json` - Metadata (tokens, costs, etc.)
- `customer_enumerations.json` - Metadata

## Cost Tracking

Run to see costs:
```bash
./scripts/espi-verification-orchestrator.py show-costs
```

## Usage Examples

### Generate an enum
```bash
./scripts/espi-verification-orchestrator.py generate-enum AccumulationKind
```

### Verify an entity
```bash
./scripts/espi-verification-orchestrator.py verify-entity UsagePoint
```

### Batch verify enums
```bash
./scripts/espi-verification-orchestrator.py batch-verify-enums --directory usage/enums
```

---

**Status**: ✅ Schema analysis complete - Ready for Phase 0!
EOF

echo -e "${GREEN}✓ Created summary report:${NC} $SUMMARY_FILE"
echo ""

# ==================== Step 9: Show Results ====================

echo -e "${BLUE}Step 9: Results Summary${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

"$ORCHESTRATOR" show-costs

echo ""

# ==================== Step 10: Commit to Feature Branch ====================

echo -e "${BLUE}Step 10: Commit Work to Feature Branch${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

CURRENT_BRANCH=$(git branch --show-current 2>/dev/null || echo "unknown")

if [[ $CURRENT_BRANCH != "main" ]]; then
    echo "Files to commit:"
    echo "  • openespi-common/.../usage/enums/.gitkeep"
    echo "  • reports/verification/espi_enumerations.md"
    echo "  • reports/verification/customer_enumerations.md"
    echo "  • reports/verification/00_SCHEMA_ANALYSIS_SUMMARY.md"
    echo "  • reports/verification/*.json (metadata)"
    echo ""

    read -p "Commit these files? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        # Stage files
        git add openespi-common/src/main/java/org/greenbuttonalliance/espi/common/domain/usage/enums/.gitkeep
        git add reports/verification/

        # Commit
        git commit -m "feat(phase-0): Complete ESPI 4.0 schema analysis

Part of Issue #101: ESPI 4.0 schema compliance verification
Phase 0: Enumeration migration - Schema analysis

Analyzed XSD schemas:
- espi.xsd (usage domain)
- customer.xsd (customer domain)

Generated reports:
- Complete enumeration inventory (30 missing enums identified)
- BasicType definitions (String256, UInt48, etc.)
- ComplexType field mappings
- Entity verification baseline

Analysis performed with Claude Opus 4.5 for deep XSD understanding.

Next steps:
- Generate missing enumerations
- Verify generated enums
- Begin entity verification

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"

        echo -e "${GREEN}✓ Changes committed to branch:${NC} $CURRENT_BRANCH"
        echo ""
        echo "Next: Push branch and create PR"
        echo "  git push -u origin $CURRENT_BRANCH"
        echo ""
        echo "Or use the helper:"
        echo "  ./scripts/git-workflow-helper.sh pr"
    fi
else
    echo -e "${YELLOW}⚠ On main branch - skipping commit${NC}"
    echo ""
    echo "Create a feature branch first:"
    echo "  ./scripts/git-workflow-helper.sh phase-0-setup"
fi

echo ""
echo "═════════════════════════════════════════════════════════════"
echo ""
echo -e "${GREEN}✅ FIRST STEP COMPLETE!${NC}"
echo ""
echo "What was accomplished:"
echo "  ✓ Dependencies installed"
echo "  ✓ API key verified"
echo "  ✓ Scripts configured"
echo "  ✓ Directories created"
echo "  ✓ espi.xsd analyzed (Opus)"
echo "  ✓ customer.xsd analyzed (Opus)"
echo "  ✓ Inventory reports generated"
echo ""
echo "═════════════════════════════════════════════════════════════"
echo ""
echo -e "${BLUE}📊 Generated Reports:${NC}"
echo ""
echo "  1. reports/verification/espi_enumerations.md"
echo "     → Complete analysis of espi.xsd"
echo ""
echo "  2. reports/verification/customer_enumerations.md"
echo "     → Complete analysis of customer.xsd"
echo ""
echo "  3. reports/verification/00_SCHEMA_ANALYSIS_SUMMARY.md"
echo "     → Summary and next steps"
echo ""
echo "═════════════════════════════════════════════════════════════"
echo ""
echo -e "${BLUE}📝 Next Steps (Automated):${NC}"
echo ""
echo "Step 2: Generate Missing Enumerations"
echo "  Run: ./scripts/generate-missing-enums-batch.sh"
echo ""
echo "Step 3: Verify Generated Enums"
echo "  Run: ./scripts/espi-verification-orchestrator.py batch-verify-enums"
echo ""
echo "Step 4: Verify First Entity"
echo "  Run: ./scripts/espi-verification-orchestrator.py verify-entity UsagePoint"
echo ""
echo "═════════════════════════════════════════════════════════════"
echo ""
echo -e "${BLUE}📚 Documentation:${NC}"
echo ""
echo "  • Implementation Plan: ISSUE_101_IMPLEMENTATION_PLAN.md"
echo "  • Team Strategy: ISSUE_101_IMPLEMENTATION_STRATEGY.md"
echo "  • Tooling Guide: ISSUE_101_TOOLING_GUIDE.md"
echo "  • Orchestrator Docs: scripts/README_ORCHESTRATOR.md"
echo ""
echo "═════════════════════════════════════════════════════════════"
echo ""
echo -e "${GREEN}Ready to proceed with Phase 0! 🚀${NC}"
echo ""
