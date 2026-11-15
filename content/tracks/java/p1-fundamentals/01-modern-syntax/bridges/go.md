---
from: go
status: draft
concept: "Everyday syntax: inference, strings and switch"
from_way: ":= inference, raw strings in backticks, and switch without fall-through."
native_way: "var inference, text blocks, switch expressions and type patterns."
sensei_tip: "Go's switch already has no fall-through; Java's arrow switch matches that, and adds switch as an expression that returns a value."
---

| Go | Java 25 |
| --- | --- |
| x := 10 | var x = 10; |
| `raw string` | a text block |
| fmt.Sprintf("Hi %s", name) | "Hi %s".formatted(name) |
| switch x { case 1, 2: } | switch (x) { case 1, 2 -> ... } |
| s, ok := o.(string) | o instanceof String s |
