---
from: go
status: draft
concept: "Integers, floats and text in Go"
from_way: "Mixing int sizes freely and treating strings as characters."
native_way: "Go has sized types (int8…int64, uint8…uint64) with wrap-around; float32 and float64 are IEEE 754; a string is immutable UTF-8 bytes, and range over it yields runes (code points)."
sensei_tip: "`math.Float64bits(f)` returns the bits; `utf8.RuneCountInString(s)` counts characters, while `len(s)` counts bytes."
---

| In Go | Underneath |
| --- | --- |
| `int32(math.MaxInt32) + 1` | wraps (constants are checked at compile time) |
| `uint8(255) + 1` | 0 |
| `len("é")` | 2 bytes |
| `for i, r := range s` | decodes UTF-8 into runes |
| `math.Float32bits(0.1)` | 0x3DCCCCCD |
