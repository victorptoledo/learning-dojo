---
from: javascript
status: draft
concept: "Service discovery by name"
from_way: "A `.env` file with `API_URL=http://10.0.0.12:3000` that changes on every redeploy."
native_way: "A Service name such as http://api.shop is stable; the Pods behind it can change freely."
sensei_tip: "Node's http.Agent with keepAlive reuses connections, which pins traffic to one Pod; for even spread across replicas, set maxSockets or use a service mesh for request-level balancing."
---

| Node.js world | Kubernetes |
| --- | --- |
| `process.env.API_URL` with an IP | a Service DNS name |
| an nginx reverse proxy | a ClusterIP Service |
| a port in `server.listen(3000)` | `containerPort`, named and used as `targetPort` |
| exposing a dev server publicly | `kubectl port-forward` |
