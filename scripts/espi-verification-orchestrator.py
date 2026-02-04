#!/usr/bin/env python3
"""
ESPI 4.0 Schema Verification Orchestrator

Automatically selects the optimal Claude model for each verification task
and executes the plan with intelligent model switching.

Usage:
    ./scripts/espi-verification-orchestrator.py --task verify-entity --entity UsagePoint
    ./scripts/espi-verification-orchestrator.py --task generate-enum --enum AccumulationKind
    ./scripts/espi-verification-orchestrator.py --batch-verify-enums
    ./scripts/espi-verification-orchestrator.py --phase 0 --auto

Requirements:
    pip install anthropic pyyaml click rich
"""

import os
import sys
import json
import yaml
from pathlib import Path
from typing import Dict, List, Optional, Tuple
from dataclasses import dataclass, asdict
from enum import Enum
import anthropic
import click
from rich.console import Console
from rich.table import Table
from rich.progress import Progress, SpinnerColumn, TextColumn
from rich.panel import Panel
from rich import print as rprint

# Initialize
console = Console()
client = anthropic.Anthropic(api_key=os.environ.get("ANTHROPIC_API_KEY"))

# Model definitions
class ClaudeModel(Enum):
    OPUS = "claude-opus-4-5-20251101"
    SONNET = "claude-sonnet-4-5-20250929"
    HAIKU = "claude-haiku-4-20250514"

# Cost per million tokens (approximate)
MODEL_COSTS = {
    ClaudeModel.OPUS: {"input": 15.00, "output": 75.00},
    ClaudeModel.SONNET: {"input": 3.00, "output": 15.00},
    ClaudeModel.HAIKU: {"input": 0.25, "output": 1.25}
}

# Task type definitions
class TaskType(Enum):
    # Phase 0 - Enumeration tasks
    ANALYZE_XSD_SCHEMA = "analyze_xsd_schema"
    EXTRACT_ENUMERATIONS = "extract_enumerations"
    GENERATE_ENUM = "generate_enum"
    VERIFY_ENUM = "verify_enum"
    MOVE_ENUM = "move_enum"
    BATCH_VERIFY_ENUMS = "batch_verify_enums"

    # Phase 1-3 - Entity verification tasks
    ANALYZE_COMPLEX_TYPE = "analyze_complex_type"
    VERIFY_ENTITY = "verify_entity"
    VERIFY_EMBEDDABLE = "verify_embeddable"
    ANALYZE_DISCREPANCIES = "analyze_discrepancies"
    GENERATE_FIXES = "generate_fixes"
    GENERATE_MIGRATION = "generate_migration"
    GENERATE_TESTS = "generate_tests"

    # Documentation and reporting
    GENERATE_REPORT = "generate_report"
    CODE_REVIEW = "code_review"

# Model selection rules
TASK_MODEL_MAPPING = {
    # Use Opus for complex analysis and architecture decisions
    TaskType.ANALYZE_XSD_SCHEMA: ClaudeModel.OPUS,
    TaskType.ANALYZE_COMPLEX_TYPE: ClaudeModel.OPUS,
    TaskType.ANALYZE_DISCREPANCIES: ClaudeModel.OPUS,

    # Use Sonnet for standard code generation and verification
    TaskType.EXTRACT_ENUMERATIONS: ClaudeModel.SONNET,
    TaskType.GENERATE_ENUM: ClaudeModel.SONNET,
    TaskType.VERIFY_ENTITY: ClaudeModel.SONNET,
    TaskType.VERIFY_EMBEDDABLE: ClaudeModel.SONNET,
    TaskType.GENERATE_FIXES: ClaudeModel.SONNET,
    TaskType.GENERATE_MIGRATION: ClaudeModel.SONNET,
    TaskType.GENERATE_TESTS: ClaudeModel.SONNET,
    TaskType.GENERATE_REPORT: ClaudeModel.SONNET,
    TaskType.CODE_REVIEW: ClaudeModel.SONNET,

    # Use Haiku for repetitive verification and simple tasks
    TaskType.VERIFY_ENUM: ClaudeModel.HAIKU,
    TaskType.BATCH_VERIFY_ENUMS: ClaudeModel.HAIKU,
    TaskType.MOVE_ENUM: ClaudeModel.HAIKU,
}

@dataclass
class TaskResult:
    task_type: TaskType
    model_used: ClaudeModel
    success: bool
    output: str
    input_tokens: int
    output_tokens: int
    cost: float
    duration_seconds: float
    error: Optional[str] = None

class CostTracker:
    """Track API costs across tasks"""

    def __init__(self):
        self.total_cost = 0.0
        self.costs_by_model = {model: 0.0 for model in ClaudeModel}
        self.task_history: List[TaskResult] = []

    def calculate_cost(self, model: ClaudeModel, input_tokens: int, output_tokens: int) -> float:
        """Calculate cost for a single API call"""
        costs = MODEL_COSTS[model]
        input_cost = (input_tokens / 1_000_000) * costs["input"]
        output_cost = (output_tokens / 1_000_000) * costs["output"]
        return input_cost + output_cost

    def record_task(self, result: TaskResult):
        """Record a completed task"""
        self.task_history.append(result)
        self.total_cost += result.cost
        self.costs_by_model[result.model_used] += result.cost

    def get_summary(self) -> Dict:
        """Get cost summary"""
        return {
            "total_cost": self.total_cost,
            "total_tasks": len(self.task_history),
            "costs_by_model": {
                model.name: cost
                for model, cost in self.costs_by_model.items()
            },
            "total_input_tokens": sum(t.input_tokens for t in self.task_history),
            "total_output_tokens": sum(t.output_tokens for t in self.task_history),
        }

    def print_summary(self):
        """Print formatted cost summary"""
        summary = self.get_summary()

        table = Table(title="Cost Summary")
        table.add_column("Model", style="cyan")
        table.add_column("Cost", style="green")
        table.add_column("Tasks", style="yellow")

        for model in ClaudeModel:
            model_tasks = [t for t in self.task_history if t.model_used == model]
            table.add_row(
                model.name,
                f"${self.costs_by_model[model]:.2f}",
                str(len(model_tasks))
            )

        console.print(table)
        console.print(f"\n[bold green]Total Cost: ${summary['total_cost']:.2f}[/bold green]")
        console.print(f"Total Tasks: {summary['total_tasks']}")
        console.print(f"Input Tokens: {summary['total_input_tokens']:,}")
        console.print(f"Output Tokens: {summary['total_output_tokens']:,}")

class ESPIVerificationOrchestrator:
    """Main orchestrator for ESPI verification tasks"""

    def __init__(self, project_root: Path):
        self.project_root = project_root
        self.cost_tracker = CostTracker()
        self.config = self._load_config()

    def _load_config(self) -> Dict:
        """Load configuration"""
        config_file = self.project_root / "scripts" / "verification-config.yaml"
        if config_file.exists():
            with open(config_file) as f:
                return yaml.safe_load(f)
        return {
            "xsd_paths": {
                "espi": "openespi-common/src/main/resources/schema/ESPI_4.0/espi.xsd",
                "customer": "openespi-common/src/main/resources/schema/ESPI_4.0/customer.xsd"
            },
            "output_dir": "reports/verification"
        }

    def select_model(self, task_type: TaskType) -> ClaudeModel:
        """Select the optimal model for a task type"""
        return TASK_MODEL_MAPPING.get(task_type, ClaudeModel.SONNET)

    def execute_task(
        self,
        task_type: TaskType,
        prompt: str,
        max_tokens: int = 4000,
        force_model: Optional[ClaudeModel] = None
    ) -> TaskResult:
        """Execute a single task with automatic model selection"""

        import time
        start_time = time.time()

        # Select model
        model = force_model or self.select_model(task_type)

        console.print(f"\n[bold blue]Task:[/bold blue] {task_type.value}")
        console.print(f"[bold cyan]Model:[/bold cyan] {model.name}")

        try:
            with Progress(
                SpinnerColumn(),
                TextColumn("[progress.description]{task.description}"),
                console=console
            ) as progress:
                progress.add_task(description=f"Executing with {model.name}...", total=None)

                response = client.messages.create(
                    model=model.value,
                    max_tokens=max_tokens,
                    messages=[{"role": "user", "content": prompt}]
                )

            # Extract usage and calculate cost
            usage = response.usage
            cost = self.cost_tracker.calculate_cost(
                model,
                usage.input_tokens,
                usage.output_tokens
            )

            duration = time.time() - start_time

            result = TaskResult(
                task_type=task_type,
                model_used=model,
                success=True,
                output=response.content[0].text,
                input_tokens=usage.input_tokens,
                output_tokens=usage.output_tokens,
                cost=cost,
                duration_seconds=duration
            )

            console.print(f"[green]✓[/green] Completed in {duration:.1f}s")
            console.print(f"[green]✓[/green] Cost: ${cost:.3f}")

        except Exception as e:
            duration = time.time() - start_time
            result = TaskResult(
                task_type=task_type,
                model_used=model,
                success=False,
                output="",
                input_tokens=0,
                output_tokens=0,
                cost=0.0,
                duration_seconds=duration,
                error=str(e)
            )
            console.print(f"[red]✗[/red] Error: {e}")

        self.cost_tracker.record_task(result)
        return result

    # ==================== Phase 0: Enumeration Tasks ====================

    def analyze_xsd_schema(self, schema: str = "espi") -> TaskResult:
        """Analyze XSD schema and extract all enumerations (OPUS)"""

        xsd_path = self.project_root / self.config["xsd_paths"][schema]
        xsd_content = xsd_path.read_text()

        prompt = f"""Analyze this ESPI 4.0 XSD schema and extract all enumeration types.

XSD Schema ({schema}.xsd):
{xsd_content}

Please provide:
1. Complete list of all simpleType enumerations
2. For each enumeration:
   - Name
   - Base type (xs:string, UInt16, etc.)
   - All enumeration values with documentation
   - Any restrictions or patterns
3. Categorize enumerations by purpose (service types, measurement types, etc.)
4. Identify shared types used across both espi.xsd and customer.xsd

Format as structured JSON for easy processing."""

        return self.execute_task(
            TaskType.ANALYZE_XSD_SCHEMA,
            prompt,
            max_tokens=8000
        )

    def generate_enum(
        self,
        enum_name: str,
        xsd_schema: str = "espi",
        target_package: str = "usage.enums"
    ) -> TaskResult:
        """Generate Java enum from XSD definition (SONNET)"""

        xsd_path = self.project_root / self.config["xsd_paths"][xsd_schema]

        # Find existing enum pattern
        pattern_file = self.project_root / "openespi-common/src/main/java/org/greenbuttonalliance/espi/common/domain/common/PhaseCodeKind.java"
        pattern_content = pattern_file.read_text() if pattern_file.exists() else ""

        prompt = f"""Generate a Java enum for {enum_name} based on the ESPI 4.0 XSD definition.

XSD Schema: {xsd_path}

Search the XSD for simpleType name="{enum_name}" and generate a complete Java enum implementation.

Requirements:
1. Package: org.greenbuttonalliance.espi.common.domain.{target_package}
2. Follow this pattern from existing enum:

{pattern_content}

3. Include:
   - All enum values from XSD with correct naming (UPPER_SNAKE_CASE)
   - getValue() method
   - fromValue() method with proper error handling
   - Comprehensive Javadoc from XSD annotations
   - Apache License header (2025)

4. If base type is UInt16/numeric, use int for value
5. If base type is xs:string, use String for value

Output the complete Java file ready to save to:
openespi-common/src/main/java/org/greenbuttonalliance/espi/common/domain/{target_package}/{enum_name}.java"""

        return self.execute_task(
            TaskType.GENERATE_ENUM,
            prompt,
            max_tokens=4000
        )

    def verify_enum(self, enum_name: str, java_file: Path) -> TaskResult:
        """Verify Java enum matches XSD definition (HAIKU)"""

        java_content = java_file.read_text()

        prompt = f"""Verify that this Java enum matches its XSD definition.

Enum: {enum_name}
File: {java_file}

{java_content}

Check:
1. All XSD enumeration values present
2. Correct value mapping (numeric or string)
3. Proper error handling in fromValue()
4. Javadoc matches XSD documentation
5. Correct package location

Report: PASS or FAIL with specific issues if any."""

        return self.execute_task(
            TaskType.VERIFY_ENUM,
            prompt,
            max_tokens=2000
        )

    def batch_verify_enums(self, enum_files: List[Path]) -> List[TaskResult]:
        """Batch verify multiple enums (HAIKU)"""

        results = []

        console.print(f"\n[bold]Batch verifying {len(enum_files)} enums...[/bold]")

        for enum_file in enum_files:
            enum_name = enum_file.stem
            result = self.verify_enum(enum_name, enum_file)
            results.append(result)

            if result.success:
                if "PASS" in result.output:
                    console.print(f"  [green]✓[/green] {enum_name}")
                else:
                    console.print(f"  [yellow]⚠[/yellow] {enum_name} - Issues found")

        return results

    # ==================== Phase 1-3: Entity Verification ====================

    def verify_entity(
        self,
        entity_name: str,
        xsd_schema: str = "espi",
        complexity: str = "standard"
    ) -> TaskResult:
        """Verify entity against XSD (SONNET or OPUS based on complexity)"""

        entity_file = self._find_entity_file(entity_name)
        if not entity_file:
            raise FileNotFoundError(f"Entity {entity_name} not found")

        entity_content = entity_file.read_text()
        xsd_path = self.project_root / self.config["xsd_paths"][xsd_schema]

        prompt = f"""Verify {entity_name} against ESPI 4.0 XSD schema.

XSD Schema: {xsd_path}
Entity Implementation:

{entity_content}

Please:
1. Extract the complexType definition for {entity_name} from XSD
2. Create a field-by-field comparison table:
   - Field name
   - XSD element type → BasicType → maxLength/restriction
   - XSD minOccurs (nullability)
   - Java field type
   - @Column annotation (length, nullable)
   - Match status (✓ or ❌)

3. Identify all discrepancies:
   - Type mismatches
   - Length mismatches
   - Nullability mismatches
   - Missing fields
   - Extra fields

4. For each discrepancy, provide:
   - Current state
   - Required state
   - Recommended fix with code
   - Risk level (Low/Medium/High)

5. Check relationships:
   - @OneToMany, @ManyToOne, @ManyToMany
   - @ElementCollection
   - @Embedded

6. Generate summary:
   - Total fields checked
   - Fields correct
   - Discrepancies found
   - Overall compliance status

Format as a structured verification report."""

        task_type = TaskType.ANALYZE_COMPLEX_TYPE if complexity == "complex" else TaskType.VERIFY_ENTITY

        return self.execute_task(
            task_type,
            prompt,
            max_tokens=8000
        )

    def generate_fixes(self, entity_name: str, discrepancies: str) -> TaskResult:
        """Generate fixes for entity discrepancies (SONNET)"""

        entity_file = self._find_entity_file(entity_name)
        entity_content = entity_file.read_text()

        prompt = f"""Generate fixes for {entity_name} discrepancies.

Current Entity:
{entity_content}

Discrepancies Found:
{discrepancies}

Please generate:
1. Updated entity code with all fixes applied
2. Flyway migration script (if schema changes needed)
3. Updated test cases (if validation logic changed)
4. List of breaking changes (if any)

Show clear diffs for each change."""

        return self.execute_task(
            TaskType.GENERATE_FIXES,
            prompt,
            max_tokens=6000
        )

    def generate_verification_report(self, entity_name: str, verification_output: str) -> TaskResult:
        """Generate formatted verification report (SONNET)"""

        prompt = f"""Generate a formal verification report for {entity_name}.

Verification Results:
{verification_output}

Create a markdown report following this structure:

# {entity_name} Verification Report

**Entity**: {entity_name}
**XSD Reference**: [schema file and line]
**Verification Date**: [date]
**Status**: [PASS/FAIL/PARTIAL]

## Field Comparison Table

[Table with all fields]

## Discrepancies Found

### ✓ Correct Mappings
[List fields that match]

### ❌ Discrepancies
[List issues with details]

## Recommended Actions

1. [Prioritized fixes]

## Flyway Migration Required

[Yes/No with details]

## Test Results

[Test status]

## Compliance Score

[X/Y fields compliant (Z%)]"""

        return self.execute_task(
            TaskType.GENERATE_REPORT,
            prompt,
            max_tokens=4000
        )

    # ==================== Utility Methods ====================

    def _find_entity_file(self, entity_name: str) -> Optional[Path]:
        """Find entity Java file"""
        patterns = [
            f"openespi-common/src/main/java/**/domain/usage/{entity_name}Entity.java",
            f"openespi-common/src/main/java/**/domain/customer/entity/{entity_name}Entity.java",
        ]

        for pattern in patterns:
            files = list(self.project_root.glob(pattern))
            if files:
                return files[0]

        return None

    def save_result(self, result: TaskResult, output_file: Path):
        """Save task result to file"""
        output_file.parent.mkdir(parents=True, exist_ok=True)

        # Save output
        output_file.write_text(result.output)

        # Save metadata
        metadata = {
            "task_type": result.task_type.value,
            "model_used": result.model_used.name,
            "success": result.success,
            "cost": result.cost,
            "duration_seconds": result.duration_seconds,
            "input_tokens": result.input_tokens,
            "output_tokens": result.output_tokens,
        }

        metadata_file = output_file.with_suffix(".json")
        metadata_file.write_text(json.dumps(metadata, indent=2))

        console.print(f"[green]✓[/green] Saved to {output_file}")

# ==================== CLI Commands ====================

@click.group()
@click.pass_context
def cli(ctx):
    """ESPI 4.0 Schema Verification Orchestrator"""
    ctx.ensure_object(dict)
    project_root = Path(__file__).parent.parent
    ctx.obj['orchestrator'] = ESPIVerificationOrchestrator(project_root)

@cli.command()
@click.option('--schema', default='espi', help='Schema to analyze (espi or customer)')
@click.pass_context
def analyze_schema(ctx, schema):
    """Analyze XSD schema and extract enumerations (OPUS)"""
    orch = ctx.obj['orchestrator']

    console.print(Panel.fit(
        f"[bold]Analyzing {schema}.xsd schema[/bold]\n"
        f"Model: [cyan]Claude Opus 4.5[/cyan] (complex analysis)",
        title="XSD Schema Analysis"
    ))

    result = orch.analyze_xsd_schema(schema)

    if result.success:
        output_file = orch.project_root / f"reports/verification/{schema}_enumerations.md"
        orch.save_result(result, output_file)

@cli.command()
@click.argument('enum_name')
@click.option('--schema', default='espi', help='Source schema (espi or customer)')
@click.option('--package', default='usage.enums', help='Target package')
@click.pass_context
def generate_enum(ctx, enum_name, schema, package):
    """Generate Java enum from XSD (SONNET)"""
    orch = ctx.obj['orchestrator']

    console.print(Panel.fit(
        f"[bold]Generating {enum_name} enum[/bold]\n"
        f"Model: [cyan]Claude Sonnet 4.5[/cyan] (code generation)",
        title="Enum Generation"
    ))

    result = orch.generate_enum(enum_name, schema, package)

    if result.success:
        output_file = orch.project_root / f"openespi-common/src/main/java/org/greenbuttonalliance/espi/common/domain/{package.replace('.', '/')}/{enum_name}.java"
        output_file.parent.mkdir(parents=True, exist_ok=True)
        output_file.write_text(result.output)
        console.print(f"[green]✓[/green] Generated {output_file}")

@cli.command()
@click.argument('entity_name')
@click.option('--schema', default='espi', help='Source schema (espi or customer)')
@click.option('--complexity', default='standard', type=click.Choice(['standard', 'complex']))
@click.pass_context
def verify_entity(ctx, entity_name, schema, complexity):
    """Verify entity against XSD (SONNET/OPUS)"""
    orch = ctx.obj['orchestrator']

    model_name = "Claude Opus 4.5" if complexity == "complex" else "Claude Sonnet 4.5"

    console.print(Panel.fit(
        f"[bold]Verifying {entity_name}Entity[/bold]\n"
        f"Model: [cyan]{model_name}[/cyan]",
        title="Entity Verification"
    ))

    result = orch.verify_entity(entity_name, schema, complexity)

    if result.success:
        output_file = orch.project_root / f"reports/verification/{entity_name}_verification.md"
        orch.save_result(result, output_file)

        # Generate formatted report
        report_result = orch.generate_verification_report(entity_name, result.output)
        if report_result.success:
            report_file = orch.project_root / f"reports/verification/{entity_name}_report.md"
            orch.save_result(report_result, report_file)

@cli.command()
@click.option('--directory', default='usage/enums', help='Enum directory to verify')
@click.pass_context
def batch_verify_enums(ctx, directory):
    """Batch verify all enums in a directory (HAIKU)"""
    orch = ctx.obj['orchestrator']

    enum_dir = orch.project_root / f"openespi-common/src/main/java/org/greenbuttonalliance/espi/common/domain/{directory}"

    if not enum_dir.exists():
        console.print(f"[red]Error:[/red] Directory not found: {enum_dir}")
        return

    enum_files = list(enum_dir.glob("*.java"))

    console.print(Panel.fit(
        f"[bold]Batch verifying {len(enum_files)} enums[/bold]\n"
        f"Directory: {directory}\n"
        f"Model: [cyan]Claude Haiku[/cyan] (cost-effective batch processing)",
        title="Batch Enum Verification"
    ))

    results = orch.batch_verify_enums(enum_files)

    # Summary
    passed = sum(1 for r in results if r.success and "PASS" in r.output)
    console.print(f"\n[bold]Results:[/bold] {passed}/{len(results)} passed")

@cli.command()
@click.pass_context
def show_costs(ctx):
    """Show cost summary"""
    orch = ctx.obj['orchestrator']
    orch.cost_tracker.print_summary()

@cli.command()
@click.option('--phase', type=int, help='Phase number (0, 1, 2, or 3)')
@click.option('--tasks', help='Specific task IDs (comma-separated)', default=None)
@click.option('--dry-run', is_flag=True, help='Show what would be done without executing')
@click.pass_context
def auto_execute(ctx, phase, tasks, dry_run):
    """Auto-execute tasks from the implementation plan"""
    orch = ctx.obj['orchestrator']

    console.print(Panel.fit(
        f"[bold]Auto-execution mode[/bold]\n"
        f"Phase: {phase if phase else 'All'}\n"
        f"Tasks: {tasks if tasks else 'All'}\n"
        f"Dry run: {dry_run}",
        title="Automated Execution"
    ))

    # This would load from ISSUE_101_IMPLEMENTATION_PLAN.md
    # and execute tasks automatically

    console.print("[yellow]Auto-execution coming soon![/yellow]")
    console.print("This will parse the plan and execute tasks automatically.")

if __name__ == "__main__":
    cli(obj={})
