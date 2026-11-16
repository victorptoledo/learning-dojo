---
from: go
status: draft
concept: "Software architecture"
from_way: "Architecture means the folder layout copied from a template repository (`cmd`, `internal`, `pkg`)."
native_way: "Architecture as the decisions that are expensive to change (package boundaries, data ownership, sync calls or events, what is deployed as one binary), each driven by quality attributes."
sensei_tip: "Go's single static binary and fast start make many small services cheap to run, which tempts teams to split early. The cost of a split is in the network and the data, not in the binary."
---

| Go world | Architectural question behind it |
| --- | --- |
| one binary or several services | deployment units, availability, team independence |
| packages, `internal/`, no import cycles | module structure and allowed dependencies |
| goroutines and channels inside one process | runtime structure within a deployable |
| NATS, Kafka consumers | synchronous versus event-driven runtime structure |
| `pprof`, `expvar`, OpenTelemetry | performance and observability |
| `go test -bench`, vegeta | checking performance scenarios |
