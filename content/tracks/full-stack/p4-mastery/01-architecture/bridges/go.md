---
from: go
status: reviewed
concept: "Packages and internal/ as boundaries"
from_way: "Every package importing every other one, or a separate service for each concept."
native_way: "One module with packages per domain, internal/ to hide implementation, a BFF or gateway where clients diverge, and services only for concrete forces."
sensei_tip: "Go's internal/ directory is a compiler-enforced module boundary: code outside the parent cannot import it."
---

| Go | Architecture |
| --- | --- |
| `internal/checkout` | a hidden module implementation |
| `go.work` | a multi-module monorepo |
| a thin Go service in front of mobile | a BFF |
| `depguard` in golangci-lint | enforced import rules |
