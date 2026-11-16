---
from: go
status: draft
concept: "Networking in Go"
from_way: "Using http.Get with the default client in production."
native_way: "net.Dial opens TCP or UDP sockets on the runtime's network poller; http.Client reuses connections through its Transport; the default client has no timeout."
sensei_tip: "Create an http.Client with a Timeout, always read and close response bodies (so connections return to the pool), and pass a context with a deadline."
---

| In Go | Underneath |
| --- | --- |
| `net.Dial("tcp", addr)` | DNS + handshake |
| `http.Transport` | the connection pool |
| `resp.Body.Close()` | returns the connection |
| `net.Resolver` | DNS, pure Go or cgo |
| `net.ListenPacket("udp", ...)` | UDP |
