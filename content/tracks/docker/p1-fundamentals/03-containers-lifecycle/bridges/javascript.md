---
from: javascript
status: draft
concept: "Process lifecycle and graceful shutdown"
from_way: "process.on('SIGTERM', ...) lets a Node.js server close its listener and connections; npm start runs your script through npm and a shell."
native_way: "Run node directly as PID 1 with the exec form, handle SIGTERM by calling server.close(), and exit before the grace period ends."
sensei_tip: "Do not use CMD [\"npm\", \"start\"] in production images: npm adds a process that may not forward signals. Use CMD [\"node\", \"dist/server.js\"]."
---

| JavaScript | Container |
| --- | --- |
| `process.on("SIGTERM", ...)` | runs on `docker stop` |
| `server.close()` | stop accepting, finish in-flight requests |
| `process.exitCode = 1` | a failure exit that `on-failure` restarts |
| pm2 or nodemon | the engine's restart policy |
