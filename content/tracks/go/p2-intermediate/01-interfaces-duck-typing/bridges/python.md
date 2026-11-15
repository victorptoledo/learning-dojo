---
from: python
status: draft
concept: "Duck typing and protocols"
from_way: "Duck typing at run time, typing.Protocol for static checks, ABCs with abstractmethod, file-like objects, isinstance checks and object as the top type."
native_way: "Interfaces are protocols checked by the compiler: any type with the methods satisfies them. io.Reader and io.Writer play the role of file-like objects, and type switches replace isinstance chains."
sensei_tip: "If you liked typing.Protocol, Go interfaces will feel natural: the same structural idea, but always checked and with no runtime_checkable surprises."
---

| Python | Go |
| --- | --- |
| duck typing at run time | structural typing at compile time |
| `class Shape(Protocol): def area(self) -> float: ...` | `type Shape interface{ Area() float64 }` |
| `class Base(ABC)` with `@abstractmethod` | an interface (plus an embedded helper struct if code is shared) |
| file-like object with `.read()` | `io.Reader` |
| `io.StringIO("a\nb")` | `strings.NewReader("a\nb")` |
| `__str__` | `String() string` (`fmt.Stringer`) |
| `isinstance(x, str)` | `s, ok := x.(string)` |
| `match x: case int(): ...` | `switch v := x.(type) { case int: ... }` |
| `object` / `Any` | `any` |
