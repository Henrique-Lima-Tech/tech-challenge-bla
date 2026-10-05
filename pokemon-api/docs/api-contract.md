# API contract: v0

> **Status: approved (D-20, D-25 to D-27, D-29, D-30).** No open questions.
> Every route lives under `/api/v1` (D-30).
> Each endpoint names its source requirement. Route and field names are a **technical decision**, not PDF text.

## Endpoints

| Method | Route | Access | Requirement | Success | Errors |
|---|---|---|---|---|---|
| GET | `/api/v1/pokemon?page={n}&size={m}` | Public | REQ-US01 | 200 | 400, 502 |
| GET | `/api/v1/pokemon/{idOrName}` | Public | REQ-US02 | 200 | 400, 404, 502 |
| GET | `/api/v1/local/pokemon?page={n}&size={m}` | Authenticated | REQ-API01 | 200 | 400, 401 |
| GET | `/api/v1/local/pokemon/{id}` | Authenticated | REQ-API01 | 200 | 400, 401, 404 |
| POST | `/api/v1/local/pokemon` | Authenticated | REQ-US03, REQ-API01 | 201 + `Location` | 400, 401, 404 (not found in the PokéAPI), 409 (already synced), 502 |
| PUT | `/api/v1/local/pokemon/{id}` | Authenticated | REQ-US04, REQ-API01 | 200 | 400, 401, 404 |
| DELETE | `/api/v1/local/pokemon/{id}` | Authenticated | REQ-API01 | 204 | 400, 401, 404 |
| POST | `/api/v1/auth/register` | Public | REQ-API02 | 201 | 400, 409 (email already used) |
| POST | `/api/v1/auth/login` | Public | REQ-API02 | 200 | 400, 401 |

**About 502:** a defensive choice for when the PokéAPI is down (REQ-T03). It is not written in the PDF.

**About `POST /api/v1/local/pokemon`:** it is both the CRUD *create* and the US03 sync. It fetches the Pokémon from the PokéAPI and stores the local copy with the proprietary fields.

**Local Pokémon per user (D-31):** every `/api/v1/local/pokemon` route works on the authenticated user's copies only. 404 also means "this id belongs to another user", and 409 on `POST` means "you already synced this Pokémon" (another user can sync it too). The owner never appears in a JSON body.

## Pagination (US01 and local list)

- **Parameters:** `page` is 0-based, default 0; `size` defaults to 20, from 1 to **100** (D-26). Out of range → 400.

```json
{ "content": [ ... ], "page": 0, "size": 20, "totalElements": 1302, "totalPages": 66 }
```

## Bodies

**`GET /api/v1/pokemon` → `content[]`** (US01: sprite, category, weight, abilities)

```json
{ "id": 1, "name": "bulbasaur", "spriteUrl": "https://...", "category": "Seed Pokémon",
  "weightKg": 6.9, "abilities": ["overgrow", "chlorophyll"] }
```

**`GET /api/v1/pokemon/{idOrName}`** (US02: image, stats, description, evolution)

```json
{ "id": 1, "name": "bulbasaur", "imageUrl": "https://...",
  "stats": [ { "name": "hp", "baseStat": 45 } ],
  "description": "A strange seed was planted on its back at birth.",
  "evolutionChain": { "name": "bulbasaur", "spriteUrl": "https://...",
                      "evolvesTo": [ { "name": "ivysaur", "spriteUrl": "https://...", "evolvesTo": [ ... ] } ] } }
```

**`POST /api/v1/local/pokemon`** (request)

```json
{ "pokemon": "pikachu", "localizedName": "ピカチュウ", "region": "Kanto", "internalTags": ["starter", "electric"] }
```

**Local Pokémon** (GET, POST and PUT response)

```json
{ "id": 10, "pokeApiId": 25, "name": "pikachu", "spriteUrl": "https://...", "category": "Mouse Pokémon",
  "weightKg": 6.0, "abilities": ["static", "lightning-rod"],
  "localizedName": "ピカチュウ", "region": "Kanto", "internalTags": ["starter", "electric"] }
```

**`PUT /api/v1/local/pokemon/{id}`** (request): replaces **all fields except the identifiers** `id` and `pokeApiId` (D-25).

```json
{ "name": "pikachu", "spriteUrl": "https://...", "category": "Mouse Pokémon", "weightKg": 6.0,
  "abilities": ["static", "lightning-rod"],
  "localizedName": "Pikachu", "region": "Kanto", "internalTags": ["mascot"] }
```

## Validation (D-27)

Any violation → 400 with the field errors in `errors[]`.

**`POST` and `PUT /api/v1/local/pokemon`**

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

**General rules**

| Case | Response |
|---|---|
| Malformed JSON or empty body | 400 |
| Path `{id}` not numeric or ≤ 0 | 400 |
| `id` or `pokeApiId` in a PUT body | 400 (rejected, not ignored) |
| Text fields | Leading and trailing whitespace is trimmed before validation |
| Catalog `idOrName` | Not blank |
| `page` | ≥ 0 |

**Auth**

| Field | Rule |
|---|---|
| `name` (register) | Required, ≤ 100 chars |
| `email` | Required, valid email format, ≤ 254 chars |
| `password` (register) | Required, 8 to 72 chars (BCrypt only uses the first 72 bytes) |
| Login | `email` and `password` required; any credential failure is a 401 with a generic message |

**`POST /api/v1/auth/register`** → 201: `{ "id": 1, "name": "Ash", "email": "ash@example.com" }`

```json
{ "name": "Ash", "email": "ash@example.com", "password": "..." }
```

**`POST /api/v1/auth/login`** → 200: `{ "accessToken": "eyJ...", "tokenType": "Bearer", "expiresIn": 3600, "name": "Ash" }` (`name` since D-32)

```json
{ "email": "ash@example.com", "password": "..." }
```

## Errors (D-17)

```json
{ "type": "about:blank", "title": "Bad Request", "status": 400, "detail": "Validation failed",
  "instance": "/api/v1/local/pokemon/10", "errors": [ { "field": "region", "message": "must not be blank" } ] }
```

- 401 uses a generic message and never reveals whether the email exists.
- Responses never contain a stack trace.
