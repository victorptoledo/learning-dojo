---
from: go
status: draft
concept: "Large repositories"
from_way: "A vendor/ directory with every dependency committed, growing with each upgrade."
native_way: "Modules fetched through the proxy and pinned in go.sum; vendor only when the build must work offline, and expect the history to grow."
sensei_tip: "Each go mod vendor upgrade rewrites thousands of files in history. If you vendor, keep those upgrades in their own commits so they are easy to skip in blame (.git-blame-ignore-revs)."
---

| Go situation | Git technique |
| --- | --- |
| a big `vendor/` | rely on the module proxy, or accept and isolate the commits |
| a Go monorepo with many modules | sparse checkout per module |
| CI building one module | `--filter=blob:none` plus sparse checkout |
| `go build` caches | live outside the repository, never commit them |
