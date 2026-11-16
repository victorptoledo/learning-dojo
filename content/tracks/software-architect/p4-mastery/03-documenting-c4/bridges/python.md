---
from: python
status: draft
concept: "Documenting architecture"
from_way: "A long README and docstrings, with a diagram exported once as a PNG."
native_way: "Mermaid or C4-PlantUML in Markdown, arc42 sections and ADRs in an MkDocs or Sphinx site built in CI, and generated API references from docstrings."
sensei_tip: "MkDocs Material renders Mermaid diagrams in Markdown out of the box, so a C4 context diagram can live in the same file as its explanation."
---

| Python | Documentation idea |
| --- | --- |
| Mermaid C4 diagrams in Markdown | diagrams as code |
| MkDocs or Sphinx sites | arc42 built with the project |
| `pydeps` graphs | component views generated from code |
| `docs/adr` | arc42 section 9 |
| docstrings with `mkdocstrings` | the code level |
| FastAPI's OpenAPI | published interface documentation |
