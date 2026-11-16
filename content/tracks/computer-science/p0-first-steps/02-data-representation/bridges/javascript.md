---
from: javascript
status: draft
concept: "Numbers and strings in JavaScript"
from_way: "Believing JavaScript has integers."
native_way: "number is binary64, so integers are exact only up to 2⁵³; bitwise operators convert to 32-bit two's complement; bigint is arbitrary precision; strings are UTF-16."
sensei_tip: "Use Number.isSafeInteger for ids from other systems, bigint for 64-bit values, and TextEncoder/TextDecoder for UTF-8 bytes."
---

| In JavaScript / TypeScript | Underneath |
| --- | --- |
| `2**53 + 1 === 2**53` | true: precision runs out |
| `(2**31) | 0` | −2147483648: 32-bit two's complement |
| `x >>> 0` | the same bits as an unsigned 32-bit number |
| `new Float32Array([0.1])[0]` | the float closest to 0.1 |
| `'😀'.length` | 2 UTF-16 units |
