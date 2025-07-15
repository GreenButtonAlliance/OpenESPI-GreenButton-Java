# Branch Strategy for OpenESPI-GreenButton-Java

## Overview

This document outlines the branching strategy for the OpenESPI-GreenButton-Java project to support team development with feature branches and pull requests.

## Branch Strategy

### Main Branch
- **Purpose:** Stable, production-ready code
- **Protection:** All changes must go through pull requests
- **Naming:** `main`

### Feature Branches
Create a new feature branch for each distinct task or feature:

**Current Tasks:**
- `feature/fix-customer-account-jpa-mapping` - Resolve CustomerAccountEntity country column duplication
- `feature/fix-thirdparty-tests` - Update test classes for Spring Boot 3.5 compatibility
- `feature/datacustodian-startup-testing` - Test DataCustodian application startup across database profiles
- `feature/oauth2-integration-testing` - Complete OAuth2AuthorizedClientManager integration testing
- `feature/authorization-server-integration` - Complete Spring Authorization Server migration

**Future Tasks:**
- `feature/performance-security-review` - Review JPA performance and security configurations
- `feature/documentation-cleanup` - Update API documentation and clean up legacy code

## Workflow Process

### For Each New Task:

```bash
# Start from main
git checkout main
git pull origin main

# Create feature branch
git checkout -b feature/task-name

# Make changes
# ... development work ...

# Commit changes
git add .
git commit -m "descriptive commit message"

# Push feature branch
git push -u origin feature/task-name

# Create Pull Request through GitHub
```

### Pull Request Guidelines

**Before Creating PR:**
- Ensure all tests pass
- Code follows project conventions
- Commits have clear, descriptive messages
- Branch is up to date with main

**PR Description Should Include:**
- Clear description of changes
- Testing performed
- Any breaking changes
- Screenshots/logs if applicable

**Review Process:**
- At least one team member review required
- All CI checks must pass
- Address reviewer feedback before merging

## Benefits of This Approach

- **Isolation:** Each feature is developed independently
- **Review:** Team can review changes before merging to main
- **Rollback:** Easy to revert specific features if issues arise
- **Parallel Work:** Team members can work on different features simultaneously
- **History:** Clear git history showing what each PR accomplished
- **Stability:** Main branch remains stable for production deployments

## Branch Naming Conventions

- **Feature branches:** `feature/short-description`
- **Bug fixes:** `fix/issue-description`
- **Hotfixes:** `hotfix/critical-issue`
- **Documentation:** `docs/update-description`

## Merge Strategy

- **Squash and merge** for feature branches to keep main history clean
- **Rebase and merge** for small, atomic changes
- **Delete feature branches** after successful merge

## Additional Notes

- Keep feature branches focused and short-lived
- Regularly sync with main to avoid conflicts
- Use descriptive commit messages following conventional commits format
- Tag releases on main branch for version tracking

---

*This strategy supports the ongoing Spring Boot 3.5 migration and ensures stable team collaboration.*