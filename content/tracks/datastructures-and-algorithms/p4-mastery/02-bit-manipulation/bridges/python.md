---
from: python
status: draft
concept: "Bit manipulation"
from_way: "Expecting Python ints to wrap at 32 or 64 bits like Java's."
native_way: "Python ints have arbitrary precision and behave as if they had infinitely many sign bits, so there is no overflow, no >>> and ~x is simply -x - 1. To emulate a 32-bit value, mask with & 0xFFFFFFFF. int.bit_count() (3.10+) and int.bit_length() are the helpers."
sensei_tip: "Because there is no fixed width, `x & -x` and `x & (x - 1)` still work for positive x, but tricks that rely on wrap-around (hashes, two's complement of negatives) need explicit masks such as `& ((1 << 64) - 1)`."
---

| Python | Java |
| --- | --- |
| `x.bit_count()` | `Integer.bitCount(x)` |
| `x.bit_length()` | `32 - Integer.numberOfLeadingZeros(x)` |
| `x & 0xFFFFFFFF` | (an int already wraps) |
| no `>>>` | `>>>` |
| `enum.Flag` | `EnumSet` |
| arbitrary precision | `long` overflow, or `BigInteger` |
