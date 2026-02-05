#!/bin/bash
#
# Step 2: Generate All Missing Enumerations (Automated)
#
# This script generates all 30 missing enums identified in Phase 0.
# It automatically uses Claude Sonnet 4.5 for code generation.
#
# Run this AFTER first-step-automated.sh
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ORCHESTRATOR="$SCRIPT_DIR/espi-verification-orchestrator.py"

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "╔════════════════════════════════════════════════════════════╗"
echo "║                                                            ║"
echo "║     Step 2: Generate Missing Enumerations (Batch)         ║"
echo "║                                                            ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

# Check if schema analysis was done
if [ ! -f "reports/verification/espi_enumerations.md" ]; then
    echo -e "${YELLOW}⚠ Schema analysis not found${NC}"
    echo "Please run first-step-automated.sh first"
    exit 1
fi

# Check/create feature branch
CURRENT_BRANCH=$(git branch --show-current 2>/dev/null || echo "main")

if [[ $CURRENT_BRANCH == "main" ]]; then
    echo -e "${YELLOW}⚠ Currently on main branch${NC}"
    echo ""
    echo "Create a feature branch for enum generation?"
    echo "  Branch: feature/issue-101-phase-0-generate-enums"
    echo ""
    read -p "Create branch? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        git checkout -b feature/issue-101-phase-0-generate-enums
        echo -e "${GREEN}✓ Created branch:${NC} feature/issue-101-phase-0-generate-enums"
    else
        echo "Continuing on main (not recommended)"
    fi
    echo ""
fi

echo -e "${BLUE}Model:${NC} Claude Sonnet 4.5 (code generation)"
echo -e "${BLUE}Tasks:${NC} 30 enumerations"
echo -e "${BLUE}Est. Cost:${NC} ~\$4.50 total (\$0.15 each)"
echo -e "${BLUE}Est. Time:${NC} ~15 minutes"
echo ""

read -p "Press Enter to start batch generation... "
echo ""

# Usage domain enums (from espi.xsd)
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Generating Usage Domain Enums (19 enums)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

USAGE_ENUMS=(
    "AccumulationKind"
    "CommodityKind"
    "DataQualifierKind"
    "FlowDirectionKind"
    "MeasurementKind"
    "TimeAttributeKind"
    "QualityOfReading"
    "AuthorizationStatus"
    "DataCustodianApplicationStatus"
    "ESPIServiceStatus"
    "ItemKind"
    "ThirdPartyApplicationType"
    "ThirdPartyApplicationUse"
    "ThirdPartyApplicatonStatus"
    "TokenEndPointMethod"
    "AnodeType"
    "ApnodeType"
    "ParticipationCategoryMPM"
    "tOUorCPPorConsumptionTier"
)

GENERATED=0
FAILED=0

for enum in "${USAGE_ENUMS[@]}"; do
    echo -n "[$((GENERATED + FAILED + 1))/19] Generating $enum... "

    if "$ORCHESTRATOR" generate-enum "$enum" --package usage.enums &> /dev/null; then
        echo -e "${GREEN}✓${NC}"
        ((GENERATED++))
    else
        echo -e "${YELLOW}✗ (will retry)${NC}"
        ((FAILED++))
    fi

    sleep 2  # Rate limiting
done

echo ""
echo -e "${GREEN}✓ Usage enums:${NC} $GENERATED/$((GENERATED + FAILED)) generated"
echo ""

# Shared enums (used by both espi.xsd and customer.xsd)
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Generating Shared Enums (7 enums)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

SHARED_ENUMS=(
    "Currency"
    "StatusCode"
    "CRUDOperation"
    "DstRuleType"
    "EnrollmentStatus"
    "UnitMultiplierKind"
    "UnitSymbolKind"
)

SHARED_GENERATED=0
SHARED_FAILED=0

for enum in "${SHARED_ENUMS[@]}"; do
    echo -n "[$((SHARED_GENERATED + SHARED_FAILED + 1))/7] Generating $enum... "

    if "$ORCHESTRATOR" generate-enum "$enum" --package common &> /dev/null; then
        echo -e "${GREEN}✓${NC}"
        ((SHARED_GENERATED++))
    else
        echo -e "${YELLOW}✗ (will retry)${NC}"
        ((SHARED_FAILED++))
    fi

    sleep 2
done

echo ""
echo -e "${GREEN}✓ Shared enums:${NC} $SHARED_GENERATED/$((SHARED_GENERATED + SHARED_FAILED)) generated"
echo ""

# Customer domain enums (from customer.xsd)
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Generating Customer Domain Enums (2 enums)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

CUSTOMER_ENUMS=(
    "MediaType"
    "RevenueKind"
)

CUSTOMER_GENERATED=0
CUSTOMER_FAILED=0

for enum in "${CUSTOMER_ENUMS[@]}"; do
    echo -n "[$((CUSTOMER_GENERATED + CUSTOMER_FAILED + 1))/2] Generating $enum... "

    if "$ORCHESTRATOR" generate-enum "$enum" --schema customer --package customer.enums &> /dev/null; then
        echo -e "${GREEN}✓${NC}"
        ((CUSTOMER_GENERATED++))
    else
        echo -e "${YELLOW}✗ (will retry)${NC}"
        ((CUSTOMER_FAILED++))
    fi

    sleep 2
done

echo ""
echo -e "${GREEN}✓ Customer enums:${NC} $CUSTOMER_GENERATED/$((CUSTOMER_GENERATED + CUSTOMER_FAILED)) generated"
echo ""

# Summary
TOTAL_GENERATED=$((GENERATED + SHARED_GENERATED + CUSTOMER_GENERATED))
TOTAL_FAILED=$((FAILED + SHARED_FAILED + CUSTOMER_FAILED))
TOTAL_EXPECTED=30

echo "═════════════════════════════════════════════════════════════"
echo ""
echo -e "${GREEN}✅ ENUM GENERATION COMPLETE${NC}"
echo ""
echo "Results:"
echo "  ✓ Usage domain: $GENERATED/19"
echo "  ✓ Shared: $SHARED_GENERATED/7"
echo "  ✓ Customer domain: $CUSTOMER_GENERATED/2"
echo "  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Total: $TOTAL_GENERATED/$TOTAL_EXPECTED"
echo ""

if [ $TOTAL_FAILED -gt 0 ]; then
    echo -e "${YELLOW}⚠ $TOTAL_FAILED failed - will need manual retry${NC}"
    echo ""
fi

echo "═════════════════════════════════════════════════════════════"
echo ""
echo "Cost Summary:"
"$ORCHESTRATOR" show-costs
echo ""

echo "═════════════════════════════════════════════════════════════"
echo ""

# Commit generated enums
CURRENT_BRANCH=$(git branch --show-current 2>/dev/null || echo "main")

if [[ $CURRENT_BRANCH != "main" ]] && [ $TOTAL_GENERATED -gt 0 ]; then
    echo -e "${BLUE}Commit Generated Enums?${NC}"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    echo "Files to commit: $TOTAL_GENERATED new enum files"
    echo ""

    read -p "Commit to branch? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        # Stage all generated enum files
        git add openespi-common/src/main/java/org/greenbuttonalliance/espi/common/domain/usage/enums/*.java 2>/dev/null || true
        git add openespi-common/src/main/java/org/greenbuttonalliance/espi/common/domain/customer/enums/*.java 2>/dev/null || true
        git add openespi-common/src/main/java/org/greenbuttonalliance/espi/common/domain/common/*.java 2>/dev/null || true

        # Commit
        git commit -m "feat(phase-0): Implement $TOTAL_GENERATED missing ESPI 4.0 enumerations

Part of Issue #101: ESPI 4.0 schema compliance verification
Phase 0: Enumeration migration

Generated enumerations:
- Usage domain: $GENERATED enums
- Shared (common): $SHARED_GENERATED enums
- Customer domain: $CUSTOMER_GENERATED enums

All enums generated from XSD schema definitions with:
- Correct enumeration values
- getValue() and fromValue() methods
- Comprehensive Javadoc
- Apache License headers

Generated using Claude Sonnet 4.5 for code generation.

Next: Verify enums against XSD schema

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"

        echo -e "${GREEN}✓ Changes committed to:${NC} $CURRENT_BRANCH"
        echo ""
        echo "Push branch:"
        echo "  git push -u origin $CURRENT_BRANCH"
        echo ""
    fi
fi

echo "═════════════════════════════════════════════════════════════"
echo ""
echo -e "${BLUE}📝 Next Step:${NC}"
echo ""
echo "Step 3: Verify Generated Enums"
echo "  Run: ./scripts/espi-verification-orchestrator.py batch-verify-enums --directory usage/enums"
echo "       ./scripts/espi-verification-orchestrator.py batch-verify-enums --directory customer/enums"
echo ""
echo "Then create PR:"
echo "  ./scripts/git-workflow-helper.sh pr"
echo ""
echo -e "${GREEN}Ready to verify! 🎯${NC}"
echo ""
