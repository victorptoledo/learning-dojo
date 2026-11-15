---
from: go
status: draft
concept: "SOLID principles"
from_way: "A package with a struct that holds a `*sql.DB` and calls `time.Now()` everywhere."
native_way: "Packages split by responsibility, interfaces declared by the consumer with one or two methods, dependencies passed to a constructor function and wired in `main`."
sensei_tip: "Go's proverb 'the bigger the interface, the weaker the abstraction' is interface segregation. `io.Reader` has one method and works with files, sockets, buffers and gzip streams."
---

| Go | SOLID idea |
| --- | --- |
| one package per concern | single responsibility |
| accepting an interface, returning a struct | open/closed, dependency inversion |
| struct embedding that hides a method | a Liskov warning sign |
| `io.Reader`, `io.Writer` | interface segregation |
| `NewService(store Store, now func() time.Time)` | dependency inversion |
