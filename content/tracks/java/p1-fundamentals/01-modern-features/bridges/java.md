---
from: java
status: draft
concept: "Java's evolution"
from_way: "Verbose classes with hundreds of lines of getters, setters, `equals()` and `hashCode()`."
native_way: "`record User(String name, int age) {}` defines a complete immutable data type in one line."
sensei_tip: "Combine `sealed interface` with pattern-matching `switch` to get exhaustiveness checks at compile time."
---
