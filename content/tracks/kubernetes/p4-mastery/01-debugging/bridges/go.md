---
from: go
status: draft
concept: "Debugging a process you cannot log in to"
from_way: "SSH in, curl the pprof endpoint on localhost and read journald logs."
native_way: "kubectl port-forward reaches net/http/pprof, kubectl logs replaces journalctl, and kubectl debug brings delve or a shell to a distroless Pod."
sensei_tip: "Keep pprof on a separate, unexposed port; kubectl port-forward pod/x 6060 then go tool pprof http://localhost:6060/debug/pprof/heap."
---

| Go world | Kubernetes |
| --- | --- |
| `journalctl -u server` | `kubectl logs` |
| a panic stack trace | `kubectl logs --previous` |
| pprof on localhost | `kubectl port-forward` to the pprof port |
| `dlv attach` over SSH | `kubectl debug --target` with delve |
