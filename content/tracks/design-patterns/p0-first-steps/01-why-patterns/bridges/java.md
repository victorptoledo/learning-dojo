---
from: java
status: draft
concept: "Design patterns"
from_way: "The 1994 shapes: an interface plus one class per strategy, abstract visitors, hand-written singletons and iterators."
native_way: "The same problems solved with Java 25 features: lambdas for strategies and commands, records for values, sealed interfaces and switch patterns for closed hierarchies, enums for singletons."
sensei_tip: "Read the JDK as a pattern catalogue: Comparator (strategy), java.io streams (decorator), List.of (static factory), HttpClient.newBuilder (builder), Iterator (iterator), java.lang.reflect.Proxy (proxy)."
---

| Java | Pattern |
| --- | --- |
| `Comparator<T>` and lambdas | strategy |
| `BufferedInputStream(new FileInputStream(f))` | decorator |
| `Collections.unmodifiableList(list)` | proxy (protection) |
| `Iterable` and the enhanced `for` | iterator |
| `sealed interface` + `record` + `switch` | visitor, state, composite |
| `enum` with one constant | singleton |
| Spring `@Component` and constructor injection | dependency injection |
