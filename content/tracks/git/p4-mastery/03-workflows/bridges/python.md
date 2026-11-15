---
from: python
status: draft
concept: "Team workflows"
from_way: "Notebook-driven work shared by email or pushed straight to main without review."
native_way: "GitHub flow: a short branch per change, a pull request with CI (pytest, ruff, type checks) and one review before merging to main."
sensei_tip: "For data and ML code, ask reviewers to check the experiment, not just the code: put the metrics or a plot in the PR description so the review covers results too."
---

| Python habit | Workflow practice |
| --- | --- |
| push to `main` directly | protected `main`, PR required |
| tests run on a laptop | required status checks |
| notebook review in screenshots | jupytext or nbdime diffs in the PR |
| a big "refactor everything" branch | small PRs, refactors separate |
