---
description: Analyze failed CI pipeline and suggest fixes
allowed-tools: Bash(gh:*), Read, Write, Edit, Grep
model: sonnet
---

## Context
- Recent workflow runs: !`gh run list --limit 5`
## Task
Analyze the most recent failed CI run and fix the issue.
**Critical: You MUST read the actual error logs before proposing any fix.**
Steps:
1. Get the failed run ID from the list above
2. Run `gh run view <run-id> --log-failed` to see actual errors
3. Analyze the root cause - not just the symptom
4. Search the codebase for relevant files
5. Implement a fix
6. Explain what failed and why your fix addresses it
   Do not guess at solutions. The logs contain the answer.