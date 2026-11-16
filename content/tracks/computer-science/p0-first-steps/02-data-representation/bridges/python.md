---
from: python
status: draft
concept: "Integers, floats and text in Python"
from_way: "Expecting Python ints to overflow like other languages, or str to be bytes."
native_way: "Python int is arbitrary precision, so it never overflows; float is binary64; str is a sequence of code points and bytes is separate; you must encode and decode explicitly."
sensei_tip: "`struct.pack('>f', x)` and `float.hex()` show the bits of a float; `decimal.Decimal` and `fractions.Fraction` avoid binary rounding."
---

| In Python | Underneath |
| --- | --- |
| `2**100` | exact: ints grow as needed |
| `0.1 + 0.2` | 0.30000000000000004 |
| `(-5) & 0xFF` | 251: the two's complement byte |
| `'é'.encode()` | b'\xc3\xa9' in UTF-8 |
| `len('😀')` | 1: one code point |
