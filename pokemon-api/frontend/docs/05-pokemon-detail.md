# Pokémon details (US02)

> The `/pokemon/:idOrName` screen: image, stats, description, evolution chain and the "Add to My Pokémon" button (the US03 sync).
> Back to the [index](README.md).

```
┌──────────────────────────────────────────────────────────────────────┐
│ ‹ Back to list                                                       │
│                                                                      │
│ ┌──────────────────────┐   #025                                      │
│ │                      │   Pikachu                                   │
│ │       [sprite]       │                                             │
│ │       240px          │   [+ Add to My Pokémon]                     │  ← logged in only
│ │                      │   Saves a copy you can edit in My Pokémon.  │
│ └──────────────────────┘                                             │
│                                                                      │
│ Description ────────────────────────────────────────────────────     │
│ When several of these Pokémon gather, their electricity could...     │
│                                                                      │
│ Stats ───────────────────────────────────────────────────────────    │
│ HP          35  █████░░░░░░░░░░░░                                    │
│ Attack      55  ████████░░░░░░░░░                                    │
│ Defense     40  ██████░░░░░░░░░░░                                    │
│ Sp. Attack  50  ███████░░░░░░░░░░                                    │
│ Sp. Defense 50  ███████░░░░░░░░░░                                    │
│ Speed       90  █████████████░░░░                                    │
│ Total      320                                                       │
│                                                                      │
│ Evolution chain ─────────────────────────────────────────────────    │
│   [sprite]        →        [sprite]        →        [sprite]         │
│    Pichu               Pikachu (current)            Raichu           │
└──────────────────────────────────────────────────────────────────────┘
```

The page shows exactly what the contract's `GET /api/v1/pokemon/{idOrName}` returns: `id`, `name`, `imageUrl`, `stats`, `description` and `evolutionChain` (each stage with `name`, `spriteUrl` and `evolvesTo`, D-29). US02 asks for nothing else (types, height and abilities are not in it).

- **Mobile:** everything in one column, with the image on top (up to 200px).
- **Image:** `imageUrl` (the PokéAPI sprite) with `alt`, not lazy (it is the main content), scaled with `image-rendering: pixelated`. `null` → placeholder.
- **Stats (`StatList`):** label, value and a bar proportional to 255 (the game's maximum). Colors: < 50 orange, 50–89 yellow, ≥ 90 green. Uses `role="meter"` with `aria-valuenow`, for screen readers. Unknown stat names are shown as they come.
- **Description:** `description`, or "No description." when it is `null`.
- **Evolution chain (`EvolutionChain`):** a **recursive** component over `{ name, spriteUrl, evolvesTo[] }`.
  - Each stage shows its sprite (80px, 96px from 768px, `alt=""` because the name is next to it, lazy) and the "?" placeholder when `spriteUrl` is `null` or the image fails.
  - Each stage is a link to `/pokemon/:name`. The current Pokémon is highlighted and is not a link (`aria-current`).
  - Between stages, an arrow (the screen reader hears "evolves into").
  - **Branching** (Eevee): evolutions of the same stage are stacked vertically, to the right of the arrow.
  - **No evolution** (Ditto): the text "This Pokémon does not evolve."
  - **Mobile:** the chain is vertical, with the arrows pointing down.
- **"Add to My Pokémon" (logged-in users only):** the US03 sync. `useCreateLocalPokemon` → `POST /api/v1/local/pokemon` with `{ pokemon: name }`. The button shows the spinner and is disabled during the call.
  - 201 → toast "Raichu saved to My Pokémon".
  - 409 → toast "Pikachu is already in My Pokémon" (the details do not say whether the user already has it, so the button is always there).
  - Other errors → toast with the `ApiError` message.
  - Below the button, the hint "Saves a copy you can edit in My Pokémon."
- **404:** the friendly "Pokémon not found" screen ([Errors and toasts](09-errors-and-toasts.md)), with the searched term and a link to the list.
- **502 / back end down:** `ErrorState` as the page heading, with "Try again".
