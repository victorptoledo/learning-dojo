---
from: java
status: draft
concept: "Documenting architecture"
from_way: "A slide deck with a diagram drawn in a drawing tool for the project kick-off, and Javadoc as the only up-to-date documentation."
native_way: "Structurizr DSL or C4-PlantUML in the repository, arc42 sections in AsciiDoc built with the Maven or Gradle AsciiDoctor plugin, and Spring Modulith's generated component diagrams and module canvases."
sensei_tip: "Spring Modulith's `Documenter` writes C4 component diagrams of your modules from the code itself, so level 3 never goes stale."
---

| Java | Documentation idea |
| --- | --- |
| Structurizr DSL, C4-PlantUML | diagrams as code |
| AsciiDoctor Maven or Gradle plugin | arc42 built with the project |
| Spring Modulith `Documenter` | component diagrams generated from code |
| `docs/adr` | arc42 section 9 |
| Javadoc | the code level |
| `springdoc-openapi` | published interface documentation |
