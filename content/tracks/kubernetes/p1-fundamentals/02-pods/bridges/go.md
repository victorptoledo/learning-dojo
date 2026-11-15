---
from: go
status: draft
concept: "A Pod is a co-located group, scaled as one"
from_way: "One process per host, with helper daemons installed next to it and sharing the file system."
native_way: "The Go binary is one container; helpers are sidecars and one-off steps are init containers, all sharing the Pod's network and volumes."
sensei_tip: "Use signal.NotifyContext(ctx, syscall.SIGTERM) and http.Server.Shutdown(ctx) so a Go service drains within the grace period."
---

| Go world | Kubernetes |
| --- | --- |
| `signal.NotifyContext` | reacting to SIGTERM |
| a helper goroutine | a sidecar container (when it must be a separate image) |
| a pre-start step in `main` | an init container |
| build tags for variants | labels for selection |
