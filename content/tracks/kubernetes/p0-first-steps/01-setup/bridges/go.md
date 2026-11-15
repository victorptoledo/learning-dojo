---
from: go
status: draft
concept: "Declared state instead of start scripts"
from_way: "A static Go binary copied to servers with scp and kept alive by systemd."
native_way: "The binary lives in a small image; a Deployment declares how many copies run, and Kubernetes keeps that true."
sensei_tip: "Kubernetes itself is written in Go, and its client library (client-go) is how you will write controllers in Phase 4."
---

| Go world | Kubernetes |
| --- | --- |
| `scp server && systemctl restart server` | `kubectl apply` of a new image |
| `Restart=always` in a unit file | the kubelet restarts the container |
| flags and env vars | `args` and `env` in the container spec |
| several hosts behind HAProxy | a Deployment behind a Service |
