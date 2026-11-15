---
from: go
status: draft
concept: "Placement rules for replicas"
from_way: "You build for amd64 and arm64 and deploy each binary to the matching hosts yourself."
native_way: "A multi-arch image runs anywhere; nodeSelector or affinity on kubernetes.io/arch decides when you need a specific one."
sensei_tip: "The scheduler is extensible in Go: the scheduling framework lets you write plugins for filter and score, though most teams never need one."
---

| Go world | Kubernetes |
| --- | --- |
| `GOARCH=arm64` builds | `kubernetes.io/arch: arm64` |
| a host list per binary | node labels and affinity |
| a custom placement script | scheduler plugins (rarely needed) |
| graceful restarts host by host | drains with a PodDisruptionBudget |
