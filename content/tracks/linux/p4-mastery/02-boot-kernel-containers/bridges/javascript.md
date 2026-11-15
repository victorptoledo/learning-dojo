---
from: javascript
status: draft
concept: "Where your JVM really runs"
from_way: "node:22 images and a Dockerfile, with the container treated as a lightweight VM."
native_way: "The container as a process on the host kernel, isolated by namespaces and limited by cgroups."
sensei_tip: "os.availableParallelism() and the V8 heap default know nothing about older cgroup setups; set --max-old-space-size to about 75% of the container's memory limit."
---

| JavaScript / TypeScript | Linux |
| --- | --- |
| os.cpus().length | host CPUs; check cpu.max |
| heap out of memory | V8 limit; 137 is the cgroup OOM kill |
| node as PID 1 | needs an init (tini, --init) to reap zombies |
| image layers | overlayfs |
