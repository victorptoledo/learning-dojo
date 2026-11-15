---
from: python
status: draft
concept: "Undoing changes"
from_way: "Ctrl+Z in the editor until the file looks right, or a .bak copy."
native_way: "Git keeps every committed version: restore a file from any commit, reset local commits, revert shared ones."
sensei_tip: "A notebook that a bad run overwrote is one command away from its committed state: git restore analysis.ipynb. Commit notebooks before long experiments."
---

| Python habit | With Git |
| --- | --- |
| `cp app.py app.py.bak` | commit, then `git restore app.py` |
| Ctrl+Z after closing the editor | `git restore --source=HEAD~1 app.py` |
| delete `build/` and `dist/` by hand | `git clean -ndx`, then `-fdx` |
| "undo yesterday's deploy commit" | `git revert <hash>` |
