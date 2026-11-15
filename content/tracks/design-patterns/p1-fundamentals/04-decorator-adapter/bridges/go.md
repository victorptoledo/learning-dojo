---
from: go
status: draft
concept: "Adapter, decorator, proxy and facade"
from_way: "Calling a vendor package's types from every handler."
native_way: "Small interfaces you own with adapter structs around vendor clients, wrapper types that embed or hold an interface (decorators), middleware functions, and a package as a facade."
sensei_tip: "`http.RoundTripper` is the hook: wrap `http.DefaultTransport` in your own RoundTripper to add retries, logging or auth to every outgoing request."
---

| Go | Pattern |
| --- | --- |
| `http.HandlerFunc(f)` | adapter (function to interface) |
| `bufio.NewReader(r)`, `gzip.NewReader(r)` | decorator |
| a `RoundTripper` wrapper | decorator or proxy |
| `httputil.ReverseProxy` | proxy (remote) |
| `net/http` `http.Get` | facade |
