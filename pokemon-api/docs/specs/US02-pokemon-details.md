# US02: Pokémon details

- **Requirements:** REQ-US02, REQ-F01, REQ-T03, REQ-T02, REQ-CORE
- **Decisions used:** D-03, D-04, D-05, D-07, D-09, D-11, D-16, D-17, D-18, D-20
- **Status:** done (approved 2026-10-03; Q1–Q8 answered with the proposals)

This is the **reference slice** (`docs/foundation-plan.md`, S1). Its classes become the pattern later stages copy.

## Contract

Copied from `docs/api-contract.md`.

| Method | Route | Access | Requirement | Success | Errors |
|---|---|---|---|---|---|
| GET | `/api/v1/pokemon/{idOrName}` | Public | REQ-US02 | 200 | 404, 502 |

**200 body** (US02: image, stats, description, evolution)

```json
{ "id": 1, "name": "bulbasaur", "imageUrl": "https://...",
  "stats": [ { "name": "hp", "baseStat": 45 } ],
  "description": "A strange seed was planted on its back at birth.",
  "evolutionChain": { "name": "bulbasaur", "evolvesTo": [ { "name": "ivysaur", "evolvesTo": [ ... ] } ] } }
```

**Errors** (D-17): `ProblemDetail` with `type`, `title`, `status`, `detail`, `instance`. Never a stack trace.

**Validation:** catalog `idOrName` not blank.

**About 502:** a defensive choice for when the PokéAPI is down (REQ-T03); not PDF text.

## PokéAPI calls (only what `docs/challenge/pokeapi.md` lists)

| Call | Fields read |
|---|---|
| `GET /pokemon/{idOrName}` | `id`, `name`, `sprites.front_default`, `stats[].stat.name`, `stats[].base_stat`, `species.url` |
| `GET {species.url}` (`/pokemon-species/{id}`) | `flavor_text_entries[].flavor_text`, `flavor_text_entries[].language.name`, `evolution_chain.url` |
| `GET {evolution_chain.url}` (`/evolution-chain/{id}`) | `chain.species.name`, `chain.evolves_to[]` (recursive) |

PokéAPI models declare only these fields and ignore unknown ones (`pokeapi.md`, "Response models").

## Rules

1. `GET /api/v1/pokemon/{idOrName}` is a **public** route (contract; D-11).
2. The data always comes from the PokéAPI, never from the database (D-09).
3. `id` and `name` come from `pokemon.id` and `pokemon.name` (REQ-US02; `pokeapi.md` §2).
4. `imageUrl` is `pokemon.sprites.front_default`; `null` when the PokéAPI returns `null` (REQ-US02 interpretation "image").
5. `stats` lists every `pokemon.stats[]` entry as `{ name: stat.name, baseStat: base_stat }`, in PokéAPI order. `effort` is not returned (REQ-US02 interpretation "core statistics" = `base_stat`; contract).
6. `description` is an English (`language.name == "en"`) entry of `species.flavor_text_entries`, cleaned of the game-file control characters (line breaks, form feeds) so it reads as one line with single spaces (REQ-US02 interpretation "narrative description"; `pokeapi.md` §3 warning). The **first** `en` entry in PokéAPI order; `null` when there is none (Q1).
7. `evolutionChain` is the tree from `evolution-chain.chain`: each node is `{ name: species.name, evolvesTo: [...] }`, recursive, **branches preserved** (eevee has 8 children at the first level). A leaf has `evolvesTo: []` (REQ-US02 interpretation "evolutionary lineage"; `pokeapi.md` §4; contract).
8. The species is reached through `pokemon.species.url` and the chain through `species.evolution_chain.url` (`pokeapi.md` §2–§4).
9. PokéAPI answers 404 to `GET /pokemon/{idOrName}` → **404** `ProblemDetail` (contract; REQ-T03).
10. The PokéAPI cannot be reached, times out, or answers any other error (4xx other than rule 9, 5xx) on any of the three calls → **502** `ProblemDetail` (contract; REQ-T03).
11. Error responses are `ProblemDetail` built in a single `@RestControllerAdvice` in `web/shared/error`; the `detail` is a fixed English message and never contains a stack trace or an upstream error body (D-17; CLAUDE.md "Security").
12. `domain` and `application` contain no Spring, Jackson or MapStruct; PokéAPI models stay in `infrastructure` (D-03, D-18, CLAUDE.md).
13. No test calls the real PokéAPI; infrastructure tests use the recorded fixtures (CLAUDE.md; `pokeapi.md`).
14. A minimal stateless `SecurityConfig` in `web/shared/security` permits `GET /api/v1/pokemon/**` and requires authentication for every other route; JWT comes in AUTH (Q2; D-07, D-11).
15. A blank `idOrName` → **400** `ProblemDetail` with `detail` "Validation failed" (Q3; contract "Validation").
16. `idOrName` is trimmed and lowercased before calling the PokéAPI (Q4).
17. The PokéAPI `RestClient` uses `pokeapi.base-url` (`https://pokeapi.co/api/v2`), connect timeout 2 s and read timeout 5 s, configurable in `application.yaml` (Q5). *Adjusted during T5:* the timeouts use Spring Boot's `spring.http.clients.connect-timeout` / `read-timeout` (applied to the auto-configured `RestClient.Builder`) instead of `pokeapi.*`, because setting a request factory by hand replaces the `MockRestServiceServer` mock in `@RestClientTest`.
18. The use case returns the domain model `PokemonDetails`; `web` maps it with MapStruct (Q6).
19. `ExternalServiceUnavailableException` lives in `domain.shared.exception`, `PokemonNotFoundException` in `domain.pokemon.exception` (Q7). No catch-all handler in this stage (Q8).
20. Logs in English at the important debug points (see "Logging" in `docs/architecture.md`): `DEBUG` for the request, each PokéAPI call, a PokéAPI 404 and the 404/400 answers; `INFO` for the base URL at startup; one `WARN` with the Pokémon and the cause when the PokéAPI fails. Only in `infrastructure` and `web` (developer request, 2026-10-03). The general rules for every stage are in "Logging (all stages)" in `docs/architecture.md`.

## Out of scope

- `GET /api/v1/pokemon` (list), caching with Caffeine (D-15): stage S2 (US01). Not added here even though the adapter is created here.
- JWT, users, protected routes (S3, AUTH).
- `category`, `weight`, `abilities`, `height`, types, moves, `evolution_details`, `is_baby`, `sprites.other.*`.
- Any database table or Flyway script.

## Open questions

All answered on 2026-10-03: the developer accepted every proposal below (now rules 6 and 14–19). The 400 was added to the error column of `docs/api-contract.md` on 2026-10-05.

1. **Description entry (rule 6).** The PDF and the contract do not say which English flavor text to use, or what to return when there is none. **Proposal:** the **first** `en` entry in PokéAPI order; `description: null` when no `en` entry exists.
2. **Route security (rule 1).** `spring-boot-starter-security-oauth2-resource-server` is already on the `web` classpath, so Spring Security's defaults will answer **401** on every route, including this public one, until AUTH (S3). **Proposal:** in this stage add a minimal `SecurityConfig` in `web/shared/security` (D-07) that permits `GET /api/v1/pokemon/**` and requires authentication for everything else, stateless, no JWT decoder yet. AUTH extends it. Alternative: exclude security auto-configuration until S3 (more rework later).
3. **Blank `idOrName`.** The contract's validation section says "Catalog `idOrName` not blank", but the endpoint's error column lists only 404 and 502. A truly empty segment (`/api/v1/pokemon/`) never reaches the route; a whitespace segment (`/api/v1/pokemon/%20`) does. **Proposal:** answer **400** (`ProblemDetail`, "Validation failed") for a blank `idOrName`, and record the 400 in the error column of the contract when you next edit it. Alternative: let it go to the PokéAPI and return its 404.
4. **Case and whitespace in `idOrName`.** The PokéAPI only knows lowercase names (`Bulbasaur` → 404). **Proposal:** trim and lowercase `idOrName` before calling the PokéAPI (the contract trims text fields; lowercasing is new). Alternative: pass it through as-is.
5. **HTTP timeouts (rule 10).** Without timeouts a hanging PokéAPI blocks the request instead of producing a 502. **Proposal:** connect timeout 2 s and read timeout 5 s on the PokéAPI `RestClient`, configurable in `application.yaml` under `pokeapi.*`, together with `pokeapi.base-url` (default `https://pokeapi.co/api/v2/`). Values are a defensive default, not PDF text.
6. **Use case output type.** `architecture.md` says use cases return `result` records. For a read-only query the result would be a field-by-field copy of the domain model (three nested records plus hand-written mapping, since `application` cannot use MapStruct). **Proposal:** the use case returns the domain model `PokemonDetails` directly, and `web` maps it to the response DTO with MapStruct. Later stages with real commands (US03/US04) still use `command`/`result` records. Alternative: add `PokemonDetailsResult` and its nested records anyway.
7. **Where the 502 exception lives.** The port must signal "PokéAPI unavailable" without leaking Spring's `RestClientException`. There is no `exception` package in `application`. **Proposal:** `ExternalServiceUnavailableException` in `domain.shared.exception` (S2 and S4 reuse it), and `PokemonNotFoundException` in `domain.pokemon.exception` (S4 reuses it for "not found in the PokéAPI").
8. **Unexpected exceptions.** Not in the contract. **Proposal:** none in this stage: Spring's default error handling already omits stack traces. Revisit only if a later stage needs a 500 `ProblemDetail`.

## Design (packages from `architecture.md`, no new packages)

| Layer | Class | Role |
|---|---|---|
| domain | `pokemon.model.PokemonDetails`, `PokemonStat`, `EvolutionStage` (records) | US02 read model; invariants: `id > 0`, `name` not blank, lists not null and immutable |
| domain | `pokemon.exception.PokemonNotFoundException` | Rule 9 |
| domain | `shared.exception.ExternalServiceUnavailableException` | Rule 10 (Q7) |
| application | `pokemon.port.in.GetPokemonDetailsUseCase` | `PokemonDetails getDetails(String idOrName)` |
| application | `pokemon.port.out.PokemonCatalogPort` | `Optional<PokemonDetails> findDetails(String idOrName)`; S2 and S4 add methods here |
| application | `pokemon.service.GetPokemonDetailsService` | Empty → `PokemonNotFoundException`; Q4 normalization |
| infrastructure | `pokeapi.model.PokeApiPokemon`, `PokeApiSpecies`, `PokeApiEvolutionChain` (records, nested records) | Only the fields in the table above |
| infrastructure | `pokeapi.client.PokeApiClient` | `RestClient` calls (D-16) |
| infrastructure | `pokeapi.config.PokeApiConfig` | `RestClient` bean with the base URL (timeouts from `spring.http.clients.*`, rule 17) |
| infrastructure | `pokeapi.mapper.PokeApiDetailsMapper` (MapStruct) | Three PokéAPI models → `PokemonDetails`; rule 6 cleaning, rule 7 recursion |
| infrastructure | `pokeapi.adapter.PokeApiCatalogAdapter` | Implements `PokemonCatalogPort`; 404 → `Optional.empty()`, other failures → `ExternalServiceUnavailableException` |
| web | `pokemon.controller.PokemonCatalogController` | `GET /api/v1/pokemon/{idOrName}` |
| web | `pokemon.dto.response.PokemonDetailsResponse`, `StatResponse`, `EvolutionStageResponse` (records) | Contract JSON |
| web | `pokemon.mapper.PokemonDetailsResponseMapper` (MapStruct) | Domain → DTO |
| web | `pokemon.config.PokemonUseCaseConfig` | `@Bean` for `GetPokemonDetailsService` |
| web | `shared.error.GlobalExceptionHandler` | `ProblemDetail` for 404 / 502 (and 400 if Q3 is accepted) |
| web | `shared.security.SecurityConfig` | Only if Q2 is accepted |

## Tasks

Each task takes ≤ 45 min of AI work and becomes one `test:` commit plus one `feat:` commit (T1 is one `test:` commit). Inside-out order:

- [x] **T1 fixtures:** record the PokéAPI responses **once** with `curl` (the only allowed call to the real API) into `infrastructure/src/test/resources/fixtures/pokeapi/`: `pokemon/1.json`, `pokemon-species/1.json`, `evolution-chain/1.json` (bulbasaur, linear), `pokemon/133.json`, `pokemon-species/133.json`, `evolution-chain/<id>.json` with the id taken from eevee's `evolution_chain.url` (branching). Pretty-printed, unmodified otherwise.
- [x] **T2 domain:** `PokemonDetails`, `PokemonStat`, `EvolutionStage`, `PokemonNotFoundException`, `ExternalServiceUnavailableException`. Tests first: invariants (`id > 0`, blank name rejected, null lists become empty, lists are immutable copies, a leaf has an empty `evolvesTo`).
- [x] **T3 application:** `GetPokemonDetailsUseCase`, `PokemonCatalogPort`, `GetPokemonDetailsService`. Tests first with the port mocked by Mockito (already a test dependency): returns the port's details; empty → `PokemonNotFoundException`; `ExternalServiceUnavailableException` propagates; Q4 normalization if accepted.
- [x] **T4 infrastructure, models + mapper:** PokéAPI model records and `PokeApiDetailsMapper`. Tests deserialize the T1 fixtures and map them: bulbasaur linear chain (bulbasaur → ivysaur → venusaur), eevee with 8 branches, 6 stats in order, `imageUrl`, description cleaned (Q1), unknown fields ignored.
- [x] **T5 infrastructure, client + adapter + config:** `PokeApiClient`, `PokeApiConfig`, `PokeApiCatalogAdapter`. Tests with `MockRestServiceServer` serving the fixtures: full details for bulbasaur and eevee (three calls, URLs followed from the responses), 404 on `/pokemon` → empty, 500 / I/O error on any call → `ExternalServiceUnavailableException`. Verify the Boot 4 `@RestClientTest` import.
- [x] **T6 web:** `PokemonCatalogController`, response DTOs, `PokemonDetailsResponseMapper`, `PokemonUseCaseConfig`, `GlobalExceptionHandler` (and `SecurityConfig` per Q2). `@WebMvcTest` with a mocked use case: 200 JSON matches the contract (field names, nested `evolvesTo`), 404 and 502 as `ProblemDetail` with `instance`, no stack trace, public without a token, 400 per Q3. Verify the Boot 4 `@WebMvcTest` / `@MockitoBean` imports. Context test (`ChallengeApplicationTests`) still green.
