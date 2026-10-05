---
description: Review the current diff with the reviewer subagent (no edits)
argument-hint: "[optional spec]"
---

Use the `reviewer` subagent to review the current changes (`git diff` + `git diff --staged` + new files), comparing them against the spec $ARGUMENTS if one is given.

Show the reviewer's findings as they come. **Do not fix anything** until the developer chooses which findings to address.
