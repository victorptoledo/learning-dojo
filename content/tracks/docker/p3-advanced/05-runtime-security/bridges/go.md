---
from: go
status: draft
concept: "Least privilege at run time"
from_way: "systemd hardening options: MemoryMax, CPUQuota, TasksMax, CapabilityBoundingSet, NoNewPrivileges, ProtectSystem=strict."
native_way: "Docker's flags map almost one to one: --memory, --cpus, --pids-limit, --cap-drop, --security-opt no-new-privileges, --read-only."
sensei_tip: "A static Go binary on distroless needs no capabilities at all and never writes outside its data directory, which makes it the easiest service to run fully locked down."
---

| systemd (Go services) | docker run |
| --- | --- |
| `MemoryMax=256M` | `--memory 256m` |
| `CPUQuota=50%` | `--cpus 0.5` |
| `TasksMax=200` | `--pids-limit 200` |
| `CapabilityBoundingSet=` | `--cap-drop ALL` |
| `NoNewPrivileges=yes` | `--security-opt no-new-privileges` |
| `ProtectSystem=strict` | `--read-only` |
