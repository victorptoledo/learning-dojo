---
from: csharp
status: draft
concept: "Architecture decision records"
from_way: "Decisions live in a Word document on SharePoint or in Azure DevOps wiki pages that drift away from the code."
native_way: "Numbered Markdown ADRs in the solution's `docs/adr` folder, reviewed in pull requests like code, linked from comments where needed, and superseded instead of edited."
sensei_tip: "The .NET runtime team keeps design documents and decisions in the repository next to the code (dotnet/designs). Do the same at your scale."
---

| .NET habit | ADR practice |
| --- | --- |
| SharePoint design document | `docs/adr/NNNN-title.md` in the repository |
| Azure DevOps wiki | ADRs reviewed in the pull request |
| a `NuGet` package added without comment | an ADR in the same pull request |
| architecture review board slides | proposed ADRs and an advice process |
| tools | `adr-tools`, `dotnet-adr`, plain files |
