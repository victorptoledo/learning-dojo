---
from: python
status: draft
concept: "An LLM feature is a service with one probabilistic dependency"
from_way: "Scripts and services where a function call returns the same value every time, tested with pytest asserts."
native_way: "The model call sits in the middle of ordinary Python: you build its context, validate its output with types, and score its quality over a set of examples."
sensei_tip: "Most of an AI feature is plain Python: data loading, validation, retries, logging. Keep the model call small and behind a function you can replace with a fake in tests, as the katas do."
---

| Python habit | In an LLM feature |
| --- | --- |
| `assert result == expected` | a pass rate over a golden set |
| `json.loads` on an API response | parse, validate against a schema, then repair or retry |
| `requests.post(...)` | a model SDK call with tokens, stop reasons and rate limits |
| notebooks for exploration | notebooks to explore, but evals and pipelines in tested modules |
| LangChain, LlamaIndex | optional helpers; learn the parts underneath first |
