---
from: python
status: draft
concept: "Value classes without boilerplate"
from_way: "@dataclass(frozen=True) or NamedTuple for immutable value types."
native_way: "record: one line declares the fields, constructor, accessors, equals, hashCode and toString."
sensei_tip: "A record is a frozen dataclass. Validation goes in the compact constructor, like __post_init__, and the accessors are name(), not name."
---

| Python | Java 25 |
| --- | --- |
| @dataclass(frozen=True) class Point | record Point(int x, int y) {} |
| __post_init__ validation | compact constructor |
| p.x | p.x() |
| dataclasses.replace(p, x=1) | new Point(1, p.y()) or a wither method |
| __eq__ / __hash__ | generated equals / hashCode |
