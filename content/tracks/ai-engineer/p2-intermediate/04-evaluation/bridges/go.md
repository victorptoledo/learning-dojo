---
from: go
status: draft
concept: "Tests become measurements"
from_way: "Table-driven tests where each row passes or fails."
native_way: "Table-driven evals where each row is scored, results are aggregated per tag, and the run is compared with a baseline file."
sensei_tip: "Benchmarks (testing.B) are the closer analogy: you compare numbers between runs and look for significant changes, not a single pass or fail."
---

| Go testing | LLM evaluation |
| --- | --- |
| a table of test cases | golden cases |
| `if got != want` | exact metric |
| `benchstat` comparing runs | baseline versus candidate |
| build tags or name filters | slices |
| `t.Errorf` on threshold | a score gate |
