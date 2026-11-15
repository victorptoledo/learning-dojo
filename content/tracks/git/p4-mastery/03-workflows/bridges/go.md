---
from: go
status: draft
concept: "Team workflows"
from_way: "The Go project's Gerrit flow: one commit per change, reviewed and submitted onto master."
native_way: "Trunk-based development with small reviewed changes is the same philosophy; on GitHub it becomes short branches, small PRs and squash or rebase merges."
sensei_tip: "The Go project keeps release branches (release-branch.go1.23) and cherry-picks fixes into them, exactly the trunk-plus-release-branches model described here."
---

| Go project | General workflow |
| --- | --- |
| Gerrit change, +2 to submit | pull request with a required approval |
| TryBots | required status checks |
| `release-branch.go1.N` | release branches, fixes cherry-picked |
| one commit per change | squash merge, or a rebased single commit |
