---
from: go
status: draft
concept: "Shutdown with a deadline"
from_way: "signal.NotifyContext for SIGTERM, srv.Shutdown(ctx) with a context deadline that stops accepting and waits for active connections, then closing the database."
native_way: "process.once(\"SIGTERM\"), server.close() plus closeIdleConnections() with a timer that calls closeAllConnections() and exits, then closing resources."
sensei_tip: "http.Server.Shutdown(ctx) is exactly server.close() + closeIdleConnections() + a deadline. Go's static binaries make tiny images easy; in Node.js, npm ci --omit=dev and a slim base image get close."
---

| Go | Node.js |
| --- | --- |
| `signal.NotifyContext(ctx, syscall.SIGTERM)` | `process.once("SIGTERM", ...)` |
| `srv.Shutdown(ctx)` | `server.close()` + `closeIdleConnections()` |
| Context deadline | Forced exit timer |
| `srv.Close()` | `server.closeAllConnections()` |
| `db.Close()` | `await pool.end()` |
| Distroless static image | `node:26-slim` + `npm ci --omit=dev` |
