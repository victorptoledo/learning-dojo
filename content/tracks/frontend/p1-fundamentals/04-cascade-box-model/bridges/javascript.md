---
from: javascript
status: draft
concept: "Styles in code vs in the cascade"
from_way: "CSS-in-JS or inline style objects where the last object spread wins."
native_way: "In plain CSS, the winner depends on specificity before order, and inline style beats any selector."
sensei_tip: "Object spread is 'last wins'; CSS is 'most specific wins, then last'. Debug in DevTools' Styles panel, which shows exactly why each declaration lost."
---

| JavaScript / Node.js | In the browser |
| --- | --- |
| `{...base, ...override}` | Order of appearance (only among equal specificity) |
| `element.style.color = ...` | Inline style, beats selectors |
| Theme object | Custom properties on `:root` |
| `getComputedStyle(el)` | The Computed panel |
