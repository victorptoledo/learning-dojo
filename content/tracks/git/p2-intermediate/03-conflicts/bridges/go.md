---
from: go
status: draft
concept: "Resolving conflicts"
from_way: "Hand-merging go.sum line by line."
native_way: "Resolve go.mod by intent (the higher required version usually wins), then let the tool rebuild go.sum with go mod tidy."
sensei_tip: "go.sum is a set of hashes, not code: after fixing go.mod, run go mod tidy and git add go.sum. Hand-edited hashes only cause checksum errors later."
---

| Go situation | What to do |
| --- | --- |
| two `require` versions of one module | keep the higher one (minimal version selection picks it anyway) |
| `go.sum` conflict | `go mod tidy`, then add |
| generated code (`*_gen.go`, protobuf) | regenerate, never merge |
| after resolving | `go vet ./... && go test ./...` |
