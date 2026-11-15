---
from: javascript
status: draft
concept: "Health endpoints and sized containers"
from_way: "A /health route and a PM2 max_memory_restart setting."
native_way: "Probes call separate liveness and readiness routes, and a memory limit with an OOM kill replaces PM2's memory restart."
sensei_tip: "Set --max-old-space-size below the container memory limit, or the V8 heap can grow past it and the kernel kills the process without a JavaScript error."
---

| Node.js world | Kubernetes |
| --- | --- |
| `max_memory_restart: '500M'` | `limits.memory` |
| `app.get('/health')` | liveness and readiness probes |
| the event loop blocked | a failing liveness probe |
| `--max-old-space-size` | heap size below the limit |
