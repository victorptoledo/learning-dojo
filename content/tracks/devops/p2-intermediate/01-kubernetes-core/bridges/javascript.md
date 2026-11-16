---
from: javascript
status: draft
concept: "Declare the service, controllers keep it running"
from_way: "pm2 cluster mode runs one Node.js process per core on a server."
native_way: "Run one process per container and let Kubernetes scale the Pods; the Service spreads requests across them."
sensei_tip: "Set a memory limit and --max-old-space-size below it, or V8 grows until the container is OOM-killed."
---

| JavaScript world | Kubernetes |
| --- | --- |
| `pm2 -i max` | replicas |
| `process.env` | ConfigMap and Secret as env |
| a `/healthz` route | probes |
| a reverse proxy | Service plus HTTPRoute |
