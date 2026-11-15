---
from: python
status: draft
concept: "Classes and dataclasses"
from_way: "Classes with __init__ and self, dataclasses for records, everything passed by reference to an object, inheritance and super(), and __str__ for printing."
native_way: "Structs for records, NewX functions, methods with an explicit receiver chosen as value or pointer, embedding instead of inheritance, and String() for printing."
sensei_tip: "In Python every variable is a reference; in Go a struct variable is the data itself. Ask \"do I want a copy or a share?\" each time you pass one."
---

| Python | Go |
| --- | --- |
| `@dataclass class Point: x: int; y: int` | `type Point struct{ X, Y int }` |
| `Point(1, 2)` | `Point{X: 1, Y: 2}` |
| `def move(self, dx): self.x += dx` | `func (p *Point) Move(dx int) { p.X += dx }` |
| `__init__` with validation | `func NewPoint(x, y int) (*Point, error)` |
| `class Dog(Animal)` | `type Dog struct{ Animal }` (no subtype relation) |
| `__str__` | `func (p Point) String() string` |
| `_private` attribute | lower case field (enforced per package) |
| `copy.copy(obj)` | `b := a` copies a struct |
| `frozen=True` | unexported fields plus methods that return new values |
