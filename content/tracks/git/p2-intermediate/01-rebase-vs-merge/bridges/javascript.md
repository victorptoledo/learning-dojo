---
from: javascript
status: draft
concept: "Rebase, merge and the reflog"
from_way: "Clicking 'Update branch' on the pull request, which merges main into the branch."
native_way: "Rebase the branch locally (or choose 'Update with rebase' on GitHub), then force-push with --force-with-lease."
sensei_tip: "A rebase often conflicts in package-lock.json. Do not hand-edit it: take main's version (during a rebase that is --ours: git checkout --ours package-lock.json), run npm install so your package.json changes are applied, and git add the result."
---

| Node workflow | Git |
| --- | --- |
| PR "Update branch" (merge) | `git merge origin/main` on the branch |
| PR "Update with rebase" | `git rebase origin/main` + force-with-lease |
| lock-file conflicts | regenerate with `npm install`, then `git add` |
| GitHub "Squash and merge" | a squash merge |
