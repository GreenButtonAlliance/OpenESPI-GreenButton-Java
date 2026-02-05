# ESPI Verification Orchestrator

Automated schema verification tool with intelligent Claude model selection.

## Overview

The ESPI Verification Orchestrator automatically selects the optimal Claude model (Opus, Sonnet, or Haiku) based on task complexity, executes verification tasks, and tracks costs.

**Key Features:**
- ✅ Automatic model selection (Opus for complex, Sonnet for standard, Haiku for batch)
- ✅ Cost tracking and reporting
- ✅ Progress visualization
- ✅ Batch processing capabilities
- ✅ Automatic report generation

## Installation

```bash
# Install dependencies
pip install anthropic pyyaml click rich

# Set API key
export ANTHROPIC_API_KEY="your-api-key-here"

# Make script executable
chmod +x scripts/espi-verification-orchestrator.py
```

## Quick Start

### 1. Analyze XSD Schema (Opus)

Extract all enumerations from the schema:

```bash
./scripts/espi-verification-orchestrator.py analyze-schema --schema espi
```

**Model Used**: Claude Opus 4.5 (complex analysis)
**Output**: `reports/verification/espi_enumerations.md`
**Est. Cost**: ~$1.50

### 2. Generate Missing Enum (Sonnet)

Generate a Java enum from XSD definition:

```bash
./scripts/espi-verification-orchestrator.py generate-enum AccumulationKind --schema espi --package usage.enums
```

**Model Used**: Claude Sonnet 4.5 (code generation)
**Output**: `openespi-common/src/main/java/.../usage/enums/AccumulationKind.java`
**Est. Cost**: ~$0.15

### 3. Verify Entity (Sonnet or Opus)

Verify an entity matches XSD schema:

```bash
# Standard entity (Sonnet)
./scripts/espi-verification-orchestrator.py verify-entity UsagePoint --schema espi

# Complex entity (Opus)
./scripts/espi-verification-orchestrator.py verify-entity UsagePoint --schema espi --complexity complex
```

**Model Used**: Claude Sonnet 4.5 or Opus 4.5
**Output**: `reports/verification/UsagePoint_verification.md`
**Est. Cost**: $0.25 (Sonnet) or $1.50 (Opus)

### 4. Batch Verify Enums (Haiku)

Verify all enums in a directory:

```bash
./scripts/espi-verification-orchestrator.py batch-verify-enums --directory usage/enums
```

**Model Used**: Claude Haiku (cost-effective batch processing)
**Output**: Individual verification results
**Est. Cost**: ~$0.05 per enum

### 5. View Cost Summary

```bash
./scripts/espi-verification-orchestrator.py show-costs
```

## Command Reference

### analyze-schema

Analyze XSD schema and extract enumerations.

```bash
./scripts/espi-verification-orchestrator.py analyze-schema [OPTIONS]

Options:
  --schema TEXT  Schema to analyze (espi or customer) [default: espi]
```

**Model**: Claude Opus 4.5 (complex analysis)

### generate-enum

Generate Java enum from XSD definition.

```bash
./scripts/espi-verification-orchestrator.py generate-enum ENUM_NAME [OPTIONS]

Arguments:
  ENUM_NAME  Name of the enum to generate

Options:
  --schema TEXT   Source schema (espi or customer) [default: espi]
  --package TEXT  Target package [default: usage.enums]
```

**Model**: Claude Sonnet 4.5 (code generation)

**Examples:**
```bash
# Generate usage domain enum
./scripts/espi-verification-orchestrator.py generate-enum AccumulationKind

# Generate customer domain enum
./scripts/espi-verification-orchestrator.py generate-enum MediaType --schema customer --package customer.enums

# Generate shared enum
./scripts/espi-verification-orchestrator.py generate-enum Currency --package common
```

### verify-entity

Verify entity against XSD schema.

```bash
./scripts/espi-verification-orchestrator.py verify-entity ENTITY_NAME [OPTIONS]

Arguments:
  ENTITY_NAME  Name of the entity (without 'Entity' suffix)

Options:
  --schema TEXT       Source schema (espi or customer) [default: espi]
  --complexity TEXT   standard or complex [default: standard]
```

**Model**:
- `standard` → Claude Sonnet 4.5
- `complex` → Claude Opus 4.5

**Examples:**
```bash
# Standard verification
./scripts/espi-verification-orchestrator.py verify-entity MeterReading

# Complex verification (for entities with many relationships)
./scripts/espi-verification-orchestrator.py verify-entity ReadingType --complexity complex

# Customer entity
./scripts/espi-verification-orchestrator.py verify-entity CustomerAccount --schema customer
```

### batch-verify-enums

Batch verify all enums in a directory.

```bash
./scripts/espi-verification-orchestrator.py batch-verify-enums [OPTIONS]

Options:
  --directory TEXT  Enum directory to verify [default: usage/enums]
```

**Model**: Claude Haiku (cost-effective)

**Examples:**
```bash
# Verify usage enums
./scripts/espi-verification-orchestrator.py batch-verify-enums --directory usage/enums

# Verify customer enums
./scripts/espi-verification-orchestrator.py batch-verify-enums --directory customer/enums
```

### show-costs

Display cost summary for all executed tasks.

```bash
./scripts/espi-verification-orchestrator.py show-costs
```

**Output:**
```
┌───────────┬──────────┬────────┐
│ Model     │ Cost     │ Tasks  │
├───────────┼──────────┼────────┤
│ OPUS      │ $5.25    │ 3      │
│ SONNET    │ $12.50   │ 45     │
│ HAIKU     │ $2.30    │ 28     │
└───────────┴──────────┴────────┘

Total Cost: $20.05
Total Tasks: 76
Input Tokens: 125,432
Output Tokens: 48,901
```

## Workflow Examples

### Phase 0: Enumeration Migration

**Step 1: Analyze Schema (one-time)**
```bash
# Analyze espi.xsd (Opus - $1.50)
./scripts/espi-verification-orchestrator.py analyze-schema --schema espi

# Analyze customer.xsd (Opus - $1.50)
./scripts/espi-verification-orchestrator.py analyze-schema --schema customer
```

**Step 2: Generate Missing Enums in Batch**
```bash
# Create a batch generation script
cat > scripts/generate-missing-enums.sh <<'EOF'
#!/bin/bash

ENUMS=(
  "AccumulationKind"
  "CommodityKind"
  "DataQualifierKind"
  "FlowDirectionKind"
  "MeasurementKind"
  "TimeAttributeKind"
)

for enum in "${ENUMS[@]}"; do
  ./scripts/espi-verification-orchestrator.py generate-enum "$enum"
  sleep 2  # Rate limiting
done
EOF

chmod +x scripts/generate-missing-enums.sh
./scripts/generate-missing-enums.sh
```

**Cost**: ~$0.15 × 30 enums = ~$4.50

**Step 3: Verify Generated Enums**
```bash
./scripts/espi-verification-orchestrator.py batch-verify-enums --directory usage/enums
```

**Cost**: ~$0.05 × 30 enums = ~$1.50

**Total Phase 0 Cost**: ~$9.00 (vs ~$50+ manual work)

### Phase 2: Entity Verification

**Verify Core Entities:**
```bash
# Batch verify script
cat > scripts/verify-usage-entities.sh <<'EOF'
#!/bin/bash

ENTITIES=(
  "UsagePoint"
  "MeterReading"
  "IntervalBlock"
  "ReadingType:complex"  # Mark complex entities
  "IntervalReading"
  "ReadingQuality"
)

for entity_spec in "${ENTITIES[@]}"; do
  IFS=':' read -r entity complexity <<< "$entity_spec"

  if [ "$complexity" = "complex" ]; then
    ./scripts/espi-verification-orchestrator.py verify-entity "$entity" --complexity complex
  else
    ./scripts/espi-verification-orchestrator.py verify-entity "$entity"
  fi

  sleep 2
done

# Show total costs
./scripts/espi-verification-orchestrator.py show-costs
EOF

chmod +x scripts/verify-usage-entities.sh
./scripts/verify-usage-entities.sh
```

**Cost**:
- 5 standard entities (Sonnet): ~$0.25 × 5 = $1.25
- 1 complex entity (Opus): ~$1.50
- **Total**: ~$2.75

## Model Selection Rules

The orchestrator automatically selects models based on task type:

| Task Type | Model | Reasoning |
|-----------|-------|-----------|
| **XSD Schema Analysis** | Opus 4.5 | Complex pattern recognition, deep understanding |
| **Analyze Complex Entity** | Opus 4.5 | Multiple relationships, complex mappings |
| **Generate Enum** | Sonnet 4.5 | Code generation, follows patterns well |
| **Verify Entity (standard)** | Sonnet 4.5 | Good balance for field comparison |
| **Generate Fixes** | Sonnet 4.5 | Code transformation |
| **Generate Tests** | Sonnet 4.5 | Understands test patterns |
| **Verify Enum** | Haiku | Simple comparison task |
| **Batch Operations** | Haiku | Repetitive, cost-effective |

### Override Model Selection

```python
# In code
result = orchestrator.execute_task(
    TaskType.VERIFY_ENTITY,
    prompt,
    force_model=ClaudeModel.OPUS  # Force Opus instead of default Sonnet
)
```

## Configuration

Edit `scripts/verification-config.yaml`:

```yaml
# Cost limits
cost_limits:
  per_task_warn: 2.00      # Warn if task > $2
  total_warn: 100.00       # Warn if total > $100
  total_stop: 500.00       # Stop if total > $500

# Model overrides (force specific models)
model_overrides:
  verify_entity: opus      # Always use Opus for entity verification
```

## Output Files

All outputs are saved to `reports/verification/`:

```
reports/verification/
├── espi_enumerations.md          # Schema analysis
├── AccumulationKind.java         # Generated enum (copied to src/)
├── UsagePoint_verification.md    # Raw verification output
├── UsagePoint_verification.json  # Task metadata (tokens, cost, etc.)
├── UsagePoint_report.md          # Formatted verification report
└── costs_summary.json            # Overall cost tracking
```

## Integration with Other Tools

### Use with IntelliJ External Tools

Add to IntelliJ → Settings → Tools → External Tools:

```
Name: Verify Entity (Auto-model)
Program: $ProjectFileDir$/scripts/espi-verification-orchestrator.py
Arguments: verify-entity $FileNameWithoutExtension$
Working directory: $ProjectFileDir$
```

### Use in GitHub Actions

```yaml
- name: Verify Changed Entities
  env:
    ANTHROPIC_API_KEY: ${{ secrets.ANTHROPIC_API_KEY }}
  run: |
    for file in $(git diff --name-only origin/main | grep Entity.java); do
      entity=$(basename "$file" Entity.java)
      ./scripts/espi-verification-orchestrator.py verify-entity "$entity"
    done
```

### Use in Pre-commit Hook

```bash
#!/bin/bash
# .git/hooks/pre-commit

CHANGED_ENTITIES=$(git diff --cached --name-only | grep Entity.java)

if [ -n "$CHANGED_ENTITIES" ]; then
  for file in $CHANGED_ENTITIES; do
    entity=$(basename "$file" Entity.java)
    ./scripts/espi-verification-orchestrator.py verify-entity "$entity" --complexity standard
  done
fi
```

## Cost Estimation

### Per-Task Estimates

| Task | Model | Est. Cost |
|------|-------|-----------|
| Analyze schema | Opus | $1.50 |
| Generate enum | Sonnet | $0.15 |
| Verify enum | Haiku | $0.05 |
| Verify entity (standard) | Sonnet | $0.25 |
| Verify entity (complex) | Opus | $1.50 |
| Generate fixes | Sonnet | $0.30 |
| Generate tests | Sonnet | $0.25 |

### Project Totals

**Phase 0 (Enumerations):**
- Analyze schemas: 2 × $1.50 = $3.00
- Generate 30 enums: 30 × $0.15 = $4.50
- Verify 37 enums: 37 × $0.05 = $1.85
- **Total**: ~$9.35

**Phases 1-3 (Entities & Types):**
- Verify 10 complex: 10 × $1.50 = $15.00
- Verify 55 standard: 55 × $0.25 = $13.75
- Generate fixes: 20 × $0.30 = $6.00
- **Total**: ~$34.75

**Grand Total**: ~$44.10 (vs $225-315 without optimization)

## Troubleshooting

### API Key Not Set

```
Error: ANTHROPIC_API_KEY environment variable not set
```

**Fix:**
```bash
export ANTHROPIC_API_KEY="your-key-here"
# Or add to ~/.bashrc or ~/.zshrc
```

### Rate Limiting

```
Error: Rate limit exceeded
```

**Fix:** Add delays between tasks:
```bash
for entity in UsagePoint MeterReading IntervalBlock; do
  ./scripts/espi-verification-orchestrator.py verify-entity "$entity"
  sleep 5  # 5 second delay
done
```

### Cost Limit Exceeded

```
Warning: Total cost ($105.50) exceeded limit ($100.00)
```

**Fix:** Increase limit in `verification-config.yaml`:
```yaml
cost_limits:
  total_warn: 200.00
```

## Advanced Usage

### Python API

Use the orchestrator programmatically:

```python
from espi_verification_orchestrator import ESPIVerificationOrchestrator, TaskType
from pathlib import Path

# Initialize
orch = ESPIVerificationOrchestrator(Path.cwd())

# Generate enum
result = orch.generate_enum("AccumulationKind", "espi", "usage.enums")
if result.success:
    print(f"Generated! Cost: ${result.cost:.2f}")

# Verify entity
result = orch.verify_entity("UsagePoint", "espi", "standard")
print(result.output)

# Show costs
orch.cost_tracker.print_summary()
```

### Custom Task

```python
from espi_verification_orchestrator import TaskType, ClaudeModel

# Execute custom task
result = orch.execute_task(
    task_type=TaskType.GENERATE_FIXES,
    prompt="Fix these issues in UsagePointEntity: ...",
    max_tokens=6000,
    force_model=ClaudeModel.OPUS  # Override default
)
```

## Best Practices

1. **Start with Analysis**: Run `analyze-schema` once at the beginning
2. **Batch Similar Tasks**: Use batch commands for enums
3. **Use Standard First**: Try `--complexity standard` before `complex`
4. **Check Costs Regularly**: Run `show-costs` periodically
5. **Save Outputs**: All outputs are auto-saved for review
6. **Review Before Applying**: Check generated code before committing

## Next Steps

After running verifications:

1. Review generated reports in `reports/verification/`
2. Apply fixes to entities
3. Run tests: `mvn test`
4. Commit changes
5. Run orchestrator again to verify fixes

## Support

For issues or questions:
- Check logs in `reports/verification/*.json`
- Review cost summary: `show-costs`
- See ISSUE_101_TOOLING_GUIDE.md for more details

---

**Happy Verifying!** 🚀
