---
from: csharp
status: draft
concept: "Reference semantics and pattern matching"
from_way: "Value types (struct, int) copied, reference types shared, ReferenceEquals versus Equals, interpolated strings and switch expressions with patterns."
native_way: "Every value is a reference to an object; immutable types (int, str, tuple) give value-like behaviour; f-strings and match with structural patterns."
sensei_tip: "C# pattern matching and Python's match are close cousins: property patterns become mapping or class patterns, and when guards become if guards."
---

| C# | Python |
| --- | --- |
| `ReferenceEquals(a, b)` / `a.Equals(b)` | `a is b` / `a == b` |
| `decimal` | `decimal.Decimal` |
| `$"{name} owes {x:F2}"` | `f"{name} owes {x:.2f}"` |
| `x switch { Point { X: 0 } => ... }` | `match x: case Point(x=0): ...` |
| `case int n when n > 0` | `case int(n) if n > 0:` |
| `null` | `None` |
| `foreach (var (i, x) in xs.Select((x, i) => (i, x)))` | `for i, x in enumerate(xs):` |
