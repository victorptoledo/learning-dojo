---
from: go
status: draft
concept: "Architecture decision records"
from_way: "Decisions are explained, if at all, in a long comment at the top of `main.go` or in the commit message that introduced them."
native_way: "Numbered Markdown ADRs in `docs/adr`, short and immutable, superseded by new ones when the decision changes, and reviewed in pull requests."
sensei_tip: "Go's own proposal process (golang/proposal) writes down the problem, the alternatives and the decision before code lands. ADRs are the lightweight version of the same habit."
---

| Go habit | ADR practice |
| --- | --- |
| a big comment in `main.go` | one decision per numbered file |
| Go proposals with discussion | proposed ADR, then accepted |
| `go.mod` requirement added | the outcome; the ADR keeps the why |
| a design doc for a large change | design doc first, ADRs for its decisions |
| tools | `adr-tools`, plain Markdown |
