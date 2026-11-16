---
from: go
status: draft
concept: "Security as a program the teams trust"
from_way: "go vet as the only check, and vulnerabilities found by customers."
native_way: "govulncheck, gosec or Semgrep, and secret scanning in CI, with a disclosure policy and fix SLAs by priority."
sensei_tip: "`govulncheck` in CI plus Go's fast release cadence makes \"upgrade the toolchain and dependencies monthly\" a realistic baseline policy for Go services."
---

| Go | Program practice |
| --- | --- |
| go vet only | govulncheck and gosec in CI |
| customers report bugs | security.txt and coordinated disclosure |
| upgrades when something breaks | a monthly upgrade routine |
| every finding blocks | block only reliable, serious ones |
| security as one person's job | champions in each team |
