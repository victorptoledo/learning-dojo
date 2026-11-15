---
from: csharp
status: draft
concept: "Value classes without boilerplate"
from_way: "record and record struct with positional parameters and with expressions."
native_way: "record classes: positional, immutable, value-equal."
sensei_tip: "Java records are C# positional records without the with expression: copy with new Point(x, p.y()) or a small wither method. Validation goes in the compact constructor."
---

| C# | Java 25 |
| --- | --- |
| record Point(int X, int Y); | record Point(int x, int y) {} |
| p with { X = 1 } | new Point(1, p.y()) |
| p.X | p.x() |
| init accessors | final components |
| record struct | a record (value types are still in preview) |
