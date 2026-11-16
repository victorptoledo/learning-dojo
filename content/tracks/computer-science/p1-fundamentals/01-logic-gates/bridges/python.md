---
from: python
status: draft
concept: "Boolean logic and bit operators in Python"
from_way: "Using and/or for bit work, or ~ expecting an unsigned result."
native_way: "and/or/not work on truthiness; &, |, ^, ~ work on the bits of ints, which behave like infinite two's complement, so ~5 is −6."
sensei_tip: "Mask to a width to simulate hardware: `(~x) & 0xFF` is the 8-bit NOT; `int.bit_count()` counts set bits."
---

| In Python | Underneath |
| --- | --- |
| `a & b`, `a \| b`, `a ^ b` | bitwise gates |
| `~5` | −6: infinite two's complement |
| `(a ^ b) & 0xFF` | an 8-bit XOR |
| `x.bit_count()` | population count |
| `not (a and b) == (not a or not b)` | De Morgan |
