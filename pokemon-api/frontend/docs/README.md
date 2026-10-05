# Front-end documentation

> One spec per part of the front end: how it looks and how it behaves. They follow the API contract in [`docs/api-contract.md`](../../docs/api-contract.md).
> What was done, step by step, is in [PROGRESS.md](PROGRESS.md).

| # | Document | What it covers |
|---|---|---|
| 0 | [Back-end mock](00-backend-mock.md) | MSW simulating the API, for the tests and for working without the back end |
| 1 | [Design system](01-design-system.md) | Colors, typography, spacing, breakpoints |
| 2 | [Shared components](02-shared-components.md) | Button, fields, Pagination, Skeleton, states... |
| 3 | [Layout and routes](03-layout-and-routes.md) | Header, route map, protected routes |
| 4 | [Pokémon list](04-pokemon-list.md) | `/pokemon`: paginated grid (US01) |
| 5 | [Pokémon details](05-pokemon-detail.md) | `/pokemon/:idOrName`: image, stats, description, evolution (US02) and "Add to My Pokémon" (the US03 sync) |
| 6 | [Sign in and sign up](06-authentication.md) | `/login`, `/register` |
| 7 | [My Pokémon](07-my-pokemon.md) | `/my-pokemon`: local list and delete |
| 8 | [Edit form](08-local-pokemon-form.md) | Edit every field of a local Pokémon (US04) |
| 9 | [Errors and toasts](09-errors-and-toasts.md) | 404, back end down, feedback messages |
| 10 | [Accessibility and quality](10-accessibility-and-quality.md) | Accessibility, tests, Dockerfile |

## Rules for every part

- **Style:** CSS Modules (`Component.module.css` next to the `.tsx`), using only the design system tokens.
- **Data:** screens only use the hooks in `src/features/*/queries.ts` and `useAuth()`. No screen calls `fetch` directly.
- **Structure:** a feature's components go in `src/features/<feature>/components/` and its pages in `pages/`. Anything used by more than one feature goes in `src/shared/components/`.
- **Quality bar:** `npm run build`, `npm run lint` and the browser console with no errors or warnings.
