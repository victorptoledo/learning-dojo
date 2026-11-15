---
from: javascript
status: draft
concept: "Declared state instead of start scripts"
from_way: "pm2 keeps a Node.js process alive and restarts it on a crash, on one machine."
native_way: "Kubernetes does what pm2 does, across many machines: it restarts containers, keeps the replica count and rolls out new versions."
sensei_tip: "Drop pm2 cluster mode inside containers: run one Node.js process per container and scale with replicas."
---

| Node.js world | Kubernetes |
| --- | --- |
| `pm2 start server.js -i 4` | a Deployment with `replicas: 4` |
| `pm2 restart` on crash | the kubelet restarts the container |
| `pm2 reload` for zero downtime | a rolling update of the Deployment |
| `process.env` from a `.env` file | env vars from a ConfigMap or Secret |
