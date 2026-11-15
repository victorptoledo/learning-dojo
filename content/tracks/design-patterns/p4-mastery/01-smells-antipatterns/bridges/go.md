---
from: go
status: draft
concept: "Code smells and refactoring"
from_way: "One huge package with long functions, `map[string]interface{}` as data, and copy-pasted error handling."
native_way: "Golden-file tests as characterisation tests, small packages, named types for primitive obsession (`type Cents int64`), gopls refactorings, and linters (gocyclo, dupl) to find candidates."
sensei_tip: "A named type such as `type AccountID string` costs nothing at runtime and stops a whole class of mix-ups; it is the cheapest fix for primitive obsession in any language."
---

| Go | Refactoring tool or idea |
| --- | --- |
| gopls rename and extract | automated moves |
| golden files in `testdata/` | characterisation tests |
| `type Cents int64` | fix primitive obsession |
| golangci-lint (gocyclo, dupl, funlen) | smell detection |
| `internal/` packages | keeping structure |
