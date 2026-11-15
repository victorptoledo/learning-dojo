---
from: go
status: draft
concept: "Rolling, revisioned releases"
from_way: "A deploy script replaces the binary host by host and restarts the systemd unit."
native_way: "The Deployment controller replaces Pods gradually, watches readiness and keeps previous ReplicaSets for rollback."
sensei_tip: "Go services start fast, so set minReadySeconds to a few seconds: it catches a binary that is ready for a moment and then crashes."
---

| Go world | Kubernetes |
| --- | --- |
| `ldflags -X main.version=...` | the image tag in the template |
| the previous binary kept as `server.old` | an old ReplicaSet |
| a health endpoint | the readiness probe |
| restart host by host | `maxSurge` / `maxUnavailable` |
