# My Pokémon

> The `/my-pokemon` screen: the logged-in user's own Pokémon (D-31), with delete.
> Back to the [index](README.md).

```
Desktop: table
┌──────────────────────────────────────────────────────────────────────┐
│ My Pokémon                                                           │
│ 3 Pokémon saved.                                                     │
│                                                                      │
│ ┌──────────────────────────────────────────────────────────────────┐ │
│ │     Name             Localized name  Region  Tags                │ │
│ │ [s] #001 Bulbasaur   Bulbassauro     Kanto   (starter)(grass)    │ │
│ │                                                [Edit] [Delete]   │ │
│ │ [s] #004 Charmander  —               Kanto   (starter)           │ │
│ │                                                [Edit] [Delete]   │ │
│ └──────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────┘

Mobile: the same information in cards
┌────────────────────────────────┐
│ [s] #001 Bulbasaur             │
│     Bulbassauro · Kanto        │
│     (starter) (grass)          │
│     [Edit]  [Delete]           │
└────────────────────────────────┘
```

- The **Edit** and **Delete** buttons show up for everyone, because the screen already requires login. There is no "New Pokémon": a Pokémon enters the list through the **Add to My Pokémon** button on the details page.
- Each row is one of the user's local Pokémon from `GET /api/v1/local/pokemon`; other users' Pokémon never show up. The number and the link to the public details page use `pokeApiId` (`/pokemon/4`); **Edit** and **Delete** use the local `id` (`/my-pokemon/2/edit`). Empty values show as "—", and the region shows as typed.
- A real `<table>` on desktop (`<th scope="col">`), accessible. Below 768px, it switches to cards.
- **Empty:** "No Pokémon saved yet. Open a Pokémon and click **Add to My Pokémon**." with a link to the list.
- **Pagination:** 20 per page, the page in the URL (`?page=2`), hidden when everything fits in one page.
- **Page past the total:** "No Pokémon on this page" with a "Back to page 1" link.

## Delete confirmation

```
┌─────────────────────────────────────┐
│ Delete Bulbasaur?                   │
│                                     │
│ The local record and its custom     │
│ fields will be deleted. The PokéAPI │
│ data is not affected.               │
│                                     │
│                [Cancel] [Delete]    │
└─────────────────────────────────────┘
```

- `ConfirmDialog` component using the native `<dialog>` (`showModal()`): it already traps the focus and closes with Esc.
- The initial focus is on **Cancel**, the safe action. **Delete** uses the `--color-danger` color.
- During the delete, the buttons are disabled.
- Success → toast "Bulbasaur deleted" and the list updates on its own (the hook invalidates the cache).
- 404 → toast "This Pokémon had already been deleted" and the list updates.
