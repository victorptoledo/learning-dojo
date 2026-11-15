---
from: javascript
status: draft
concept: "Structural types, symbols and property descriptors"
from_way: "TypeScript interfaces that match by shape, well-known symbols such as Symbol.iterator and Symbol.toPrimitive, Object.defineProperty with get and set, and no operator overloading."
native_way: "Protocols that match by shape, special methods for every operator and built-in (__iter__, __add__, __eq__), descriptors with __get__ and __set__, and properties."
sensei_tip: "TypeScript interfaces and Python Protocols are the same idea. Python goes further with special methods: a class can define +, ==, <, len(), in and iteration, which JavaScript only partly allows through symbols."
---

| JavaScript / TypeScript | Python |
| --- | --- |
| `interface Repo { get(k: string): T }` | `class Repo(Protocol): def get(self, k: str) -> T: ...` |
| `[Symbol.iterator]()` | `__iter__` |
| `[Symbol.toPrimitive]` / `valueOf` tricks | `__add__`, `__lt__`, `__format__`, ... |
| `Object.defineProperty(o, "x", { get, set })` | a descriptor with `__get__` / `__set__`, or `property` |
| `toString()` | `__str__` / `__repr__` |
| `abstract class` (TypeScript) | `class X(ABC)` with `@abstractmethod` |
| `Map` keys by reference | dict keys by `__hash__` and `__eq__` |
