# Pokémon list (US01)

> The `/pokemon` screen: paginated grid with sprite, category, weight and abilities.
> Back to the [index](README.md).

```
┌──────────────────────────────────────────────────────────────────────┐
│ Pokémon                                                              │
│ 1,351 Pokémon from the PokéAPI.                                      │
│                                                                      │
│ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐  │
│ │   [sprite]   │ │   [sprite]   │ │   [sprite]   │ │   [sprite]   │  │
│ │ #001         │ │ #002         │ │ #003         │ │ #004         │  │
│ │ Bulbasaur    │ │ Ivysaur      │ │ Venusaur     │ │ Charmander   │  │
│ │ Seed Pokémon │ │ Seed Pokémon │ │ Seed Pokémon │ │ Lizard Pokém.│  │
│ │ ⚖ 6.9 kg     │ │ ⚖ 13 kg      │ │ ⚖ 100 kg     │ │ ⚖ 8.5 kg     │  │
│ │ (overgrow)   │ │ (overgrow)   │ │ (overgrow)   │ │ (blaze)      │  │
│ │ (chlorophyll)│ │ (chlorophyll)│ │ (chlorophyll)│ │ (solar-power)│  │
│ └──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘  │
│                         ...                                          │
│                                                                      │
│          [‹ Previous]   Page 1 of 68   [Next ›]                      │
└──────────────────────────────────────────────────────────────────────┘

Card on mobile (< 480px): horizontal, to fit more on the screen
┌────────────────────────────────┐
│ [sprite]  #001 Bulbasaur       │
│  72px     Seed Pokémon · 6.9 kg│
│           (overgrow)(chloroph.)│
└────────────────────────────────┘
```

**Card (`PokemonCard`)**
- The whole card is a link (`<Link>`) to `/pokemon/:id`, with a visible focus and a slight lift on hover.
- 96px sprite (72px on mobile), with `alt="Bulbasaur"`, `loading="lazy"` and fixed `width` and `height` (so the layout does not jump).
- `null` `spriteUrl` → gray circle with "?".
- Name with a capital first letter. Weight formatted in en-US (`6.9 kg`) with `Intl.NumberFormat`.
- Abilities as small chips (`--color-chip` background, `--text-sm`).

**Pagination (`Pagination`)**
- The page lives in the URL: `?page=1` in the URL is `page=0` in the API. Going back and reloading keep the page.
- "Previous" is disabled on the first page and "Next" on the last one.
- Invalid `?page` (text, 0, negative) → treated as page 1.
- Changing the page scrolls to the top. The previous page's cards stay on screen, with reduced opacity, until the new one arrives (`keepPreviousData`).

**States:** loading = 8 skeleton cards · error = `ErrorState` with "Try again" · page past the total = `EmptyState` with a "Back to page 1" link.
