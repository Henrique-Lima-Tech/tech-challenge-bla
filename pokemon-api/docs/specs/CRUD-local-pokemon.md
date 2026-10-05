# CRUD: Local Pokémon read and delete

- **Requirements:** REQ-API01, REQ-DATA, REQ-CORE, REQ-T03, REQ-TEST
- **Decisions used:** D-03, D-07, D-09, D-11, D-17, D-20, D-24, D-26
- **Status:** done (approved 2026-10-05; Q1–Q4 answered with the proposals)
- **Updated by D-31 (2026-10-05):** local Pokémon now belong to the user who synced them. Every use case and `LocalPokemonRepositoryPort` method also takes the user id, another user's id answers 404, and the duplicate check is per user (`uk_local_pokemon_user_poke_api_id`, migration V3). The rest of this spec still holds.

Completes the CRUD of the local dataset. Create (`POST`, US03) and update (`PUT`, US04) already exist; this stage adds **list**, **get by id** and **delete**. The frontend already calls these three routes (`frontend/src/features/local-pokemon/api.ts`) and gets 405 today.

## Contract

Copied from `docs/api-contract.md`.

| Method | Route | Access | Requirement | Success | Errors |
|---|---|---|---|---|---|
| GET | `/api/v1/local/pokemon?page={n}&size={m}` | Authenticated | REQ-API01 | 200 | 400, 401 |
| GET | `/api/v1/local/pokemon/{id}` | Authenticated | REQ-API01 | 200 | 401, 404 |
| DELETE | `/api/v1/local/pokemon/{id}` | Authenticated | REQ-API01 | 204 | 401, 404 |

**Pagination:** `page` is 0-based, default 0; `size` defaults to 20, from 1 to 100 (D-26). Out of range → 400.

```json
{ "content": [ ... ], "page": 0, "size": 20, "totalElements": 1302, "totalPages": 66 }
```

**Local Pokémon** (each `content` item and the `GET /{id}` body):

```json
{ "id": 10, "pokeApiId": 25, "name": "pikachu", "spriteUrl": "https://...", "category": "Mouse Pokémon",
  "weightKg": 6.0, "abilities": ["static", "lightning-rod"],
  "localizedName": "ピカチュウ", "region": "Kanto", "internalTags": ["starter", "electric"] }
```

**General rules:** path `{id}` not numeric or ≤ 0 → 400; `page` ≥ 0. Errors are `ProblemDetail` (D-17).

## Rules

1. The three routes require authentication: without a valid token → 401 with a generic message (contract; D-07, D-11; CLAUDE.md "Security").
2. Data comes only from the local database; no PokéAPI call (D-09).
3. ~~Any authenticated user can read and delete any local Pokémon: there are no roles and no ownership (D-11).~~ Replaced on 2026-10-05 by D-31: each user only reads and deletes their own copies.
4. `GET /api/v1/local/pokemon` returns one page in the contract's pagination shape, each item in the "Local Pokémon" shape (contract; REQ-API01 "consistent return structures").
5. `page` defaults to 0 and must be ≥ 0; `size` defaults to 20 and must be 1 to 100; any other value → 400 `ProblemDetail` "Validation failed" with `errors[]` (contract "Pagination"; D-26).
6. `GET /api/v1/local/pokemon/{id}` returns the stored Pokémon in the "Local Pokémon" shape (contract).
7. `GET` or `DELETE` of an `{id}` that is not stored → 404 `ProblemDetail` (contract; REQ-T03).
8. `DELETE /api/v1/local/pokemon/{id}` removes the Pokémon and answers 204 with no body (contract).
9. `domain` and `application` stay framework-free; JPA entities never leave `infrastructure`; the persistence adapter translates framework exceptions (D-03; CLAUDE.md "Code").
10. Logs as in "Logging (all stages)" of `docs/architecture.md`: `DEBUG` when each request arrives (controller) and after a delete (persistence adapter, with the id); no line per read; no bodies (CLAUDE.md "Code").
11. The list is sorted by `id` ascending (Q1).
12. `GET` or `DELETE` with a path `{id}` that is not numeric or ≤ 0 → 400 `ProblemDetail`, as `PUT /{id}` already does (Q2; contract "General rules").
13. A page past the end → 200 with an empty `content` and the real `totalElements` / `totalPages` (Q3).
14. Delete is a hard delete of the row, its abilities and its internal tags; the same `pokeApiId` can be synced again afterwards (Q4).

## Out of scope

- Search, filters or sorting parameters ("Not asked for by the PDF": search/filters).
- Soft delete, audit fields. (Ownership was added later, by D-31.)
- Any change to `POST` or `PUT`.
- Seed data (stage S7, DELIVERY).

## Open questions

All answered on 2026-10-05: the developer accepted every proposal below (now rules 11–14). The 400 was added to the error column of `docs/api-contract.md` on 2026-10-05.

1. **Order of the list.** The contract does not say how the list is sorted, and without an order the database may return pages inconsistently. **Proposal:** sort by `id` ascending (the order in which Pokémon were synced).
2. **400 for an invalid `{id}` on `GET` and `DELETE`.** The contract's general rules say "path `{id}` not numeric or ≤ 0 → 400", but the error column of these two routes lists only 401 and 404 (the same gap US02 had). **Proposal:** answer 400, exactly as `PUT /{id}` already does (`@Positive` + the existing handler), and add 400 to both rows of `docs/api-contract.md` when a human next edits it.
3. **A page past the end.** For example `page=50` when there are 3 Pokémon. **Proposal:** 200 with an empty `content` and the real `totalElements` / `totalPages` (no error), as the PokéAPI list and Spring Data do.
4. **What delete removes.** **Proposal:** a hard delete of the row and its abilities and internal tags. Afterwards the same `pokeApiId` can be synced again (`POST` gives 201, not 409).

## Design (packages from `architecture.md`, no new packages)

| Layer | Class | Role |
|---|---|---|
| application | `pokemon.port.in.ListLocalPokemonUseCase` | `PageResult<LocalPokemonResult> list(int page, int size)` |
| application | `pokemon.port.in.GetLocalPokemonUseCase` | `LocalPokemonResult get(long id)` |
| application | `pokemon.port.in.DeleteLocalPokemonUseCase` | `void delete(long id)` |
| application | `pokemon.service.ListLocalPokemonService`, `GetLocalPokemonService`, `DeleteLocalPokemonService` | Implement the use cases; not found → `LocalPokemonNotFoundException` (existing) |
| application | `pokemon.port.out.LocalPokemonRepositoryPort` (existing) | New methods `PageResult<LocalPokemon> findPage(int page, int size)` and `boolean deleteById(long id)` (`false` when nothing was stored) |
| infrastructure | `pokemon.persistence.adapter.LocalPokemonPersistenceAdapter` (existing) | Implements the two new methods with `LocalPokemonJpaRepository` (Q1 order, Q4 delete) |
| web | `pokemon.controller.LocalPokemonController` (existing) | `GET`, `GET /{id}`, `DELETE /{id}`; validation as in `PokemonCatalogController.list` and `LocalPokemonController.update` |
| web | `pokemon.mapper.LocalPokemonMapper` (existing) | `PageResult<LocalPokemonResult>` → `PageResponse<LocalPokemonResponse>` |
| web | `pokemon.config.PokemonUseCaseConfig` (existing) | `@Bean` for the three services |

No new domain class: `LocalPokemon` and `LocalPokemonNotFoundException` already exist. No new migration: the tables exist (`V2__create_local_pokemon_tables.sql`).

## Tasks

Each task takes ≤ 45 min of AI work and becomes one `test:` commit plus one `feat:` commit. Inside-out order (no domain task: nothing new in `domain`):

- [x] **T1 application, get and delete:** `GetLocalPokemonUseCase`, `DeleteLocalPokemonUseCase`, their services, and `deleteById` on the port. Tests first with the port mocked: returns the stored Pokémon; unknown id → `LocalPokemonNotFoundException` on get and on delete; delete calls the port once.
- [x] **T2 application, list:** `ListLocalPokemonUseCase`, `ListLocalPokemonService`, and `findPage` on the port. Tests first: maps each `LocalPokemon` to `LocalPokemonResult` keeping page, size, totals; empty page.
- [x] **T3 infrastructure:** `findPage` and `deleteById` in `LocalPokemonPersistenceAdapter`. `@DataJpaTest` on H2 in memory: page content sorted by `id` (Q1), totals, a page past the end is empty with real totals (Q3), abilities and tags loaded; `deleteById` removes the row and its abilities and tags and returns `true`, returns `false` for an unknown id; the same `pokeApiId` can be saved again after a delete (Q4).
- [x] **T4 web:** the three endpoints in `LocalPokemonController`, the page mapping in `LocalPokemonMapper`, the beans in `PokemonUseCaseConfig`. `@WebMvcTest` with mocked use cases: 200 page JSON in the contract shape; defaults `page=0`, `size=20`; `page=-1`, `size=0`, `size=101` → 400 with `errors[]`; `GET /{id}` 200; unknown id → 404 on get and delete; `DELETE` → 204 with empty body; `{id}` = `0` or `abc` → 400 (Q2); 401 without a token on all three routes.
- [x] **T5 docs:** add the three routes to the endpoint table and the curl tour in `backend/README.md` and the user story table of the root `README.md` (CRUD row).
