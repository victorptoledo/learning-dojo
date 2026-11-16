---
from: go
status: reviewed
concept: "Validators in net/http"
from_way: "Handlers that always write 200 with the full body and no caching headers."
native_way: "Set Cache-Control per route, compute an ETag, answer If-None-Match with 304 and If-Match mismatches with 412."
sensei_tip: "http.ServeContent handles If-None-Match, If-Modified-Since and ranges for you when you serve files or byte slices with a modtime."
---

| Go | Data fetching |
| --- | --- |
| `w.Header().Set("Cache-Control", "no-cache")` | stored but revalidated |
| `w.Header().Set("ETag", etag)` + `w.WriteHeader(304)` | cheap revalidation |
| compare `r.Header.Get("If-Match")` | 412 on a stale write |
| `http.ServeContent` | conditional requests for static content |
