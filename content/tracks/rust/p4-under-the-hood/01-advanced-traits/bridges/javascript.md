---
from: javascript
status: draft
concept: "Iteration protocols and value types"
from_way: "`[Symbol.iterator]()` for `for...of`, no operator overloading, and branded types only in TypeScript."
native_way: "`IntoIterator` for `for`, operator traits, and newtypes with real compile-time checks."
sensei_tip: "A TypeScript brand (`number & { __brand: 'UserId' }`) is a trick; a Rust newtype is the same idea with full compiler support."
---

| JavaScript | Rust |
| --- | --- |
| `[Symbol.iterator]()` | `impl IntoIterator` |
| `{ value, done }` | `Option<Item>` from `next` |
| `a.plus(b)` | `a + b` with `impl Add` |
| branded types in TS | `struct UserId(u64);` |
