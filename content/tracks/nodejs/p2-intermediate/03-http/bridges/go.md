---
from: go
status: draft
concept: "net/http in Node.js"
from_way: "http.HandleFunc with a ResponseWriter and *Request, ServeMux patterns with methods and wildcards, http.MaxBytesReader, server timeouts on http.Server, and http.Client with a Timeout."
native_way: "createServer((req, res) => ...) with a ServerResponse and IncomingMessage, routing by hand or a framework, your own body limit, server timeouts, and fetch with an AbortSignal."
sensei_tip: "node:http is very close to net/http: res.writeHead is w.WriteHeader, a body limit is http.MaxBytesReader. The big difference is that Go runs each handler on its own goroutine; in Node.js one slow synchronous handler delays the others."
---

| Go | Node.js |
| --- | --- |
| `http.HandleFunc("GET /users/{id}", h)` | Route on `req.method` + regex |
| `w.WriteHeader(201)` | `res.writeHead(201)` |
| `http.MaxBytesReader` | Count bytes while reading `req` |
| `srv.ReadHeaderTimeout` | `server.headersTimeout` |
| `http.Client{Timeout: 3 * time.Second}` | `fetch(url, { signal: AbortSignal.timeout(3000) })` |
| `httptest.NewServer` | `server.listen(0)` in tests |
