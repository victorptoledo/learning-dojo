---
from: javascript
status: draft
concept: "Undecidability and JavaScript tooling"
from_way: "Trusting TypeScript's types as proofs."
native_way: "TypeScript's type system is intentionally unsound in places (any, casts, bivariance) and is itself Turing complete: type-level recursion can be cut off with \"type instantiation is excessively deep\"."
sensei_tip: "When tsc reports \"excessively deep and possibly infinite\", it has hit the undecidability wall and given up after a depth limit."
---

| In JavaScript / TypeScript | Underneath |
| --- | --- |
| TypeScript types | a checker with deliberate unsound escapes |
| "Type instantiation is excessively deep" | a depth limit on a Turing-complete type system |
| ESLint rules | syntactic, decidable checks |
| `AbortController` | cancel what may never finish |
| `eval` | defeats static analysis |
