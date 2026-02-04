#!/bin/bash
#
# Git Workflow Helper for Issue #101
#
# Manages feature branch creation, commits, and PR preparation
# following the recommended strategy from ISSUE_101_IMPLEMENTATION_STRATEGY.md
#

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# ==================== Git Status Check ====================

check_git_status() {
    echo -e "${BLUE}Checking Git status...${NC}"

    # Check if we're in a git repo
    if ! git rev-parse --git-dir > /dev/null 2>&1; then
        echo -e "${RED}✗ Not a git repository${NC}"
        exit 1
    fi

    # Check for uncommitted changes
    if ! git diff-index --quiet HEAD -- 2>/dev/null; then
        echo -e "${YELLOW}⚠ You have uncommitted changes${NC}"
        echo ""
        git status --short
        echo ""
        read -p "Stash these changes before creating branch? (y/n) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            git stash push -m "Auto-stash before Issue #101 branch creation"
            echo -e "${GREEN}✓ Changes stashed${NC}"
        fi
    fi

    # Check current branch
    CURRENT_BRANCH=$(git branch --show-current)
    echo -e "${GREEN}✓ Current branch:${NC} $CURRENT_BRANCH"

    # Check if main is up to date
    git fetch origin --quiet
    BEHIND=$(git rev-list --count HEAD..origin/main 2>/dev/null || echo "0")

    if [ "$BEHIND" != "0" ]; then
        echo -e "${YELLOW}⚠ Your main is $BEHIND commits behind origin/main${NC}"
        read -p "Pull latest changes? (y/n) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            git checkout main
            git pull origin main
            echo -e "${GREEN}✓ Pulled latest changes${NC}"
        fi
    else
        echo -e "${GREEN}✓ Main branch is up to date${NC}"
    fi

    echo ""
}

# ==================== Branch Naming Convention ====================

# Branch naming: feature/issue-101-<phase>-<description>
#
# Examples:
#   feature/issue-101-phase-0-enum-setup
#   feature/issue-101-phase-0-generate-enums-batch-1
#   feature/issue-101-phase-1-common-embeddables
#   feature/issue-101-phase-2-usage-core-entities

create_phase_branch() {
    local PHASE=$1
    local DESCRIPTION=$2

    BRANCH_NAME="feature/issue-101-phase-${PHASE}-${DESCRIPTION}"

    echo -e "${CYAN}Creating feature branch...${NC}"
    echo -e "${BLUE}Branch:${NC} $BRANCH_NAME"
    echo ""

    # Make sure we're starting from main
    git checkout main

    # Create and checkout new branch
    git checkout -b "$BRANCH_NAME"

    echo -e "${GREEN}✓ Created and switched to:${NC} $BRANCH_NAME"
    echo ""

    # Save branch name for later use
    echo "$BRANCH_NAME" > .git/CURRENT_ISSUE_101_BRANCH

    echo -e "${YELLOW}Remember to push this branch before creating a PR:${NC}"
    echo "  git push -u origin $BRANCH_NAME"
    echo ""
}

# ==================== Phase-Specific Branch Creation ====================

create_phase_0_setup_branch() {
    echo "╔════════════════════════════════════════════════════════════╗"
    echo "║  Phase 0: Enumeration Migration - Setup Branch            ║"
    echo "╚════════════════════════════════════════════════════════════╝"
    echo ""
    echo "This branch will contain:"
    echo "  • New directory: domain/usage/enums/"
    echo "  • Schema analysis reports"
    echo "  • Configuration updates"
    echo ""
    echo "Strategy: Additive changes only (low conflict risk)"
    echo "Merge: After schema analysis complete"
    echo ""

    read -p "Create branch? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        check_git_status
        create_phase_branch "0" "enum-setup"

        # Create directory structure
        mkdir -p "$PROJECT_ROOT/openespi-common/src/main/java/org/greenbuttonalliance/espi/common/domain/usage/enums"
        touch "$PROJECT_ROOT/openespi-common/src/main/java/org/greenbuttonalliance/espi/common/domain/usage/enums/.gitkeep"

        # Initial commit
        git add openespi-common/src/main/java/org/greenbuttonalliance/espi/common/domain/usage/enums/.gitkeep
        git commit -m "chore: Create domain/usage/enums directory for Phase 0

Part of Issue #101: ESPI 4.0 schema compliance verification
Phase 0: Enumeration migration setup

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"

        echo -e "${GREEN}✓ Initial commit created${NC}"
        echo ""
        echo -e "${CYAN}Next steps:${NC}"
        echo "  1. Run: ./scripts/first-step-automated.sh"
        echo "  2. Review generated reports"
        echo "  3. Commit reports: git add reports/ && git commit"
        echo "  4. Push: git push -u origin $(git branch --show-current)"
    fi
}

create_phase_0_generate_enums_branch() {
    local BATCH=$1  # batch-1, batch-2, or all

    echo "╔════════════════════════════════════════════════════════════╗"
    echo "║  Phase 0: Generate Enums - Batch $BATCH                   ║"
    echo "╚════════════════════════════════════════════════════════════╝"
    echo ""
    echo "This branch will contain:"
    echo "  • New Java enum classes"
    echo "  • ~10-15 enums per batch"
    echo ""
    echo "Strategy: Small batches (merge every 2-3 days)"
    echo ""

    read -p "Create branch? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        check_git_status
        create_phase_branch "0" "generate-enums-${BATCH}"

        echo -e "${CYAN}Branch ready for enum generation${NC}"
        echo ""
        echo "Run: ./scripts/generate-missing-enums-batch.sh"
    fi
}

create_phase_0_migration_branch() {
    echo "╔════════════════════════════════════════════════════════════╗"
    echo "║  Phase 0: Enum Migration (COORDINATED)                    ║"
    echo "╚════════════════════════════════════════════════════════════╝"
    echo ""
    echo -e "${YELLOW}⚠ WARNING: This is a coordinated migration window${NC}"
    echo ""
    echo "This branch will:"
    echo "  • Move existing enums to new locations"
    echo "  • Rename ServiceCategory → ServiceKind"
    echo "  • Update all imports across codebase"
    echo ""
    echo "Strategy: Coordinated 48-hour window"
    echo "  • Announce to team 1 week in advance"
    echo "  • All team members merge work before this starts"
    echo "  • Quick execution (1-2 days)"
    echo "  • Immediate merge after testing"
    echo ""
    echo -e "${RED}Only create this branch during the coordinated migration window!${NC}"
    echo ""

    read -p "Create migration branch? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        check_git_status
        create_phase_branch "0" "enum-migration"

        echo -e "${YELLOW}Migration branch created${NC}"
        echo ""
        echo "TODO:"
        echo "  1. Move enums to new locations"
        echo "  2. Update imports"
        echo "  3. Run full test suite: mvn clean test"
        echo "  4. Quick PR review"
        echo "  5. Immediate merge"
        echo "  6. Announce completion to team"
    fi
}

create_phase_1_branch() {
    local COMPONENT=$1  # common-embeddables, customer-types, etc.

    echo "╔════════════════════════════════════════════════════════════╗"
    echo "║  Phase 1: Supporting Classes - $COMPONENT                 ║"
    echo "╚════════════════════════════════════════════════════════════╝"
    echo ""

    read -p "Create branch? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        check_git_status
        create_phase_branch "1" "$COMPONENT"

        echo -e "${GREEN}✓ Branch created for Phase 1: $COMPONENT${NC}"
        echo ""
        echo "Verify 3-5 classes, then merge"
    fi
}

create_phase_2_branch() {
    local COMPONENT=$1  # usage-core, usage-summaries, etc.

    echo "╔════════════════════════════════════════════════════════════╗"
    echo "║  Phase 2: Usage Domain Entities - $COMPONENT              ║"
    echo "╚════════════════════════════════════════════════════════════╝"
    echo ""

    read -p "Create branch? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        check_git_status
        create_phase_branch "2" "$COMPONENT"

        echo -e "${GREEN}✓ Branch created for Phase 2: $COMPONENT${NC}"
        echo ""
        echo "Verify 3-5 entities, then merge"
    fi
}

create_phase_3_branch() {
    local COMPONENT=$1  # customer-account, customer-entities, etc.

    echo "╔════════════════════════════════════════════════════════════╗"
    echo "║  Phase 3: Customer Domain Entities - $COMPONENT           ║"
    echo "╚════════════════════════════════════════════════════════════╝"
    echo ""

    read -p "Create branch? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        check_git_status
        create_phase_branch "3" "$COMPONENT"

        echo -e "${GREEN}✓ Branch created for Phase 3: $COMPONENT${NC}"
        echo ""
        echo "Verify 3-5 entities, then merge"
    fi
}

# ==================== Commit Helper ====================

commit_verification_work() {
    local ENTITY_NAMES=$1  # Comma-separated entity names

    echo -e "${CYAN}Committing verification work...${NC}"
    echo ""

    # Show what will be committed
    git status --short

    echo ""
    read -p "Commit these changes? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Commit cancelled"
        return
    fi

    # Generate commit message
    BRANCH=$(git branch --show-current)

    if [[ $BRANCH == *"phase-0"* ]]; then
        COMMIT_MSG="feat: Implement missing enumerations for ESPI 4.0 compliance

Added enumerations: $ENTITY_NAMES

Part of Issue #101, Phase 0: Enumeration migration
All enums verified against XSD schema definitions

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"

    elif [[ $BRANCH == *"phase-1"* ]] || [[ $BRANCH == *"phase-2"* ]] || [[ $BRANCH == *"phase-3"* ]]; then
        COMMIT_MSG="fix: Verify entity schema compliance for $ENTITY_NAMES

Part of Issue #101: ESPI 4.0 schema compliance verification
- Field types verified against XSD
- Column lengths corrected
- Nullability constraints validated

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"

    else
        COMMIT_MSG="chore: Schema compliance work for $ENTITY_NAMES

Part of Issue #101

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
    fi

    # Stage changes
    git add -A

    # Commit
    git commit -m "$COMMIT_MSG"

    echo -e "${GREEN}✓ Committed${NC}"
    echo ""
    echo "Don't forget to push:"
    echo "  git push -u origin $(git branch --show-current)"
}

# ==================== PR Preparation ====================

prepare_pr() {
    echo "╔════════════════════════════════════════════════════════════╗"
    echo "║  Prepare Pull Request                                     ║"
    echo "╚════════════════════════════════════════════════════════════╝"
    echo ""

    BRANCH=$(git branch --show-current)

    if [[ $BRANCH == "main" ]]; then
        echo -e "${RED}✗ Cannot create PR from main branch${NC}"
        exit 1
    fi

    echo -e "${BLUE}Current branch:${NC} $BRANCH"
    echo ""

    # Run tests
    echo "Running tests before PR..."
    if mvn test -q; then
        echo -e "${GREEN}✓ All tests passing${NC}"
    else
        echo -e "${RED}✗ Tests failing${NC}"
        echo ""
        read -p "Create PR anyway? (y/n) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            echo "Fix tests first, then run this again"
            exit 1
        fi
    fi

    # Push branch
    echo ""
    echo "Pushing branch to origin..."
    git push -u origin "$BRANCH"

    echo ""
    echo -e "${GREEN}✓ Branch pushed${NC}"
    echo ""

    # Generate PR title and body
    if [[ $BRANCH == *"phase-0"* ]]; then
        PR_TITLE="feat(phase-0): Enumeration migration for ESPI 4.0 compliance"
        PR_BODY="## Summary

Part of Issue #101: ESPI 4.0 Schema Compliance Verification

This PR implements Phase 0 enumeration migration:
- Created domain/usage/enums directory structure
- Implemented missing enumerations from XSD schema
- Verified all enum values match ESPI 4.0 specification

## Changes

- New enumerations: [list added enums]
- Schema analysis reports
- Package structure updates

## Testing

- [ ] All enums verified against XSD
- [ ] Unit tests passing
- [ ] No breaking changes

## Related

Fixes #101 (Phase 0)

🤖 Generated with [Claude Code](https://claude.com/claude-code)"

    else
        PR_TITLE="fix: Schema compliance verification for Issue #101"
        PR_BODY="## Summary

Part of Issue #101: ESPI 4.0 Schema Compliance Verification

## Changes

[Describe changes]

## Testing

- [ ] Tests passing
- [ ] Schema compliance verified

## Related

Part of #101

🤖 Generated with [Claude Code](https://claude.com/claude-code)"
    fi

    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "PR Details:"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    echo "Title: $PR_TITLE"
    echo ""
    echo "Body:"
    echo "$PR_BODY"
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""

    if command -v gh &> /dev/null; then
        read -p "Create PR with GitHub CLI? (y/n) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            gh pr create --title "$PR_TITLE" --body "$PR_BODY"
            echo -e "${GREEN}✓ PR created!${NC}"
        else
            echo "Create PR manually at: https://github.com/GreenButtonAlliance/OpenESPI-GreenButton-Java/compare/$BRANCH"
        fi
    else
        echo "Create PR at: https://github.com/GreenButtonAlliance/OpenESPI-GreenButton-Java/compare/$BRANCH"
        echo ""
        echo "Or install GitHub CLI: brew install gh"
    fi
}

# ==================== Main Menu ====================

show_menu() {
    echo "╔════════════════════════════════════════════════════════════╗"
    echo "║                                                            ║"
    echo "║           Git Workflow Helper - Issue #101                 ║"
    echo "║                                                            ║"
    echo "╚════════════════════════════════════════════════════════════╝"
    echo ""
    echo "Phase 0: Enumeration Migration"
    echo "  1) Create Phase 0 setup branch (schema analysis)"
    echo "  2) Create Phase 0 enum generation branch"
    echo "  3) Create Phase 0 migration branch (coordinated)"
    echo ""
    echo "Phase 1: Supporting Classes"
    echo "  4) Create Phase 1 branch (specify component)"
    echo ""
    echo "Phase 2: Usage Domain Entities"
    echo "  5) Create Phase 2 branch (specify component)"
    echo ""
    echo "Phase 3: Customer Domain Entities"
    echo "  6) Create Phase 3 branch (specify component)"
    echo ""
    echo "Utilities"
    echo "  7) Commit current work"
    echo "  8) Prepare Pull Request"
    echo "  9) Check Git status"
    echo ""
    echo "  0) Exit"
    echo ""
    read -p "Select option: " OPTION

    case $OPTION in
        1) create_phase_0_setup_branch ;;
        2)
            read -p "Batch number or 'all': " BATCH
            create_phase_0_generate_enums_branch "$BATCH"
            ;;
        3) create_phase_0_migration_branch ;;
        4)
            read -p "Component (common-embeddables, customer-types, etc.): " COMP
            create_phase_1_branch "$COMP"
            ;;
        5)
            read -p "Component (usage-core, usage-summaries, etc.): " COMP
            create_phase_2_branch "$COMP"
            ;;
        6)
            read -p "Component (customer-account, customer-entities, etc.): " COMP
            create_phase_3_branch "$COMP"
            ;;
        7)
            read -p "Entity names (comma-separated): " ENTITIES
            commit_verification_work "$ENTITIES"
            ;;
        8) prepare_pr ;;
        9) check_git_status ;;
        0) exit 0 ;;
        *) echo "Invalid option" ;;
    esac
}

# ==================== CLI Mode ====================

if [ $# -eq 0 ]; then
    # Interactive mode
    show_menu
else
    # Command-line mode
    case "$1" in
        phase-0-setup) create_phase_0_setup_branch ;;
        phase-0-generate) create_phase_0_generate_enums_branch "${2:-batch-1}" ;;
        phase-0-migration) create_phase_0_migration_branch ;;
        phase-1) create_phase_1_branch "$2" ;;
        phase-2) create_phase_2_branch "$2" ;;
        phase-3) create_phase_3_branch "$2" ;;
        commit) commit_verification_work "$2" ;;
        pr) prepare_pr ;;
        status) check_git_status ;;
        *)
            echo "Usage: $0 [command] [args]"
            echo ""
            echo "Commands:"
            echo "  phase-0-setup              Create Phase 0 setup branch"
            echo "  phase-0-generate [batch]   Create enum generation branch"
            echo "  phase-0-migration          Create migration branch"
            echo "  phase-1 <component>        Create Phase 1 branch"
            echo "  phase-2 <component>        Create Phase 2 branch"
            echo "  phase-3 <component>        Create Phase 3 branch"
            echo "  commit <entities>          Commit current work"
            echo "  pr                         Prepare pull request"
            echo "  status                     Check git status"
            echo ""
            echo "Or run without arguments for interactive menu"
            ;;
    esac
fi
