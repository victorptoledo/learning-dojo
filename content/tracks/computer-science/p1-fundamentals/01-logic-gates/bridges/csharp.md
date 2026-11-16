---
from: csharp
status: draft
concept: "Boolean logic and bit operators in C#"
from_way: "Treating bool and bit operations as unrelated."
native_way: "&& and || short-circuit; & and | on bools evaluate both sides; on integers they are the gates; System.Numerics.BitOperations exposes popcount and leading-zero instructions."
sensei_tip: "`BitOperations.PopCount`, `LeadingZeroCount` and `RotateLeft` map to single instructions; they are the gate-level circuits of the CPU."
---

| In C# | Underneath |
| --- | --- |
| `a & b`, `a \| b`, `a ^ b` | bitwise gates |
| `~a` | NOT |
| `uint` shifts | logical; `int` `>>` is arithmetic |
| `BitOperations.PopCount(x)` | one instruction |
| `a >>> b` (C# 11) | unsigned right shift on signed types |
