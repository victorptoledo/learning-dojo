---
from: javascript
status: draft
concept: "Workspaces plus a type-aware build graph"
from_way: "npm, pnpm or Yarn workspaces link packages; Turborepo or Nx run scripts across them."
native_way: "The same workspaces and task runners, plus project references so tsc checks each package once and in dependency order."
sensei_tip: "Keep your workspace setup; add composite tsconfig files and a solution tsconfig, and switch the type-check script to tsc -b."
---

| JavaScript | TypeScript |
| --- | --- |
| `npm run build --workspaces` | `tsc -b` for the TypeScript part |
| `"main": "src/index.js"` for internal packages | `"exports": { ".": "./src/index.ts" }` (bundlers) or built `dist/` |
| Turborepo pipelines | The same, with `tsc -b` as one task |
