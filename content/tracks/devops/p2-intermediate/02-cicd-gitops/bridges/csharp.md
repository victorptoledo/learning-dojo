---
from: csharp
status: draft
concept: "Promote one artefact; let the environment pull its state"
from_way: "Azure DevOps release pipelines push one build through stages with approvals."
native_way: "The pipeline builds once; promotions are pull requests to environment folders, and a GitOps agent (Argo CD, Flux) pulls them."
sensei_tip: "Keep the approvals but move them to pull request reviews with code owners on the production folder."
---

| C# world | GitOps |
| --- | --- |
| a release stage | an environment folder |
| a stage approval | a PR review on that folder |
| variable groups | overlays and external secrets |
| redeploy an old release | `git revert` |
