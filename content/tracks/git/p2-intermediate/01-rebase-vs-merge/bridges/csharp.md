---
from: csharp
status: draft
concept: "Rebase, merge and the reflog"
from_way: "Azure DevOps pull requests completed with the default 'Merge (no fast-forward)'."
native_way: "Azure DevOps and GitHub both offer merge, squash, rebase and fast-forward, and semi-linear (rebase, then a merge commit). Pick one per repository."
sensei_tip: "Azure DevOps can enforce the merge type per branch policy. Choosing 'Squash' or 'Rebase and fast-forward' there keeps main readable without asking everyone to remember."
---

| Azure DevOps merge type | Git equivalent |
| --- | --- |
| Merge (no fast-forward) | `git merge --no-ff` |
| Squash commit | `git merge --squash` + commit |
| Rebase and fast-forward | `git rebase main` + `git merge --ff-only` |
| Semi-linear merge | rebase, then `git merge --no-ff` |
