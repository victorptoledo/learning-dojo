---
from: javascript
status: draft
concept: "Null safety and errors"
from_way: "null and undefined, optional chaining, and exceptions or rejected promises for errors."
native_way: "Option and Result: absence and failure as values the compiler makes you handle."
sensei_tip: "a?.b ?? c becomes a.map(|a| a.b).unwrap_or(c). A Result is like a promise that can only be resolved or rejected, but synchronous and typed."
---

| JavaScript / TypeScript | Rust |
| --- | --- |
| null / undefined | None |
| throw new Error() | Err(...) |
| try/catch | match or ? |
| a ?? b | a.unwrap_or(b) |
| a?.b | a.map(\|a\| a.b) |
