# US04: Update a Pokémon stored in the local database

- **Requirements:** REQ-US04, REQ-API01, REQ-CORE, REQ-DATA, REQ-DB, REQ-T02, REQ-T03, REQ-TEST
- **Decisions used:** D-03, D-04, D-05, D-09, D-11, D-12, D-13, D-14, D-17, D-18, D-20, D-24, D-25, D-27
- **Status:** done (written 2026-10-03; reviewed by the developer at the end of the stage)
- **Updated by D-31 (2026-10-05):** local Pokémon now belong to the user who synced them. Every use case and `LocalPokemonRepositoryPort` method also takes the user id, another user's id answers 404, and the duplicate check is per user (`uk_local_pokemon_user_poke_api_id`, migration V3). The rest of this spec still holds.

Stage S5 of `docs/foundation-plan.md`. Builds on US03 (`docs/specs/US03-pokemon-sync.md`): the `LocalPokemon` domain model with its invariants, `LocalPokemonController`, `LocalPokemonRepositoryPort`, `LocalPokemonPersistenceAdapter` and the mappers are reused. Patterns copied from "Reference patterns" in `docs/architecture.md`.

## Contract

Copied from `docs/api-contract.md`.

| Method | Route | Access | Requirement | Success | Errors |
|---|---|---|---|---|---|
| PUT | `/api/v1/local/pokemon/{id}` | Authenticated | REQ-US04, REQ-API01 | 200 | 400, 401, 404 |

"`PUT /api/v1/local/pokemon/{id}` (request): replaces **all fields except the identifiers** `id` and `pokeApiId` (D-25)."

**Request**

```json
{ "name": "pikachu", "spriteUrl": "https://...", "category": "Mouse Pokémon", "weightKg": 6.0,
  "abilities": ["static", "lightning-rod"],
  "localizedName": "Pikachu", "region": "Kanto", "internalTags": ["mascot"] }
```

**Response** (local Pokémon representation, the same shape as GET and POST)

```json
{ "id": 10, "pokeApiId": 25, "name": "pikachu", "spriteUrl": "https://...", "category": "Mouse Pokémon",
  "weightKg": 6.0, "abilities": ["static", "lightning-rod"],
  "localizedName": "Pikachu", "region": "Kanto", "internalTags": ["mascot"] }
```

**Validation (D-27)**: any violation → 400 with the field errors in `errors[]`.

| Field | Required | Rule |
|---|---|---|
| `name` | Yes | Not blank, ≤ 50 chars |
| `spriteUrl` | No | Valid `http`/`https` URL, ≤ 500 chars |
| `category` | No | ≤ 50 chars |
| `weightKg` | Yes | 0 to 9999.9, at most 1 decimal place |
| `abilities` | Yes | 1 to 10 items; each not blank, ≤ 50 chars; no duplicates |
| `localizedName` | No | ≤ 100 chars |
| `region` | No | ≤ 100 chars |
| `internalTags` | No | ≤ 20 tags; each not blank, ≤ 30 chars; no duplicates (case-insensitive) |

General rules: malformed JSON or empty body → 400; path `{id}` not numeric or ≤ 0 → 400; `id` or `pokeApiId` in the body → 400 (rejected, not ignored); text fields are trimmed before validation.

**Errors** (D-17): `ProblemDetail` with `type` `about:blank`, `title`, `status`, `detail`, `instance`; validation errors add `errors[]` of `{field, message}`. Never a stack trace.

## Rules

1. `PUT /api/v1/local/pokemon/{id}` requires a valid JWT; without one → 401 `ProblemDetail` "Authentication required" (D-11; contract). `SecurityConfig` already protects `/api/v1/local/**` and does not change.
2. The PUT **replaces every field except `id` and `pokeApiId`**: `name`, `spriteUrl`, `category`, `weightKg`, `abilities`, `localizedName`, `region` and `internalTags` take the value sent in the body (D-25; contract).
3. It is a **full replacement**: an optional field absent from the body becomes `null` (`spriteUrl`, `category`, `localizedName`, `region`) or an empty list (`internalTags`) (D-25; stage notes, answered by the developer).
4. `id` comes from the path and `pokeApiId` from the stored record; neither can be changed (D-25).
5. Success → **200** with the local Pokémon representation of the stored record (contract).
6. No record with that `id` → **404** `ProblemDetail` (REQ-US04 "404 responses for missing records"; contract).
7. Request validation as in the table → **400** "Validation failed" + `errors[]` with explicit English messages; every text field, including each ability and each tag, is trimmed before validation (D-27; contract general rules).
8. `id` or `pokeApiId` sent in the body → **400**, never ignored (D-27). Any other unknown property is ignored, as for `POST` (Jackson 3 default; stage notes, answered by the developer).
9. Path `{id}` not numeric or ≤ 0 → **400** "Validation failed" + `errors[]` (contract general rules), without reaching the application layer.
10. Malformed JSON or an empty body → **400** (contract general rules), with no stack trace.
11. The body is validated **before** the record is looked up: an invalid body for a non-existent `id` answers 400, not 404 (stage notes, answered by the developer; it is also what Spring does, since argument resolution runs before the controller method).
12. The domain invariants of `LocalPokemon` (US03, A7) are the business rules of the update too; the model is reused unchanged (REQ-CORE; stage notes).
13. Persistence goes through the existing `LocalPokemonRepositoryPort`, implemented in `infrastructure/pokemon/persistence`; persistence tests run on H2 in memory (REQ-DATA; D-13; D-24). **No new Flyway migration:** US04 adds no column (stage notes).
14. Two updates of the same record at the same time: **the last write wins**; no optimistic locking and no version column (stage notes, answered by the developer; the PDF asks for no concurrency control).
15. Logging as in "Logging" of `docs/architecture.md`: controller `DEBUG` on arrival with the path id (never a body value); persistence adapter `DEBUG` after the update with the id; `GlobalExceptionHandler` `DEBUG` for the 404.
16. US01, US02, AUTH and US03 behavior does not change and their tests keep their expectations (stage notes).

## Out of scope

- `GET` list, `GET` by id and `DELETE` on `/api/v1/local/pokemon` (S6 CRUD).
- Partial update (`PATCH`): the contract has no such route.
- Re-reading the Pokémon from the PokéAPI during an update: the local copy is the source of the local data (D-09), and `pokeApiId` cannot change (D-25).
- Changing `id` or `pokeApiId`, or creating a record through `PUT` (upsert): the contract answers 404 for a missing record.
- Optimistic locking, `If-Match`/`ETag`, audit columns (`updatedAt`), change history (`decisions.md` "Out of scope"; nothing in the PDF).
- Seed data (S7).

## Decisions taken by the agent

Implementation details already answered by the docs, the reference patterns or the developer's answers.

| # | Decision | Justification |
|---|---|---|
| A1 | New domain exception `LocalPokemonNotFoundException(long id)` (`domain.pokemon.exception`, message `"Local Pokemon not found: " + id`), with an `@ExceptionHandler` in `GlobalExceptionHandler` returning 404 with the fixed detail `"Local Pokemon not found"`. | CLAUDE.md: "Every new domain exception gets an `@ExceptionHandler` ... with a fixed English `detail`". A separate exception keeps "the local record does not exist" apart from `PokemonNotFoundException`, which the same controller already uses for "the PokéAPI does not know this Pokémon" (US03 rule 6). The contract fixes the **status** (404), not the detail text. S6 reuses it for `GET /{id}` and `DELETE /{id}`. |
| A2 | Two additions to `LocalPokemonRepositoryPort`: `Optional<LocalPokemon> findById(long id)`; the existing `save(LocalPokemon)` is reused for the update (a `LocalPokemon` with a non-null `id` is merged into the existing row). No existing method or test changes. | Stage notes ("Adding methods is expected"). `architecture.md`: "the `port.out` returns `Optional` ... the service turns an empty result into the domain's not-found exception". JPA `merge` already performs exactly the full replacement D-25 asks for, including the two list tables; a second port method would duplicate it. |
| A3 | `UpdateLocalPokemonService` loads the record, keeps `id` and `pokeApiId` from it, and rebuilds the model with the command's values, then saves. | D-25 and rule 4; it is also the only way the domain invariants run over the new values (rule 12). |
| A4 | The identifiers are rejected by declaring them in the request record with `@Null(message = "must not be sent")` (`Long id`, `Integer pokeApiId`). Any other unknown property stays ignored. | D-27 "rejected, not ignored", and the developer's answer: rejecting every unknown property would mean reconfiguring Jackson for the whole API, including the `POST` of US03. Bean Validation reports the offending field in `errors[]`, like every other 400. |
| A5 | Absent optional fields become `null`, and an absent `internalTags` becomes `[]`. Blank optional text (`"region": "  "`) is stored as `""`, as in US03 (A15). | Rule 3 (D-25 + the developer's answer); `LocalPokemon` turns a `null` list into an empty one (US03 A7). |
| A6 | Names: `UpdateLocalPokemonUseCase.update(UpdateLocalPokemonCommand)`, `UpdateLocalPokemonService`, `UpdateLocalPokemonRequest`; the response is the existing `LocalPokemonResponse` and the result the existing `LocalPokemonResult`. The bean goes into the existing `PokemonUseCaseConfig`; `LocalPokemonMapper` gets `toCommand(long pokemonId, UpdateLocalPokemonRequest request)` and reuses `toResponse`. | US03 naming (A9) and `architecture.md` "web.`<feature>`.config"; the contract gives `PUT` and `POST` the same response body, so the DTO is shared. The mapper parameter is named `pokemonId` so MapStruct cannot confuse it with the request's (always `null`) `id` property. |
| A7 | `weightKg` is validated with `@NotNull`, `@DecimalMin("0")`, `@DecimalMax("9999.9")` and `@Digits(integer = 4, fraction = 1)`. Trailing zeros count: `6.00` is two decimal places → 400. | D-27 "0 to 9999.9, at most 1 decimal place", read literally on the text the client sent. The domain is laxer on purpose (it compares the significant scale, US03 A7): `web` validates the format, `domain` the invariant (CLAUDE.md "Validate input format in `web` and invariants in `domain`"). |
| A8 | `abilities` duplicates are **exact** (`@UniqueElements` from Hibernate Validator, which compares with `equals`); `internalTags` duplicates are case-insensitive (the existing `@UniqueIgnoringCase`). | D-27: the `abilities` row says "no duplicates", only the `internalTags` row says "(case-insensitive)". `@UniqueElements` ships with the Hibernate Validator that `spring-boot-starter-validation` already provides, so it is not a new dependency. |
| A9 | `spriteUrl` format is checked by a new constraint `@HttpUrl` in `web/pokemon/dto/request` (absolute URI, scheme `http` or `https`, host present), next to `@Size(max = 500)`. | D-27 "Valid `http`/`https` URL". Hibernate Validator's `@URL` can pin only one protocol at a time, and a regex would be less readable. Same package and shape as the existing `@MaxUtf8Bytes` and `@UniqueIgnoringCase` (AUTH and US03 patterns). It repeats the `LocalPokemon` check on purpose: format in `web`, invariant in `domain`. Blank text (`"   "` → `""`) is **not** a valid URL, so `spriteUrl` answers 400 where a blank `category`, `localizedName` or `region` is stored as `""` (A5); `LocalPokemon` rejects `""` the same way. |
| A10 | Path id: `@PathVariable @Positive(message = "must be greater than 0") final long id`. A non-numeric value falls into the existing `handleTypeMismatch` ("must be an integer"); `0` and negative values into `handleHandlerMethodValidationException`. Only the stale comment of `handleTypeMismatch` is updated, not its code. | US02/US01 pattern for parameter validation (`architecture.md` "Method validation on path variables", "Query parameter validation"); rule 9. |
| A11 | `LocalPokemonPersistenceAdapter.findById` is annotated `@Transactional(readOnly = true)` so the two `@ElementCollection` lists are loaded while the session is open. | `@ElementCollection` is lazy by default, and the mapper reads both lists after the repository call. Without the annotation the adapter would depend on `spring.jpa.open-in-view`, which only exists in a web request. Proved by a `@DataJpaTest` method running outside the test transaction (`Propagation.NOT_SUPPORTED`). |
| A12 | Every validation rule of the table gets at least one `@WebMvcTest` case (parameterized, as in `LocalPokemonControllerTest`), plus one case per general rule; one `@SpringBootTest` proves the replacement end to end against H2. | REQ-US04 "robust validation" and the stage notes ("Every validation rule must have at least one test"); US03 test patterns. |
| A13 | The new web tests live in `LocalPokemonUpdateControllerTest` and `LocalPokemonUpdateIntegrationTest`, not inside the US03 classes. | The US03 classes are already long and their fixtures are `POST`-shaped; `PokeApiCatalogAdapterSummaryTest` set the precedent of one class per operation. |
| A14 | `GlobalExceptionHandler.handleHandlerMethodValidationException` also maps `ParameterErrors` (the `@Valid` body's errors) to their field path, through a new private `fieldErrorsOf`. Nothing else in the handler changes, and A10 still holds for `handleTypeMismatch`. | Verified while building: a constraint on **any** parameter (here `@Positive` on the path id) makes Spring validate the whole method, so the body violations arrive as `HandlerMethodValidationException` instead of `MethodArgumentNotValidException`; without this, every body error is reported under the parameter name (`request`) and `errors[]` would not match the contract (D-17, D-27). `PUT` is the only handler method that mixes a constrained parameter with a `@Valid` body, and the existing 400 tests of `PokemonCatalogController` and `AuthController` are unchanged and green. |

## Open questions

The four questions raised before the spec were answered by the developer and became rules 3, 8, 11 and 14 (and A4, A5). Nothing is left blocking.

Suggestions not implemented (ideas nobody asked for):

1. **Concurrency control.** Rule 14 lets the last write win. An `@Version` column plus `If-Match`/`ETag` would detect a lost update, but it is not in the PDF and would add a 409/412 to the contract.
2. **`PATCH` for a partial update.** The contract has no such route; a client that wants to change one field has to send the whole representation.
3. **Blank optional text.** `"region": "  "` is stored as `""` (US03 A15). Turning blank optional text into `null` would be a new rule for both `POST` and `PUT`.

## Tasks

Each task takes ≤ 45 min of AI work and becomes one `test:` commit plus one `feat:` commit. Inside-out order:

- [x] **T1 domain:** `pokemon.exception.LocalPokemonNotFoundException`. Test first: fixed English message with the id, and the id exposed for the handler's log. `LocalPokemon` is reused unchanged (rule 12).
- [x] **T2 application:** `LocalPokemonRepositoryPort.findById`, `UpdateLocalPokemonUseCase`, `UpdateLocalPokemonCommand`, `UpdateLocalPokemonService`. Tests first with mocked ports: replaces every non-identifier field and keeps `id`/`pokeApiId`; absent optional fields become `null`/`[]`; an unknown id throws `LocalPokemonNotFoundException` without saving; a value that breaks a domain invariant throws `IllegalArgumentException` without saving.
- [x] **T3 infrastructure:** `LocalPokemonPersistenceAdapter.findById` (A11) and the update path of `save`. `@DataJpaTest` with `ddl-auto=validate`: `findById` round-trips every field and the list order, and works outside a transaction; an unknown id → empty; saving a record with an id replaces every column and both list tables (fewer, more and zero elements).
- [x] **T4 web:** `UpdateLocalPokemonRequest`, `@HttpUrl` + validator, `LocalPokemonController.update`, `LocalPokemonMapper.toCommand`, the `PokemonUseCaseConfig` bean and the `GlobalExceptionHandler` 404. `@WebMvcTest`: 200 + contract JSON; trimming; limits accepted; every validation rule of the table and every general rule (malformed, empty, identifiers in the body, path id not numeric or ≤ 0); 404; 401 without a token. `@SpringBootTest`: the real chain replaces the row and empties the lists.
