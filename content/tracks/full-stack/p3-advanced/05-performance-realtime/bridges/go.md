---
from: go
status: reviewed
concept: "Goroutines make connections cheap, not free"
from_way: "One goroutine per WebSocket and an in-memory map of clients, which breaks with a second instance."
native_way: "SSE with http.Flusher for server-to-client streams, WebSockets only for two-way traffic, and Redis or PostgreSQL LISTEN/NOTIFY to fan out across instances, with heartbeats."
sensei_tip: "For SSE in Go, set Content-Type: text/event-stream, write data: lines and call Flush; check r.Context().Done() to notice disconnects."
---

| Go | Performance and real-time |
| --- | --- |
| `w.(http.Flusher).Flush()` | Server-Sent Events |
| in-memory client map | pub/sub for several instances |
| `errgroup.Group` | parallel calls with cancellation |
| `db.SetMaxOpenConns` | the database pool size |
