# Design system

> Visual direction, colors, typography, spacing and breakpoints. The base of every other screen.
> Back to the [index](README.md).

## Visual direction

- **Clean and functional:** light background, white cards and plenty of white space. Color belongs to the Pokémon images, not to the interface.
- **One accent color:** Pokédex red, used only on main actions and active links.
- **Mobile first:** each screen is designed for 360px first and then gains columns.
- **Always show the state:** every screen that fetches data has designed *loading*, *empty* and *error* states (see [Shared components](02-shared-components.md) and [Errors and toasts](09-errors-and-toasts.md)).

### Style in code

- **CSS Modules** (`Component.module.css` next to the `.tsx`), no UI library.
- **Global tokens** in `src/index.css` as CSS variables. Components only use `var(--...)`, never loose colors.

---

## Design tokens (`src/index.css`)

### Colors

| Token | Value | Use |
|---|---|---|
| `--color-bg` | `#f5f6f8` | Page background |
| `--color-surface` | `#ffffff` | Cards, header, forms |
| `--color-border` | `#e2e5ea` | Borders and dividers |
| `--color-text` | `#1f2328` | Main text |
| `--color-text-muted` | `#5f6773` | Labels, metadata |
| `--color-primary` | `#d62828` | Active link, highlights on the background |
| `--color-primary-bg` | `#d62828` | Main button background (white text) |
| `--color-primary-hover` | `#b51f1f` | Main button hover |
| `--color-danger` | `#b42318` | Error messages |
| `--color-danger-bg` | `#b42318` | Delete button background (white text) |
| `--color-danger-hover` | `#912018` | Delete button hover |
| `--color-on-color` | `#ffffff` | Text on the primary and delete buttons |
| `--color-hover` | `#eef0f3` | Hover background of secondary/ghost buttons and evolution links |
| `--color-chip` | `#eaedf1` | Chip background (abilities, tags) |
| `--color-skeleton` | `#e7e9ee` | Skeleton blocks and the image placeholder |
| `--color-focus` | `#2563eb` | Focus outline (keyboard) |
| `--color-stat-low` / `-mid` / `-high` | `#e8590c` / `#e0a800` / `#2f9e44` | Stat bars: < 50, 50–89, ≥ 90 |

Light theme only: the PDF does not ask for a dark theme. Minimum AA contrast (4.5:1) for text, checked by calculation.

### Typography, spacing and shape

| Token | Value |
|---|---|
| `--font-sans` | `system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif` |
| Sizes | `--text-sm` 0.875rem · `--text-base` 1rem · `--text-lg` 1.25rem · `--text-xl` 1.75rem · `--text-2xl` 2.25rem |
| Spacing | 4px scale: `--space-1` 4px · `--space-2` 8px · `--space-3` 12px · `--space-4` 16px · `--space-6` 24px · `--space-8` 32px · `--space-12` 48px |
| `--radius` | 12px (cards) · `--radius-sm` 6px (buttons, inputs, badges) |
| `--shadow` | `0 1px 3px rgb(0 0 0 / 0.08)` |
| Max content width | 1200px, centered, with a 16px side margin on mobile |

Pokémon numbers always have 3 digits: `#001`, `#025`, `#133`.

### Breakpoints

| Name | Width | Pokémon grid columns |
|---|---|---|
| mobile | < 480px | 1 (horizontal card) |
| wide mobile | ≥ 480px | 2 |
| tablet | ≥ 768px | 3 |
| desktop | ≥ 1024px | 4 |
