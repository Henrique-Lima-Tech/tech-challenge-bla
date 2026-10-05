---
description: Implement ONE task from an approved spec, with TDD
argument-hint: <pokemon-api/docs/specs/file.md> <Tn>
---

Implement **only** the task given in: $ARGUMENTS

1. Read `CLAUDE.md`, the spec, `pokemon-api/docs/architecture.md` and the decisions the spec cites. If the spec is not "approved", or has open questions that affect this task, **stop and ask**.
2. If another feature already has code in the same layer, follow its pattern.
3. **Red:** write the task's tests, run them (`cd pokemon-api/backend && ./mvnw -q -pl <module> -am test`) and show the failure.
4. **Green:** implement the minimum to pass and run again.
5. **Refactor:** only if needed, with the tests green.
6. Do not touch files from other tasks, add dependencies or create packages. Everything in English.
7. Mark the task as done in the spec.

At the end, report:
- the files changed;
- the test results;
- the suggested commit messages (`test: ...` and `feat: ...`). **Do not commit unless the developer asks.**
