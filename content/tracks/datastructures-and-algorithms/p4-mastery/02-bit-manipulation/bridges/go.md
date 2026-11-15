---
from: go
status: draft
concept: "Bit manipulation"
from_way: "Looking for Java's >>> or a NOT operator ~."
native_way: "Go has unsigned types (uint8 ... uint64), so >> on them shifts in zeros; unary ^x is bitwise NOT and &^ is AND NOT (bit clear). The math/bits package offers OnesCount, TrailingZeros, LeadingZeros, RotateLeft and Len."
sensei_tip: "`x &^ (1 << i)` clears bit i in one operator, and iota-based constants (`const ( Read = 1 << iota; Write; Exec )`) are Go's idiom for flag sets."
---

| Go | Java |
| --- | --- |
| `^x` (unary) | `~x` |
| `x &^ y` | `x & ~y` |
| `uint64(x) >> k` | `x >>> k` |
| `bits.OnesCount64(x)` | `Long.bitCount(x)` |
| `bits.TrailingZeros64(x)` | `Long.numberOfTrailingZeros(x)` |
| `1 << iota` constants | `EnumSet` or `int` flags |
