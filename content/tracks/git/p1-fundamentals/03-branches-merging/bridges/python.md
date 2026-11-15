---
from: python
status: draft
concept: "Branches as pointers"
from_way: "Keeping script_v2.py and script_old.py side by side."
native_way: "One file name, many branches: switch to compare, merge to combine, delete the losers."
sensei_tip: "An editable install (pip install -e .) follows the branch you switch to, but a stale .pyc or a changed dependency list does not. Reinstall when pyproject.toml differs between branches."
---

| Python habit | With Git |
| --- | --- |
| `script_v2.py` | branch `try-v2` |
| comment out the old code "just in case" | delete it; the history keeps it |
| a notebook copy per experiment | a branch per experiment |
| `diff old.py new.py` | `git diff main..try-v2 -- script.py` |
