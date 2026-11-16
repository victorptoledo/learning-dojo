---
from: java
status: draft
concept: "Integers, floats and text in Java"
from_way: "Assuming int arithmetic fails loudly and double is fine for money."
native_way: "int and long are two's complement and wrap silently; byte is signed; double is IEEE 754 binary64; String is UTF-16 code units."
sensei_tip: "Use Math.addExact and friends where overflow is a bug, BigDecimal with an explicit RoundingMode for money, and always pass StandardCharsets.UTF_8 when converting bytes."
---

| In Java | Underneath |
| --- | --- |
| `Integer.MAX_VALUE + 1` | wraps to MIN_VALUE |
| `(byte) 200` | −56: bytes are signed |
| `x >>> 1` vs `x >> 1` | logical vs arithmetic shift |
| `Float.floatToIntBits(f)` | the raw IEEE 754 bits |
| `"😀".length()` | 2: a surrogate pair of UTF-16 units |
