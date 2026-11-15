---
from: python
status: draft
concept: "Behaviour as a value"
from_way: "Functions as values, lambda, and generics as hints."
native_way: "Lambdas implementing functional interfaces, method references, and generics checked by the compiler."
sensei_tip: "A Python callable fits any call; a Java lambda must match a functional interface: Function<T,R>, Predicate<T>, Supplier<T>. Pick the standard one before writing your own."
---

| Python | Java 25 |
| --- | --- |
| lambda x: x + 1 | x -> x + 1 |
| def f(fn): ... | void f(Function<T, R> fn) |
| str.upper as a function | String::toUpperCase |
| list[int] | List<Integer> |
| TypeVar bound | <T extends Comparable<T>> |
