---
from: python
status: draft
concept: "Rebase, merge and the reflog"
from_way: "Merging main into a feature branch every morning, leaving a trail of 'Merge branch main' commits."
native_way: "git pull --rebase on your feature branch: your commits are replayed on top of main and the history stays a line."
sensei_tip: "After rebasing, run the tests again: every replayed commit now sits on new code, and a rebase without conflicts can still break behaviour."
---

| Python habit | Git |
| --- | --- |
| `git merge main` daily | `git rebase main` (or `pull --rebase`) |
| a CI run on the merge commit | a CI run on the rebased tip |
| "I lost my commit after a reset" | `git reflog`, then `git reset --hard HEAD@{n}` |
