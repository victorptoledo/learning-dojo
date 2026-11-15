---
from: javascript
status: draft
concept: "The same test runners, plus type tests and typed lint rules"
from_way: "Jest, Mocha or Vitest for tests and ESLint for patterns; no type tests, and lint rules cannot know what a value's type is."
native_way: "The same runners on .ts files, type-level tests for generic code, and typescript-eslint rules that use type information (floating promises, unsafe any)."
sensei_tip: "Keep your test habits. Add two things: run tsc over the tests, and enable typescript-eslint's type-checked configuration, whose rules plain ESLint could never implement."
---

| JavaScript | TypeScript |
| --- | --- |
| `jest` / `vitest` on `.js` | The same on `.ts` (or `node --test`) |
| `eslint:recommended` | + `tseslint.configs.strictTypeChecked` |
| Nothing | `@ts-expect-error` and `expectTypeOf` type tests |
| `.eslintrc` | `eslint.config.js` (flat config) |
