---
from: go
status: draft
concept: "Connecting to other services"
from_way: "net.Dial errors, *net.OpError and the net/http client with its timeouts."
native_way: "The kernel's view of the same sockets through ip, ss and nc."
sensei_tip: "http.DefaultClient has no timeout. Create a client with Timeout set, and read the error: connect: connection refused, i/o timeout and no such host are the three layers of this lesson."
---

| Go | Linux |
| --- | --- |
| net.LookupHost(h) | getent hosts h |
| dial tcp: connect: connection refused | nothing listens: ss -tlnp |
| dial tcp: i/o timeout | dropped packets |
| net.Listen("tcp", "127.0.0.1:8080") | loopback only |
| golang.org/x/crypto/ssh | ssh |
