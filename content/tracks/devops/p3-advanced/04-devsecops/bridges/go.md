---
from: go
status: draft
concept: "Security checks run in the delivery path"
from_way: "go get the latest version of a module and trust it."
native_way: "go.sum and the checksum database pin every module, govulncheck reports only vulnerabilities your code actually calls, and images are signed."
sensei_tip: "govulncheck's call-graph analysis cuts noise: it tells you whether the vulnerable function is reachable, not just present."
---

| Go world | DevSecOps |
| --- | --- |
| `go.sum` | pinned dependencies |
| `govulncheck` | SCA with reachability |
| `gosec` | SAST |
| `go version -m binary` | an embedded SBOM of modules |
