---
from: csharp
status: draft
concept: "Everyday syntax: inference, strings and switch"
from_way: "var, raw string literals, switch expressions and is patterns."
native_way: "The same ideas with Java syntax: var, text blocks, switch expressions, instanceof patterns."
sensei_tip: "C# developers feel at home: x switch { 1 => \"a\", _ => \"b\" } is switch (x) { case 1 -> \"a\"; default -> \"b\"; }, and o is string s is o instanceof String s."
---

| C# | Java 25 |
| --- | --- |
| var x = 10; | var x = 10; |
| """raw string""" | """text block""" |
| $"Hi {name}" | "Hi %s".formatted(name) |
| x switch { ... } | switch (x) { case ... -> ... } |
| o is string s | o instanceof String s |
