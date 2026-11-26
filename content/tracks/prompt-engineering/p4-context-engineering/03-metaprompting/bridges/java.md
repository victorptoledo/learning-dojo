---
from: java
status: draft
concept: "Generated sources need checks too"
from_way: "Annotation processors and code generators (MapStruct, OpenAPI Generator) whose output is compiled and tested before it ships."
native_way: "Treat a model-written prompt like generated source: validate the contract (placeholders and output format), run the evaluation set, hold out data the generator never saw, and review the diff."
sensei_tip: "Overfitting a prompt to its development cases is like tuning a JIT benchmark until it measures nothing: keep a holdout set the optimiser never sees, the way you keep a test set out of training."
---

| Java | Metaprompting concept |
| --- | --- |
| OpenAPI Generator output | a generated prompt draft |
| compiling the generated sources | checking placeholders before scoring |
| a JMH benchmark with warm-up | scoring on a development set with a margin |
| a separate integration test suite | the holdout set |
| code review of generated diffs | human review of the new prompt |
