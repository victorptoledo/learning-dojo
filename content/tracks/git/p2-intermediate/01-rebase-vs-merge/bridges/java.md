---
from: java
status: draft
concept: "Rebase, merge and the reflog"
from_way: "SVN-style merges of long-lived branches, with 'svn:mergeinfo' and fear of every merge."
native_way: "Short-lived branches kept up to date with rebase, landed with a merge or fast-forward, and the reflog as a local undo history."
sensei_tip: "IntelliJ's 'Update Project' asks Merge or Rebase: that is git pull with or without --rebase. Pick Rebase for your feature branches and set it as the default."
---

| Java / SVN habit | Git |
| --- | --- |
| `svn merge` with mergeinfo | `git merge`, the merge base is computed from the DAG |
| Update Project (merge) | `git pull` |
| Update Project (rebase) | `git pull --rebase` |
| IntelliJ Local History to recover | `git reflog` for committed work |
