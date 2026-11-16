---
from: go
status: draft
concept: "Modularity and package dependencies"
from_way: "A `util` or `common` package imported by everything, and packages split by technical kind (`models`, `handlers`, `services`)."
native_way: "Packages named for what they provide, unexported identifiers by default, `internal/` to hide packages from other modules, and small interfaces declared by the consumer."
sensei_tip: "Go forbids import cycles at compile time, so ADP is free. The trap moves elsewhere: a giant `models` package everything imports is the zone of pain Go cannot detect for you."
---

| Go | Modularity idea |
| --- | --- |
| lower-case identifiers | information hiding |
| `internal/` directories | packages hidden from other modules |
| import cycle compile error | the acyclic dependencies principle, enforced |
| interfaces declared by the consumer | the stable side owns the abstraction |
| `go list -deps`, `goda` | the import graph, for metrics |
| a `util` package used everywhere | the zone of pain |
