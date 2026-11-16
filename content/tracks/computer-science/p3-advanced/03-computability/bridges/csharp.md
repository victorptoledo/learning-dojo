---
from: csharp
status: draft
concept: "Undecidability and C# tooling"
from_way: "Expecting nullable reference types to be watertight."
native_way: "C#'s nullable analysis is a flow analysis with known holes (arrays, the ! operator, unannotated libraries); the definite-assignment rules are conservative, like Java's."
sensei_tip: "Treat nullable warnings as a helpful approximation; `[NotNullWhen]` and similar attributes give the analysis facts it cannot infer."
---

| In C# | Underneath |
| --- | --- |
| nullable reference types | approximate flow analysis |
| `!` (null-forgiving) | tells the analyser to trust you |
| "use of unassigned local variable" | conservative definite assignment |
| Roslyn analysers | decidable, syntactic or approximate rules |
| `CancellationToken` | stop work that may not halt |
