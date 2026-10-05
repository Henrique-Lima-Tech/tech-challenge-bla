# Pokédex: front-end

A React + TypeScript SPA that consumes the challenge's Spring Boot API: paginated list (US01), details (US02), saving a Pokémon to the user's own collection, My Pokémon (US03), listing, editing (US04) and deleting the saved ones, with sign up, login and protected routes. It follows the API contract in [`docs/api-contract.md`](../docs/api-contract.md).

**Stack:** Vite, React 19, TypeScript, React Router 7, TanStack Query 5, React Hook Form + Zod, sonner (toasts), CSS Modules. Tests with Vitest, Testing Library and MSW.

## How to run

Requirement: Node 20.19 or newer (required by Vite 8).

### With the back-end mock (no Spring Boot needed)

```bash
cp .env.example .env.development.local   # already has VITE_USE_MOCKS=true
npm install
npm run dev                              # http://localhost:5173
```

Demo user of the mock: `demo@pokedex.dev / demo1234`. The mock only exists in `npm run dev`; the production build has none of it.

### With the real back end

Start the back end on `http://localhost:8080` and set `VITE_USE_MOCKS=false` in `.env.development.local`, keeping `VITE_API_URL` empty. The Vite dev server forwards `/api` to `BACKEND_URL`, so the back end needs no CORS. For the same reason, pointing `VITE_API_URL` straight at the back end does not work.

### With Docker

```bash
docker build -t pokedex-frontend .
docker run -p 3000:80 -e BACKEND_URL=http://backend:8080 --network <backend-network> pokedex-frontend
```

Nginx serves the build and forwards `/api` to `BACKEND_URL`, so the front end and the API share the same origin. From the `pokemon-api/` folder, `docker compose up --build` starts the front end (http://localhost:3000) together with the back end.

## Scripts

| Command | What it does |
|---|---|
| `npm run dev` | Development server |
| `npm test` | Tests (Vitest + Testing Library + MSW) |
| `npm run test:coverage` | Tests with a coverage report (terminal table + `coverage/index.html`) |
| `npm run lint` | Lint (oxlint) |
| `npm run build` | Type check + production build in `dist/` |
| `npm run format` | Formats with Prettier |

## Environment variables

| Variable | Where | Default | Description |
|---|---|---|---|
| `VITE_API_URL` | build | `http://localhost:8080` (empty in `.env.example`) | Back-end URL as seen by the browser. Empty = same origin as the front end, with `/api` forwarded by Vite (dev) or Nginx (Docker) |
| `VITE_USE_MOCKS` | dev | `false` | `true` turns on the mock (MSW) in `npm run dev` |
| `BACKEND_URL` | dev server, container | `http://localhost:8080` (dev), `http://backend:8080` (container) | Where Vite (dev) or Nginx (container) forwards `/api` |

## Structure

```
src/
  app/          routes, layout (header) and global providers
  features/     one folder per feature, each with types → api → queries (+ schemas, components, pages)
    pokemon/        list and details (US01, US02)
    local-pokemon/  My Pokémon, per user: save (sync), list, edit and delete (US03, US04)
    auth/           login, sign up, AuthProvider, ProtectedRoute
  shared/       what every feature uses: HTTP client, components, formatting
  mocks/        fake back end (MSW) for dev and tests
```

- **Dependency rule:** `app → features → shared`. `shared` never imports from a feature.
- **State:** server data lives in TanStack Query (cache, loading and invalidation after each change). The only global state is whether the user is logged in and their name (Context, based on the token and name saved at login). Page numbers live in the URL.
- **Errors:** every error response becomes an `ApiError` in the `httpClient` (`ProblemDetail` format), and field errors go straight to the form. A 401 with a token logs the user out.

## Documentation

The spec of each screen, the design system and the mock are in [`docs/`](docs/README.md). The API contract is [`docs/api-contract.md`](../docs/api-contract.md) at the root of the repository. What was done, step by step, is in [`docs/PROGRESS.md`](docs/PROGRESS.md).
