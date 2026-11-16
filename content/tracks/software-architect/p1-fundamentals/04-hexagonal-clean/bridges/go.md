---
from: go
status: draft
concept: "Hexagonal and clean architecture"
from_way: "Handlers that use `*sql.DB` and vendor clients directly, with business rules in the handler."
native_way: "A domain package with structs, functions and the small interfaces it consumes, adapter packages that implement them, and `cmd/<app>/main.go` as the composition root."
sensei_tip: "'Accept interfaces, return structs' is the port idea: the domain declares the interface it consumes, so the adapter package imports the domain and never the other way round."
---

| Go | Hexagonal idea |
| --- | --- |
| a domain package with no infrastructure imports | the core |
| `type Orders interface` declared by the domain | driven port |
| `net/http` handlers, consumers | driving adapters |
| a `postgres` package implementing `domain.Orders` | driven adapter |
| `cmd/app/main.go` | composition root |
| the import cycle ban, `depguard` | enforcing the direction |
