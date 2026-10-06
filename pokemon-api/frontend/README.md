# Pokédex: front-end

React + TypeScript SPA that consumes the Pokémon API: paginated list (US01), details (US02), My Pokémon with save (US03), edit (US04) and delete, plus sign up, login and protected routes.

**Stack:** Vite, React 19, TypeScript, React Router 7, TanStack Query 5, React Hook Form + Zod, sonner (toasts), CSS Modules. Tests with Vitest, Testing Library and MSW.

## How to run

Requirement: Node 20.19 or newer. Start the back end on `http://localhost:8080`, then:

```bash
cp .env.example .env.development.local
npm install
npm run dev                              # http://localhost:5173
```

Keep `VITE_API_URL` empty: Vite forwards `/api` to `BACKEND_URL` (default `http://localhost:8080`), so the front end and the API share the same origin. In Docker, Nginx does the same (`docker compose up --build` from `pokemon-api/`).

| Command | What it does |
|---|---|
| `npm test` | Tests |
| `npm run lint` | Lint (oxlint) |
| `npm run build` | Type check + production build in `dist/` |

## Structure

```
src/
  app/          routes, layout and global providers
  features/     one folder per feature: types → api → queries (+ schemas, components, pages)
    pokemon/        list and details (US01, US02)
    local-pokemon/  My Pokémon: save, list, edit and delete (US03, US04)
    auth/           login, sign up, AuthProvider, ProtectedRoute
  shared/       HTTP client, components, formatting
  mocks/        fake back end (MSW) for the tests
```

- **Dependency rule:** `app → features → shared`. `shared` never imports from a feature.
- **State:** server data lives in TanStack Query (cache, loading and invalidation after each change). The only global state is the logged-in user (Context). Page numbers live in the URL.
- **Errors:** every error response becomes an `ApiError` (`ProblemDetail` format), and field errors go straight to the form. A 401 with a token logs the user out.
