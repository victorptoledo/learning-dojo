---
from: go
status: draft
concept: "Developers own their service in production"
from_way: "You hand over a static binary and a systemd unit file, and someone else runs it."
native_way: "The same binary goes into a tiny image through CI; your team deploys it, sets its SLOs and carries the pager."
sensei_tip: "Most DevOps tools (Docker, Kubernetes, Terraform, Prometheus) are written in Go: reading their source is a skill worth having."
---

| Go world | DevOps |
| --- | --- |
| `go build` | a reproducible CI build |
| a static binary | a distroless image |
| `expvar` and pprof | metrics and profiling in production |
| a systemd unit | a Deployment manifest |
