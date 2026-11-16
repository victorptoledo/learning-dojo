---
from: go
status: draft
concept: "Documenting architecture"
from_way: "Package comments and a diagram in a Google Doc linked from the README."
native_way: "Structurizr DSL or Mermaid in the repository, arc42 and ADRs as Markdown, `go doc` for the code level, and package graphs generated with `goda` or `go mod graph`."
sensei_tip: "Structurizr's tooling is written for the JVM, but the DSL is just text: keep the `.dsl` file in your Go repository and render it in CI with the Structurizr CLI container."
---

| Go | Documentation idea |
| --- | --- |
| Structurizr DSL, Mermaid | diagrams as code |
| Markdown sites (Hugo, MkDocs) | arc42 built with the project |
| `goda graph`, `go mod graph` | component views generated from code |
| `docs/adr` | arc42 section 9 |
| package comments, `go doc` | the code level |
| protobuf and OpenAPI docs | published interface documentation |
