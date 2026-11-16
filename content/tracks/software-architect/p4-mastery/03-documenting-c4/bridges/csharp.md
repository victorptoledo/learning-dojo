---
from: csharp
status: draft
concept: "Documenting architecture"
from_way: "Visio diagrams on SharePoint and a Word architecture document written once for a review board."
native_way: "Structurizr DSL (or the Structurizr .NET library) or Mermaid in the repository, arc42 and ADRs in DocFX or a Markdown site, and dependency graphs from NDepend or Visual Studio's code maps."
sensei_tip: "Visual Studio and NDepend can draw dependency graphs of the real solution. Use them to check that your hand-drawn container and component diagrams still tell the truth."
---

| C# / .NET | Documentation idea |
| --- | --- |
| Structurizr DSL, Mermaid | diagrams as code |
| DocFX, Markdown sites | arc42 built with the project |
| NDepend, code maps | component views generated from code |
| `docs/adr` | arc42 section 9 |
| XML doc comments | the code level |
| Swashbuckle, NSwag | published interface documentation |
