---
from: csharp
status: draft
concept: "Hard problems from C#"
from_way: "Writing a custom backtracking solver for a timetable."
native_way: ".NET can call Google OR-Tools (official package), Microsoft's Z3 bindings and commercial MIP solvers; NuGet's dependency resolution itself solves a hard constraint problem."
sensei_tip: "Use the Google.OrTools package's CP-SAT for scheduling, and set `MaxTimeInSeconds` so the solver returns a good answer in bounded time."
---

| In C# | Underneath |
| --- | --- |
| `Google.OrTools` CP-SAT | constraint solving |
| `Microsoft.Z3` | SMT |
| NuGet / package resolution | a satisfiability problem in practice |
| `Parallel.ForEach` over 2ⁿ cases | still exponential |
| simulated annealing | a heuristic with no guarantee |
