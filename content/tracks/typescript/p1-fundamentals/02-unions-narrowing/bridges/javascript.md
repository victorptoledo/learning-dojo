---
from: javascript
status: draft
concept: "The checks you already write, understood by the compiler"
from_way: "typeof, in and instanceof checks guard against wrong values at runtime, but nothing confirms every case is handled."
native_way: "The same checks narrow the static type, and a never check makes a forgotten case a compile error."
sensei_tip: "Keep writing the checks you know; TypeScript reads them. The new habit is the tag field plus assertNever: it turns 'I forgot a case' into a build failure."
---

| JavaScript | TypeScript |
| --- | --- |
| `if (typeof x === "string")` | The same, and `x` is `string` inside |
| `{ type: "click", x, y }` events | Discriminated union on `type` |
| `default: throw new Error("unknown")` | `default: return assertNever(event)` (compile-time) |
| `x ?? fallback`, `a?.b` | The same operators, also narrowing |
