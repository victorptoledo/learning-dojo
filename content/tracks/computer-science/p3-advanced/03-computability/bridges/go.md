---
from: go
status: draft
concept: "Undecidability and Go tooling"
from_way: "Expecting go vet to find every bug."
native_way: "go vet and staticcheck run approximate analyses that avoid false positives; the race detector observes actual executions; the compiler's escape analysis is conservative."
sensei_tip: "`go build -gcflags=-m` shows escape analysis decisions: a conservative answer (\"escapes to heap\") where the exact one is undecidable."
---

| In Go | Underneath |
| --- | --- |
| `go vet` | decidable and approximate checks |
| `-race` | finds races in runs that happen, not all runs |
| `-gcflags=-m` | conservative escape analysis |
| `context.WithTimeout` | the practical halting answer |
| `unreachable code` vet check | syntactic, not semantic |
