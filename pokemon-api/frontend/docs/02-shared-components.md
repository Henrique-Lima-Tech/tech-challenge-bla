# Shared components

> Pieces reused across several screens (`src/shared/components/`).
> Back to the [index](README.md).

| Component | Responsibility |
|---|---|
| `Layout` (in `src/app/`) | Header + `<main>` + `<Outlet />` |
| `Button`, `ButtonLink` | Variants `primary`, `secondary`, `danger`, `ghost`. The `loading` prop shows the spinner and disables the button. `ButtonLink` is a `<Link>` with the same look |
| `TextField` | Label + input + error message + `aria-*`. Compatible with React Hook Form's `register()` |
| `Chip` | Neutral chip (abilities, tags) |
| `Pagination` | Previous / "Page X of Y" / Next, from the 0-based `page` and `totalPages`. Hidden when there is only one page |
| `Skeleton` | Animated gray block (respects `prefers-reduced-motion`) |
| `ErrorState` | Icon, the `ApiError` message and a "Try again" button |
| `EmptyState` | Icon, text and an optional action |
| `NotFoundPage` | Full 404 page (`StatusPages.tsx`) |
| `PokemonImage` | Image with fixed `width`/`height`; with no image, or if it fails, a circle with "?" |
| `ConfirmDialog` | Native `<dialog>` for confirmation |
| `ProtectedRoute` (in `features/auth/components/`) | Sends anyone not logged in to `/login`, keeping the page of origin |

> `Layout` and `ProtectedRoute` use `useAuth()`, so they do not live in `shared/`. The dependency rule is `app → features → shared`: `shared` never imports from a feature.

Each feature's components live in the feature itself (`features/pokemon/components/PokemonCard.tsx`, `StatList.tsx`, `EvolutionChain.tsx`...).

Formatting helpers in `src/shared/format.ts`: `formatPokemonNumber(25) → "#025"`, `capitalize`, `formatKg` and `formatInteger` (en-US).

Other utilities in `src/shared/`: `cx` (joins classes), `useDocumentTitle`, `useMediaQuery` and `forms/applyFieldErrors` (puts the back end's field errors into React Hook Form; `abilities[0]` goes to the `abilities` field).
