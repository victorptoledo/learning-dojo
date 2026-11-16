---
from: javascript
status: draft
concept: "Diagnose a service from the host"
from_way: "pm2 or nodemon keeps your Node.js process alive and pm2 logs shows its output."
native_way: "systemd (or the container runtime) supervises node; journalctl, ss and top replace the pm2 dashboard."
sensei_tip: "Node.js does not exit on SIGTERM if it has open handles; call server.close() in a SIGTERM handler or the grace period ends in SIGKILL."
---

| JavaScript world | Linux |
| --- | --- |
| `pm2 start` | a systemd service |
| `pm2 logs` | `journalctl -u app -f` |
| `process.on('SIGTERM')` | graceful shutdown |
| `--max-old-space-size` | the memory limit of the cgroup |
