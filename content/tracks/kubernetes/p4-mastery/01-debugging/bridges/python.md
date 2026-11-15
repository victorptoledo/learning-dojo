---
from: python
status: draft
concept: "Debugging a process you cannot log in to"
from_way: "SSH in, read the gunicorn error log, and drop into pdb or py-spy on the box."
native_way: "Logs come from kubectl logs, the platform's view from describe and events, and kubectl debug brings py-spy or a shell into the Pod."
sensei_tip: "py-spy dump --pid 1 from an ephemeral container (with --target and SYS_PTRACE allowed in a debug profile) shows where a hung worker is stuck."
---

| Python world | Kubernetes |
| --- | --- |
| `tail -f error.log` | `kubectl logs -f` |
| a traceback after a crash | `kubectl logs --previous` |
| `py-spy dump` over SSH | `kubectl debug --target` with py-spy |
| `curl localhost:8000` on the box | `kubectl port-forward` |
