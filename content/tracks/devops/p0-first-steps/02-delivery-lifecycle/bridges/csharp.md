---
from: csharp
status: draft
concept: "Integrate continuously, release in small batches"
from_way: "A TFS or Azure DevOps release pipeline with stages and manual approvals, run once per sprint."
native_way: "The same pipeline runs on every merge; approvals are kept only where risk needs them, and the DORA metrics show the effect."
sensei_tip: "Azure DevOps and GitHub both expose deployment history through their APIs: compute your four keys from it instead of guessing."
---

| C# world | DevOps |
| --- | --- |
| a sprint release | many small releases |
| a manual approval per stage | approvals only for production, or none |
| a build definition | pipeline as code in the repository |
| hotfix branches | roll forward with a small fix |
