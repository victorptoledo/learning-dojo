---
from: csharp
status: draft
concept: "The layers under a C# program"
from_way: "Thinking of .NET as the whole computer."
native_way: "Roslyn compiles C# to CIL in an assembly; the CLR's RyuJIT compiles methods to machine code when first called (tiered), and the program runs as threads of an OS process."
sensei_tip: "BenchmarkDotNet's disassembly diagnoser shows the machine code your C# became, a quick way to see two layers at once."
---

| In C# | Underneath |
| --- | --- |
| `dotnet build` | C# to CIL (an .dll) |
| first call of a method | JIT to machine code |
| `Task.Run` | work queued to thread-pool OS threads |
| `double` | IEEE 754 binary64 |
| `decimal` | a 128-bit decimal type implemented in software |
