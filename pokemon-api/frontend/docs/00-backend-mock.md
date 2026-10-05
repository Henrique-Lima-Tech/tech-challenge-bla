# Back-end mock (MSW)

> Lets us build and test the screens before the back end is ready, using the same API contract ([`docs/api-contract.md`](../../docs/api-contract.md)).
> Back to the [index](README.md).

## How it works

- [MSW](https://mswjs.io) intercepts the `fetch` calls in the browser (Service Worker in `public/mockServiceWorker.js`) and answers with fake data.
- The front end **does not know** it is talking to a mock: the `httpClient` and the hooks are the same. The back end now implements every endpoint below: with `VITE_USE_MOCKS=false` the front end uses it, and the mock stays for the tests and for working without the back end.
- The same handlers are used in the tests (Vitest), with `setupServer` from `msw/node`.
- `main.tsx` starts the worker before the first render; `src/test/setup.ts` starts the server for the tests and resets it between them.

## Files

| File | Content |
|---|---|
| `src/mocks/data.ts` | 17 fixed Pokémon (Kanto starters, the Pikachu line, Ditto, Eevee and its evolutions) with their evolution chains |
| `src/mocks/db.ts` | In-memory state: local Pokémon and users. Starts with 3 local Pokémon (bulbasaur, charmander, pikachu) and the demo user |
| `src/mocks/handlers.ts` | One handler per endpoint (table below) |
| `src/mocks/browser.ts` | `setupWorker(...handlers)` for the browser |
| `src/mocks/server.ts` | `setupServer(...handlers)` for the tests |

## Simulated endpoints

| Endpoint | Mock behavior |
|---|---|
| `GET /api/v1/pokemon?page&size` | A page of the fixed list (`id`, `name`, `spriteUrl`, `category`, `weightKg`, `abilities`). `page < 0` or `size` outside 1–100 → 400. Page past the total → `content: []` |
| `GET /api/v1/pokemon/{idOrName}` | `id`, `name`, `imageUrl`, `stats`, `description`, `evolutionChain` (`{ name, spriteUrl, evolvesTo[] }`). Finds by id or name (case insensitive). Unknown → 404. Blank → 400 |
| `POST /api/v1/local/pokemon` | Logged in. Body `{ pokemon, localizedName?, region?, internalTags? }`. Creates the local copy with a new local `id` → 201 + `Location`. Unknown in the catalog → 404. Already local → 409. Invalid → 400 with `errors[]` |
| `GET /api/v1/local/pokemon?page&size` | Logged in. Paginated list of the local Pokémon, sorted by `id`. `page < 0` or `size` outside 1–100 → 400 |
| `GET /api/v1/local/pokemon/{id}` | Logged in. 404 if it does not exist, 400 if the id is not a positive number |
| `PUT /api/v1/local/pokemon/{id}` | Logged in. Replaces **every field except** `id` and `pokeApiId`, with the validation rules of the contract. `id` or `pokeApiId` in the body → 400. 404, 400 with `errors[]` per field (list items as `abilities[0]`) |
| `DELETE /api/v1/local/pokemon/{id}` | Logged in. 404 or 204 |
| `POST /api/v1/auth/register` | `{ name, email, password }`. 400 if invalid, 409 if the email is already registered, 201 `{ id, name, email }` |
| `POST /api/v1/auth/login` | `{ email, password }`. Missing fields → 400. Wrong credentials → generic 401. Otherwise `{ accessToken, tokenType, expiresIn, name }` |

- No token on the local routes → 401. There are no roles. Each user only sees and changes their own local Pokémon: another user's id answers 404, and 409 only means "you already synced it" (D-31). The 3 starting Pokémon belong to the demo user.
- Every error uses the `ProblemDetail` format (`type`, `title`, `status`, `detail`, `errors[]`), like the back end.
- The fake token is `mock-token-<email>`.
- Validation uses the front end's own Zod schemas (`localPokemonUpdateSchema`, `registerRequestSchema`), so the mock behaves like the back end.
- Demo user of the mock: `demo@pokedex.dev / demo1234`. The real demo user comes from the back-end seed (stage S7).

## Rules to keep the console clean

- `worker.start({ onUnhandledRequest: 'bypass', quiet: true })`: PokéAPI images go straight through and MSW writes no logs.
- The mock only runs in `npm run dev` (`import.meta.env.DEV`). In `npm run build`, Vite removes all the mock code, so production always uses the real back end. `public/mockServiceWorker.js` is still copied to `dist/`; the `Dockerfile` deletes it from the image.
