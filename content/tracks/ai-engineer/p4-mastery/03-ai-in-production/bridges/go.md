---
from: go
status: draft
concept: "Operate it for years, not weeks"
from_way: "Go's compatibility promise, toolchain upgrades and services run with SLOs and on-call."
native_way: "No compatibility promise from models: every upgrade is evaluated and rolled out gradually, with SLOs and runbooks covering quality and cost as well as availability."
sensei_tip: "Models do not follow anything like the Go 1 compatibility promise; assume every new version behaves differently until your evals say otherwise."
---

| Go operations | AI feature operations |
| --- | --- |
| `go.mod` versions | pinned model ids |
| Go toolchain upgrades | model migrations |
| SLOs on latency and errors | SLOs plus quality and cost targets |
| canary deploys | shadow traffic and staged rollout |
| postmortems | postmortems that add eval cases |
