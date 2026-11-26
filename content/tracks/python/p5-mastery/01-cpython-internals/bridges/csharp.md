---
from: csharp
status: draft
concept: "The CLR versus CPython"
from_way: "C# compiled to IL, a tiered JIT and ReadyToRun or Native AOT, a generational tracing GC, IDisposable for deterministic cleanup, and ILSpy to read IL."
native_way: "Python compiled to bytecode on import, a specialising interpreter with an experimental JIT, reference counting that frees objects at once plus a cycle collector, and dis to read bytecode."
sensei_tip: "Reference counting gives Python deterministic destruction most of the time, but cycles and other implementations (PyPy) break that; keep using with blocks as you would using in C#."
---

| .NET | CPython |
| --- | --- |
| IL in assemblies | bytecode in `.pyc` files |
| ILSpy / `ildasm` | `dis` |
| tiered JIT, Native AOT | specialising interpreter, experimental JIT |
| generational tracing GC | refcounting + generational cycle collector |
| `GC.Collect()` | `gc.collect()` |
| `string.Intern` | `sys.intern` |
| AppDomains (old) / AssemblyLoadContext | subinterpreters |
