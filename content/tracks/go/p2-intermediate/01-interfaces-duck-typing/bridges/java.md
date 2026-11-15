---
from: java
status: draft
concept: "Interfaces and polymorphism"
from_way: "Interfaces declared up front and implemented explicitly with implements, large service interfaces with one implementation, abstract classes, instanceof and casts, and Object as the universal type."
native_way: "Interfaces satisfied implicitly, kept to one or two methods and declared by the code that uses them, embedding for composition, type switches, and any for the rare boundary."
sensei_tip: "Stop writing the interface before the implementation. Write the concrete type first; extract a one-method interface on the consumer side the day a second implementation or a test fake needs it."
---

| Java | Go |
| --- | --- |
| `class Rect implements Shape` | `Rect` just has `Area()` and `Perimeter()` |
| `interface UserService` with 15 methods | `type userGetter interface{ Get(id string) (User, error) }` at the consumer |
| `abstract class` with shared code | an embedded struct with helper methods |
| `InputStream` / `OutputStream` | `io.Reader` / `io.Writer` |
| `toString()` | `fmt.Stringer` |
| `if (o instanceof Rect r)` | `if r, ok := s.(Rect); ok` |
| `switch (o) { case Rect r -> ... }` | `switch x := s.(type) { case Rect: ... }` |
| `Object` | `any` |
| `@FunctionalInterface` | a function type: `type HandlerFunc func(...)` |
