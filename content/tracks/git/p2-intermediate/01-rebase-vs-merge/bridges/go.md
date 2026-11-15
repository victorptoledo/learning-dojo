---
from: go
status: draft
concept: "Rebase, merge and the reflog"
from_way: "Assuming every history looks like the Go project's: one reviewed commit per change, no merge commits."
native_way: "That linear history comes from rebasing each change onto the tip before it lands (Gerrit's model). Plain Git leaves the choice to the team."
sensei_tip: "The Go repository itself only accepts rebased, single-commit changes. If your team likes that style, use squash or rebase merges and protect main against merge commits."
---

| Go project habit | Git mechanism |
| --- | --- |
| one commit per change | squash, or interactive rebase before review |
| change rebased before submit | `git rebase origin/master` |
| no merge commits on master | fast-forward only (`git merge --ff-only`) |
| "where was my change before the rebase?" | `git reflog` |
