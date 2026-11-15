---
from: go
status: draft
concept: "An edge proxy configured by objects"
from_way: "A Go reverse proxy with httputil.ReverseProxy and a hand-written route table."
native_way: "Envoy, NGINX or another controller is the proxy; Gateway API objects are its route table, reconciled by a controller written much like your own."
sensei_tip: "Most Gateway API controllers are Go programs built on controller-runtime: read the Envoy Gateway or Cilium code to see reconciliation driving a real proxy."
---

| Go world | Kubernetes |
| --- | --- |
| `httputil.ReverseProxy` | the data-plane proxy |
| a route table in a map | HTTPRoute rules |
| `autocert.Manager` | cert-manager |
| a weighted random choice of backends | weighted `backendRefs` |
