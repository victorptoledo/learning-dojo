---
from: java
status: draft
concept: "Polishing a branch before review"
from_way: "Squash everything when merging the pull request, whatever the commits looked like."
native_way: "Shape the branch into a few meaningful commits with git rebase -i, so the review and the history show the steps (refactor, then feature, then tests)."
sensei_tip: "IntelliJ's Git log has 'Interactively Rebase from Here', with fixup, squash and reword in a dialog. It writes the same todo list for you."
---

| Java / IDE habit | Git |
| --- | --- |
| "Squash and merge" only | `git rebase -i` before review, merge as is |
| IntelliJ: Interactively Rebase from Here | `git rebase -i <commit>` |
| Commit "fix review" | `git commit --fixup=<hash>` |
| Backport a fix to `release/2.4` | `git cherry-pick -x <hash>` |
