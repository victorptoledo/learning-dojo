---
from: csharp
status: draft
concept: "Integers, floats and text in C#"
from_way: "Assuming overflow is always caught."
native_way: "int arithmetic wraps by default (unchecked) but can be checked; uint, ulong and byte are unsigned; double is binary64; decimal is base-10; string is UTF-16."
sensei_tip: "Wrap risky arithmetic in `checked { }` (or enable CheckForOverflowUnderflow), use decimal for money, and `BitConverter.SingleToInt32Bits` to see float bits."
---

| In C# | Underneath |
| --- | --- |
| `int.MaxValue + 1` | wraps unless in a checked context |
| `checked(a + b)` | throws OverflowException |
| `(byte)300` | 44: unsigned byte, wrapped |
| `0.1m + 0.2m == 0.3m` | true: decimal is base 10 |
| `Encoding.UTF8.GetBytes(s)` | the UTF-8 bytes |
