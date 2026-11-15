---
from: java
status: draft
concept: "Classes, records and enums"
from_way: "private fields with getters and setters, constructors, records for values with compact constructors, and enums with fields."
native_way: "Public attributes that can become properties later, @dataclass for data types, frozen dataclasses for values, __post_init__ for validation and Enum or StrEnum."
sensei_tip: "A Java record maps to @dataclass(frozen=True, slots=True), and its compact constructor to __post_init__. Do not write getters: a property can replace an attribute later without changing callers."
---

| Java | Python |
| --- | --- |
| `private int x; public int getX()` | `self.x`, later `@property def x(self)` |
| `record Point(int x, int y)` | `@dataclass(frozen=True) class Point: x: int; y: int` |
| compact constructor | `__post_init__` |
| `withX(...)` | `dataclasses.replace(p, x=...)` |
| `static Account fromCsv(...)` | `@classmethod def from_csv(cls, ...)` |
| `super(owner)` | `super().__init__(owner)` |
| `enum Status { OPEN, CLOSED }` | `class Status(StrEnum): OPEN = auto()` |
| `toString()` | `__repr__` |
