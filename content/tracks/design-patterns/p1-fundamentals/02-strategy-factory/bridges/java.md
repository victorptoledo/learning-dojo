---
from: java
status: draft
concept: "Strategy and factory"
from_way: "A `calculateShipping()` method with 15 branches like `if (type == EXPRESS) ... else if (type == STANDARD)`."
native_way: "A `@FunctionalInterface` strategy, implemented by lambdas or classes, looked up in a `Map` filled by a factory (or by Spring), so the calling code never changes."
sensei_tip: "In languages with first-class functions, a strategy is often just a closure passed as a parameter. In Java that means a functional interface: `Comparator`, `Function`, or your own."
---

| Java | Pattern |
| --- | --- |
| `list.sort(Comparator.comparing(...))` | strategy |
| `Map<String, ShippingRule>` | registry |
| `List.of`, `Optional.of`, `Integer.valueOf` | static factory method |
| `Collection.iterator()` | factory method |
| enum constants with behaviour | closed set of strategies |
| Spring `Map<String, Bean>` injection | registry built by the container |
