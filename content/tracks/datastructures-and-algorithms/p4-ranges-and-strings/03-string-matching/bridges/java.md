---
from: java
status: draft
concept: "String matching"
from_way: "`text.indexOf(pattern, from)` in a loop, or `Pattern.compile(Pattern.quote(p)).matcher(text).find()`."
native_way: "`indexOf` is the naive search with fast comparisons; write KMP over `char[]` when inputs can be repetitive or long. `java.util.regex` backtracks, so a literal pattern is no faster through it."
sensei_tip: "`Math.multiplyHigh(a, b)` returns the high 64 bits of a 128-bit product, which is what a rolling hash modulo 2^61 - 1 needs to multiply without overflow."
---

| Need | Java |
| --- | --- |
| one match, ordinary text | `text.indexOf(pattern)` |
| overlapping matches, any text | KMP over `text.toCharArray()` |
| hash product without overflow | `Math.multiplyHigh(a, b)` and `a * b` for the low half |
| set of window hashes | `HashSet<Long>` (or a primitive set for speed) |
| code points instead of chars | `text.codePoints()` |
