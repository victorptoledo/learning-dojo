---
from: go
status: draft
concept: "Run only what is needed, with only the rights it needs"
from_way: "A static binary run as root, with net/http/pprof imported and listening on all interfaces."
native_way: "A static binary in a scratch or distroless image as a non-root user, pprof on a separate local listener, and timeouts on the server."
sensei_tip: "Importing `net/http/pprof` registers handlers on the default mux; serve your app on its own `http.ServeMux` and keep pprof on a localhost-only listener."
---

| Go | Hardened deployment |
| --- | --- |
| import _ "net/http/pprof" on the main mux | pprof on 127.0.0.1 only |
| http.ListenAndServe without timeouts | http.Server with Read/Write timeouts |
| FROM golang as the runtime image | distroless static, nonroot |
| running as uid 0 | USER nonroot |
| old Go toolchain | current release with security fixes |
