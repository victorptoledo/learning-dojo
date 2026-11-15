---
from: python
status: draft
concept: "Where your JVM really runs"
from_way: "Docker images such as python:3.13-slim, seen as small machines that run your app."
native_way: "A container is a Linux process with namespaces, a cgroup and an image's files; it shares the host kernel."
sensei_tip: "os.cpu_count() returns the host's CPUs, not the container's limit. Use os.process_cpu_count() (3.13+) or read /sys/fs/cgroup/cpu.max to size worker pools."
---

| Python | Linux |
| --- | --- |
| os.cpu_count() | the host CPUs; the cgroup may allow fewer |
| MemoryError | the heap; the cgroup OOM kills with 137 |
| os.getpid() == 1 in a container | the pid namespace |
| python:3.13-slim layers | overlayfs in a mount namespace |
