---
from: go
status: draft
concept: "Client-server communication"
from_way: "net/http with explicit status codes and headers."
native_way: "The same explicitness, guided by HTTP semantics."
sensei_tip: "http.ServeContent already handles If-Modified-Since and Range requests; for ETags set the header and check If-None-Match yourself."
---

| Go | In practice |
| --- | --- |
| w.WriteHeader(http.StatusCreated) | 201 Created |
| w.Header().Set("ETag", v) | an entity tag |
| http.ServeContent | conditional and range requests |
| r.Method == http.MethodGet | safe, idempotent methods |
