---
from: javascript
status: draft
concept: "Strong dynamic typing and truthiness"
from_way: "Loose coercion (\"1\" + 1 is \"11\", == converts types), number as a 64-bit float for everything, template literals and truthiness with NaN and empty strings."
native_way: "Dynamic but strong: \"1\" + 1 raises TypeError, int is unbounded and separate from float, f-strings, and empty containers are falsy."
sensei_tip: "In Python an empty list and an empty dict are falsy, unlike JavaScript where [] and {} are truthy. And there is only one equality, ==, which never coerces types."
---

| JavaScript | Python |
| --- | --- |
| `"1" + 1 === "11"` | `"1" + 1` raises `TypeError` |
| `===` | `==` (no coercion), `is` for identity |
| `` `${name} owes ${x.toFixed(2)}` `` | `f"{name} owes {x:.2f}"` |
| `[]` and `{}` are truthy | `[]` and `{}` are falsy |
| `null` / `undefined` | `None` |
| `for (const [i, x] of xs.entries())` | `for i, x in enumerate(xs):` |
| `BigInt` | `int` (always unbounded) |
| `switch (x) { case 1: ... }` | `match x: case 1: ...` |
