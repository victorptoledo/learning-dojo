---
from: go
status: draft
concept: "Value classes without boilerplate"
from_way: "Structs compared with == when all fields are comparable, with constructors as functions."
native_way: "Records: value types with generated equality, accessors and toString."
sensei_tip: "A Go struct with a NewPoint function that validates is a record with a compact constructor; the difference is that a record cannot be built around the validation."
---

| Go | Java 25 |
| --- | --- |
| type Point struct { X, Y int } | record Point(int x, int y) {} |
| func NewPoint(...) (Point, error) | compact constructor that throws |
| p1 == p2 | p1.equals(p2) |
| fmt.Printf("%+v", p) | p.toString() |
