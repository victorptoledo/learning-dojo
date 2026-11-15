---
from: csharp
status: draft
concept: "Team workflows"
from_way: "Azure DevOps branch policies on a TFVC-style Dev, Main and Release branch hierarchy."
native_way: "The same branch policies (reviewers, build validation, linked work items) applied to a single main with short-lived branches."
sensei_tip: "Azure DevOps 'build validation' is a required status check and 'automatically included reviewers' is CODEOWNERS. The concepts carry over directly to GitHub rulesets."
---

| Azure DevOps | GitHub / general |
| --- | --- |
| Branch policies | branch protection / rulesets |
| Build validation | required status checks |
| Automatically included reviewers | CODEOWNERS |
| Dev → Main → Release hierarchy | trunk-based `main` + release branches when needed |
