# US01: Pokémon list

- **Requirements:** REQ-US01, REQ-US01-NTH, REQ-T04, REQ-F01, REQ-T03, REQ-T02, REQ-CORE
- **Decisions used:** D-03, D-04, D-05, D-09, D-11, D-15, D-16, D-17, D-18, D-19, D-20, D-26, D-28
- **Status:** done (written 2026-10-03; Q1–Q4 answered with the recommendations, Q5 answered with D-28)

Stage S2 of `docs/foundation-plan.md`. Reuses the US02 reference slice (`docs/specs/US02-pokemon-details.md`, "Reference patterns (from US02)" in `docs/architecture.md`).

## Contract

Copied from `docs/api-contract.md`.

| Method | Route | Access | Requirement | Success | Errors |
|---|---|---|---|---|---|
| GET | `/api/v1/pokemon?page={n}&size={m}` | Public | REQ-US01 | 200 | 400, 502 |

**Parameters:** `page` is 0-based, default 0; `size` defaults to 20, from 1 to **100** (D-26). Out of range → 400.

**200 body**

```json
{ "content": [ ... ], "page": 0, "size": 20, "totalElements": 1302, "totalPages": 66 }
```

**`content[]` item** (US01: sprite, category, weight, abilities)

```json
{ "id": 1, "name": "bulbasaur", "spriteUrl": "https://...", "category": "Seed Pokémon",
  "weightKg": 6.9, "abilities": ["overgrow", "chlorophyll"] }
```

**Errors** (D-17): `ProblemDetail` with `type`, `title`, `status`, `detail`, `instance`; validation errors add `errors[]` of `{field, message}`. Never a stack trace.

**About 502:** a defensive choice for when the PokéAPI is down (REQ-T03); not PDF text.

## PokéAPI calls (only what `docs/challenge/pokeapi.md` lists)

| Call | Fields read |
|---|---|
| `GET /pokemon?limit={size}&offset={page × size}` | `count`, `results[].url` |
| `GET {results[].url}` (`/pokemon/{id}/`), once per item | `id`, `name`, `sprites.front_default`, `weight`, `abilities[].ability.name`, `species.url` |
| `GET {species.url}` (`/pokemon-species/{id}/`), once per item | `genera[].genus`, `genera[].language.name` |

## Rules

1. `GET /api/v1/pokemon` is a **public** route (contract; D-11).
2. The data always comes from the PokéAPI, never from the database (D-09).
3. `page` (default 0, ≥ 0) and `size` (default 20, 1 to 100) map to the PokéAPI list as `offset = page × size` and `limit = size` (contract; D-26; `pokeapi.md` §1).
4. `totalElements` is the list's `count`; `totalPages` is `ceil(totalElements / size)` (contract; stage notes).
5. `page`/`size` echo the request. A page past the last one answers 200 with an empty `content` and the real `totalElements` (contract sets only `page ≥ 0`; A5).
6. `content[]` keeps the order of the PokéAPI list `results` (A6).
7. Each item: `id` and `name` from `pokemon.id` / `pokemon.name` (REQ-US01; `pokeapi.md` §2).
8. `spriteUrl` is `pokemon.sprites.front_default`; `null` when the PokéAPI returns `null` (REQ-US01 interpretation "sprite"; same as US02 rule 4).
9. `category` is the `genus` of the first `genera[]` entry with `language.name == "en"`; `null` when there is none (REQ-US01 interpretation "category"; A3).
10. `weightKg` is `pokemon.weight ÷ 10` (hectograms → kg), with one decimal place, e.g. `69` → `6.9` (REQ-US01 interpretation "mass"; `pokeapi.md` §2; contract field name).
11. `abilities` lists every `abilities[].ability.name` in PokéAPI order, **hidden abilities included** (REQ-US01 interpretation "skills"; contract example lists the hidden `chlorophyll`).
12. The item details (`/pokemon/{id}` + `/pokemon-species/{id}`) of one page are fetched **in parallel on virtual threads**; order is kept (D-19; rule 6).
13. `page < 0`, `size < 1`, `size > 100` → **400** `ProblemDetail`, `detail` "Validation failed", `errors[]` with the parameter name and an explicit English message (contract "Validation"; D-26; CLAUDE.md).
14. A non-integer `page` or `size` (`?page=abc`) → **400** with the same shape: `errors[]` `{ field: "page", message: "must be an integer" }` (contract "Out of range → 400" / "Any violation → 400 with the field errors"; A7).
15. The PokéAPI cannot be reached, times out, or answers any error on **any** call of the page (list, a Pokémon, a species; a 404 on an item is a secondary call) → **502** `ProblemDetail` "PokeAPI is unavailable" for the whole request (contract; REQ-T03; "External failures" convention in `architecture.md`).
16. PokéAPI responses are cached with Spring Cache + Caffeine on `PokeApiClient` (list, Pokémon, species, evolution chain, the US02 methods included); every cache is bounded by `maximumSize=2000,expireAfterWrite=24h`. A repeated call with the same arguments does not reach the PokéAPI again, and a test proves it (D-15; REQ-US01-NTH; REQ-T04; `pokeapi.md` Fair Use Policy; Q1, Q2).
17. At most **10 PokéAPI calls are in flight at once across the whole application**: one shared `Semaphore` in `PokeApiCatalogAdapter` wraps every PokéAPI call the adapter makes (the US02 details calls included); virtual threads stay (D-28; D-19; Q5).
18. `domain` and `application` contain no Spring, Jackson or MapStruct; PokéAPI models stay in `infrastructure` (D-03, D-18, CLAUDE.md).
19. No test calls the real PokéAPI; the list response is recorded **once** with `curl` (CLAUDE.md; `pokeapi.md`).
20. Logs as in "Logging (all stages)" of `architecture.md`: `DEBUG` for the request (page, size), the list call and each item call; one `WARN` with page, size and cause when the PokéAPI fails (and with the Pokémon for the details call, unchanged); `DEBUG` for the 400 answers.

## Out of scope

- Search, filters or sorting (requirements "Not asked for by the PDF").
- Stats, description, evolution chain in the list (US02 only); height, types, moves.
- Cache eviction endpoints, cache metrics, cache warm-up.
- Any database table or Flyway script.

## Decisions taken by the agent

Implementation details answered by the docs or the US02 reference patterns.

| # | Decision | Justification |
|---|---|---|
| A1 | Domain read model `PokemonSummary(id, name, spriteUrl, category, weightKg, abilities)`; invariants `id > 0`, `name` not blank, `weightKg` not null and ≥ 0, `abilities` `null` → empty, immutable copy. `spriteUrl` and `category` may be `null`. | Same shape as `PokemonDetails` (reference pattern); US03 can reuse it for the sync. |
| A2 | `weightKg` is a `BigDecimal` with scale 1 (`BigDecimal.valueOf(weight, 1)`). | Exact decimal: `6.9`, never `6.8999…`; matches the "1 decimal place" rule of D-27 that US04 will apply to the same field. |
| A3 | `category` = first `en` genus; `null` when none. | Mirrors the answered US02 Q1 (first `en` flavor text, `null` when none). |
| A4 | `PageResult<T>(content, page, size, totalElements, totalPages)` in `application.shared.pagination` (package already listed), `totalPages` computed by `PageResult.of(content, page, size, totalElements)`; `web.shared.pagination.PageResponse<T>` record for the JSON. | Packages exist in `architecture.md` for exactly this; the local list (S6) reuses both. Use case returns the read model directly (US02 Q6). |
| A5 | A page past the last one → 200, empty `content`. | The contract defines only `page ≥ 0` as a rule; the PokéAPI itself answers an empty `results` with the real `count`. |
| A6 | Items follow the order of the PokéAPI list, regardless of which parallel call finishes first. | The list is ordered by id; the client needs a stable page. |
| A7 | `?page=abc` → 400 "Validation failed" + `errors[]`, by overriding `handleTypeMismatch` in `GlobalExceptionHandler`. | The default Spring `detail` echoes the input ("Failed to convert 'page' with value: 'abc'"); CLAUDE.md asks for a fixed English `detail`, the contract for `errors[]`. |
| A8 | Pagination parameters validated with `@Min`/`@Max` on the `@RequestParam`s, explicit English messages: "must be greater than or equal to 0", "must be greater than or equal to 1", "must be less than or equal to 100". | US02 pattern (method validation on parameters → existing `handleHandlerMethodValidationException`). |
| A9 | Item URLs are followed from the list `results[].url` (`PokeApiClient.getPokemon(url)`), the species from `pokemon.species.url` (existing `getSpecies`). | "Follow URLs returned by the PokéAPI with `uri(URI.create(url))`" convention; stage notes (`/pokemon/{id}`). |
| A10 | Parallel fetch: `Executors.newVirtualThreadPerTaskExecutor()` in try-with-resources inside the adapter, one `CompletableFuture` per item, joined in list order. A `RestClientException` in any item is translated once, in the adapter, into `ExternalServiceUnavailableException` (one `WARN`). | D-19; "Exception translation" and "WARN once" conventions. |
| A17 | D-28 cap: property `pokeapi.max-concurrent-calls` (`10` in `application.yaml`, no default in code; a value < 1 fails at startup), injected with `@Value` into the adapter constructor, and a `Semaphore` field of the singleton adapter (*changed at the developer's request, 2026-10-03: was a `static final` constant*); a permit is held only around one client call, never across two, so a page cannot deadlock itself. An interrupted wait → `ExternalServiceUnavailableException` with the interrupt flag restored. *Added after review:* the first failing item interrupts the others (`executor.shutdownNow()`), so a failing page does not keep the global permits busy until every item times out; the first failure is the one reported. A non-HTTP failure inside an item (for example a broken domain invariant) is rethrown unwrapped, as in `findDetails`. The permit is taken before the client call, so a cache hit also passes through it (cost negligible on virtual threads). | D-28 ("inside the infrastructure PokéAPI adapter", "global"); the client is the cache boundary, the adapter is the only caller. |
| A11 | Summary mapping added to the existing `PokeApiDetailsMapper` and `PokemonDetailsResponseMapper` (new methods) instead of new mapper classes. | Stage notes ("adding new methods to … the existing mappers is expected"); a new mapper bean would also force changes to the `@Import` of the US02 adapter test. |
| A12 | New PokéAPI model `PokeApiPokemonList(count, results)`; `PokeApiPokemon` gains `weight` and `abilities`. `next`/`previous` are not declared. | Only fields the code reads (`pokeapi.md` "Response models"). |
| A13 | Fixtures (`count` was 1351 when recorded): `pokemon-list/offset-0-limit-2.json` (the one list call), plus `pokemon/2.json` and `pokemon-species/2.json` (ivysaur) so the list test has two real items to check order and parallelism. | Stage notes: "record more only if a test needs them". |
| A18 | A 404 from `/pokemon/{idOrName}` is cached as "not found" like a success (Spring Cache stores the empty `Optional` as `null`); exceptions are never cached. | Spring Cache semantics; the PokéAPI data is static, and D-15 caches the calls. |
| A19 | Two edge-case tests (page past the end, very large `page`) use a small inline list body `{"count": 1351, "results": []}` instead of a recorded fixture. | Recording them would need more real PokéAPI calls; the body only carries `count` and an empty `results`, both documented fields. |
| A14 | The parallel adapter test uses an **unordered** `MockRestServiceServer` (a `MockServerRestClientCustomizer(UnorderedRequestExpectationManager::new)` test bean), in a new test class, so the US02 adapter test keeps its ordered server untouched. | Parallel calls arrive in any order; Boot's `MockRestServiceServerAutoConfiguration` customizer is `@ConditionalOnMissingBean`. |
| A15 | `@EnableCaching` lives in `infrastructure.shared.cache.CacheConfig` (package already listed); `application.yaml` sets `spring.cache.type: caffeine` and `spring.cache.caffeine.spec`, so the Boot auto-configured `CaffeineCacheManager` is used and creates the four caches (`pokeapi-pokemon-list`, `pokeapi-pokemon`, `pokeapi-species`, `pokeapi-evolution-chain`) on first use, each with the same spec. *Adjusted during T6:* the first draft also listed `spring.cache.cache-names`; dropped, because the names would be duplicated in `application.yaml` (module `web`) and the cache test (module `infrastructure`), and a missing name fails only on first call, not at startup. | D-15; less code than a hand-built `CacheManager`. |
| A16 | Use case `ListPokemonUseCase.list(int page, int size)`; port `PokemonCatalogPort.findSummaries(int page, int size)`. `offset` is computed as a `long` in the adapter (no `int` overflow for a large `page`). | One use case per interface (reference pattern); limit/offset is a PokéAPI detail. |

## Open questions

All answered on 2026-10-03. Q1–Q4: the developer accepted the recommendations (now rules 16 and the design). Q5: the developer chose a **global** cap of 10 concurrent PokéAPI calls, recorded as D-28 (rule 17); no `@Cacheable(sync = true)`.

1. **Cache bounds.** D-15 gives no size or expiry. List pages are keyed by `(offset, limit)`, so an unbounded cache can grow with arbitrary `page`/`size` combinations. **Recommendation:** one spec for every cache, `maximumSize=2000,expireAfterWrite=24h` (2000 ≥ every Pokémon and species; PokéAPI data is effectively static).
2. **Where the cache goes, and US02.** D-15 says "annotations on the PokéAPI adapter". **Recommendation:** put `@Cacheable` on `PokeApiClient` (raw PokéAPI responses: list, Pokémon, species, evolution chain), including the three **existing** US02 methods (`findPokemon`, `getSpecies`, `getEvolutionChain`; annotation only, no signature change). That caches "resources" as the Fair Use Policy asks, and the list and the details share the species cache. Alternative: `@Cacheable` on the adapter's `findSummaries` / `findDetails` (whole results, no sharing).
3. **`PokeApiSpecies` gains `genera`.** Adding a record component breaks three `new PokeApiSpecies(entries, chain)` calls in the US02 `PokeApiDetailsMapperTest`. **Recommendation:** append `null` as the third argument in those three calls (setup only; no assertion changes). Alternative: keep a 2-argument constructor in production code only for that test.
4. **Controller.** The list route belongs in `PokemonCatalogController` (same resource, `/api/v1/pokemon`). Its new constructor dependency (`ListPokemonUseCase`) makes the two existing `@WebMvcTest(PokemonCatalogController.class)` classes (`PokemonCatalogControllerTest`, `SecurityConfigTest`) fail to start. **Recommendation:** add the method to `PokemonCatalogController` and one `@MockitoBean ListPokemonUseCase` field to each of those two test classes (setup only; no test method changes). Alternative: a separate `PokemonListController`, no edits to US02 tests.
5. **Parallelism cap.** `size=100` means 100 concurrent Pokémon calls, then 100 species calls. **Recommendation:** no cap (D-19 as written; the cache absorbs repeats; no unsourced number). Alternative: a semaphore with a fixed limit (a new value to choose).

Raised by the self-review, not implemented (rule 16 is approved as written):

6. **Shared `pokeapi-pokemon` cache.** `findPokemon(idOrName)` (US02, user-typed key, 404s cached as "not found") and `getPokemon(url)` (US01) share one cache bounded at 2000 entries. Their keys never overlap, so sharing brings no reuse, and many requests for unknown names on the public details route can evict the list's Pokémon entries. **Possible change:** a separate cache for `findPokemon`, or not caching 404s (`unless = "#result == null"`).

## Design (packages from `architecture.md`, no new packages)

| Layer | Class | Role |
|---|---|---|
| domain | `pokemon.model.PokemonSummary` (record) | US01 read model (A1, A2) |
| application | `shared.pagination.PageResult<T>` (record) | Page of results, `totalPages` computed (A4) |
| application | `pokemon.port.in.ListPokemonUseCase` | `PageResult<PokemonSummary> list(int page, int size)` |
| application | `pokemon.port.out.PokemonCatalogPort` | + `PageResult<PokemonSummary> findSummaries(int page, int size)` |
| application | `pokemon.service.ListPokemonService` | Delegates to the port |
| infrastructure | `pokeapi.model.PokeApiPokemonList` (new); `PokeApiPokemon` + `weight`, `abilities`; `PokeApiSpecies` + `genera` | A12, Q3 |
| infrastructure | `pokeapi.client.PokeApiClient` | + `getPokemonList(long offset, int limit)`, `getPokemon(String url)`; `@Cacheable` (Q2) |
| infrastructure | `pokeapi.mapper.PokeApiDetailsMapper` | + `toSummary(pokemon, species)`, `@Named` category and weight (A11) |
| infrastructure | `pokeapi.adapter.PokeApiCatalogAdapter` | + `findSummaries`: list call, parallel item fetch (A10), `PageResult`; D-28 semaphore around every client call (A17) |
| infrastructure | `shared.cache.CacheConfig` | `@EnableCaching` (A15) |
| web | `pokemon.controller.PokemonCatalogController` | + `GET /api/v1/pokemon` (Q4) |
| web | `pokemon.dto.response.PokemonSummaryResponse`, `shared.pagination.PageResponse<T>` (records) | Contract JSON |
| web | `pokemon.mapper.PokemonDetailsResponseMapper` | + `PageResult<PokemonSummary>` → `PageResponse<PokemonSummaryResponse>` (A11) |
| web | `pokemon.config.PokemonUseCaseConfig` | + `@Bean ListPokemonUseCase` |
| web | `shared.error.GlobalExceptionHandler` | + `handleTypeMismatch` override (A7) |
| web | `application.yaml` | `spring.cache.*` (A15, Q1) |

## Tasks

Inside-out order. Each task becomes one `test:` commit plus one `feat:` commit (T1 is one `test:` commit).

- [x] **T1 fixtures:** record **once** with `curl` into `infrastructure/src/test/resources/fixtures/pokeapi/`: `pokemon-list/offset-0-limit-2.json` (`/pokemon?offset=0&limit=2`), `pokemon/2.json`, `pokemon-species/2.json`. Pretty-printed, unmodified otherwise.
- [x] **T2 domain + application pagination:** `PokemonSummary`, `PageResult`. Tests first: invariants (A1), immutable `abilities`, `totalPages` (1302/20 → 66, 0 elements → 0, exact multiple), `page`/`size`/`totalElements` invariants, immutable `content`.
- [x] **T3 application:** `ListPokemonUseCase`, `PokemonCatalogPort.findSummaries`, `ListPokemonService`. Tests first with the port mocked: returns the port's page; `ExternalServiceUnavailableException` propagates.
- [x] **T4 infrastructure, models + mapper:** `PokeApiPokemonList`, new fields on `PokeApiPokemon` / `PokeApiSpecies` (Q3), `PokeApiDetailsMapper.toSummary`. Tests on the fixtures: list `count` and `results`; bulbasaur → `spriteUrl`, `category` "Seed Pokémon", `weightKg` 6.9, abilities `[overgrow, chlorophyll]`; eevee three abilities; no `en` genus → `null` category.
- [x] **T5 infrastructure, client + adapter:** `getPokemonList`, `getPokemon(url)`, `findSummaries` with virtual threads. New test class with an unordered `MockRestServiceServer` (A14): page 0 size 2 → 2 items in list order with `totalElements` = recorded `count`; `offset`/`limit` in the URL for page 3 size 2; empty page past the end; 500 / I/O error on the list, on an item, on a species, 404 on an item → `ExternalServiceUnavailableException`. D-28: a unit test with a mocked `PokeApiClient` whose calls block briefly proves at most 10 calls are in flight for a 30-item page (and more than one, so it is still parallel).
- [x] **T6 infrastructure, cache:** `CacheConfig`, `@Cacheable` on `PokeApiClient` (Q2), cache settings (Q1). Test: `@RestClientTest` + cache auto-configuration; the second identical call of each cached method is served without a second request (`ExpectedCount.once()` + `server.verify()`); different arguments do reach the server.
- [x] **T7 web:** controller method, `PokemonSummaryResponse`, `PageResponse`, mapper methods, `PokemonUseCaseConfig` bean, `handleTypeMismatch`, `application.yaml`. `@WebMvcTest`: 200 JSON in contract shape, defaults `page=0`/`size=20`, public without a token, 400 for `page=-1`, `size=0`, `size=101`, `page=abc` (with `errors[]`), 502 as `ProblemDetail`. Context test still green.
