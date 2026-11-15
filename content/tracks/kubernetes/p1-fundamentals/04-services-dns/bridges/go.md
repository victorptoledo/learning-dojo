---
from: go
status: draft
concept: "Service discovery by name"
from_way: "A list of backend addresses in flags and a round-robin in the client, or a Consul resolver."
native_way: "The Service name resolves to one virtual IP and the kernel spreads connections over the ready Pods."
sensei_tip: "gRPC keeps one long-lived HTTP/2 connection, so a ClusterIP gives it no balancing: use a headless Service with the dns:/// resolver and round_robin, or a mesh."
---

| Go world | Kubernetes |
| --- | --- |
| `-backends=a:8080,b:8080` | a Service selector |
| a client-side round-robin | kube-proxy connection balancing |
| a gRPC resolver | a headless Service + `dns:///` |
| `net.Listen(":8080")` | `containerPort: 8080` |
