---
from: javascript
status: draft
concept: "Functions that work for any type, now with types"
from_way: "Utility functions such as first, groupBy and pluck work on any data, and callers trust the result."
native_way: "Generic signatures keep the caller's types: pluck(users, \"name\") returns string[], and a wrong key is a compile error."
sensei_tip: "Generics are how TypeScript types the flexible helpers JavaScript is full of. Write the JavaScript first, then ask: which types flow from the inputs to the output?"
---

| JavaScript | TypeScript |
| --- | --- |
| `const first = (xs) => xs[0]` | `const first = <T,>(xs: T[]) => xs[0]` |
| `obj[key]` with any string | `<T, K extends keyof T>(obj: T, key: K): T[K]` |
| `Object.keys(defaults)` | `keyof typeof defaults` (type level) |
| JSDoc `@template T` | `<T>` |
