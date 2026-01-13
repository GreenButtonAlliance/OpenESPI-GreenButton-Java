---
description: Generate and create commit from staged changes
allowed-tools: Bash(git add:*), Bash(git diff:*), Bash(git log:*), Bash(git status:*), Bash(git commit:*)
model: haiku
---

## Context
- Current branch: !`git branch --show-current`
- Commits on this branch: !`git log main..HEAD --oneline`
- Full diff from main: !`git diff main...HEAD --stat`
## Task
Generate a pull request description that includes:
1. **Summary** - One paragraph explaining what this PR does
2. **Changes** - Bullet list of key changes based on commits
3. **Testing** - How to verify these changes work
   Use the commit messages to understand intent. Keep the description concise but complete.
   After generating, ask if I want to create the PR with `gh pr create`.

## Context
- Current git status: !`git status`
- Staged changes: !`git diff --cached`
- Unstaged changes: !`git diff`
- Recent commits for style: !`git log --oneline -10`
## Task
Based on the staged changes, generate a conventional commit message.
Format: `type(scope): description`
Types: feat, fix, docs, style, refactor, test, chore
Match the commit style used in this repository's recent history.
After generating the message, ask if I want to commit with it.