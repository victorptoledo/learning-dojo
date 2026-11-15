---
from: go
status: draft
concept: "Health endpoints and sized containers"
from_way: "A /healthz handler and a binary that sees every CPU of the machine."
native_way: "Probes hit /livez and /readyz, and requests and limits size the container; the Go runtime must be told about them."
sensei_tip: "Set GOMEMLIMIT a little below the memory limit (with the downward API) so the GC works harder before the kernel kills the process; Go 1.25+ also sets GOMAXPROCS from the CPU limit."
---

| Go world | Kubernetes |
| --- | --- |
| `http.HandleFunc("/healthz", ...)` | liveness probe |
| a readiness flag set after warm-up | readiness probe |
| `GOMAXPROCS` | the CPU limit or request |
| `GOMEMLIMIT` | the memory limit |
