# Accessibility, tests and delivery

> Clean console, accessibility, tests and Dockerfile.
> Back to the [index](README.md).

- Every input has a `<label>`.
- The focus is always visible: `:focus-visible` with a 2px outline in `--color-focus`.
- The Tab order follows the visual order, and the whole page works with the keyboard alone.
- Each page has a single `<h1>`. `document.title` changes per page (e.g. "Pikachu · Pokédex").
- Images have `alt`. Decorative images have `alt=""`.
- Animations are off with `prefers-reduced-motion: reduce`.
- **Console with no warnings:** stable `key`s (ids, never indexes), no link inside a link, no input switching between controlled and uncontrolled, no missing image or favicon.
- Checked at 360px, 768px and 1280px.
- Tests with Vitest + Testing Library, with 100% coverage (`npm run test:coverage`).
- `Dockerfile`: Node build served by Nginx.
