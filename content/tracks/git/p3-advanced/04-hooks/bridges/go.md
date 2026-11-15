---
from: go
status: draft
concept: "Automating checks around commits"
from_way: "Running gofmt and go vet in an editor plugin, and trusting everyone has it."
native_way: "A pre-commit hook (or lefthook, itself a Go binary) runs gofmt -l and go vet on the staged packages; CI runs the same and go test."
sensei_tip: "test -z \"$(gofmt -l $(git diff --cached --name-only --diff-filter=ACM -- '*.go'))\" checks only staged Go files and fails when any of them is not formatted."
---

| Go tooling | Git hook |
| --- | --- |
| `gofmt -l` | `pre-commit` |
| `go vet ./...` | `pre-commit` or `pre-push` |
| `go test ./...` | `pre-push`, and CI |
| lefthook | installs and runs the hooks from `lefthook.yml` |
