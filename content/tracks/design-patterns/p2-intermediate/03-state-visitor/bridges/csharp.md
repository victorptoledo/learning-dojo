---
from: csharp
status: draft
concept: "State, visitor, composite and template method"
from_way: "Enum states with switch statements everywhere, and `Accept(IVisitor)` in every node class."
native_way: "Records for states and nodes, switch expressions with type and property patterns, and abstract records as the closed base."
sensei_tip: "A switch expression over a record hierarchy cannot prove it covers every subtype (the compiler only warns that it may not be exhaustive), so keep a throwing discard arm `_ => throw new UnreachableException()` until your C# version offers closed union types."
---

| C# | Pattern |
| --- | --- |
| `abstract record State`, `record Paid(...) : State` | state |
| `shape switch { Circle c => ..., Rect r => ... }` | visitor |
| Roslyn `CSharpSyntaxVisitor` | classic visitor |
| records holding `ImmutableList<Node>` | composite |
| `IEnumerable<T>` with `yield return` | iterator |
