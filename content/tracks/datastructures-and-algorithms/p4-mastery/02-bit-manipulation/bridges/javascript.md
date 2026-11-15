---
from: javascript
status: draft
concept: "Bit manipulation"
from_way: "Using bitwise operators on large numbers and getting strange negative results."
native_way: "JavaScript numbers are doubles, but every bitwise operator first converts its operands to 32-bit signed integers, so masks beyond bit 31 are lost. `>>>` exists and yields an unsigned 32-bit result. For 64-bit masks use BigInt (`1n << 40n`)."
sensei_tip: "`x >>> 0` is the idiom to read a 32-bit value as unsigned, and `Math.clz32(x)` counts leading zeros. There is no built-in popcount for numbers; BigInt-based or table-based bit counts are common."
---

| JavaScript | Java |
| --- | --- |
| bitwise ops on 32-bit ints | `int` operations |
| `x >>> 0` | `Integer.toUnsignedLong(x)` |
| `Math.clz32(x)` | `Integer.numberOfLeadingZeros(x)` |
| `1n << 40n` (BigInt) | `1L << 40` |
| `Uint32Array` as a bitset | `BitSet` / `long[]` |
