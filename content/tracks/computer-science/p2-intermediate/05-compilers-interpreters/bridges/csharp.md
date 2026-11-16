---
from: csharp
status: draft
concept: "C#'s compilers"
from_way: "Treating Roslyn as a black box."
native_way: "Roslyn is a compiler-as-a-service: it exposes syntax trees and semantic models to analysers and source generators, and emits CIL, a stack-machine bytecode that RyuJIT compiles to machine code."
sensei_tip: "Write a Roslyn analyser or source generator once: it is the most practical way to see lexing, parsing, ASTs and semantic analysis in real code."
---

| In C# | Underneath |
| --- | --- |
| Roslyn `SyntaxTree` | the AST |
| `SemanticModel` | names and types resolved |
| CIL `ldloc`, `add`, `stloc` | LOAD, ADD, STORE |
| RyuJIT, ReadyToRun, Native AOT | JIT, precompiled and ahead-of-time code |
| `System.Linq.Expressions` | expression trees compiled at run time |
