---
from: go
status: draft
concept: "Small, reversible changes; practised incidents; blameless learning"
from_way: "A single binary replaced on every host at once by a script."
native_way: "Progressive rollouts per cell or region with automatic rollback, and operations through Systems Manager."
sensei_tip: "Expose build info (`runtime/debug.ReadBuildInfo`) in logs and health output, so during an incident you know exactly which version each task runs."
---

| Go world | AWS |
| --- | --- |
| a deploy script for all hosts | waves by region or cell |
| `kill -HUP` | an instance refresh or a new task definition |
| pprof during an outage | dashboards, traces and logs first |
| an incident chat | Incident Manager with a timeline |
