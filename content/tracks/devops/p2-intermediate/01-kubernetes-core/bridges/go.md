---
from: go
status: draft
concept: "Declare the service, controllers keep it running"
from_way: "You write a control loop yourself: watch something, compare, act."
native_way: "Kubernetes is built of exactly those loops (written in Go): controllers watch objects and reconcile them."
sensei_tip: "Set GOMEMLIMIT a little under the memory limit so the garbage collector works harder before the kernel kills the Pod."
---

| Go world | Kubernetes |
| --- | --- |
| a `for { reconcile() }` loop | a controller |
| `http.Server.Shutdown` | graceful termination on SIGTERM |
| `/healthz` handler | probes |
| `GOMAXPROCS` | the CPU request and limit |
