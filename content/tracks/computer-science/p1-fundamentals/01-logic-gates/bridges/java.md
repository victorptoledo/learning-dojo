---
from: java
status: draft
concept: "Boolean logic and bit operators in Java"
from_way: "Seeing &&, || and & as interchangeable."
native_way: "&& and || short-circuit on booleans; &, |, ^ and ~ apply the gate to every bit of an int at once, exactly like a row of gates."
sensei_tip: "`Integer.bitCount`, `highestOneBit` and `numberOfTrailingZeros` compile to single instructions on modern CPUs: hardware circuits exposed as methods."
---

| In Java | Underneath |
| --- | --- |
| `a & b`, `a | b`, `a ^ b` | AND, OR, XOR gates on 32 bits in parallel |
| `~a` | NOT on every bit |
| `!(a && b) == (!a \|\| !b)` | De Morgan |
| `a + b` | a hardware adder |
| `x / 8` vs `x >> 3` | a slow divider vs wires (with a sign fix-up) |
