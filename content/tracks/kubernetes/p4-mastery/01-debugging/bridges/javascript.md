---
from: javascript
status: draft
concept: "Debugging a process you cannot log in to"
from_way: "pm2 logs and node --inspect on the server, reached through an SSH tunnel."
native_way: "kubectl logs replaces pm2 logs, and kubectl port-forward replaces the SSH tunnel to the inspector port."
sensei_tip: "Send SIGUSR1 to the Node.js process from an ephemeral container to enable the inspector at runtime, then port-forward 9229 and attach Chrome DevTools."
---

| Node.js world | Kubernetes |
| --- | --- |
| `pm2 logs` | `kubectl logs -l app=web --prefix` |
| `node --inspect` + SSH tunnel | `kubectl port-forward pod/x 9229` |
| an uncaught exception crash | CrashLoopBackOff and `--previous` logs |
| `pm2 monit` | `kubectl top pods` |
