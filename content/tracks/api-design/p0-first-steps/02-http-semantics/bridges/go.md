---
from: go
status: draft
concept: "HTTP methods, safety and idempotency"
from_way: "A single `http.HandleFunc(\"/api\", ...)` that switches on a field of the body."
native_way: "Go 1.22+ method patterns: `mux.HandleFunc(\"GET /orders/{id}\", ...)`, `\"PUT /orders/{id}\"`; the mux answers 405 with `Allow` for you."
sensei_tip: "With method patterns, `net/http` answers 405 and sets `Allow` automatically; `r.PathValue(\"id\")` reads the path variable."
---

| Go | HTTP concept |
| --- | --- |
| `"GET /orders/{id}"` | safe, idempotent |
| `"PUT /orders/{id}"` | idempotent |
| `"POST /orders"` | not idempotent |
| `w.Header().Set("Content-Type", ...)` | representation metadata |
| `r.Header.Get("Accept")` | content negotiation |
