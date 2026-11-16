---
from: javascript
status: draft
concept: "Examples as fixtures and snapshots"
from_way: "Test fixtures and snapshot files that show what output is expected for a given input."
native_way: "Few-shot examples are fixtures inside the prompt: relevant, diverse, balanced inputs with their exact expected outputs, in one consistent format."
sensei_tip: "A snapshot test teaches a reviewer what the output looks like; a few-shot example teaches the model the same thing. Both go wrong when every snapshot shows the same case."
---

| JavaScript | Few-shot concept |
| --- | --- |
| test fixtures | the example pool |
| `test.each([...])` | a table of input and output pairs |
| snapshot files | the exact output format to imitate |
| `array.filter` on the pool | selecting relevant examples per request |
| a shared formatter function | identical formatting for every example |
