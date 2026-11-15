---
from: javascript
status: draft
concept: "Immutability by default"
from_way: "let and const, where const only stops reassignment and objects stay mutable inside."
native_way: "Immutable bindings by default, and immutability that reaches inside the value."
sensei_tip: "Rust's let is like const, but deeper: a const array in JavaScript can still be pushed to, while a Rust Vec without mut cannot change at all."
---

| JavaScript / TypeScript | Rust |
| --- | --- |
| const x = 5 | let x = 5; |
| let x = 5; x++ | let mut x = 5; x += 1; |
| Object.freeze(obj) | the default |
| const MAX = 10 | const MAX: u32 = 10; |
