# Technical decisions

> Every decision here was approved by the developer. The AI follows them without asking.
> Anything **not** in this file and not in `docs/challenge/requirements.md` is not decided: the AI asks before using it.
> To change a decision, ask a human. Superseded decisions stay in the table, struck through, for history.
> **Applies to:** `Project` (the whole repository), `Backend` (`backend/`) or `Frontend` (`frontend/`).

## Decided

| ID | Applies to | Decision | Origin / reason |
|---|---|---|---|
| D-01 | Backend | Java 25 + Spring Boot 4.1.1 + Maven (wrapper in `backend/`) | Existing `pom.xml`; confirmed on 2026-10-02 |
| D-02 | Backend | ~~PostgreSQL as the relational database~~ **Superseded by D-12** | — |
| D-03 | Backend | Backend split into **4 Maven modules**: `domain`, `application`, `infrastructure`, `web`. `domain` and `application` have no framework dependencies (see D-18 for Lombok). | Developer request; layer separation enforced by the build (REQ-CORE) |
| D-04 | Backend | Base package `com.tech.challenge.<layer>.<feature>`; features `pokemon`, `user`, `shared` | Developer request |
| D-05 | Project | **Everything in English**: code (classes, methods, variables, tables, columns, JSON, log and error messages, commits) **and documentation**. Updated on 2026-10-03 to include documentation. | Developer request |
| D-06 | Project | Folder `challenge/` renamed to `backend/`. The old backlogs were moved out of the AI context and later deleted. | Single source of truth |
| D-07 | Backend | JWT authentication with `spring-boot-starter-security-oauth2-resource-server` (no hand-written authentication filter). Updated on 2026-10-03: the original name `spring-boot-starter-oauth2-resource-server` is deprecated in Boot 4.1.1 (verified in S0). `web/shared/security` holds the public/protected route configuration. | Less hand-written security code (REQ-API02) |
| D-08 | Backend | ~~Persistence tests with Testcontainers (PostgreSQL)~~ **Superseded by D-13** | — |
| D-09 | Backend | **Catalog** (US01/US02) always comes from the PokéAPI. **Local data** (US03/US04/CRUD) comes from the database. | Direct reading of REQ-US01..04 |
| D-10 | Project | **One developer; every stage is implemented by an AI agent**, one stage at a time, each in a fresh agent session. The developer approves specs, reviews tests and decides commits. `docs/` covers the backend; the frontend has its own docs in `frontend/docs/`. | Updated on 2026-10-05 |
| D-11 | Backend | Routes are **public** or **authenticated**. No roles (USER/ADMIN). | REQ-API02 only asks for "protected versus public routes" |
| D-12 | Backend | **H2 in file mode** as the relational database **in every environment** (dev, Docker). In Docker, the database file lives in a volume so it survives container re-creation. No PostgreSQL. | Decision, 2026-10-02. The PDF accepts "a relational database or appropriate data storage solution" (REQ-DB). Removes Docker from development and tests; file mode persists across restarts. |
| D-13 | Backend | Persistence tests run on **H2 itself**, no Testcontainers. No other database engine anywhere. | Consequence of D-12; one engine avoids SQL dialect differences |
| D-14 | Backend | **Flyway** for schema migrations **and** seed data, on top of H2 in file mode. Each script runs exactly once. During development, editing an already-applied script requires deleting the local database file or adding a new script. | Decision (formerly P-01). With a persistent database, seed data must be inserted once; a deleted demo record must not come back on restart (REQ-DEL02). |
| D-15 | Backend | **Spring Cache + Caffeine** (in-memory cache) on PokéAPI calls | Formerly P-02. REQ-US01-NTH / REQ-T04 and the PokéAPI Fair Use Policy. One dependency plus annotations on the PokéAPI adapter. |
| D-16 | Backend | Spring **`RestClient`** to call the PokéAPI | Formerly P-03. Native synchronous HTTP client, no extra dependency. |
| D-17 | Backend | Errors returned as **`ProblemDetail`** (RFC 9457) from a single `@RestControllerAdvice` in `web/shared/error` | Formerly P-04. REQ-T03 and "consistent return structures" (REQ-API01). |
| D-18 | Backend | **Lombok** and **MapStruct**. Lombok: allowed in all modules (compile-time only, `provided` scope; it adds no runtime dependency to `domain`/`application`). MapStruct: mappers live in `infrastructure` and `web` (`componentModel = "spring"`). Records stay the default for DTOs, commands and results. The POM must configure annotation processors in this order: Lombok, MapStruct, `lombok-mapstruct-binding`. | Developer decision; replaces the hand-written mapper proposal (P-05, rejected) |
| D-19 | Backend | US01 fetches the items of a page **in parallel** (virtual threads). Updated on 2026-10-03: concurrency is capped globally by D-28. | Formerly P-06. One page of 20 items = 41 PokéAPI calls. |
| D-20 | Project | API contract in `docs/api-contract.md` (v0 approved; open questions answered in D-25 to D-27) | Formerly P-07. Routes and JSON are fixed before implementation, so agents do not invent them. |
| D-21 | Frontend | **React + TypeScript + Vite** single-page app in `frontend/`, consuming the API through a same-origin `/api` proxy (Vite in development, Nginx in Docker) | Restored on 2026-10-05 (it was removed while the docs covered only the backend). REQ-T05 and REQ-FE01 name React or Vue as examples. |
| D-22 | Project | **`docker-compose.yml`** that starts the backend (with the H2 volume from D-12) and the frontend, in addition to each part's Dockerfile | Formerly P-09; updated on 2026-10-05 to include the frontend. Starts the whole application with one command for the demo. The PDF only requires a Dockerfile (REQ-DEL03). |
| D-23 | Backend | **No architecture tests (ArchUnit).** The `web/.../architecture` test folder was removed. | Formerly P-10. Not in the PDF; the 4 Maven modules already stop `domain`/`application` from importing Spring or JPA at compile time. |
| D-24 | Backend | Tests use **H2 in memory** (the application uses H2 in file mode, D-12). Same engine, and every test run starts clean. Refines D-13. | Decision, 2026-10-03 |
| D-25 | Backend | `PUT /api/v1/local/pokemon/{id}` replaces **all fields except the identifiers** (`id`, `pokeApiId`). | Decision, 2026-10-03. REQ-US04: "update operations for any Pokemon" |
| D-26 | Backend | Maximum page size is **100** (`size` from 1 to 100) on every paginated endpoint. | Decision, 2026-10-03 |
| D-27 | Backend | Validation limits as listed in the "Validation" section of `docs/api-contract.md`. Identifiers sent in a PUT body are **rejected** (400), not ignored. | Decision, 2026-10-03. REQ-US04 ("400 for malformed payloads", "further defensive logic"). The PDF sets no limits; these are defensive defaults. |
| D-28 | Backend | At most **10 concurrent PokéAPI calls**, **global** (shared by all requests, not per request): one shared `Semaphore` inside the infrastructure PokéAPI adapter. Virtual threads stay (D-19). | Decision, 2026-10-03. D-19 assumed 20 items (41 calls), but D-26 raised `size` to 100: 201 PokéAPI calls per cache miss. The Fair Use Policy (`docs/challenge/pokeapi.md:8`) says abusers get their IP permanently banned. A per-request cap does not protect the PokéAPI when several users call at once. |
| D-29 | Backend | Each stage of `evolutionChain` in `GET /api/v1/pokemon/{idOrName}` has a `spriteUrl`, built from the species id in the stage's `species.url` (`.../pokemon-species/{id}/`) as `https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/{id}.png`; `null` when that URL has no numeric id. This is a **recorded exception** to the "documented fields only" rule of `docs/challenge/pokeapi.md`: the pattern is the one `sprites.front_default` uses for a species' default form, but the PokéAPI does not document it. | Decision, 2026-10-03 (developer request). The evolution chain carries no sprite; one `GET /pokemon/{name}` per stage would add up to 9 PokéAPI calls per details page (eevee). |
| D-30 | Project | Every route of the API lives under **`/api/v1`** (`/api/v1/pokemon`, `/api/v1/local/pokemon`, `/api/v1/auth`). The frontend calls the same paths; the `/api` proxy (Vite, Nginx) is unchanged. | Decision, 2026-10-05 (developer request). Lets a future incompatible change ship as `/api/v2` while `/api/v1` keeps working. |
| D-31 | Backend | Each local Pokémon **belongs to the user who synced it** (the JWT `sub`, the user id). A user lists, reads, updates and deletes only their own copies; another user's id answers 404, like a missing one. One copy per user and Pokémon: `UNIQUE (user_id, poke_api_id)`. Still no roles (D-11). | Decision, 2026-10-05 (developer request). Replaces rule 3 of the CRUD spec. Migration V3 gave the existing local Pokémon to the oldest user |
| D-32 | Project | The login response also returns the user's **`name`** (`{ accessToken, tokenType, expiresIn, name }`), so the front end shows it in the header next to Sign out. No `/me` route. | Decision, 2026-10-05 (developer request) |

## Rejected

| ID | Applies to | Proposal | Reason |
|---|---|---|---|
| P-05 | Backend | Hand-written mappers, no Lombok and no MapStruct | The developer prefers Lombok + MapStruct for cleaner code (D-18) |

## Out of scope (do not implement without an explicit request)

Backend: CI, JaCoCo/coverage thresholds, architecture tests, OpenAPI/Swagger, user roles, refresh tokens, Redis, messaging, HATEOAS, scheduled or batch sync.
