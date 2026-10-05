# Front-end progress

> What was done on the front end, in order. Entries are history: later entries may undo earlier ones.
> How each part looks and behaves today is in the specs of this folder (see the [index](README.md)).

---

## 2026-10-02: Foundation and back-end mock

- Vite + React 19 + TypeScript, with React Router, TanStack Query, React Hook Form + Zod and sonner. Tests with Vitest, Testing Library and MSW. Lint with oxlint (the Vite template's default).
- Structure by feature, with the rule `app → features → shared`. A single HTTP client (`httpClient`) sends the token, turns every `ProblemDetail` into an `ApiError` and logs out on a 401.
- An MSW mock of the whole API, used in the tests and, optionally, in `npm run dev`. It let the screens be built before the back end. It followed a first draft of the contract, replaced on 2026-10-03.
- Fixes: the URL is built by joining `VITE_API_URL` and the path (empty means the same origin, as behind Nginx), an empty success body returns `undefined`, and the app still opens if MSW fails to start.

---

## 2026-10-02: Screens (parts 1 to 10)

- Design system tokens in `index.css` and the shared components: `Button`, `TextField`, `Pagination`, `Skeleton`, the state messages, `ConfirmDialog` (native `<dialog>`) and `PokemonImage`.
- Layout and routes: sticky header, "Skip to content", focus moved to `<main>` on navigation, `ProtectedRoute` and a friendly page when a screen breaks.
- Screens: paginated list with the page in the URL, details with stats and a recursive evolution chain, sign in and sign up, My Pokémon (table on desktop, cards on mobile), edit form and toasts (`notify`).
- Code splitting per route with `React.lazy`: the initial chunk went from 547 kB to 253 kB.
- `Dockerfile` (Node 20 build, Nginx 1.27): SPA fallback, long cache for hashed assets and `/api` forwarded to `BACKEND_URL`.
- Checked: axe-core with 0 violations on every screen, keyboard order, no horizontal scroll at 360px, 768px and 1280px.

---

## 2026-10-03: Scope, English, validation and coverage

- Scope aligned with the PDF. Removed: search, My Pokémon filters, the `notes` field, USER/ADMIN roles, dark theme and the code only they used.
- Site, docs and tests in English; numbers formatted in `en-US`.
- Full validation: clean `npm ci`, `tsc`, oxlint, Prettier, knip, a broad ESLint run (React, jsx-a11y, Sonar), axe-core and an end-to-end script in Chrome. Fixed on the way: two copies of React Router loaded in the tests (`react-router/dom` import) and typed-lint warnings.
- Test coverage raised to 100% (`npm run test:coverage`).

---

## 2026-10-03: Front end aligned with the approved API contract

**Why:** the back end follows [`docs/api-contract.md`](../../docs/api-contract.md) (D-20, D-25 to D-27). The front end and its mock now follow it too:
- Details have only `id`, `name`, `imageUrl`, `stats`, `description` and `evolutionChain`. Types, height, abilities and "Local data" left the page.
- Sync is `POST /local/pokemon`: 201 shows "saved", 409 shows "already in the local database".
- A local Pokémon has no `version`. `PUT` replaces every field except the identifiers, so the form edits every field.
- Sign in by email; sign up with name, email and password (8 to 72). There is no `/me`: the session is the saved token.
- The Vite dev server forwards `/api` to `BACKEND_URL`, like Nginx, so the back end needs no CORS.
- Bug fixed: clicking "Sign up" on the sign in page lost the click. The form now validates on submit.
- Mock demo user `demo@pokedex.dev / demo1234`, shown on the sign in page only while the mock runs.

Checked with the mock and with the real back end (stages S1 to S3): 169 tests, 100% coverage.

---

## 2026-10-03: Sprites in the evolution chain (D-29)

- `EvolutionStage` is `{ name, spriteUrl, evolvesTo }`. Each stage shows its sprite (80px, 96px from 768px) and the "?" placeholder when there is none.

---

## 2026-10-05: Local CRUD with the real back end, `/api/v1` and comments

- The back end's CRUD stage (S6) added the list, get and delete of local Pokémon. The front end already called them (they answered 405 before), so its code did not change.
- Every route moved to `/api/v1` (back end, contract, front end, mock, tests and docs). Nginx and the Vite proxy already forward anything under `/api`.
- Nginx sends `Host $http_host`, so the `Location` header keeps the port (`http://localhost:3000/api/v1/...`).
- Code comments in the back end's style: JSDoc on types, API methods (`@throws {ApiError} <status> if ...`), schemas, helpers and the more complex components; `//` only for a reason the code cannot show. This replaces the "0 comments" rule of 2026-10-03.
- Subtitles with the total first: "1,351 Pokémon from the PokéAPI." and "3 Pokémon saved."
- `readFrom` also rejects protocol-relative paths such as `//evil.dev`, and `ProblemDetail` lost the `timestamp` field that nothing sends.

**How it was checked:** 170 tests with 100% coverage; the back end's `mvnw verify` (358 tests); 81 API checks through Nginx (every route, status and validation rule of the contract); 24 end-to-end checks in Chrome with the real back end, with a clean console.

---

## 2026-10-05: My Pokémon per user (D-31)

**Why:** every logged-in user saw the same local Pokémon, which did not match a screen called "My Pokémon". The developer asked for one collection per user (D-31).

- The back end now keeps an owner for each local Pokémon: each user lists, edits and deletes only their own, another user's id answers 404, and two users can sync the same Pokémon.
- No route, type or call changed in the front end. Texts: "3 Pokémon saved.", "Raichu saved to My Pokémon", "Pikachu is already in My Pokémon" and "This Pokémon is not in My Pokémon."
- The MSW mock keeps an owner per local Pokémon and behaves the same way; the 3 starting Pokémon belong to the demo user.

**How it was checked:** 172 front-end tests with 100% coverage (2 new for the mock with two users); the back end's `mvnw clean verify` with 368 tests (an integration test with two real users).

---

## 2026-10-05: User name in the header (D-32)

- The login response now has `name` (D-32). The front end saves it with the token and shows it next to "Sign out" (gray, cut with "…" when long; the screen reader hears "Signed in as Ash"). Sign out and an expired session remove it.
- A session saved before this change shows no name until the next sign in.
- The MSW mock returns the name too.

**How it was checked:** 174 front-end tests with 100% coverage; the back end's `mvnw clean verify` with 369 tests (a real user signs in and gets their name).

---

## 2026-10-05: "Add to My Pokémon" button

- The details page button is now **"+ Add to My Pokémon"** (it was "⟳ Sync"), with the hint "Saves a copy you can edit in My Pokémon." The empty My Pokémon list points to it. Only the screen texts changed: it is still the US03 sync (`POST /api/v1/local/pokemon`).

**How it was checked:** 174 tests with 100% coverage.

