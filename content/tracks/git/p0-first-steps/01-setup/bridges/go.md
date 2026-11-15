---
from: go
status: draft
concept: "Version control in a Go module"
from_way: "GOPATH-era layouts where the import path had to match a folder on disk."
native_way: "A Go module is usually one Git repository: go.mod and go.sum are committed, and module versions are Git tags such as v1.2.0."
sensei_tip: "The Go toolchain reads versions straight from Git tags, so a pushed tag is a public release. Never move or delete one: the checksum database remembers the old commit."
---

| Go concept | Git concept |
| --- | --- |
| module path `github.com/acme/api` | the repository URL |
| `go.mod`, `go.sum` | committed, changed in the same commit as the code |
| version `v1.2.0` | the tag `v1.2.0` |
| `v2` major version | a `/v2` module path, often still the same repository |
