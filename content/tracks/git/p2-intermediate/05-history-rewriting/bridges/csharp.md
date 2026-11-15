---
from: csharp
status: draft
concept: "Polishing a branch before review"
from_way: "Shelvesets updated again and again, then one check-in with a long comment."
native_way: "A branch whose commits you reshape freely before the pull request completes, with interactive rebase."
sensei_tip: "Visual Studio can squash commits from the history view, but it has no fixup/autosquash. Learn git commit --fixup and git rebase -i --autosquash on the command line; they save the most time."
---

| TFVC / Visual Studio | Git |
| --- | --- |
| Update the shelveset | amend, or a fixup commit |
| Squash commits (VS history view) | `git rebase -i` with `squash` |
| Port a changeset to a release branch | `git cherry-pick -x` |
| (none) | `--autosquash` |
