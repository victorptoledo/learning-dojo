---
from: python
status: draft
concept: "Tests become measurements"
from_way: "pytest tests with assert, parametrize and a green or red run."
native_way: "An eval harness that runs every golden case, scores it with several metrics, aggregates per tag and compares with a stored baseline report."
sensei_tip: "Keep the harness in pytest if you like (one test asserting the gate), but store the full report as JSON so you can diff runs and inspect every regression."
---

| pytest | LLM evaluation |
| --- | --- |
| `@pytest.mark.parametrize` | golden cases |
| `assert x == y` | exact match |
| markers | tags and slices |
| `--lf` (last failed) | regressions against the baseline |
| a CI job failing on red | a gate on scores |
