---
from: java
status: draft
concept: "State, visitor, composite and template method"
from_way: "Status strings checked with `equals` everywhere, `accept(Visitor)` in every class, abstract base classes with hook methods."
native_way: "Sealed interfaces of records, exhaustive switches with record patterns, recursive records for composites, and callbacks instead of abstract template classes."
sensei_tip: "Never write `default ->` in a switch over a sealed type you own: it turns the compiler's exhaustiveness check off, and the next added type fails at runtime instead of at compile time."
---

| Java | Pattern |
| --- | --- |
| `sealed interface` + `record` states | state |
| `switch (x) { case Circle(var r) -> ... }` | visitor |
| `record Folder(List<Node> children)` | composite |
| `JdbcTemplate`, `TransactionTemplate` | template method with callbacks |
| `AbstractList` | classic template method |
| `Iterable`, `Spliterator` | iterator |
