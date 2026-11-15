---
from: go
status: draft
concept: "Staging and atomic commits"
from_way: "Running go mod tidy and gofmt across the module and committing everything that changed."
native_way: "Commit go.mod and go.sum changes with the code that needs them, keep mechanical changes (gofmt, renames) in their own commits."
sensei_tip: "The Go project's own convention is a subject prefixed by the package: `net/http: fix handling of empty Host header`. It tells a reviewer where to look before they open the diff."
---

| Go habit | With Git |
| --- | --- |
| `gofmt -w .` plus a fix | two commits |
| `go mod tidy` changes go.sum | committed with the code that added the import |
| binaries built with `go build` | ignored |
| "When did this function change?" | `git log -L :FuncName:file.go` |
