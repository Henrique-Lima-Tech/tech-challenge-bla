# Layout and routes

> Header, route map and protected routes.
> Back to the [index](README.md).

## Route map

| Route | Screen | Access | Hook |
|---|---|---|---|
| `/` | redirects to `/pokemon` | public | |
| `/pokemon?page=N` | List (US01) | public | `usePokemonList` |
| `/pokemon/:idOrName` | Details (US02) | public ("Add to My Pokémon" only for logged-in users) | `usePokemonDetails`, `useCreateLocalPokemon` |
| `/login` | Sign in | logged out only | `useAuth().login` |
| `/register` | Sign up | logged out only | `useRegister` |
| `/my-pokemon` | Local list | logged in | `useLocalPokemonList`, `useDeleteLocalPokemon` |
| `/my-pokemon/:id/edit` | Edit form (US04) | logged in | `useLocalPokemon`, `useUpdateLocalPokemon` |
| `*` | Page not found | public | |

A protected route without login → redirects to `/login` and, after the login, goes back to where the user was.
The PDF only asks for public and protected routes, so there are no roles: a logged-in user can add, edit and delete their own Pokémon (D-31).

## General layout (header)

```
Desktop
┌──────────────────────────────────────────────────────────────────────┐
│  ◓ Pokédex      Pokémon   My Pokémon                  Ash  [Sign out] │
└──────────────────────────────────────────────────────────────────────┘

Mobile (360px)
┌────────────────────────────────┐
│ ◓ Pokédex             [Sign in]│
│ Pokémon   My Pokémon           │
└────────────────────────────────┘
```

- The header sticks to the top (`position: sticky`), with a `--color-surface` background and a bottom border.
- The current page's link is highlighted (underlined in `--color-primary`, with `aria-current="page"`).
- "My Pokémon" only shows up for logged-in users.
- Logged out: **Sign in** button (hidden on the sign in and sign up pages). Logged in: the user's name in gray, small text (cut with "…" when long; the screen reader hears "Signed in as Ash") and the outlined **Sign out** button. The name comes from the login response (D-32).
- On mobile, the links move down to a second row. No hamburger menu is needed, because there are only two links.
- The user counts as logged in while a token is saved. If the token has expired, the first protected request answers 401, and the app logs out with a toast.
- The layout also has a "Skip to content" link and `ScrollRestoration`. A screen that breaks while rendering shows `RouteErrorPage` ([Errors and toasts](09-errors-and-toasts.md)).
