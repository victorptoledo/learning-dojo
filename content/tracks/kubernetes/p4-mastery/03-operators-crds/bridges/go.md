---
from: go
status: draft
concept: "Your own declarative APIs"
from_way: "Go structs for a config format and a daemon that applies it."
native_way: "kubebuilder generates the CRD from Go types with markers, and controller-runtime runs the informers, caches, work queue and leader election around your Reconcile."
sensei_tip: "Return ctrl.Result{RequeueAfter: 30 * time.Second} while a child is not ready instead of sleeping, and always compare observedGeneration before reporting Ready."
---

| Go world | Kubernetes |
| --- | --- |
| a struct with `json` tags | the CRD types (`spec`, `status`) |
| `// +kubebuilder:validation` markers | the generated OpenAPI schema |
| a daemon's main loop | `Reconcile(ctx, req)` |
| `defer cleanup()` | a finalizer |
