---
from: go
status: draft
concept: "Where your JVM really runs"
from_way: "A static binary in a scratch or distroless image, the smallest kind of container."
native_way: "Exactly what a container is: one process, its namespaces, its cgroup, and a few files."
sensei_tip: "A Go binary in FROM scratch shows what the image really is: just a root filesystem. Go 1.25+ sets GOMAXPROCS from the cgroup CPU limit; set GOMEMLIMIT from the memory limit."
---

| Go | Linux |
| --- | --- |
| runtime.NumCPU() | the CPUs the process may run on |
| GOMAXPROCS | follows cpu.max since Go 1.25 |
| FROM scratch | an image with only your files |
| syscall.Unshare / CLONE_NEW* | how runtimes create namespaces |
