---
from: java
status: draft
concept: "Closed hierarchies"
from_way: "An abstract base class plus `instanceof` chains or the visitor pattern; nothing tells you when a new subtype is not handled."
native_way: "A `sealed interface` with record subtypes, taken apart by an exhaustive `switch` with record patterns and `when` guards."
sensei_tip: "If you have written a visitor only to get compile-time completeness, a sealed interface and a switch give you the same guarantee in a fraction of the code."
---

| Java 8 | Java 21+ |
| --- | --- |
| `abstract class Shape` with any subclass | `sealed interface Shape permits Circle, Square` |
| `if (s instanceof Circle) { Circle c = (Circle) s; ... } else if ...` | `switch (s) { case Circle c -> ...; case Square q -> ...; }` |
| `Visitor<R>` with `visitCircle`, `visitSquare` | One switch expression per operation |
| `default: throw new IllegalStateException()` | No default: the compiler checks completeness |
| Getters to read the parts | `case Circle(double r) ->` |
