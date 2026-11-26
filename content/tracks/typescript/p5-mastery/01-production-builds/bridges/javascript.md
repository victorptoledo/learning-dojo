---
from: javascript
status: draft
concept: "The build you may not need any more"
from_way: "Node runs .js directly; bundlers serve front ends; there is no type check at all."
native_way: "Node runs .ts directly by stripping types, bundlers strip types too, and tsc adds a type-check step that plain JavaScript never had."
sensei_tip: "If your JavaScript service had no build step, it does not need one in TypeScript either on Node 26. It needs one more CI step: tsc."
---

| JavaScript | TypeScript |
| --- | --- |
| `node src/main.js` | `node src/main.ts` |
| `esbuild src/main.js --bundle` | The same with `.ts`, plus `tsc --noEmit` |
| Babel | Type stripping (Node, esbuild, SWC) |
| `npm test` in CI | `tsc && eslint . && npm test` |
