---
from: go
status: draft
concept: "Architecture is cost: measure it, attribute it, optimise it"
from_way: "Small Go binaries on cheap VMs, cost rarely discussed."
native_way: "Go's small footprint pays off: tiny Fargate tasks, fast Lambda starts and easy Graviton builds; tags and budgets make it visible."
sensei_tip: "Go services usually need little memory: start Fargate tasks at 0.25 vCPU and 512 MB and scale from metrics, instead of copying another team's task size."
---

| Go world | AWS |
| --- | --- |
| one VM per service | small tasks packed by Fargate |
| `GOARCH=amd64` | `GOARCH=arm64` on Graviton |
| ad hoc cost checks | Budgets and anomaly detection |
| a team's server list | cost allocation tags |
