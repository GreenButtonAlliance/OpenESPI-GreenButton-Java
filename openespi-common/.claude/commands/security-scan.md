---
description: Security scan of staged changes before commit
allowed-tools: Bash(git diff:*), Read, Grep
model: sonnet
---
## Context
- Staged changes: !`git diff --cached`
- Changed files: !`git diff --cached --name-only`
## Task
Review the staged changes for security issues.
Check for:
- Hardcoded secrets, API keys, or credentials
- SQL injection vulnerabilities
- XSS attack vectors
- Insecure dependencies being added
- Authentication/authorization bypasses
- Sensitive data exposure
  For each issue found:
1. File and line number
2. What the vulnerability is
3. How to fix it
   If no issues found, confirm the changes are safe to commit.
   Be thorough but avoid false positives on obvious non-issues.