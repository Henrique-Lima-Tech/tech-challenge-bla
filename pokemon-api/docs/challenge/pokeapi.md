# PokéAPI: what this project uses

> **Source:** `Documentation - PokéAPI.html` (PokéAPI v2). Only the resources and fields needed for US01–US04 are listed here.
> If a field is **not on this page and not in the documentation**, do not use it. Example: `sprites.other.official-artwork` exists in real responses but is **not documented** in the HTML we have.

- **Base URL:** `https://pokeapi.co/api/v2/`
- **No authentication**, `GET` only.
- **Fair Use Policy:** "Locally cache resources whenever you request them." Abusers get their IP permanently banned.
- **Tests never call the real API.** They use recorded responses in `backend/infrastructure/src/test/resources/fixtures/pokeapi/`.

## 1. Paginated list: `GET /pokemon?limit={n}&offset={m}`

- The default page holds **20 items**. `limit` changes the size and `offset` moves through pages.
- Response (`NamedAPIResourceList`): `count` (int), `next` (url or null), `previous` (url or null), `results[]` with `name` and `url`.
- ⚠️ The list returns **only name and URL**. Sprite, weight, abilities (endpoint 2) and category (endpoint 3) need one call per item.

## 2. Pokémon: `GET /pokemon/{id or name}`

| Field | Type | Used for |
|---|---|---|
| `id`, `name` | int, string | US01, US02, US03 |
| `weight` | int, **hectograms** | US01 *mass* (÷ 10 = kg) |
| `height` | int, **decimetres** | Not asked for by the PDF |
| `abilities[]` | `is_hidden` (bool), `slot` (int), `ability.name` | US01 *skills* |
| `sprites.front_default` | string (url) | US01 *sprite*, US02 *image* |
| `stats[]` | `stat.name`, `base_stat` (int), `effort` (int) | US02 *core statistics* (`base_stat`) |
| `species` | `name`, `url` | Leads to endpoint 3 |

Documentation example: `clefairy` → `id: 35`, `height: 6`, `weight: 75`.

## 3. Species: `GET /pokemon-species/{id or name}`

| Field | Type | Used for |
|---|---|---|
| `genera[]` | `genus` (string), `language.name` | US01 *category*. Filter `language.name == "en"`. |
| `flavor_text_entries[]` | `flavor_text`, `language.name`, `version.name` | US02 *description*. Filter `en`. ⚠️ The documentation warns the text is "left unprocessed as it is found in game files" and has special characters that need cleaning. |
| `evolution_chain` | `url` (`APIResource`, unnamed) | Leads to endpoint 4 |

## 4. Evolution chain: `GET /evolution-chain/{id}`

- **Unnamed** resource: the id comes from the URL in `species.evolution_chain.url`.
- `chain` is a `ChainLink` with:
  - `is_baby` (bool)
  - `species` (`name`, `url`): the URL ends with the species id (`.../pokemon-species/{id}/`); D-29 builds each stage's sprite URL from it
  - `evolution_details[]` (for example `trigger`, `min_level`, `item`)
  - `evolves_to[]`: a **recursive list of `ChainLink`** that can branch.
- ⚠️ Notice at the top of the documentation: in `evolution_details`, `base_form` and `evolved_form` were renamed to `required_pokemon_form` and `evolved_pokemon_form`. This only matters if those fields are used (US02 does not need them).

## Response models

The PokéAPI response models (in `infrastructure/.../pokeapi/model`) **declare only the fields above** and **ignore unknown fields**.
