---
from: go
status: draft
concept: "Finding why a service is slow"
from_way: "pprof (net/http/pprof), go tool trace and the runtime's GODEBUG settings."
native_way: "USE on the machine, then pprof on the process."
sensei_tip: "Since Go 1.19 set GOMEMLIMIT a little below the container's memory limit; since Go 1.25 GOMAXPROCS follows the cgroup CPU limit. Both keep a Go service from fighting its own cgroup."
---

| Go | Linux |
| --- | --- |
| go tool pprof cpu profile | top, pidstat, perf top |
| runtime.ReadMemStats | free -h, VmRSS |
| GOMEMLIMIT | the cgroup memory.max |
| goroutine dump (SIGQUIT) | strace -f -p PID |
| too many open files | ulimit -n, LimitNOFILE= |
