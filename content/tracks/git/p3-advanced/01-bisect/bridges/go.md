---
from: go
status: draft
concept: "Automated regression hunting"
from_way: "Rebuilding old tagged versions by hand to see when a benchmark got slower."
native_way: "git bisect run with go test -run on one test, or with old/new terms and a benchmark threshold for performance."
sensei_tip: "For performance regressions use git bisect start --term-old=fast --term-new=slow and a script that runs go test -bench and exits 1 when ns/op crosses a threshold."
---

| Go habit | With bisect |
| --- | --- |
| `go test -run TestTotals ./checkout` | the run command |
| `go vet` or build fails at a commit | `exit 125` |
| benchmark got slower | custom terms `fast` / `slow` |
| the Go project's own bisect tool | `golang.org/x/tools/cmd/bisect` hunts inside a build, git bisect across commits |
