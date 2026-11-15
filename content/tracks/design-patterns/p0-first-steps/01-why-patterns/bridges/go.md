---
from: go
status: draft
concept: "Design patterns"
from_way: "Translating Java hierarchies into Go: interfaces declared next to their single implementation, getters, constructors for everything."
native_way: "Small interfaces defined by the consumer, struct embedding instead of inheritance, functions as values and explicit wiring in main; there are no classes to build hierarchies from."
sensei_tip: "Go's standard library is a pattern book: io.Reader wrappers (decorator), http.Handler middleware (chain of responsibility), sort.Slice (strategy), sync.Once (singleton)."
---

| Go | Pattern |
| --- | --- |
| `sort.Slice(s, less)` | strategy |
| `bufio.NewReader(r)` | decorator |
| `http.Handler` middleware | chain of responsibility |
| `sync.Once` | lazy singleton |
| consumer-defined interfaces | program to an interface |
| functional options (`WithTimeout(...)`) | builder |
