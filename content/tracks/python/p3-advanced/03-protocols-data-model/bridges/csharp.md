---
from: csharp
status: draft
concept: "Interfaces, operator overloading and properties"
from_way: "Explicit interface implementation, abstract classes, operator overloads declared as static methods, Equals and GetHashCode, IComparable, indexers, and properties with get and set."
native_way: "Protocols matched by structure, ABCs, operators as special methods with NotImplemented, __eq__ and __hash__, __lt__, __getitem__ as the indexer, and descriptors behind properties."
sensei_tip: "C# operator overloading maps directly to special methods, with one addition: return NotImplemented and Python tries the other operand's reflected method, which is how 0 + money in sum() can work."
---

| C# | Python |
| --- | --- |
| `interface IRepository<T>` | `class Repository[T](Protocol)` |
| `public static Money operator +(Money a, Money b)` | `def __add__(self, other)` |
| `Equals` / `GetHashCode` | `__eq__` / `__hash__` |
| `IComparable<T>` | `__lt__` + `total_ordering` |
| `public T this[int i]` (indexer) | `__getitem__` / `__setitem__` |
| `IFormattable.ToString(format, provider)` | `__format__` |
| `public int Qty { get; set; }` with validation | `property`, or a descriptor reused across fields |
| source generators / reflection | `__init_subclass__`, class decorators |
