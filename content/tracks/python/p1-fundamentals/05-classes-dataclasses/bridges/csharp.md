---
from: csharp
status: draft
concept: "Classes, records and properties"
from_way: "Auto-properties with get and set, access modifiers, record types with with-expressions and init-only setters, and enums backed by integers."
native_way: "Plain attributes and @property, underscore conventions, dataclasses (frozen for records) with replace(), and Enum or StrEnum."
sensei_tip: "C# records and frozen dataclasses are the same idea: value equality, immutability and copies with changes. `record with { X = 5 }` is `replace(p, x=5)`."
---

| C# | Python |
| --- | --- |
| `public string Owner { get; set; }` | `self.owner` |
| `public int Balance => _balance;` | `@property def balance(self)` |
| `record Point(double X, double Y);` | `@dataclass(frozen=True) class Point` |
| `p with { X = 5 }` | `replace(p, x=5)` |
| `init` accessors | `frozen=True` |
| `: base(owner)` | `super().__init__(owner)` |
| `enum Status { Open, Closed }` | `class Status(Enum)` / `StrEnum` |
| `override ToString()` | `__repr__` / `__str__` |
