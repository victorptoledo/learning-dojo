---
from: csharp
status: draft
concept: "What the CPU does with C# code"
from_way: "Thinking the JIT output does not depend on your code's shape."
native_way: "RyuJIT compiles CIL to machine code with tiered compilation and profile-guided optimisation; System.Runtime.Intrinsics exposes SIMD, and Vector<T> auto-sizes to the CPU."
sensei_tip: "`Vector<T>` and `Vector128/256` let you write SIMD by hand; use them in hot numeric loops after measuring with BenchmarkDotNet."
---

| In C# | Underneath |
| --- | --- |
| a method call | call/ret, often inlined |
| `Vector256.Add` | one AVX instruction on 8 ints |
| `stackalloc` | memory in the current stack frame |
| tiered JIT | quick code first, optimised code for hot methods |
| `StackOverflowException` | cannot be caught: the process ends |
