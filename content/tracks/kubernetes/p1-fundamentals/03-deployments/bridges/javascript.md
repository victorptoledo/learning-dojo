---
from: javascript
status: draft
concept: "Rolling, revisioned releases"
from_way: "pm2 reload restarts the workers one by one so the app never stops answering."
native_way: "A Deployment does a reload across the cluster: one new Pod at a time, each waited for until ready, and the old version kept for undo."
sensei_tip: "Vercel-style instant rollbacks map to rollout undo: the old ReplicaSet still exists, so going back only scales it up."
---

| Node.js world | Kubernetes |
| --- | --- |
| `pm2 reload` | a rolling update |
| a `version` in `package.json` | the image tag |
| the previous build kept on the server | an old ReplicaSet |
| a health route checked by the load balancer | the readiness probe |
