---
from: go
status: draft
concept: "Table-driven examples"
from_way: "Table-driven tests: a slice of structs with an input and the expected output, covering the interesting cases."
native_way: "Few-shot examples are a table in the prompt: inputs and exact outputs, balanced across labels, interleaved, and never taken from the evaluation set."
sensei_tip: "If you can write the test table, you can write the example set. Then keep them separate: the examples teach, the table measures."
---

| Go | Few-shot concept |
| --- | --- |
| `[]struct{ in, want string }` | the example pool |
| one case per behaviour | diverse examples |
| `map[string]int` of labels | checking balance |
| a single `format` helper | one consistent example format |
| separate `testdata/` files | evaluation inputs kept out of the prompt |
