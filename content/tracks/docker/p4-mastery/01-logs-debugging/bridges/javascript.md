---
from: javascript
status: draft
concept: "Logs and diagnostics"
from_way: "console.log plus pm2's log files, and node --inspect for the Chrome DevTools debugger."
native_way: "console.log or pino to stdout is already right; the engine collects it. For debugging, run node --inspect=0.0.0.0:9229 and publish 9229 on 127.0.0.1 in development."
sensei_tip: "pino writes JSON lines fast and synchronously enough for containers; avoid file transports inside the image."
---

| JavaScript | Containers |
| --- | --- |
| pm2 log files | `docker logs` and the log driver |
| `node --inspect` | `--inspect=0.0.0.0:9229` with `-p 127.0.0.1:9229:9229` |
| `process.on("uncaughtException")` | log and exit; the restart policy restarts |
| heap snapshots | written to a volume or tmpfs |
