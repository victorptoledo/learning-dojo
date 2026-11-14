---
from: java
status: draft
concept: "The everyday syntax after Java 8"
from_way: "Repeated generic types, string concatenation over many lines, switch statements with break, and instanceof followed by a cast."
native_way: "`var` for obvious local types, text blocks for multi-line strings, switch expressions with `->` and `yield`, and `instanceof Type name`."
sensei_tip: "If a switch statement only assigns one variable in every case, turn it into a switch expression: the compiler then checks that no case is missing."
---

| Java 8 | Java 25 |
| --- | --- |
| `Map<String, List<Order>> byClient = new HashMap<>();` | `var byClient = new HashMap<String, List<Order>>();` |
| `"line 1\n" + "line 2\n"` | `"""` text block `"""` |
| `case A: x = 1; break;` | `case A -> 1;` inside `x = switch (...) { ... };` |
| `if (o instanceof Foo) { Foo f = (Foo) o; ... }` | `if (o instanceof Foo f) { ... }` |
| `catch (IOException ignored)` | `catch (IOException _)` |
