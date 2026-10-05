---
description: Write the spec for one feature from the requirements (no code)
argument-hint: <US01|US02|US03|US04|CRUD|AUTH|DELIVERY>
---

Write the spec for feature **$ARGUMENTS**.

1. Read `CLAUDE.md`, `pokemon-api/docs/challenge/requirements.md`, `pokemon-api/docs/decisions.md`, `pokemon-api/docs/architecture.md`, `pokemon-api/docs/api-contract.md`, `pokemon-api/docs/challenge/pokeapi.md` and `pokemon-api/docs/specs/_template.md`.
2. Create `pokemon-api/docs/specs/<ID>-<slug>.md` following the template, in English.
3. **Every rule cites its source** (`REQ-` or `D-`). Anything without a source goes to "Open questions", never to "Rules".
4. Copy the contract from `pokemon-api/docs/api-contract.md`. Do not create new routes, fields or status codes. If the contract does not cover something, record it as an open question.
5. Split the work into tasks of ≤ 45 min, inside-out (domain → application → infrastructure → web), each one testable on its own.
6. **Do not write code.**

At the end, show the developer only three things for approval: the rules, the open questions and the task list.
