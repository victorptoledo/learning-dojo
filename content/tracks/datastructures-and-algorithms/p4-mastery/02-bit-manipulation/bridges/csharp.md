---
from: csharp
status: draft
concept: "Bit manipulation"
from_way: "Writing flag enums without the [Flags] attribute and checking them with HasFlag in hot loops."
native_way: "C# has unsigned types (uint, ulong), so >> on them shifts in zeros and there is a >>> operator since C# 11 for signed types. [Flags] enums combine values with |, and System.Numerics.BitOperations offers PopCount, TrailingZeroCount and LeadingZeroCount."
sensei_tip: "`(flags & Feature.Beta) != 0` is faster than `flags.HasFlag(Feature.Beta)` in older runtimes; `BitArray` is the BitSet of .NET, and `BitOperations.IsPow2(x)` replaces the n & (n - 1) trick."
---

| C# | Java |
| --- | --- |
| `uint`, `ulong` | (none: use `>>>` and `Integer.toUnsignedLong`) |
| `[Flags] enum` | `EnumSet` |
| `BitOperations.PopCount(x)` | `Integer.bitCount(x)` |
| `BitOperations.TrailingZeroCount(x)` | `Integer.numberOfTrailingZeros(x)` |
| `BitArray` | `BitSet` |
| `checked` arithmetic | `Math.addExact` |
