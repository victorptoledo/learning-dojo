---
from: javascript
status: draft
concept: "Tests become measurements"
from_way: "Jest or Vitest suites with expect, snapshot tests and CI checks."
native_way: "An eval run over a JSONL golden set that reports scores per tag and compares with a baseline report."
sensei_tip: "Snapshot tests are the wrong model for LLM output: the text changes every run. Snapshot the scores and the list of failing cases instead."
---

| JavaScript testing | LLM evaluation |
| --- | --- |
| `test.each(cases)` | golden cases |
| `expect(x).toBe(y)` | exact metric |
| snapshot of the output | a stored baseline report |
| coverage threshold | a score gate |
| `describe` blocks | slices |
