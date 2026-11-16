---
from: javascript
status: draft
concept: "Boolean logic and bit operators in JavaScript"
from_way: "Applying bitwise operators to large numbers and getting surprises."
native_way: "&, |, ^, ~ and shifts convert operands to 32-bit two's complement integers, apply the gates, and convert back; bigint has its own unbounded versions."
sensei_tip: "Keep bit tricks within 32 bits, or switch to bigint (`1n << 40n`); `x | 0` truncates to a 32-bit integer."
---

| In JavaScript / TypeScript | Underneath |
| --- | --- |
| `a & b` | 32-bit AND |
| `~0` | −1 |
| `1 << 31` | −2147483648 |
| `5 >>> 1` | logical shift right |
| `(1n << 40n) & x` | bigint bit test |
