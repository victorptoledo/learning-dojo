---
from: javascript
status: draft
concept: "Behaviour as a value"
from_way: "Arrow functions anywhere, and TypeScript generics erased at runtime."
native_way: "Lambdas typed by functional interfaces, method references, and generics with wildcards."
sensei_tip: "Java generics are also erased at runtime, like TypeScript's, so you cannot do new T(). The PECS rule (extends to read, super to write) is the part TypeScript has no direct equivalent for."
---

| JavaScript / TypeScript | Java 25 |
| --- | --- |
| x => x + 1 | x -> x + 1 |
| (a, b) => a - b | Comparator<T> (a, b) -> ... |
| s => s.length | String::length |
| Array<T> | List<T> |
| <T extends Shape> | <T extends Shape> |
