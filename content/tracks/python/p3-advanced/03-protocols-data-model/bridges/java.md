---
from: java
status: draft
concept: "Interfaces, operators and object contracts"
from_way: "Interfaces that classes must implement explicitly, abstract classes, equals and hashCode contracts, Comparable, no operator overloading, and annotations plus reflection for frameworks."
native_way: "Protocols that match by structure, ABCs for shared behaviour, special methods for every operator and built-in, NotImplemented for unsupported operands, and descriptors instead of reflection."
sensei_tip: "The equals/hashCode contract is identical in Python (__eq__/__hash__). What is new is operator overloading: give a Money type __add__ and it reads like arithmetic, with no plus() methods."
---

| Java | Python |
| --- | --- |
| `interface Repository<T>` + `implements` | `class Repository[T](Protocol)`, no inheritance |
| `abstract class Exporter` | `class Exporter(ABC)` with `@abstractmethod` |
| `equals` / `hashCode` | `__eq__` / `__hash__` |
| `Comparable<T>.compareTo` | `__lt__` (+ `functools.total_ordering`) |
| `a.plus(b)` | `a + b` via `__add__` |
| `AbstractList` (implement get and size) | `collections.abc.Sequence` (`__getitem__`, `__len__`) |
| `toString()` / `String.format` | `__repr__`, `__str__`, `__format__` |
| reflection and annotation processing | descriptors, `__set_name__`, `__init_subclass__` |
