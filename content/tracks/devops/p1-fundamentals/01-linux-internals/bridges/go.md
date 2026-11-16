---
from: go
status: draft
concept: "Diagnose a service from the host"
from_way: "You run the binary with nohup and tail its log file."
native_way: "A systemd unit supervises the binary and journald keeps its output; ss, top and /proc show what it is doing."
sensei_tip: "Expose net/http/pprof on a local-only port: `curl localhost:6060/debug/pprof/goroutine?debug=2` beats guessing which goroutine is stuck."
---

| Go world | Linux |
| --- | --- |
| `nohup ./api &` | a systemd service with Restart= |
| `signal.NotifyContext` | SIGTERM handling |
| pprof | `top`, `perf`, `/proc/PID` |
| `GOMAXPROCS` | the CPU quota of the cgroup |
