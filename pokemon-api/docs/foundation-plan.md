# Work plan: backend, built by AI agents

> Version 4 (2026-10-03). Scope: **the backend only**. The frontend lives in `frontend/`, with its own docs in `frontend/docs/`.
> Working model (D-10): **one developer**. **Every stage is implemented by an AI agent**, one stage at a time, each in a fresh agent session. The developer approves specs, reviews tests and decides commits.

## 1. Strategy

| Principle | In practice |
|---|---|
| **Single source of truth** | The PDF became `docs/challenge/requirements.md`. Decisions live in `docs/decisions.md`. Old backlogs were removed, so they never reach the AI context. |
| **One stage = one agent session** | Each stage starts with a clean context, reads `CLAUDE.md` and only the docs it needs, and ends merged into `main`. No context drift between stages. |
| **Reference slice first (US02)** | The first user story fixes the patterns: Boot 4 imports, test style, MapStruct mapper, error handling. What was confirmed goes into `architecture.md`, and later stages copy it. |
| **Contract first** | `docs/api-contract.md` is approved (D-20, D-25 to D-27), so agents never invent routes, fields or limits. |
| **Vertical slices** | Each stage is finished end to end (domain → web) before the next one starts. |

## 2. AI infrastructure

`CLAUDE.md`, `AGENTS.md` and `.claude/` live at the repository root, one level above `pokemon-api/`; the other files below are inside `pokemon-api/`.

| File | Purpose |
|---|---|
| `CLAUDE.md` (+ `AGENTS.md` → symlink) | Global rules loaded in every session: sources of truth, stack, scope, English, TDD, security, Definition of Done |
| `docs/challenge/requirements.md` | Requirements with IDs, literal PDF text, marked interpretations and a "not asked for by the PDF" list |
| `docs/challenge/pokeapi.md` | Only the 4 PokéAPI endpoints and fields used |
| `docs/decisions.md` | Approved decisions and the out-of-scope list |
| `docs/architecture.md` | Modules, packages, role of each piece, testing per layer, Boot 4 section |
| `docs/api-contract.md` | Routes, JSON and validation rules |
| `docs/specs/_template.md` | One-page spec template |
| `.claude/commands/spec.md`, `task.md`, `review.md` | The 3 steps inside each stage |
| `.claude/agents/reviewer.md` | Read-only reviewer with a clean context |
| `.claude/settings.json` | Allows `./mvnw` and `git status/diff/log`; blocks `.env` |

## 3. Methodology: short spec + TDD + scope rules

| Technique | Use | Why |
|---|---|---|
| Spec-Driven, light version | One page per stage; every rule cites a `REQ-` or `D-` | Stops the agent from inventing requirements |
| TDD | Test first in `domain` and `application`; in the other layers when cheap | The PDF prefers TDD; `test:` → `feat:` commits make it visible in the history |
| Guardrails | Maven modules (the build keeps frameworks out of the core), `CLAUDE.md` rules, reviewer subagent | No extra tooling needed |
| Human checkpoints | Spec approval, test review after each task, commit decision | The developer's time goes where mistakes are most expensive |

## 4. Stages (in order)

| # | Stage | Requirements | Main content | Done when | Status |
|---|---|---|---|---|---|
| S0 | **FOUNDATION** | — | Parent POM + 4 module POMs, dependencies from the decisions, `ChallengeApplication` moved to `web`, H2/Flyway configuration | `./mvnw verify` green; Boot 4 findings written in `architecture.md` | ✅ Done |
| S1 | **US02** (reference slice) | REQ-US02, REQ-F01, REQ-T03 | `PokemonCatalogPort`, `RestClient` adapter with recorded fixtures, `GET /api/v1/pokemon/{idOrName}`, `ProblemDetail` handler (404, 502) | Endpoint matches the contract; patterns recorded in `architecture.md` | ✅ Done |
| S2 | **US01** | REQ-US01, REQ-US01-NTH, REQ-T04 | `GET /api/v1/pokemon` paginated, parallel item fetch (D-19), Caffeine cache (D-15) | Endpoint matches the contract; cache covered by a test | ✅ Done |
| S3 | **AUTH** | REQ-API02, REQ-DB | User table (Flyway), register, login, JWT, `/api/v1/local/**` protected | 401 on protected routes without a token; 201/200/400/409/401 per contract | ✅ Done |
| S4 | **US03** | REQ-US03, REQ-DB, REQ-DATA | Local Pokémon table (Flyway), `POST /api/v1/local/pokemon` (sync + proprietary fields) | 201/400/404/409/502 per contract | ✅ Done |
| S5 | **US04** | REQ-US04, REQ-CORE | `PUT /api/v1/local/pokemon/{id}` with all validation rules (D-25, D-27) | 200/400/404 per contract; every validation rule has a test | ✅ Done |
| S6 | **CRUD** | REQ-API01 | `GET` list, `GET` by id, `DELETE` on `/api/v1/local/pokemon` | Full CRUD per contract | ✅ Done |
| S7 | **DELIVERY** | REQ-DEL01..03, REQ-T01 | Seed data and demo credentials (Flyway, D-14), Dockerfile + `docker-compose.yml` (D-22), README | Fresh clone → `docker compose up` → demo works with seeded data | 🟡 Dockerfile, `docker-compose.yml` and README done; **seed data and demo credentials pending** |

**Why AUTH comes before US03:** the local endpoints are then created already protected. If security came last, every web test from US03 to CRUD would have to be rewritten to send a token.

**Dependencies:** S0 → S1 → everything else. S1 creates `PokemonCatalogPort`, which S2 and S4 reuse.

## 5. Stage workflow

```
fresh session ──► paste the stage prompt (section 7) ──► spec ──► ✋ developer approves
   ──► for each task: failing tests ──► code ──► green ──► ✋ developer reviews tests, says "commit", then "next"
   ──► /review ──► fix accepted findings ──► merge feat/<stage> into main
```

## 6. Schedule (Sat 2026-10-03 → Mon 2026-10-05)

| When | Stages | ✅ Milestone |
|---|---|---|
| **Sat afternoon** | S0, S1 | Reference slice US02 in `main` |
| **Sat night** | S2 | Catalog complete (US01 + US02) |
| **Sun morning** | S3, S4 | Auth + sync in `main` |
| **Sun afternoon** | S5, S6 | **Mandatory backend complete. Feature freeze: Sun 20:00** |
| **Sun night** | S7 | Delivery-ready: Docker, seed, README |
| **Mon morning** | Bug fixing, buffer | **Delivery** |

**If late, cut in this order:**
1. Parallel fetch in US01 (D-19).
2. `docker-compose.yml` (the Dockerfile stays).
3. Cache (D-15).

**Never cut:** US01–US04, 400/404 handling, auth with protected routes, tests, seed, Dockerfile, README.

## 7. Agent prompts

The prompts are pasted into a session opened at the repository root, so their paths start with `pokemon-api/`.

### S0: FOUNDATION

```text
You are implementing stage S0 FOUNDATION of pokemon-api/docs/foundation-plan.md: the multi-module Maven build. Nothing else.

Before anything, read CLAUDE.md, pokemon-api/docs/decisions.md and pokemon-api/docs/architecture.md. They are the only sources of truth.

Environment: Java 25 is installed. Maven runs through the wrapper in pokemon-api/backend/ (`cd pokemon-api/backend && ./mvnw ...`). Work on a new branch `feat/foundation` created from an up-to-date main.

GOAL
`./mvnw verify` passes inside pokemon-api/backend/ with the 4 modules: domain, application, infrastructure, web.

STEPS
1. PLAN FIRST. Before editing any file, show me a table with every dependency you intend to add: module, artifact, scope, and the decision (D-xx) that justifies it. Any dependency without a decision does not go in. STOP and wait for my approval.
2. Turn pokemon-api/backend/pom.xml into the parent POM (packaging pom, modules list, shared properties). Keep the existing Spring Boot parent and Java 25 (D-01).
3. Create the 4 module POMs with the dependency direction from pokemon-api/docs/architecture.md:
   - domain: no dependencies except Lombok (provided). Tests: JUnit Jupiter, AssertJ, Mockito only.
   - application: depends on domain; Lombok (provided). Same test dependencies as domain.
   - infrastructure: depends on application; web client, JPA, H2, Flyway, Spring Cache + Caffeine, security/JWT and password hashing support, Lombok, MapStruct (D-07, D-12, D-14 to D-18).
   - web: depends on application and infrastructure; Spring MVC, Bean Validation, OAuth2 resource server, Lombok, MapStruct, Boot test support. It is the only module with spring-boot-maven-plugin.
4. Configure the annotation processors in this order: Lombok, MapStruct, lombok-mapstruct-binding (D-18).
5. Move the Spring Initializr project out of pokemon-api/backend/src: move ChallengeApplication to pokemon-api/backend/web/src/main/java/com/tech/challenge/ and application.yaml to pokemon-api/backend/web/src/main/resources/, then delete pokemon-api/backend/src. Keep the existing .gitkeep folders.
6. Configuration in application.yaml only:
   - the application uses H2 in file mode (D-12), and the database file path is added to pokemon-api/backend/.gitignore;
   - tests use H2 in memory (D-24);
   - Flyway reads db/migration and db/seed (D-14). Empty folders are fine.
7. Make the existing context test pass from the web module.

RULES
- No production code: no entities, controllers, use cases, security configuration classes or migrations. If the context test cannot start without a Java configuration class, STOP and ask.
- Spring Boot 4 artifact names differ from Boot 3. Do not guess: verify every artifact by building. If a dependency version or name is uncertain, STOP and ask.
- Everything in English.

AT THE END
- Show the `./mvnw verify` output.
- List every dependency added with its D-xx.
- Write what you confirmed (Boot 4 starter names, test annotations if any, Lombok/MapStruct on Java 25) into the "Spring Boot 4: confirmed pitfalls" section of pokemon-api/docs/architecture.md. Only facts you verified by building.
- Suggest a commit message. Do not commit until I say "commit".
```

### S1 to S6: one user story (replace `<STAGE>` with US02, US01, AUTH, US03, US04 or CRUD)

```text
You are implementing stage <STAGE> of pokemon-api/docs/foundation-plan.md, and only <STAGE>. Read CLAUDE.md first; it defines the sources of truth and the rules you must follow. Then read the row for <STAGE> in section 4 of pokemon-api/docs/foundation-plan.md.

Work in two phases. Do not skip the stops.

PHASE 1: SPEC
- Run the /spec workflow for <STAGE> (.claude/commands/spec.md).
- Reuse the patterns and classes of the stages already merged into main (check pokemon-api/docs/specs/ and the code). Do not duplicate them.
- STOP. Show me the rules, the open questions and the task list. Wait for my approval before writing any code.

PHASE 2: TASKS (only after I approve the spec)
- Create the branch `feat/<stage-lowercase>` from an up-to-date main.
- Implement the tasks in order, one at a time, following the /task workflow (.claude/commands/task.md): failing tests first, then the minimum code, then green.
- After EACH task, STOP and report: files changed, test output, suggested commit messages. Commit only when I say "commit", then continue with the next task when I say "next".
- If a task needs something outside the spec (a new field, route, dependency, package, or a change to a class from an earlier stage), stop and ask instead of doing it.

When all tasks are done, run the /review workflow and show the findings without fixing them.
```

For **S1 (US02)** only, add this line to the prompt: *"This is the reference slice: at the end, write the patterns you established and the Spring Boot 4 facts you verified into pokemon-api/docs/architecture.md."*

### S7: DELIVERY

Use the S1–S6 prompt with `<STAGE>` = DELIVERY. The spec covers:
- the seed scripts (demo Pokémon + a demo user, D-14);
- the Dockerfile and `docker-compose.yml` with the H2 volume (D-12, D-22);
- the README: environment setup and technical documentation, REQ-DEL01.
