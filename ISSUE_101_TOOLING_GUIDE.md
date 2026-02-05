# Issue #101 Tooling Guide
## Recommended Tools and Claude Models for ESPI 4.0 Schema Compliance

**For**: Senior Spring Developers working on schema verification and enumeration migration

---

## Claude Model Strategy

### Model Selection by Task Type

| Task Type | Recommended Model | Why | Cost |
|-----------|------------------|-----|------|
| **XSD Analysis** | Claude Opus 4.5 | Superior pattern recognition for complex schemas | Higher |
| **Code Generation** | Claude Sonnet 4.5 | Excellent balance of quality and speed | Medium |
| **Repetitive Verification** | Claude Haiku | Fast, cost-effective for similar tasks | Lower |
| **Architecture Decisions** | Claude Opus 4.5 | Best reasoning for trade-offs | Higher |
| **Documentation** | Claude Sonnet 4.5 | Natural language + code understanding | Medium |
| **Code Review** | Claude Sonnet 4.5 | Good at spotting issues | Medium |
| **Batch Processing** | Claude Haiku | Process many files quickly | Lower |

### Phase-Specific Model Recommendations

#### Phase 0: Enumeration Migration

**XSD Enumeration Extraction** → **Claude Opus 4.5**
```
Task: Analyze espi.xsd and customer.xsd to extract all enumeration definitions
Why: Complex XSD schemas with nested types, requires deep understanding
Example prompt:
  "Extract all simpleType enumerations from this XSD, including their:
   - Name
   - Base type
   - Enumeration values with documentation
   - Any restrictions or patterns"
```

**Enum Code Generation** → **Claude Sonnet 4.5**
```
Task: Generate Java enum classes from XSD definitions
Why: Repetitive but requires good Java code generation
Example prompt:
  "Generate a Java enum for ServiceKind based on this XSD simpleType:
   [paste XSD definition]

   Requirements:
   - Package: org.greenbuttonalliance.espi.common.domain.usage.enums
   - Include getValue() and fromValue() methods
   - Add Javadoc from XSD documentation
   - Follow project conventions"
```

**Batch Verification** → **Claude Haiku**
```
Task: Verify 30+ enum implementations match XSD
Why: Fast, repetitive comparison task
Example prompt:
  "Verify this Java enum matches the XSD definition:
   Java: [paste enum code]
   XSD: [paste simpleType]

   Check: values, order, types, documentation"
```

#### Phase 1-3: Entity Verification

**Field Type Mapping** → **Claude Sonnet 4.5**
```
Task: Map XSD complexType fields to JPA entity annotations
Why: Requires understanding both XSD and JPA conventions
Example prompt:
  "Map this XSD complexType to JPA entity annotations:
   [paste XSD definition]

   Current entity: [paste Java class]

   Verify: field types, lengths, nullability, relationships"
```

**Discrepancy Analysis** → **Claude Opus 4.5**
```
Task: Analyze complex mismatches and recommend fixes
Why: Requires reasoning about impacts and trade-offs
Example prompt:
  "I found these discrepancies in UsagePointEntity:
   [list discrepancies]

   Analyze:
   1. Which are critical vs minor?
   2. Migration strategy for each?
   3. Potential breaking changes?
   4. Recommended fix order?"
```

**Test Generation** → **Claude Sonnet 4.5**
```
Task: Generate unit tests for verified entities
Why: Good at understanding test patterns and edge cases
Example prompt:
  "Generate JUnit 5 tests for UsagePointEntity:
   [paste entity]

   Include:
   - Field validation tests
   - Relationship tests
   - Null handling
   - Edge cases
   - AssertJ assertions"
```

---

## Claude Code (CLI) Usage Patterns

### Recommended Workflow

**For Each Verification Task:**

```bash
# 1. Start Claude Code in the repository
cd /path/to/OpenESPI-GreenButton-Java
claude

# 2. Use specialized prompts for different phases
```

### Phase 0: Enumeration Migration Prompts

**Extract Missing Enums:**
```
Prompt:
"I need to identify all missing enumerations from espi.xsd.

Please:
1. Read openespi-common/src/main/resources/schema/ESPI_4.0/espi.xsd
2. Extract all simpleType definitions with enumeration restrictions
3. Check which ones exist in openespi-common/src/main/java/.../domain
4. Create a list of missing enums with their XSD definitions
5. For each missing enum, provide:
   - XSD name
   - Base type
   - Enumeration values
   - Recommended Java package location"
```

**Generate Enum Implementation:**
```
Prompt:
"Generate a Java enum for AccumulationKind:

1. Read the XSD definition from espi.xsd
2. Create AccumulationKind.java in domain/usage/enums/
3. Include:
   - Proper package declaration
   - Enum values from XSD
   - getValue() method
   - fromValue() method with validation
   - Javadoc from XSD annotations
4. Follow the pattern from existing enums like PhaseCodeKind"
```

**Batch Migration:**
```
Prompt:
"I need to move AmiBillingReadyKind from domain/common to domain/usage/enums.

Please:
1. Read the current implementation
2. Create new file in domain/usage/enums/
3. Find all usages across the codebase
4. Update all import statements
5. Create a migration checklist
6. Don't delete the old file yet (we'll do that in final cleanup)"
```

### Phase 1-3: Entity Verification Prompts

**Verify Single Entity:**
```
Prompt:
"Verify UsagePointEntity against the ESPI 4.0 XSD schema:

1. Read the XSD definition for UsagePoint complexType from espi.xsd
2. Read UsagePointEntity.java
3. Create a field-by-field comparison table:
   - Field name
   - XSD type → BasicType → restriction
   - Java type and @Column annotation
   - Match status (✓ or ❌)
4. Identify all discrepancies
5. Suggest fixes with code examples
6. Note any breaking changes"
```

**Generate Verification Report:**
```
Prompt:
"Create a complete verification report for MeterReadingEntity:

1. Extract XSD definition
2. Compare with entity implementation
3. Check Flyway migration script alignment
4. Run entity tests and report results
5. Generate a markdown report following the template in ISSUE_101_IMPLEMENTATION_PLAN.md
6. Save report to reports/MeterReadingEntity_verification.md"
```

**Fix Discrepancies:**
```
Prompt:
"Fix the discrepancies found in IntervalBlockEntity:

Discrepancies:
- Field 'duration' has length 512, should be 256 (String256)
- Field 'start' is nullable, should be NOT NULL (minOccurs=1)

Please:
1. Update the entity with correct annotations
2. Generate Flyway migration script
3. Update tests if needed
4. Show me the diff before applying"
```

---

## Claude API for Automation

### Batch Processing Script

For processing multiple entities in parallel:

```python
#!/usr/bin/env python3
"""
Automated entity verification using Claude API
"""
import anthropic
import os
from pathlib import Path

client = anthropic.Anthropic(api_key=os.environ.get("ANTHROPIC_API_KEY"))

def verify_entity(entity_name, xsd_file, java_file):
    """Verify a single entity against XSD"""

    with open(xsd_file) as f:
        xsd_content = f.read()

    with open(java_file) as f:
        java_content = f.read()

    prompt = f"""Verify {entity_name} against ESPI 4.0 XSD schema.

XSD Definition:
{xsd_content}

Java Implementation:
{java_content}

Please provide:
1. Field-by-field comparison
2. List of discrepancies
3. Recommended fixes
4. Risk assessment (Low/Medium/High)

Format as structured JSON."""

    message = client.messages.create(
        model="claude-sonnet-4-5-20250929",
        max_tokens=4000,
        messages=[
            {"role": "user", "content": prompt}
        ]
    )

    return message.content[0].text

# Process all usage domain entities
entities = [
    "UsagePoint",
    "MeterReading",
    "IntervalBlock",
    # ... etc
]

for entity in entities:
    result = verify_entity(
        entity,
        f"schema/ESPI_4.0/espi.xsd",
        f"domain/usage/{entity}Entity.java"
    )

    # Save report
    Path(f"reports/{entity}_verification.json").write_text(result)
    print(f"✓ Verified {entity}")
```

**When to use:**
- Processing 10+ similar entities
- Generating bulk reports
- Running nightly verification scans
- CI/CD integration

**Model**: Claude Sonnet 4.5 (good balance for batch work)

---

## IDE Integration

### IntelliJ IDEA + Claude Code

**Recommended Setup:**

1. **Terminal Integration**
```bash
# Add to ~/.zshrc or ~/.bashrc
alias claude-espi='cd ~/Git/OpenESPI-GreenButton-Java && claude'

# Quick verification alias
alias verify-entity='claude --prompt "Verify this entity against XSD schema"'
```

2. **External Tools Configuration**

IntelliJ → Settings → Tools → External Tools → Add:
```
Name: Verify Entity with Claude
Program: /usr/local/bin/claude
Arguments: --prompt "Verify $FileName$ against ESPI XSD schema"
Working directory: $ProjectFileDir$
```

Now: Right-click entity file → External Tools → Verify Entity with Claude

3. **File Watchers for Auto-Documentation**

Generate verification reports automatically when entities change:
```
File type: Java
Scope: Project Files
Program: claude
Arguments: --prompt "Generate verification report for $FileName$"
```

### VS Code + Claude

**Recommended Extensions:**
- Claude Code extension (if available)
- Spring Boot Tools
- XML Tools (for XSD)
- JUnit Test Runner

**Workspace Settings:**
```json
{
  "claude.contextFiles": [
    "CLAUDE.md",
    "ISSUE_101_IMPLEMENTATION_PLAN.md",
    "openespi-common/src/main/resources/schema/ESPI_4.0/*.xsd"
  ],
  "claude.defaultModel": "claude-sonnet-4-5"
}
```

---

## Specialized Tools

### 1. XSD Analysis Tools

**XMLSpy / Oxygen XML Editor**
- Visual XSD schema browser
- Generate sample XML from schema
- Validate XML against schema
- **Use for**: Understanding complex XSD structures before verification

**xsd2java (JAXB)**
```bash
# Generate reference Java classes from XSD
xjc -d generated -p org.example.reference \
    openespi-common/src/main/resources/schema/ESPI_4.0/espi.xsd

# Compare with actual implementation
diff -r generated/ openespi-common/src/main/java/.../domain/
```
- **Use with Claude**: Feed differences to Claude for analysis

### 2. Code Analysis Tools

**SonarQube / SonarLint**
```bash
# Run analysis
mvn sonar:sonar

# Check for:
# - String length violations
# - Missing null checks
# - Unused enum values
```
- **Use with Claude**: Feed SonarQube reports to Claude for remediation

**SpotBugs**
```bash
mvn spotbugs:check

# Look for:
# - Enum usage issues
# - Null pointer risks
# - Type mismatches
```

**ArchUnit** (for enforcing package structure)
```java
@Test
public void enums_should_be_in_correct_packages() {
    classes()
        .that().areEnums()
        .and().haveSimpleNameEndingWith("Kind")
        .should().resideInAPackage("..usage.enums..")
        .orShould().resideInAPackage("..customer.enums..")
        .orShould().resideInAPackage("..common")
        .check(importedClasses);
}
```

### 3. Database Tools

**Flyway Desktop / Flyway CLI**
```bash
# Validate migration scripts
flyway validate

# Generate migration from entity changes
# (Use Claude to help generate SQL)
```

**DBeaver / DataGrip**
- Compare entity definitions with database schema
- Export schema as DDL
- **Use with Claude**: Compare DDL with Flyway scripts

### 4. Testing Tools

**Testcontainers**
```java
@Testcontainers
class EntityVerificationTest {
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Test
    void verifyUsagePointSchema() {
        // Test with real database
    }
}
```

**ArchUnit** (for schema compliance)
```java
@Test
public void entities_should_have_correct_column_lengths() {
    fields()
        .that().areAnnotatedWith(Column.class)
        .and().haveRawType(String.class)
        .should(haveColumnLengthMatchingXsdBasicType())
        .check(importedClasses);
}
```

### 5. Documentation Tools

**PlantUML**
```bash
# Generate entity relationship diagrams
# Feed to Claude for validation

@startuml
entity UsagePoint {
  * id : UUID
  --
  serviceCategory : ServiceKind
  description : String(256)
}
@enduml
```

**AsciiDoc / Markdown**
- Store verification reports
- Claude can generate AsciiDoc directly

---

## MCP Servers (Model Context Protocol)

### Recommended MCP Servers for This Project

#### 1. **GitHub MCP** (Already Available)
```bash
# List all tasks
claude> What are all the open issues related to Issue #101?

# Create tracking issues
claude> Create a GitHub issue for verifying UsagePointEntity
```

#### 2. **Filesystem MCP** (Already Available)
```bash
# Search for enum usage
claude> Find all files importing AmiBillingReadyKind

# Batch file operations
claude> Move all *Kind.java files from common/ to usage/enums/
```

#### 3. **Database MCP** (If Available)
```bash
# Compare entity with database
claude> Show me the database schema for usage_point table

# Validate Flyway migrations
claude> Check if migration scripts match current entity definitions
```

#### 4. **Custom MCP: XSD Schema Server**

**Create a custom MCP server for XSD operations:**

```python
# xsd_mcp_server.py
from mcp.server import Server
import xml.etree.ElementTree as ET

server = Server("xsd-analyzer")

@server.tool()
def extract_simple_types(xsd_path: str) -> list:
    """Extract all simpleType definitions from XSD"""
    tree = ET.parse(xsd_path)
    root = tree.getroot()

    simple_types = []
    for simple_type in root.findall(".//{http://www.w3.org/2001/XMLSchema}simpleType"):
        name = simple_type.get("name")
        # Extract restrictions, enumerations, etc.
        simple_types.append({
            "name": name,
            "type": extract_base_type(simple_type),
            "restrictions": extract_restrictions(simple_type)
        })

    return simple_types

@server.tool()
def extract_complex_type(xsd_path: str, type_name: str) -> dict:
    """Extract specific complexType definition"""
    # Implementation
    pass

@server.tool()
def validate_java_enum_against_xsd(
    xsd_path: str,
    enum_name: str,
    java_code: str
) -> dict:
    """Validate Java enum matches XSD simpleType"""
    # Implementation
    pass
```

**Use with Claude Code:**
```bash
# Start with custom MCP server
claude --mcp xsd-analyzer

# Now Claude has access to XSD tools
claude> Extract all simpleTypes from espi.xsd
claude> Validate ServiceKind enum against XSD definition
```

---

## Workflow Automation

### GitHub Actions

**Automated Verification on PR:**

```yaml
# .github/workflows/schema-verification.yml
name: ESPI Schema Verification

on:
  pull_request:
    paths:
      - 'openespi-common/src/main/java/**/domain/**/*.java'

jobs:
  verify-schema-compliance:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Set up Java
        uses: actions/setup-java@v3
        with:
          java-version: '25'

      - name: Verify Entity Changes
        env:
          ANTHROPIC_API_KEY: ${{ secrets.ANTHROPIC_API_KEY }}
        run: |
          # Get changed entity files
          CHANGED_FILES=$(git diff --name-only origin/main...HEAD | grep Entity.java)

          # Verify each with Claude API
          python3 scripts/verify-entities.py $CHANGED_FILES

      - name: Comment PR with Results
        uses: actions/github-script@v6
        with:
          script: |
            // Post verification results as PR comment
            const fs = require('fs');
            const report = fs.readFileSync('verification-report.md', 'utf8');
            github.rest.issues.createComment({
              issue_number: context.issue.number,
              owner: context.repo.owner,
              repo: context.repo.repo,
              body: report
            });
```

### Pre-commit Hooks

**Validate before committing:**

```bash
# .git/hooks/pre-commit
#!/bin/bash

# Check for enum imports from wrong packages
if git diff --cached --name-only | grep -q "Entity.java"; then
    echo "Checking entity enum imports..."

    # Use Claude to validate
    claude --prompt "Check staged entity files for correct enum imports" --non-interactive

    if [ $? -ne 0 ]; then
        echo "❌ Enum import validation failed"
        exit 1
    fi
fi
```

---

## Recommended Tool Stack by Phase

### Phase 0: Enumeration Migration

| Tool | Purpose | Model |
|------|---------|-------|
| Claude Code CLI | Interactive enum generation | Sonnet 4.5 |
| Claude API + Python | Batch verification of 30+ enums | Haiku |
| XMLSpy | XSD schema browsing | - |
| IntelliJ Refactor | Safe file moves | - |
| GitHub Actions | PR validation | Sonnet 4.5 (API) |

**Daily Workflow:**
```bash
# Morning: Generate new enums
claude-espi
> "Generate next 5 missing enums from my list"

# Afternoon: Verify implementations
python3 scripts/batch-verify-enums.py

# Evening: Review and commit
git add domain/usage/enums/
git commit -m "feat: Add AccumulationKind and CommodityKind enums"
```

### Phase 1-3: Entity Verification

| Tool | Purpose | Model |
|------|---------|-------|
| Claude Code CLI | Interactive verification | Opus 4.5 (complex) / Sonnet 4.5 (standard) |
| DBeaver | Database schema comparison | - |
| SonarQube | Code quality checks | - |
| ArchUnit | Enforce package rules | - |
| Testcontainers | Integration testing | - |

**Daily Workflow:**
```bash
# Verify 3-5 entities per day
claude-espi
> "Verify UsagePointEntity against XSD"
> "Generate fixes for MeterReadingEntity discrepancies"
> "Create Flyway migration for IntervalBlockEntity changes"

# Run tests
mvn test -Dtest=UsagePointRepositoryTest

# Commit small batches
git add openespi-common/src/main/java/...UsagePointEntity.java
git commit -m "fix: Correct UsagePoint field lengths per ESPI 4.0"
```

---

## Cost Optimization

### Model Selection Strategy

**Use Opus 4.5 for (~10% of work):**
- ✅ First-time XSD analysis of complex types
- ✅ Architecture decisions (enum placement, package structure)
- ✅ Complex discrepancy analysis
- ✅ Migration strategy planning

**Use Sonnet 4.5 for (~70% of work):**
- ✅ Entity verification
- ✅ Code generation
- ✅ Test generation
- ✅ Documentation
- ✅ Code review

**Use Haiku for (~20% of work):**
- ✅ Batch verification of similar entities
- ✅ Simple enum generation (after pattern established)
- ✅ Import statement updates
- ✅ Repetitive checks

**Estimated Cost for Entire Project:**
- Opus 4.5: ~$50-75 (complex analysis)
- Sonnet 4.5: ~$150-200 (main work)
- Haiku: ~$25-40 (batch operations)
- **Total: ~$225-315** for complete 102-task project

**Cost Savings Tips:**
1. **Establish patterns early** with Opus, then use Haiku for repetition
2. **Batch similar tasks** to reuse context
3. **Cache XSD analysis** - analyze schema once with Opus, reuse results
4. **Use local tools** (grep, diff) for simple comparisons

---

## Productivity Metrics

### Expected Output with Recommended Tools

**Without Claude assistance:**
- 1-2 entities verified per day
- ~50-60 days for 102 tasks
- High error rate on manual XSD interpretation

**With Claude Code + recommended tools:**
- 3-5 entities verified per day
- ~20-30 days for 102 tasks
- Automated consistency checking

**With full automation (Claude API + scripts):**
- 10-15 entities verified per day
- ~7-10 days for 102 tasks (batch processing)
- Near-perfect consistency

### Recommended Approach: Hybrid

**Phase 0 (Enums): Interactive with Claude Code**
- Learn patterns from first 5 enums (Opus)
- Generate remaining enums with templates (Sonnet)
- Batch verify all (Haiku + automation)
- **Result**: 4 weeks → 2 weeks

**Phase 1-3 (Entities): Semi-automated**
- Verify complex entities interactively (Opus/Sonnet)
- Batch process similar entities (Haiku + scripts)
- Automated PR checks (CI/CD + API)
- **Result**: 8 weeks → 4-5 weeks

**Total Project**: 12 weeks → **6-7 weeks** with proper tooling

---

## Quick Reference: Task → Tool → Model

| Task | Best Tool | Best Model | Example |
|------|-----------|------------|---------|
| Analyze XSD schema | Claude Code | Opus 4.5 | "Explain this complexType structure" |
| Generate enum | Claude Code | Sonnet 4.5 | "Create AccumulationKind enum" |
| Verify entity fields | Claude Code | Sonnet 4.5 | "Compare UsagePoint with XSD" |
| Fix discrepancies | Claude Code | Sonnet 4.5 | "Update entity annotations" |
| Generate tests | Claude Code | Sonnet 4.5 | "Create JUnit tests" |
| Batch verify 20 enums | Claude API + Script | Haiku | Run automated script |
| Create migration | Claude Code | Sonnet 4.5 | "Generate Flyway SQL" |
| Code review | GitHub + Claude | Sonnet 4.5 | Automated PR comments |
| Architecture decision | Claude Code | Opus 4.5 | "Should we create common/enums?" |
| Documentation | Claude Code | Sonnet 4.5 | "Generate verification report" |

---

## Conclusion

### Optimal Tool Stack

**For Senior Spring Developer:**

1. **Primary**: Claude Code CLI with Sonnet 4.5 (70% of work)
2. **Complex Analysis**: Claude Code with Opus 4.5 (10% of work)
3. **Batch Operations**: Claude API + Python scripts with Haiku (20% of work)
4. **IDE**: IntelliJ IDEA + External Tools integration
5. **Analysis**: XMLSpy for XSD, SonarQube for code quality
6. **Testing**: Testcontainers + ArchUnit
7. **Automation**: GitHub Actions + pre-commit hooks

**Expected Results:**
- ✅ 50% faster than manual work
- ✅ 90%+ accuracy on first attempt
- ✅ Consistent patterns across all 102 tasks
- ✅ Automated validation and reporting
- ✅ Total cost: ~$225-315 for entire project

**Start Here:**
```bash
# Set up Claude Code with project context
cd ~/Git/OpenESPI-GreenButton-Java
claude

# First task: Analyze XSD schema
> "Read espi.xsd and extract all simpleType enumerations.
   Create a comprehensive inventory with:
   - Name
   - Base type
   - Enum values
   - Documentation
   Save to reports/espi-enumerations.md"

# Then: Generate first enum
> "Generate AccumulationKind enum based on the XSD definition.
   Follow the pattern from existing enums.
   Save to domain/usage/enums/AccumulationKind.java"
```

You're ready to start! 🚀
