# Errors and toasts

> Error pages, failure state and feedback messages.
> Back to the [index](README.md).

## Error pages

```
┌────────────────────────────────┐
│             (?)                │
│   Pokémon not found            │
│   There is no Pokémon with the │
│   name or number "pikachuu".   │
│   [View all Pokémon]           │
└────────────────────────────────┘
```

- **`NotFoundPage`**: unknown route and Pokémon 404, with the message adapted to each case.
- **Back end down** (`ApiError.status === 0` or 5xx): `ErrorState` with "Could not connect to the server" and the "Try again" button (`refetch`).
- **A screen that breaks while rendering:** `RouteErrorPage` with "Something went wrong" and a "Back to home" link.

## Toasts

Uses `sonner` (the `<Toaster>` is in `AppProviders`). Success stays 3s; an error stays 5s and does not go away on its own if it has an action.

| Action | Success | Error |
|---|---|---|
| Add to My Pokémon | "Raichu saved to My Pokémon" | 409: "Pikachu is already in My Pokémon" (info); otherwise the `ApiError` message |
| Edit | "Changes saved" | field errors in the form, a toast only if it is generic |
| Delete | "Bulbasaur deleted" | 404: "This Pokémon had already been deleted" (info); otherwise the `ApiError` message |
| Logout | "You have signed out" | |
| Session expired (401) | | "Your session has expired. Please sign in again." |
