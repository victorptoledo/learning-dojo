---
from: javascript
status: draft
concept: "String matching"
from_way: "`text.indexOf(p, from)` or `text.includes(p)` in a loop, or `text.split(p).length - 1` to count."
native_way: "Engines optimise `indexOf` well for ordinary text, but nothing in the language guarantees a linear worst case. Write KMP over the string's char codes (`charCodeAt`) for repetitive inputs and overlapping counts."
sensei_tip: "A rolling hash in JavaScript needs care: numbers are doubles, exact only up to 2^53. Use a modulus below 2^26 so products stay exact, or use `BigInt` (correct but slower)."
---

| JavaScript | Java |
| --- | --- |
| `text.indexOf(p, from)` | `text.indexOf(p, from)` |
| `text.split(p).length - 1` (non-overlapping) | a counting loop |
| `charCodeAt(i)` | `charAt(i)` |
| `BigInt` for a 64-bit hash | `long` with `Math.multiplyHigh` |
| `[...text]` for code points | `text.codePoints()` |
