---
from: java
status: draft
concept: "Content-addressed, immutable objects"
from_way: "Mutable objects compared with equals(), and centralised systems that stored incremental deltas per file."
native_way: "Git objects behave like immutable value objects whose identity is the hash of their content: a record with the same fields is the same object."
sensei_tip: "Think of a commit as a Java record (tree, parents, author, message) whose hashCode is cryptographic and is its address. You never mutate it; you build a new one."
---

| Java | Git |
| --- | --- |
| `record Commit(Tree tree, List<Commit> parents, ...)` | a commit object |
| `String` interning: equal strings share one instance | equal content shares one blob |
| `final` references to immutable objects | parents can never change |
| a variable pointing to an object | a branch pointing to a commit |
