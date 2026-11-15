---
from: go
status: draft
concept: "Routing and caching at the edge"
from_way: "http.FileServer serving assets straight from the binary or a directory."
native_way: "Put a CDN in front, fingerprint the assets, and let the Go server answer only dynamic requests and revalidations."
sensei_tip: "http.ServeContent handles If-None-Match and If-Modified-Since for you; set an ETag and the CDN revalidates with cheap 304s instead of full downloads."
---

| Go | Edge concept |
| --- | --- |
| `w.Header().Set("Cache-Control", "public, max-age=31536000, immutable")` | forever-cached asset |
| `http.ServeContent` | `ETag` / `304` revalidation |
| `net.LookupHost(h)` / `net.Resolver` | DNS lookups |
| `embed.FS` with hashed names | fingerprinted assets in the binary |
| `httputil.ReverseProxy` | what a PoP is, in miniature |
