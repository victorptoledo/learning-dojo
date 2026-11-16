---
from: go
status: draft
concept: "Table-driven evaluation and benchmarks"
from_way: "Table-driven tests with exact comparisons, and benchstat to tell real performance changes from noise."
native_way: "A table of cases with tags, graders per criterion, several runs per case, results per slice, and a careful comparison with the baseline that respects variance."
sensei_tip: "benchstat taught Go developers not to trust one run of a benchmark; apply the same discipline to prompts: run several times and treat small differences as noise until proven otherwise."
---

| Go | Evaluation concept |
| --- | --- |
| a table of test cases | the test set |
| subtests `t.Run(name, ...)` | per-case results |
| `if got != want` | an exact-match grader |
| `benchstat old.txt new.txt` | comparing baseline and candidate with variance |
| `-run` with a pattern | evaluating one slice |
