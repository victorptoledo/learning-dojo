---
from: javascript
status: draft
concept: "Describing the objects you already pass around"
from_way: "Objects are created and passed freely; nothing checks that a function receives an object with the properties it reads."
native_way: "The same objects, with a type that names the required properties; any object with them fits, and typos in literals are caught."
sensei_tip: "You do not need classes to use TypeScript well. Describe the shapes your functions read, and keep writing plain objects."
---

| JavaScript | TypeScript |
| --- | --- |
| `function hello(who) { return who.name }` | `function hello(who: { name: string })` |
| `user.nmae` → `undefined` | error TS2551: Property 'nmae' does not exist. Did you mean 'name'? |
| `Object.freeze(arr)` | `readonly string[]` (compile time), freeze for runtime |
| `arr[10]` → `undefined` | `T \| undefined` with `noUncheckedIndexedAccess` |
