---
from: go
status: draft
concept: "Remotes and remote-tracking branches"
from_way: "go get fetching modules through GOPROXY, which caches what the repository served."
native_way: "git fetch updates origin/* from the remote; the module proxy is a similar cache, but of immutable versions, not of moving branches."
sensei_tip: "For private modules set GOPRIVATE=github.com/acme/* so the go command talks to Git directly with your SSH key or token instead of the public proxy."
---

| Go | Git |
| --- | --- |
| `GOPROXY` cache | remote-tracking refs, refreshed by fetch |
| `go get example.com/lib@latest` | the newest tag on the remote |
| `GOPRIVATE` | fetch straight from the remote URL |
| `go list -m -versions` | `git ls-remote --tags` |
