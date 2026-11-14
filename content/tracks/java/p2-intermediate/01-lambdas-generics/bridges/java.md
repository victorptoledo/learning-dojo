---
from: java
status: draft
concept: "Behaviour as a value"
from_way: "Anonymous inner classes for every Comparator and Runnable, raw types and casts in older code, and a custom interface for each callback."
native_way: "Lambdas and method references for the java.util.function interfaces, generics everywhere, and PECS wildcards in reusable signatures."
sensei_tip: "If an anonymous class has one method and no state, it should be a lambda; if the lambda only calls one method, it should be a method reference."
---

| Before | Now |
| --- | --- |
| `new Comparator<User>() { public int compare(User a, User b) { ... } }` | `Comparator.comparing(User::name)` |
| `interface Callback { void done(Result r); }` | `Consumer<Result>` |
| `List list = new ArrayList(); (String) list.get(0)` | `List<String> list = new ArrayList<>();` |
| `copy(List<T> dest, List<T> src)` | `copy(List<? super T> dest, List<? extends T> src)` |
