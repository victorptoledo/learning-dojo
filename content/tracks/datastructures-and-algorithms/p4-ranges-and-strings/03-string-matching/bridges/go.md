---
from: go
status: draft
concept: "String matching"
from_way: "`strings.Index(s[from:], p)` in a loop, or `strings.Count(s, p)`."
native_way: "`strings.Index` uses assembly for short patterns and Rabin-Karp for long ones, so it is fast in practice. `strings.Count` counts non-overlapping matches, so overlapping counts need your own KMP."
sensei_tip: "Go strings are bytes, not runes: `s[i]` is a byte, and matching on bytes of valid UTF-8 never matches half of a character. `math/bits.Mul64` gives the 128-bit product a 2^61 - 1 rolling hash needs."
---

| Go | Java |
| --- | --- |
| `strings.Index(s, p)` | `s.indexOf(p)` |
| `strings.Count(s, p)` (non-overlapping) | a counting loop |
| `bits.Mul64(a, b)` | `Math.multiplyHigh(a, b)` and `a * b` |
| `s[i]` is a byte | `s.charAt(i)` is a UTF-16 unit |
| `map[uint64]struct{}` | `HashSet<Long>` |
