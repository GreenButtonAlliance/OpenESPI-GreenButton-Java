---
description: Prepare a new release with version bump and changelog
allowed-tools: Bash(git:*), Bash(gh:*), Bash(npm:*), Read, Write, Edit
model: haiku
---

## Context
- Current version (package.json): !`cat package.json | grep '"version"' | head -1`
- Commits since last tag: !`git log $(git describe --tags --abbrev=0)..HEAD --oneline`
- Existing tags: !`git tag --sort=-v:refname | head -5`
## Task
Prepare a release. Ask me what version bump type: major, minor, or patch.
Then:
1. Update version in package.json
2. Generate changelog entry from commits since last tag
3. Group changes by type (Features, Fixes, Other)
4. Stage the changes
5. Ask if I want to create the git tag
   Do not push or publish - just prepare locally for my review.