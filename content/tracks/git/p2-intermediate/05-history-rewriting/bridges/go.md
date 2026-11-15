---
from: go
status: draft
concept: "Polishing a branch before review"
from_way: "Gerrit's model, where each change is a single commit amended with every review round."
native_way: "On GitHub or GitLab a change is a branch: keep a few commits, add fixups during review, autosquash before merging."
sensei_tip: "If you like Gerrit's one-commit-per-change, git commit --amend plus git push --force-with-lease gives you the same thing on a GitHub branch."
---

| Gerrit / Go project | GitHub-style branch |
| --- | --- |
| amend the change, new patch set | fixup commit, autosquash later |
| `Change-Id:` trailer | the branch name and pull request |
| relation chain of changes | several commits on one branch |
| backport with cherry-pick | `git cherry-pick -x` |
