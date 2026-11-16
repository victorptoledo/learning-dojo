---
from: go
status: draft
concept: "Governance, style guides and review"
from_way: "`gofmt` and `go vet` as the only shared rules."
native_way: "Spectral or Vacuum (written in Go) in CI on the OpenAPI document, shared internal packages for problems and cursors, and design review of the contract."
sensei_tip: "Vacuum is a fast Spectral-compatible linter written in Go, easy to drop into a Go-based CI image."
---

| Go | Governance concept |
| --- | --- |
| Vacuum / Spectral | automated style guide |
| internal `problem` and `cursor` packages | consistency by reuse |
| `go vet` analyzers | code conventions |
| contract diff in the pull request | design review |
| catalog-info.yaml | API catalogue |
