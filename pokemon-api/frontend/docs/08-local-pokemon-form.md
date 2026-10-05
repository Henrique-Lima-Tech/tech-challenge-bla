# Edit form (US04)

> The `/my-pokemon/:id/edit` screen. `:id` is the local id.
> Back to the [index](README.md).

```
┌──────────────────────────────────────────────────────────────────────┐
│ ‹ My Pokémon                                                         │
│ Edit Bulbasaur                                                       │
│                                                                      │
│ [sprite] #001 Bulbasaur   View details                               │
│                                                                      │
│ ┌ Pokémon data ────────────────────────────────────────────────────┐ │
│ │ Name                          Category                           │ │
│ │ [bulbasaur            ]       [Seed Pokémon            ]         │ │
│ │ Weight (kg)                   Sprite URL                         │ │
│ │ [6.9                  ]       [https://...             ]         │ │
│ │ Abilities (comma separated)                              2/10    │ │
│ │ [overgrow, chlorophyll                                 ]         │ │
│ └──────────────────────────────────────────────────────────────────┘ │
│ ┌ Custom fields ───────────────────────────────────────────────────┐ │
│ │ Localized name                Region                             │ │
│ │ [Bulbassauro          ]       [Kanto                   ]         │ │
│ │ Tags (comma separated)                                   2/20    │ │
│ │ [starter, grass                                        ]         │ │
│ │ Preview: (starter) (grass)                                       │ │
│ └──────────────────────────────────────────────────────────────────┘ │
│                                         [Cancel]  [Save changes]     │
└──────────────────────────────────────────────────────────────────────┘
```

- **Every field except the identifiers can be edited**, because the contract's `PUT /api/v1/local/pokemon/{id}` replaces all fields except `id` and `pokeApiId` (decision D-25: "update operations for any Pokemon").
- The top shows the sprite, the PokéAPI number and the name, with a "View details" link to `/pokemon/:pokeApiId`.
- **`LocalPokemonForm` component:** receives the loaded `LocalPokemon` and the submit function. The page fetches the data and handles the loading and error states.
- **Two groups:** "Pokémon data" (name, category, weight, sprite URL, abilities) and "Custom fields" (the US03 fields: localized name, region, tags). Two columns from 768px, one on mobile.
- **Validation (`localPokemonFormSchema`, the contract's rules):**

| Field | Rule |
|---|---|
| Name | required, ≤ 50 |
| Sprite URL | optional, valid `http`/`https` URL, ≤ 500 |
| Category | optional, ≤ 50 |
| Weight (kg) | required, 0 to 9999.9, at most 1 decimal place |
| Abilities | 1 to 10, each ≤ 50 |
| Localized name | optional, ≤ 100 |
| Region | optional, ≤ 100 (free text) |
| Tags | up to 20, each ≤ 30 |

- Abilities and tags are typed as text separated by commas and converted with `parseList()` (trimmed, lowercased, without empty or repeated items). The counters show `n/10` and `n/20`, and the tags preview shows the normalized chips.
- Empty optional fields are sent as `null`; no tags is an empty list.
- **Back-end errors:**
  - 400 → each `fieldError` shows on its field (`setError`), including list items (`abilities[0]` goes to Abilities), and the focus goes to the first one.
  - 404 when opening the edit page, or an invalid id → the "Pokémon not found" screen.
  - Any other error → a toast with the `ApiError` message.
- **Success:** toast "Changes saved" and back to `/my-pokemon`.
- With no changes (`isDirty === false`), the Save button is disabled.
- While loading, it shows a skeleton of the form, not an empty form.
