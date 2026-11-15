---
from: go
status: draft
concept: "Creational patterns"
from_way: "A `Builder` struct with chained setters copied from Java, and a global var initialised in `init()`."
native_way: "`NewX(...)` constructor functions, functional options (`NewServer(addr, WithTimeout(5*time.Second))`), zero values that are useful, and `sync.Once` when you really need lazy one-time setup."
sensei_tip: "Dave Cheney's functional options are Go's builder: variadic `func(*Server)` values with defaults, readable at the call site and extensible without breaking callers."
---

| Go | Pattern |
| --- | --- |
| `NewClient(opts ...Option)` | builder (functional options) |
| `NewX(...)` functions | factory |
| useful zero values (`var mu sync.Mutex`) | no constructor needed |
| `sync.Once` | lazy singleton |
| copying a struct value (`c2 := c1`) | prototype (shallow) |
