---
from: go
status: draft
concept: "Behaviour as a value"
from_way: "Function values and generics with type sets since 1.18."
native_way: "Lambdas for functional interfaces and generics with bounds and wildcards."
sensei_tip: "Go's func(int) int is a function type; Java has no function types, only interfaces with one method, which is why the java.util.function interfaces exist."
---

| Go | Java 25 |
| --- | --- |
| func(x int) int { return x + 1 } | x -> x + 1 |
| func Map[T, R any](...) | <T, R> List<R> map(...) |
| constraints.Ordered | <T extends Comparable<T>> |
| strings.ToUpper as a value | String::toUpperCase |
