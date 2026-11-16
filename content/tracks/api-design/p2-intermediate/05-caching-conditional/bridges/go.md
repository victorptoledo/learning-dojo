---
from: go
status: draft
concept: "HTTP caching and conditional requests"
from_way: "Handlers that always write the full body."
native_way: "Set `ETag` and `Cache-Control`; `http.ServeContent` handles `If-None-Match`, `If-Modified-Since` and ranges for content you can seek; check `If-Match` yourself for writes."
sensei_tip: "`http.ServeContent` implements the RFC 9110 precondition order for reads (including 304 and 412); study its source to see the rules applied."
---

| Go | HTTP caching |
| --- | --- |
| `w.Header().Set("ETag", strconv.Quote(v))` | ETag |
| `http.ServeContent` | conditional reads |
| `w.WriteHeader(http.StatusNotModified)` | 304 |
| `r.Header.Get("If-Match")` | conditional write |
| `version` column in `UPDATE ... WHERE version = $n` | optimistic locking |
