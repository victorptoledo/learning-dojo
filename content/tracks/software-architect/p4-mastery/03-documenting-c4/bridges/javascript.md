---
from: javascript
status: draft
concept: "Documenting architecture"
from_way: "A Figma board or whiteboard photo linked from the README, out of date since the second sprint."
native_way: "Mermaid C4 diagrams in Markdown rendered by GitHub, Structurizr DSL for larger models, arc42 and ADRs in a Docusaurus or VitePress site, and dependency graphs generated with `dependency-cruiser`."
sensei_tip: "`dependency-cruiser --output-type dot` draws the real module graph of your code. Put it next to the hand-written component diagram and see whether they agree."
---

| JavaScript / TypeScript | Documentation idea |
| --- | --- |
| Mermaid in GitHub Markdown | diagrams as code |
| Docusaurus, VitePress | arc42 built with the project |
| `dependency-cruiser` graphs, Nx graph | component views generated from code |
| `docs/adr` | arc42 section 9 |
| TypeDoc | the code level |
| OpenAPI or GraphQL schema docs | published interface documentation |
