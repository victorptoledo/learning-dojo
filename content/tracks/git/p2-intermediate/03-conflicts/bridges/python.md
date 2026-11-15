---
from: python
status: draft
concept: "Resolving conflicts"
from_way: "Resolving a conflict, seeing no markers left, and committing without running anything."
native_way: "Resolve, then run the formatter and the tests before git add: Python has no compiler to catch a bad merge."
sensei_tip: "A merge that leaves two functions with the same name in a module does not fail in Python: the second definition silently wins. pytest and a linter (ruff) catch what the interpreter will not."
---

| Python situation | What to do |
| --- | --- |
| conflict in `poetry.lock` / `uv.lock` | take one side, `poetry lock` / `uv lock`, add |
| conflict caused by black formatting | agree on one formatter version, reformat, add |
| duplicate function after merge | ruff reports redefinition (F811) |
| after any resolution | `pytest` before `--continue` |
