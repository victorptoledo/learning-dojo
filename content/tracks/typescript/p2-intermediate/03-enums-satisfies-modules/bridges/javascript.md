---
from: javascript
status: draft
concept: "Constants and modules"
from_way: "Frozen objects (Object.freeze) for constants, ES modules or CommonJS (require, module.exports), and the type field in package.json."
native_way: "as const for typed constants, satisfies to check them, the same ES modules plus import type, and tsc options (nodenext, bundler) that mirror Node's or the bundler's rules."
sensei_tip: "TypeScript does not change JavaScript modules at all; it models them. The new habit is import type, and .ts extensions when Node runs the source."
---

| JavaScript | TypeScript |
| --- | --- |
| `Object.freeze({ Red: "red" })` | `{ Red: "red" } as const` (plus freeze if you need it at runtime) |
| JSDoc `@type {Record<Status, Status[]>}` | `satisfies Record<Status, readonly Status[]>` |
| `import { x } from "./x.js"` | `import { x } from "./x.ts"` (Node runs .ts) |
| `require("./x.cjs")` | `import x from "./x.cjs"` (interop) |
