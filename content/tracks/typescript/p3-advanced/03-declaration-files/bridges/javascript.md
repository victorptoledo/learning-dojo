---
from: javascript
status: draft
concept: "Types for the JavaScript you already have"
from_way: "Packages and scripts have no types; editors guess from usage and JSDoc."
native_way: "A .d.ts next to (or instead of) the JavaScript tells TypeScript the exact shapes; @types packages provide them for popular libraries."
sensei_tip: "You can type an existing JavaScript library without touching its code: write a .d.ts. Many teams migrate this way, one module at a time."
---

| JavaScript | TypeScript |
| --- | --- |
| `module.exports = function slugify(text, opts)` | `declare module "slugify" { export default function slugify(text: string, opts?: {...}): string }` |
| `window.analytics = ...` | `declare global { interface Window { analytics?: ... } }` |
| JSDoc in a `.js` file | `tsc --allowJs --declaration` generates `.d.ts` from it |
