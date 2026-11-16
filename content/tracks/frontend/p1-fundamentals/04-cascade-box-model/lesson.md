# The Cascade, Specificity and the Box Model

CSS looks simple until two rules disagree. Knowing **which declaration wins** and **how big a
box really is** turns CSS from guesswork into engineering.

## Rules, selectors and declarations

```css
.card > h2:first-child {   /* selector */
  color: #1c1917;           /* declaration: property + value */
  margin-block: 0 0.5rem;
}
```

| Selector | Matches |
| --- | --- |
| `h2`, `.card`, `#main` | Type, class, id |
| `[type="email"]` | An attribute |
| `.card h2` / `.card > h2` | A descendant / a direct child |
| `h2 + p` / `h2 ~ p` | The next sibling / any later sibling |
| `:hover`, `:focus-visible`, `:checked`, `:invalid` | A state |
| `:first-child`, `:nth-child(2n)`, `:not(.done)` | Structure and logic |
| `:is(h1, h2)`, `:where(h1, h2)` | Any of a list (`:where` adds no specificity) |
| `.card:has(img)` | A parent that contains something |
| `::before`, `::marker`, `::placeholder` | A pseudo-element |

## The cascade: who wins

When several declarations set the same property on the same element, the browser sorts them
by, in order:

1. **Origin and importance**: user-agent styles < user styles < author (your) styles;
   `!important` reverses the order. Transitions and animations also have a place here.
2. **Context**: styles inside a shadow DOM vs outside (Phase 4).
3. **Cascade layers** (`@layer`): later layers win over earlier ones; unlayered styles win
   over all layers.
4. **Specificity**: a score `(ids, classes/attributes/pseudo-classes, types/pseudo-elements)`
   compared left to right. `#nav a` (1,0,1) beats `.menu .item a` (0,2,1).
   Inline `style=""` beats any selector.
5. **Order of appearance**: the later declaration wins.

```css
@layer reset, base, components, utilities;
@layer base       { a { color: blue; } }
@layer components { .btn { color: white; } }   /* wins over base, whatever the specificity */
```

Keep specificity low and flat (mostly single classes) so later rules can override earlier
ones without an arms race. `!important` is for utilities and user styles, not for winning
fights.

## Inheritance

Some properties **inherit** from the parent (`color`, `font-*`, `line-height`, `visibility`,
`cursor`); most layout properties do not (`margin`, `border`, `width`, `display`). The keywords
`inherit`, `initial`, `unset` and `revert` control it explicitly. Set fonts and colors once on
`body` and let them flow.

## Computed values and units

| Unit | Relative to | Use for |
| --- | --- | --- |
| `px` | A reference pixel | Borders, fine details |
| `rem` | The root font size (usually 16px) | Spacing, font sizes, breakpoints: they follow the user's font setting |
| `em` | The element's own font size | Padding that scales with the text |
| `%` | The containing block (for widths) | Fluid sizes |
| `vw`, `vh`, `dvh` | The viewport (`dvh` follows mobile toolbars) | Full-screen sections |
| `ch` | The width of "0" | Readable line lengths: `max-width: 65ch` |

DevTools' **Computed** panel shows the final value of each property and which rule set it.

## Custom properties (CSS variables)

```css
:root {
  --color-accent: #166534;
  --space: 1rem;
}
@media (prefers-color-scheme: dark) {
  :root { --color-accent: #4ade80; }
}
.button { background: var(--color-accent); padding: var(--space); }
```

Custom properties cascade and inherit like other properties, can change at runtime and in
media queries, and are the usual way to implement **design tokens** and themes.

## The box model

Every element is a box: **content**, then **padding**, then **border**, then **margin**.

```text
┌──────────── margin ────────────┐
│ ┌────────── border ──────────┐ │
│ │ ┌──────── padding ───────┐ │ │
│ │ │        content         │ │ │
│ │ └────────────────────────┘ │ │
│ └────────────────────────────┘ │
└────────────────────────────────┘
```

- With the default `box-sizing: content-box`, `width: 300px; padding: 20px; border: 1px`
  gives a 342px-wide box. With `box-sizing: border-box`, the box is 300px and the content
  shrinks. Almost every project starts with
  `*, *::before, *::after { box-sizing: border-box; }`.
- Vertical margins between blocks **collapse**: `margin-bottom: 2rem` above
  `margin-top: 1rem` gives 2rem, not 3rem. Margins do not collapse inside flex and grid
  containers.
- `display` decides how a box takes part in layout: `block` (full width, new line), `inline`
  (flows in text; width and height ignored), `inline-block`, `flex`, `grid`, `none`.
- **Logical properties** (`margin-inline`, `padding-block`, `inset-inline-start`) follow the
  writing direction, so a right-to-left language works without new CSS.

## Overflow and stacking

`overflow: auto` adds scrollbars when content does not fit; `hidden` clips it. `position`
(`relative`, `absolute`, `fixed`, `sticky`) takes boxes out of the normal flow; `z-index`
orders positioned boxes inside a **stacking context** (created by `position` with `z-index`,
`opacity < 1`, `transform` and others). A `z-index: 9999` that "does not work" is usually
trapped in another stacking context.

## Summary

- The cascade sorts by origin and importance, layers, specificity, then order.
- Keep specificity low; use `@layer` to order whole groups of rules.
- Inherited properties flow from parents; set typography once.
- Use `rem` for sizes, custom properties for tokens and themes.
- `border-box` makes widths predictable; vertical margins collapse in normal flow.

## Further reading

- MDN, "Introducing the CSS Cascade": <https://developer.mozilla.org/en-US/docs/Web/CSS/Cascade>
- MDN, "Specificity": <https://developer.mozilla.org/en-US/docs/Web/CSS/Specificity>
- MDN, "The box model": <https://developer.mozilla.org/en-US/docs/Learn_web_development/Core/Styling_basics/Box_model>
- W3C, CSS Cascading and Inheritance Level 5: <https://www.w3.org/TR/css-cascade-5/>
- W3C, Selectors Level 4: <https://www.w3.org/TR/selectors-4/>
- web.dev, "Learn CSS": <https://web.dev/learn/css>
