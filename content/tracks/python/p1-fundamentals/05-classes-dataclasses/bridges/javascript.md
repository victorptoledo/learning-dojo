---
from: javascript
status: draft
concept: "Classes, plain objects and enums"
from_way: "ES classes with constructor and this, #private fields, getters, plain object literals for data, Object.freeze, and string unions or frozen objects as enums."
native_way: "Classes with __init__ and an explicit self, _internal by convention, properties, dataclasses for data types, frozen dataclasses for values and Enum classes."
sensei_tip: "self is always explicit in Python and never rebinds like this does in callbacks: a bound method such as account.deposit keeps its object when you pass it around."
---

| JavaScript / TypeScript | Python |
| --- | --- |
| `constructor(owner) { this.owner = owner }` | `def __init__(self, owner): self.owner = owner` |
| `#balance` | `_balance` (convention) |
| `get balance() { ... }` | `@property def balance(self)` |
| `static fromCsv(line)` | `@classmethod def from_csv(cls, line)` |
| `{ x: 1, y: 2 }` object for data | `@dataclass class Point` |
| `Object.freeze(obj)` | `@dataclass(frozen=True)` |
| `type Status = "open" \| "closed"` | `class Status(StrEnum)` |
| `{ ...p, x: 5 }` | `replace(p, x=5)` |
