---
from: python
status: draft
concept: "Staging and atomic commits"
from_way: "Running black on the whole project and committing the reformat together with a bug fix."
native_way: "Format in its own commit, fix in another, staged precisely with git add -p, and the formatting commit listed in .git-blame-ignore-revs so blame skips it."
sensei_tip: "Put the hash of a mass-reformat commit in .git-blame-ignore-revs and run git config blame.ignoreRevsFile .git-blame-ignore-revs: blame then shows who really wrote each line."
---

| Python habit | With Git |
| --- | --- |
| `black .` plus a fix in one commit | two commits |
| `__pycache__/` appears in status | `.gitignore` |
| `print` debugging left in | `git diff --staged` before committing catches it |
| "When did this test start skipping?" | `git log -S "pytest.mark.skip" -- tests/` |
