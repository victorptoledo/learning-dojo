---
from: python
status: draft
concept: "Everyday syntax: inference, strings and switch"
from_way: "Dynamic types, f-strings, triple-quoted strings and match statements."
native_way: "Static types with var inference, text blocks, switch expressions and pattern matching for instanceof."
sensei_tip: "var in Java looks like Python's untyped names but is fixed at compile time: var x = 10; x = \"ten\"; does not compile."
---

| Python | Java 25 |
| --- | --- |
| x = 10 | var x = 10; |
| '''multi-line''' | """ text block """ |
| f"Hi {name}" | "Hi %s".formatted(name) |
| match code: case 1 \| 2: | switch (code) { case 1, 2 -> ... } |
| isinstance(o, str) | o instanceof String s |
