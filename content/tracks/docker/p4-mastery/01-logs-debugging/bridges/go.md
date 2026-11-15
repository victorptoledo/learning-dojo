---
from: go
status: draft
concept: "Logs and diagnostics"
from_way: "log/slog to stdout under systemd's journal, and pprof endpoints or dlv on the server."
native_way: "slog with a JSON handler to stdout is exactly what the engine expects; expose net/http/pprof on a port bound to localhost and reach it from a tool container in the same network namespace."
sensei_tip: "A Go binary on distroless has no shell, but docker run --network container:api curlimages/curl localhost:6060/debug/pprof/goroutine?debug=2 dumps every goroutine."
---

| Go | Containers |
| --- | --- |
| `slog.NewJSONHandler(os.Stdout, nil)` | collected by `docker logs` |
| `journalctl -u svc` | `docker logs svc` |
| `net/http/pprof` | reached through `--network container:api` |
| `dlv attach` | a debug container with `--pid container:api` and `SYS_PTRACE` |
