#!/bin/bash
#
# Example: Phase 0 Enumeration Migration using Orchestrator
#
# This script demonstrates how the orchestrator automatically switches
# between models (Opus, Sonnet, Haiku) based on task complexity.
#

set -e  # Exit on error

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ORCHESTRATOR="$SCRIPT_DIR/espi-verification-orchestrator.py"

echo "=================================================="
echo "ESPI Phase 0: Enumeration Migration"
echo "Demonstrates automatic model switching"
echo "=================================================="
echo ""

# Check if orchestrator is executable
if [ ! -x "$ORCHESTRATOR" ]; then
    echo "Making orchestrator executable..."
    chmod +x "$ORCHESTRATOR"
fi

# Check API key
if [ -z "$ANTHROPIC_API_KEY" ]; then
    echo "ERROR: ANTHROPIC_API_KEY not set"
    echo "Please run: export ANTHROPIC_API_KEY='your-key-here'"
    exit 1
fi

echo "Step 1: Analyze XSD Schema (Claude Opus 4.5)"
echo "-------------------------------------------"
echo "Task: Deep analysis of ESPI 4.0 schema structure"
echo "Model: Opus (best for complex analysis)"
echo "Est. Cost: ~\$1.50"
echo ""
read -p "Press Enter to continue..."

$ORCHESTRATOR analyze-schema --schema espi

echo ""
echo "✓ Schema analysis complete!"
echo "  Output: reports/verification/espi_enumerations.md"
echo ""

echo "Step 2: Generate Missing Enums (Claude Sonnet 4.5)"
echo "------------------------------------------------"
echo "Task: Generate 5 example enums from XSD definitions"
echo "Model: Sonnet (best for code generation)"
echo "Est. Cost: ~\$0.15 per enum"
echo ""
read -p "Press Enter to continue..."

ENUMS=(
    "AccumulationKind"
    "CommodityKind"
    "DataQualifierKind"
    "FlowDirectionKind"
    "MeasurementKind"
)

for enum in "${ENUMS[@]}"; do
    echo ""
    echo "Generating $enum..."
    $ORCHESTRATOR generate-enum "$enum" --package usage.enums
    sleep 2  # Rate limiting
done

echo ""
echo "✓ Enum generation complete!"
echo "  Files created in: domain/usage/enums/"
echo ""

echo "Step 3: Verify Generated Enums (Claude Haiku)"
echo "-------------------------------------------"
echo "Task: Batch verify all generated enums match XSD"
echo "Model: Haiku (best for repetitive verification)"
echo "Est. Cost: ~\$0.05 per enum"
echo ""
read -p "Press Enter to continue..."

$ORCHESTRATOR batch-verify-enums --directory usage/enums

echo ""
echo "✓ Batch verification complete!"
echo ""

echo "Step 4: Cost Summary"
echo "-------------------"

$ORCHESTRATOR show-costs

echo ""
echo "=================================================="
echo "Phase 0 Example Complete!"
echo "=================================================="
echo ""
echo "What happened:"
echo "  1. Opus analyzed the complex XSD schema structure"
echo "  2. Sonnet generated 5 Java enums following patterns"
echo "  3. Haiku verified all enums in batch (cost-effective)"
echo ""
echo "Model Switching:"
echo "  ✓ Complex task → Opus (most capable)"
echo "  ✓ Code generation → Sonnet (balanced)"
echo "  ✓ Repetitive verification → Haiku (cheapest)"
echo ""
echo "Next steps:"
echo "  - Review generated enums in domain/usage/enums/"
echo "  - Check verification reports in reports/verification/"
echo "  - Run: mvn test"
echo "  - Generate remaining enums with the same pattern"
echo ""
echo "To generate all missing enums, edit and run:"
echo "  scripts/generate-all-missing-enums.sh"
echo ""
