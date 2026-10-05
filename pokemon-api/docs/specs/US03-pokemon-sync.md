# US03: Sync a Pokémon into the local database

- **Requirements:** REQ-US03, REQ-API01, REQ-DB, REQ-DATA, REQ-CORE, REQ-T02, REQ-T03, REQ-TEST
- **Decisions used:** D-03, D-04, D-05, D-09, D-11, D-12, D-13, D-14, D-16, D-17, D-18, D-20, D-24, D-25, D-27
- **Status:** done (written 2026-10-03; reviewed by the developer at the end of the stage)
- **Updated by D-31 (2026-10-05):** local Pokémon now belong to the user who synced them. Every use case and `LocalPokemonRepositoryPort` method also takes the user id, another user's id answers 404, and the duplicate check is per user (`uk_local_pokemon_user_poke_api_id`, migration V3). The rest of this spec still holds.

Stage S4 of `docs/foundation-plan.md`. Reuses the US02/US01 catalog patterns and the AUTH persistence, logging and security patterns ("Reference patterns" in `docs/architecture.md`).

## Contract

Copied from `docs/api-contract.md`.

| Method | Route | Access | Requirement | Success | Errors |
|---|---|---|---|---|---|
| POST | `/api/v1/local/pokemon` | Authenticated | REQ-US03, REQ-API01 | 201 + `Location` | 400, 401, 404 (not found in the PokéAPI), 409 (already synced), 502 |

"It is both the CRUD *create* and the US03 sync. It fetches the Pokémon from the PokéAPI and stores the local copy with the proprietary fields."

**Request**

```json
{ "pokemon": "pikachu", "localizedName": "ピカチュウ", "region": "Kanto", "internalTags": ["starter", "electric"] }
```

**Response** (local Pokémon representation)

```json
{ "id": 10, "pokeApiId": 25, "name": "pikachu", "spriteUrl": "https://...", "category": "Mouse Pokémon",
  "weightKg": 6.0, "abilities": ["static", "lightning-rod"],
  "localizedName": "ピカチュウ", "region": "Kanto", "internalTags": ["starter", "electric"] }
```

**Validation (D-27)**: any violation → 400 with the field errors in `errors[]`.

| Field | Required | Rule |
|---|---|---|
| `pokemon` (POST only) | Yes | Not blank, ≤ 50 chars |
| `name` (PUT) | Yes | Not blank, ≤ 50 chars |
| `spriteUrl` (PUT) | No | Valid `http`/`https` URL, ≤ 500 chars |
| `category` (PUT) | No | ≤ 50 chars |
| `weightKg` (PUT) | Yes | 0 to 9999.9, at most 1 decimal place |
| `abilities` (PUT) | Yes | 1 to 10 items; each not blank, ≤ 50 chars; no duplicates |
| `localizedName` | No | ≤ 100 chars |
| `region` | No | ≤ 100 chars |
| `internalTags` | No | ≤ 20 tags; each not blank, ≤ 30 chars; no duplicates (case-insensitive) |

General rules: malformed JSON or empty body → 400; text fields are trimmed before validation.

**Errors** (D-17): `ProblemDetail` with `type` `about:blank`, `title`, `status`, `detail`, `instance`; validation errors add `errors[]` of `{field, message}`. Never a stack trace.

## Rules

1. `POST /api/v1/local/pokemon` requires a valid JWT; without one → 401 `ProblemDetail` "Authentication required" (D-11; contract; AUTH rule 4). `SecurityConfig` already protects `/api/v1/local/**` and does not change.
2. `pokemon` is an id or a name. It is trimmed and lowercased in the application service before the catalog is called (`architecture.md` "PokéAPI lookups by name").
3. `pokeApiId`, `name`, `spriteUrl`, `category`, `weightKg` and `abilities` come from the PokéAPI through `PokemonCatalogPort` (D-09; REQ-US03), with the same meaning as in US01 (`sprites.front_default`, English genus, `weight` ÷ 10, `abilities[].ability.name`; `pokeapi.md`).
4. The proprietary fields `localizedName`, `region` and `internalTags` come from the request and are stored with the copy (REQ-US03).
5. Success → **201**, the local Pokémon representation, and a `Location` header pointing at `/api/v1/local/pokemon/{id}` (contract).
6. The PokéAPI does not know the Pokémon → **404** `ProblemDetail` "Pokemon not found" (contract; US02 convention).
7. The PokéAPI fails (other status, timeout, unreadable body, a 404 on the species call) → **502** "PokeAPI is unavailable" (contract; `architecture.md` "External failures").
8. A Pokémon whose `pokeApiId` is already stored → **409** `ProblemDetail` "Pokemon already synced" (contract). `poke_api_id` is unique in the database, and the 409 comes from that constraint, so two concurrent syncs of the same Pokémon end in one 201 and one 409 (stage notes).
9. Request validation as in the table (POST rows) → **400** "Validation failed" + `errors[]` with explicit English messages; text fields, including each tag, are trimmed before validation (D-27; contract general rules).
10. The domain model `LocalPokemon` holds the invariants of **every** local Pokémon field from the D-27 table (POST and PUT rows), so US04 reuses it; a violation is an `IllegalArgumentException` (stage notes; `architecture.md` "Validate input format in `web` and invariants in `domain`").
11. Schema in a new Flyway script `V2__…` in `db/migration`; V1 is not edited (D-14; `architecture.md`). No seed data (S7).
12. Persistence goes through a `port.out` implemented in `infrastructure/pokemon/persistence`; persistence tests run on H2 in memory (REQ-DATA; D-13; D-24).
13. Logging as in "Logging" of `architecture.md`: controller `DEBUG` on arrival, without the `pokemon` identifier on purpose (it comes from the JSON body, where `StrictHttpFirewall` does not reject CR/LF, so logging it could forge log lines); persistence adapter `DEBUG` after the save with the id and `pokeApiId`; the 409 translation `WARN` without the cause; `GlobalExceptionHandler` `DEBUG` for the 409.
14. US01, US02 and AUTH behavior does not change; their tests keep their expectations (stage notes), except `AuthFlowIntegrationTest.shouldReachProtectedRouteWhenRegisteredUserLogsIn`, which now posts `{}` to `/api/v1/local/pokemon` and expects 400 (Q1).

## Out of scope

- `GET`, `PUT` and `DELETE` on `/api/v1/local/pokemon` (S5, S6).
- Seed data (S7).
- Stats, description, evolution chain or any PokéAPI field beyond the US01 fields (`requirements.md` "Not asked for by the PDF").
- Re-sync or update of an already synced Pokémon (409, rule 8).
- Search, filters, batch or scheduled sync (`decisions.md` "Out of scope").

## Decisions taken by the agent

Implementation details answered by the docs or the reference patterns.

| # | Decision | Justification |
|---|---|---|
| A1 | New port method `PokemonCatalogPort.findSummary(idOrName)` → `Optional<PokemonSummary>`, implemented in `PokeApiCatalogAdapter` with the existing `findPokemon` + `getSpecies` client calls (both cached, D-15), the D-28 permits and `PokeApiDetailsMapper.toSummary`. Existing methods and tests unchanged. | Stage notes allow a new method only if none fits: `findDetails` has no category, weight or abilities, and `findSummaries` is paginated. `PokemonSummary` holds exactly the US01 fields the contract needs. |
| A2 | Name or id: the service fetches the Pokémon from the PokéAPI first, then stores it; "already synced" is decided by `pokeApiId`, so `"25"`, `"pikachu"` and `" Pikachu "` are the same Pokémon. | A name can only be resolved to a `pokeApiId` through the PokéAPI; the unique column is `poke_api_id` (stage notes). |
| A3 | No lookup before the insert: the unique constraint `uk_local_pokemon_poke_api_id` is the only duplicate check. The adapter's `saveAndFlush` turns that constraint violation into `PokemonAlreadySyncedException`; any other integrity violation is rethrown (AUTH A9, A10). | Stage notes: the 409 is guaranteed by the database constraint. A prior lookup would add a port method and a race without changing any answer: the PokéAPI call is needed anyway to resolve a name. |
| A4 | Lowercasing uses `Locale.ROOT` after `trim()` (`"Pikachu"` → `"pikachu"`). | `architecture.md` "PokéAPI lookups by name"; same as `GetPokemonDetailsService`. |
| A5 | Absent proprietary fields are accepted: `localizedName` and `region` stay `null`, `internalTags` becomes `[]`. | Contract: "Required: No"; domain pattern "lists copied with `List.copyOf`, `null` → empty". |
| A6 | Tags that differ only in case (`"Starter"`, `"starter"`) → 400 on `internalTags`, message "must not contain duplicates". Comparison with `toLowerCase(Locale.ROOT)`. Checked in `web` (custom constraint `@UniqueIgnoringCase` in `web/pokemon/dto/request`, like `MaxUtf8Bytes`) and in `LocalPokemon`. | D-27: "no duplicates (case-insensitive)". Hibernate Validator's `@UniqueElements` is case-sensitive. |
| A7 | `LocalPokemon` invariants (rule 10): `id` `null` or positive; `pokeApiId` positive; `name` not blank, ≤ 50; `spriteUrl` `null` or ≤ 500 and an absolute `http`/`https` URI with a host; `category` `null` or ≤ 50; `weightKg` not `null`, 0 to 9999.9, scale ≤ 1 after stripping trailing zeros; `abilities` 1 to 10, each not blank, ≤ 50, no duplicates (exact); `localizedName`, `region` `null` or ≤ 100; `internalTags` ≤ 20, each not blank, ≤ 30, no duplicates ignoring case. Lists are immutable copies; `null` lists → empty (then `abilities` fails "1 to 10"). | D-27 table, every row; `PokemonSummary` / `User` record pattern. The domain does not trim: trimming is a web format rule (contract). |
| A8 | The use case returns a `LocalPokemonResult` record (built with `LocalPokemonResult.from(LocalPokemon)`), not the domain model. | `architecture.md`: records for use case output; returning the domain model is allowed only for a read-only query (US02 Q6). MapStruct is not allowed in `application`. |
| A9 | Names: `SyncPokemonUseCase.sync(SyncPokemonCommand)`, `SyncPokemonService`, `LocalPokemonRepositoryPort.save(LocalPokemon)`, `PokemonAlreadySyncedException` (`domain.pokemon.exception`, fixed message "Pokemon already synced"), `LocalPokemonController`, `SyncPokemonRequest`, `LocalPokemonResponse`, `LocalPokemonMapper`. The service bean goes into the existing `PokemonUseCaseConfig`. | Existing packages and naming (`GetPokemonDetailsUseCase`, `UserRepositoryPort`, `EmailAlreadyUsedException`); `architecture.md` "web.<feature>.config". |
| A10 | Schema `V2__create_local_pokemon_tables.sql`: table `local_pokemon` (`id BIGINT GENERATED BY DEFAULT AS IDENTITY` PK, `poke_api_id INTEGER NOT NULL` with `uk_local_pokemon_poke_api_id`, `name VARCHAR(50) NOT NULL`, `sprite_url VARCHAR(500)`, `category VARCHAR(50)`, `weight_kg NUMERIC(5,1) NOT NULL`, `localized_name VARCHAR(100)`, `region VARCHAR(100)`); lists in `local_pokemon_abilities` (`local_pokemon_id`, `position`, `ability VARCHAR(50)`) and `local_pokemon_internal_tags` (`local_pokemon_id`, `position`, `tag VARCHAR(30)`), PK (`local_pokemon_id`, `position`), named FKs to `local_pokemon`. JPA `@ElementCollection` + `@OrderColumn(name = "position")`. | AUTH migration pattern (named constraints, D-27 lengths, `ddl-auto: validate`). A list in its own table is the relational form (REQ-DB) and keeps the order sent; a joined string would break on a tag containing the separator. |
| A11 | `Location` is the absolute URL of the new resource, built from the current request (`ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")`). | Contract "201 + `Location`"; the standard form of the header. |
| A12 | List element errors use Spring's field path (`internalTags[0]`); list size and duplicate errors use `internalTags`. | Default `FieldError.getField()`, already used by `handleMethodArgumentNotValid`. |
| A13 | Concurrency proof: a `@SpringBootTest` sends two POSTs for the same Pokémon at the same time, with `PokemonCatalogPort` mocked so both requests pass the PokéAPI step before either inserts; the answers are one 201 and one 409, and one row is stored. | Stage notes ("two concurrent syncs ... one 201 and one 409"); no test calls the real PokéAPI. |
| A14 | The 401 test is a `@WebMvcTest(LocalPokemonController.class)` test that posts a valid body without a token and expects the generic 401 `ProblemDetail` and no use case call; the other web tests send `Authorization: Bearer <token>` minted with the `JwtEncoder` bean. | AUTH pattern `SecurityConfigTest`. |
| A15 | Empty optional text (`"region": "  "` → `""` after trimming) is stored as sent (`""`), not turned into `null`. | The contract only limits the length; turning `""` into `null` would be a new rule (see open question 3). |

## Open questions

Real decisions for the developer:

1. **(Answered 2026-10-03: recommendation accepted.)** **AUTH test broken by the new controller.** `AuthFlowIntegrationTest.shouldReachProtectedRouteWhenRegisteredUserLogsIn` (S3) sends `GET /api/v1/local/pokemon` with a valid token and expects **404** ("No /api/v1/local/** controller exists yet"). Once `POST /api/v1/local/pokemon` exists, that `GET` answers **405** and the test fails. **Recommendation:** change that request to `POST /api/v1/local/pokemon` with an empty JSON object `{}` and expect **400** (validation runs only after security accepted the token). It does not call the PokéAPI and stays valid when S5/S6 add more routes. Update its comment accordingly.

Suggestions not implemented (ideas nobody asked for):

2. **PokéAPI data outside the D-27 limits.** If the PokéAPI ever returns a Pokémon that breaks a `LocalPokemon` invariant (for example no abilities, or the same ability twice), the domain throws `IllegalArgumentException` and the request answers 500. No such Pokémon is known; a dedicated error would need a contract change.
3. **Blank optional text.** `"region": "  "` is stored as `""`. Turning blank optional text into `null` would be a new rule.

## Tasks

Each task takes ≤ 45 min of AI work and becomes one `test:` commit plus one `feat:` commit. Inside-out order:

- [x] **T1 domain:** `pokemon.model.LocalPokemon`, `pokemon.exception.PokemonAlreadySyncedException`. Tests first: every invariant of A7 (accepted limits and rejected values), `null` lists → empty, immutable copies, fixed exception message.
- [x] **T2 application:** `PokemonCatalogPort.findSummary`, `LocalPokemonRepositoryPort`, `SyncPokemonUseCase`, `SyncPokemonCommand`, `LocalPokemonResult`, `SyncPokemonService`. Tests first with mocked ports: stores the catalog data plus the proprietary fields and returns the saved result; trims and lowercases `pokemon`; unknown → `PokemonNotFoundException` without saving; `ExternalServiceUnavailableException` and `PokemonAlreadySyncedException` propagate; absent proprietary fields accepted.
- [x] **T3 infrastructure, PokéAPI:** `PokeApiCatalogAdapter.findSummary`. `@RestClientTest` with the recorded fixtures: summary built from Pokémon + species; 404 → empty; other failure, including a species 404 → `ExternalServiceUnavailableException`.
- [x] **T4 infrastructure, persistence:** `V2__create_local_pokemon_tables.sql`, `LocalPokemonEntity`, `LocalPokemonJpaRepository`, `LocalPokemonPersistenceMapper`, `LocalPokemonPersistenceAdapter`. `@DataJpaTest` with `ddl-auto=validate`: save assigns an id and round-trips every field with list order; duplicate `pokeApiId` → `PokemonAlreadySyncedException`; another integrity violation is rethrown.
- [x] **T5 web:** `LocalPokemonController`, `SyncPokemonRequest`, `UniqueIgnoringCase` + validator, `LocalPokemonResponse`, `LocalPokemonMapper`, `PokemonUseCaseConfig` bean, `GlobalExceptionHandler` 409. `@WebMvcTest`: 201 + `Location` + contract JSON; trimming; every POST validation rule; malformed and empty body; 404, 409, 502; 401 without a token. `@SpringBootTest`: two concurrent syncs → one 201 and one 409 (A13). Open question 1 applied to `AuthFlowIntegrationTest`.
