# Flexbox, Grid and Responsive Design

Modern CSS has two layout systems built for the job: **flexbox** for one dimension (a row or a
column) and **grid** for two (rows and columns together). With media and container queries,
they make layouts that work from a 320px phone to a 2560px monitor.

## Flexbox: one direction

```css
.toolbar {
  display: flex;
  flex-direction: row;          /* or column */
  flex-wrap: wrap;              /* let items go to the next line */
  gap: 0.5rem 1rem;             /* row gap, column gap */
  justify-content: space-between; /* along the main axis */
  align-items: center;          /* across it */
}
.toolbar .search { flex: 1 1 12rem; } /* grow, shrink, basis */
```

- `flex: 1` means "share the free space"; `flex: 0 0 auto` means "keep your size".
- An item's `margin-left: auto` (or `margin-top: auto` in a column) pushes it to the far
  end: the classic way to keep a card's button at its bottom.
- Flex items shrink to their minimum content width at most; `min-width: 0` lets long words or
  code blocks shrink further (or add `overflow-wrap: anywhere`).

## Grid: two directions

```css
.cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);   /* three equal columns */
  gap: 1rem;
}
.layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 16rem;
  grid-template-areas: "main aside";
}
.cards-auto {
  grid-template-columns: repeat(auto-fill, minmax(14rem, 1fr)); /* as many as fit */
}
```

- `fr` is a fraction of the free space. `1fr` really means `minmax(auto, 1fr)`: a column never
  gets narrower than its content, so a long URL can blow it up. `minmax(0, 1fr)` fixes that.
- Items in a grid row stretch to the same height by default (`align-items: stretch`).
- `grid-column: 1 / -1` spans all columns; `grid-template-areas` names regions.
- **Subgrid** (`grid-template-rows: subgrid`) lets nested items line up with the parent grid.

## Which one?

| Layout | Use |
| --- | --- |
| A nav bar, a row of buttons, centering one thing | Flexbox |
| Content-sized items that wrap | Flexbox with `flex-wrap` |
| A page skeleton, a card grid, anything aligned in rows **and** columns | Grid |
| Centering in both axes | `display: grid; place-items: center` |

## Responsive design

**Mobile first**: write the one-column layout with no media query, then add
`min-width` queries as the screen grows. Small screens get the simplest CSS.

```css
.cards { display: grid; gap: 1rem; }
@media (min-width: 40rem) { .cards { grid-template-columns: repeat(2, 1fr); } }
@media (min-width: 64rem) { .cards { grid-template-columns: repeat(3, 1fr); } }
```

- Use `rem` (or `em`) in media queries so they respect the user's font size.
- Choose breakpoints where **your content** breaks, not by device names.
- Fluid values reduce the number of breakpoints:
  `font-size: clamp(1rem, 0.9rem + 0.5vw, 1.25rem)`, `max-width: min(72rem, 100%)`.
- Images: `max-width: 100%; height: auto`, and `srcset`/`sizes` to send small files to small
  screens.
- Test without a horizontal scrollbar at 320px (WCAG 1.4.10, Reflow).

## Media features beyond width

```css
@media (prefers-color-scheme: dark)   { :root { --bg: #111; --text: #eee; } }
@media (prefers-reduced-motion: reduce) { * { animation: none; transition: none; } }
@media (hover: hover) and (pointer: fine) { .card:hover { box-shadow: ...; } }
```

## Container queries

A media query asks about the **viewport**; a container query asks about the **component's
parent**. The same card can be narrow in a sidebar and wide in main content:

```css
.card-list { container-type: inline-size; }
@container (min-width: 30rem) {
  .card { display: grid; grid-template-columns: 10rem 1fr; }
}
```

Container queries are Baseline in all modern browsers and make components truly reusable.

## Summary

- Flexbox for one axis, grid for two; `gap` spaces both.
- `minmax(0, 1fr)` stops a grid column from being stretched by its content.
- Mobile first with `min-width` queries in `rem`; breakpoints follow the content.
- Media features cover color schemes, reduced motion and input types.
- Container queries size components by their container, not the viewport.

## Further reading

- MDN, "Flexbox": <https://developer.mozilla.org/en-US/docs/Learn_web_development/Core/CSS_layout/Flexbox>
- MDN, "CSS grid layout": <https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_grid_layout>
- MDN, "Responsive design": <https://developer.mozilla.org/en-US/docs/Learn_web_development/Core/CSS_layout/Responsive_Design>
- MDN, "CSS container queries": <https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_containment/Container_queries>
- W3C, CSS Flexible Box Layout Level 1: <https://www.w3.org/TR/css-flexbox-1/>
- W3C, CSS Grid Layout Level 2: <https://www.w3.org/TR/css-grid-2/>
- web.dev, "Learn Responsive Design": <https://web.dev/learn/design>
