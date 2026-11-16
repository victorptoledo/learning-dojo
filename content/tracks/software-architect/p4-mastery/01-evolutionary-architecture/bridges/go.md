---
from: go
status: draft
concept: "Evolutionary architecture"
from_way: "A rewrite of an old service in a new repository, with the old one left running unchanged 'until the new one is done'."
native_way: "`depguard` and `go-arch-lint` rules in CI, benchmark thresholds with `benchstat`, `govulncheck`, SLO alerts, interfaces plus flags for branch by abstraction, and a reverse proxy in front of the old service for the strangler fig."
sensei_tip: "`httputil.ReverseProxy` in a few lines is a strangler facade: route migrated paths to the new handler and the rest to the old service, and move one path at a time."
---

| Go | Evolutionary idea |
| --- | --- |
| `depguard`, `go-arch-lint` | structural fitness functions |
| `go test -bench` with `benchstat` in CI | performance fitness function |
| `govulncheck` | security fitness function |
| Prometheus alerts on SLOs | continuous fitness function |
| an interface and a flag | branch by abstraction |
| `httputil.ReverseProxy` | strangler fig facade |
