---
from: go
status: draft
concept: "Boolean logic and bit operators in Go"
from_way: "Looking for a ~ operator in Go."
native_way: "Go uses ^x for bitwise NOT, &^ for AND NOT (bit clear), and unsigned types for logical shifts; math/bits exposes hardware instructions."
sensei_tip: "`bits.OnesCount`, `bits.LeadingZeros` and `bits.Add64` (with carry) compile to single instructions: the last one is literally a full adder chain."
---

| In Go | Underneath |
| --- | --- |
| `a & b`, `a \| b`, `a ^ b` | gates |
| `^a` | NOT |
| `a &^ b` | clear the bits of b |
| `bits.Add64(x, y, carry)` | an adder with carry in and out |
| `x >> 1` on uint | logical shift |
