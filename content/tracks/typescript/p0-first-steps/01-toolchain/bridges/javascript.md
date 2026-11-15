---
from: javascript
status: draft
concept: "JavaScript with a checker in front"
from_way: "Plain JavaScript runs as written; mistakes such as a typo in a property name show up at runtime as undefined or TypeError."
native_way: "The same JavaScript plus annotations; tsc reports the mistakes before running, then the annotations are erased."
sensei_tip: "Rename a .js file to .ts and most of it already type-checks. Start there, then add types where tsc infers any or complains."
---

| JavaScript | TypeScript |
| --- | --- |
| `node app.js` | `node app.ts` (Node strips the types) |
| JSDoc `/** @param {string} name */` | `function f(name: string)` |
| `// @ts-check` in a .js file | `strict` in tsconfig.json |
| Errors at runtime | Most errors at check time |
