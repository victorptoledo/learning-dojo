---
from: python
status: draft
concept: "pytest for prompts"
from_way: "pytest with parametrised cases and exact asserts, and a coverage report."
native_way: "A JSON Lines file of cases with tags, graders that return pass or fail per case, several runs per case, and a report per tag compared with the baseline."
sensei_tip: "Keep cases in JSON Lines (one case per line) so that diffs in code review show exactly which cases were added or changed, the same way a parametrize list does."
---

| Python | Evaluation concept |
| --- | --- |
| `@pytest.mark.parametrize` cases | the test set |
| `pytest.mark.<tag>` | slices |
| `assert answer == expected` | an exact-match grader |
| `re.search` in a test | a pattern grader |
| comparing two coverage reports | comparing candidate and baseline |
