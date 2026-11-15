---
from: csharp
status: draft
concept: "Closed sets of alternatives"
from_way: "Abstract records with derived records and switch expressions with type patterns."
native_way: "sealed interfaces with permits, records and record patterns."
sensei_tip: "C# cannot close a hierarchy, so its switch needs a discard arm. Java's sealed types let the compiler prove the switch complete."
---

| C# | Java 25 |
| --- | --- |
| abstract record Shape | sealed interface Shape |
| record Circle(double R) : Shape | record Circle(double r) implements Shape |
| s switch { Circle c => ... } | switch (s) { case Circle c -> ... } |
| Circle { R: > 10 } | case Circle(var r) when r > 10 |
