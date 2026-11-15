---
from: csharp
status: draft
concept: "Behaviour as a value"
from_way: "Lambdas with Func and Action delegates, and reified generics with in/out variance."
native_way: "Lambdas for functional interfaces, method references, erased generics with use-site wildcards."
sensei_tip: "Func<T, R> is Function<T, R>, Action<T> is Consumer<T>, Predicate<T> is Predicate<T>. C#'s declaration-site out T is Java's ? extends T at each use."
---

| C# | Java 25 |
| --- | --- |
| Func<int, int> f = x => x + 1 | Function<Integer, Integer> f = x -> x + 1 |
| Action<T> | Consumer<T> |
| IEnumerable<out T> | List<? extends T> |
| typeof(T) at runtime | not available (erasure) |
| method group | method reference Type::method |
