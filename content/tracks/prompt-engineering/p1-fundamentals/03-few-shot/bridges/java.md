---
from: java
status: draft
concept: "Examples as test fixtures"
from_way: "JUnit parameterised tests with a table of inputs and expected outputs, chosen to cover edge cases."
native_way: "Few-shot examples are a small table of inputs and ideal outputs in the prompt, chosen to cover the boundaries, balanced across labels and kept apart from the test set."
sensei_tip: "Pick examples the way you pick @ParameterizedTest cases: one per equivalence class and one per tricky boundary, never three copies of the happy path."
---

| Java testing | Few-shot concept |
| --- | --- |
| `@CsvSource` rows | examples (input and output) |
| equivalence classes | diverse examples, one per kind of input |
| boundary values | examples near the edge between two labels |
| fixtures shared with production data | never reuse evaluation inputs as examples |
| a consistent `toString()` | one identical format for every example |
