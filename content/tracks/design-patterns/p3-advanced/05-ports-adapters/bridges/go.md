---
from: go
status: draft
concept: "Ports and adapters"
from_way: "One `main` package where handlers call the database and vendor clients."
native_way: "An internal domain package that declares small interfaces it needs, adapter packages that implement them, and `main` as the composition root; Go's import cycle ban helps keep the direction."
sensei_tip: "Go's rule 'accept interfaces, return structs' is the port idea: the domain package declares the interface it consumes, and the adapter package never needs to be imported by the domain."
---

| Go | Ports and adapters |
| --- | --- |
| `type Rates interface { Rate(from, to string) (decimal, error) }` in `domain` | driven port |
| `net/http` handlers | driving adapters |
| a `postgres` package implementing `domain.Accounts` | driven adapter |
| `cmd/server/main.go` | composition root |
| `internal/` and no import cycles | enforcing the direction |
