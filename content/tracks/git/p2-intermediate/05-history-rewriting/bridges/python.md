---
from: python
status: draft
concept: "Polishing a branch before review"
from_way: "A branch full of 'wip', 'fix tests', 'black' commits pushed straight to review."
native_way: "Fold the noise into the commits it belongs to with fixup commits and autosquash, and check each commit with --exec."
sensei_tip: "git rebase -i --exec 'pytest -x -q' main runs the tests after every commit and stops at the first one that breaks them, so every commit in the branch is green."
---

| Python habit | Git |
| --- | --- |
| commit "black" | fold it into the commit it formats (`fixup`) |
| commit "fix tests" | `git commit --fixup=<commit that broke them>` |
| "does every commit pass?" | `git rebase -i --exec "pytest -x -q" main` |
| port a fix to the 3.x maintenance branch | `git cherry-pick -x` |
