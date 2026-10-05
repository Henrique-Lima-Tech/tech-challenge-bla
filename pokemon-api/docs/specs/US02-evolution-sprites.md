# US02: Sprites in the evolution chain

- **Requirements:** REQ-US02, REQ-T03, REQ-T02, REQ-CORE
- **Decisions used:** D-09, D-17, D-18, D-20, D-28, D-29
- **Status:** done (approved 2026-10-03; open question 1 answered with the proposal)

A small change to the US02 slice (`docs/specs/US02-pokemon-details.md`): each stage of the evolution chain gets the sprite of its Pokémon, so the front end can show an image per stage.

## Contract

Copied from `docs/api-contract.md` (only the changed body).

**`GET /api/v1/pokemon/{idOrName}`** → 200

```json
{ "id": 1, "name": "bulbasaur", "imageUrl": "https://...",
  "stats": [ { "name": "hp", "baseStat": 45 } ],
  "description": "A strange seed was planted on its back at birth.",
  "evolutionChain": { "name": "bulbasaur", "spriteUrl": "https://...",
                      "evolvesTo": [ { "name": "ivysaur", "spriteUrl": "https://...", "evolvesTo": [ ... ] } ] } }
```

Routes, status codes and errors do not change.

## Rules

1. Every stage of `evolutionChain`, at every level, has a `spriteUrl` (contract; D-29).
2. `spriteUrl` is `https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/{id}.png`, where `{id}` is the number at the end of the stage's `species.url` (`https://pokeapi.co/api/v2/pokemon-species/{id}/`) (D-29; `pokeapi.md` §4).
3. When `species.url` is missing, blank or does not end with a numeric id, `spriteUrl` is `null` and the rest of the details answer as before; it never turns into a 500 or a 502 (D-29; REQ-T03).
4. No extra PokéAPI call: a details request still makes the same 3 calls (D-29; D-28).
5. The Pokémon's own `imageUrl` does not change: it is still `sprites.front_default` (US02 rule 4).
6. Building the URL is PokéAPI knowledge, so it lives in `infrastructure` (the PokéAPI mapper); `domain` only holds the nullable `spriteUrl` of `EvolutionStage`, and `web` copies it to the response (D-18; `architecture.md`).
7. US01, US03 and AUTH do not change. US02 tests keep their expectations, except that the evolution chain JSON now has `spriteUrl` (REQ-T02).
8. Front end: each stage of the evolution chain shows its sprite next to the name, with `alt=""` (the name is already next to it) and the existing "?" placeholder when `spriteUrl` is `null` or the image fails to load. The mock returns `spriteUrl` in every stage (REQ-US02; front-end docs 05).

## Out of scope

- A stage `id`, evolution triggers or levels (`evolution_details`), other sprite variants (shiny, artwork).
- Any change to the list (US01), the local Pokémon (US03) or auth.

## Open questions

1. **Default form vs species.** The pattern uses the species id. For every species, the default Pokémon has the same id as the species, so the sprite is the default form's. If a URL built this way ever answers 404, the front end shows the "?" placeholder (rule 8). **Proposal:** accept this; no extra check.

## Tasks

Each task becomes one `test:` commit plus one `feat:` commit. Inside-out order:

- [x] **T1 domain:** `EvolutionStage` gets a nullable `spriteUrl`. Tests first: kept as given, `null` allowed, existing invariants unchanged.
- [x] **T2 infrastructure:** `PokeApiDetailsMapper` builds `spriteUrl` from `species.url`. Tests with the recorded fixtures: bulbasaur chain (1, 2, 3) and eevee (133 and its 8 branches); `null` for a missing, blank or non-numeric URL.
- [x] **T3 web:** `EvolutionStageResponse` gets `spriteUrl`. `@WebMvcTest`: the details JSON has `spriteUrl` at every level, and `null` is serialized as `null`.
- [x] **T4 front end:** `EvolutionStage` type, `EvolutionChain` shows `PokemonImage` per stage, mock data, tests and docs 05.
