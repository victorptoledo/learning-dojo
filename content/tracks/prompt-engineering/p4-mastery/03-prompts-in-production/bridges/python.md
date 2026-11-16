---
from: python
status: draft
concept: "Resilient calls and structured logs"
from_way: "requests calls with no timeout, print debugging, and exceptions as the only failure signal."
native_way: "Model calls with timeouts, bounded retries, fallbacks and guardrails, and a structured log record per call with prompt version, model, tokens, stop reason and latency."
sensei_tip: "Emit one JSON log line per model call with the same keys every time; then a single query answers 'what changed this morning' by grouping on model and prompt_version."
---

| Python | Production concept |
| --- | --- |
| `httpx` with `timeout=` | timeouts on model calls |
| `tenacity` retries with a stop condition | bounded retries |
| `logging` with a JSON formatter | one structured record per call |
| `functools.lru_cache` on answers | caching repeated requests |
| masking functions before logging | privacy in prompt logs |
