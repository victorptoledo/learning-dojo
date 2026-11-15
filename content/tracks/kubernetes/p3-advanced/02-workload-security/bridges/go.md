---
from: go
status: draft
concept: "Least privilege for the process and the network"
from_way: "A static binary on a VM, run by systemd with hardening options such as NoNewPrivileges and ProtectSystem."
native_way: "The security context is the systemd hardening of Kubernetes: non-root, no new privileges, no capabilities, read-only file system, seccomp."
sensei_tip: "A static Go binary in a distroless :nonroot image passes the restricted standard with no extra work: nothing to write, no shell, UID 65532."
---

| systemd option | Kubernetes |
| --- | --- |
| `User=app` | `runAsUser` / `runAsNonRoot` |
| `NoNewPrivileges=yes` | `allowPrivilegeEscalation: false` |
| `CapabilityBoundingSet=` | `capabilities.drop: [ALL]` |
| `ProtectSystem=strict` | `readOnlyRootFilesystem: true` |
| `SystemCallFilter=` | `seccompProfile` |
