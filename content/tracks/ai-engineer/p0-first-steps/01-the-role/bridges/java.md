---
from: java
status: draft
concept: "An LLM feature is a service with one probabilistic dependency"
from_way: "Deterministic services: a Spring controller calls repositories and other services, and JUnit asserts exact results."
native_way: "The same service shape, but one dependency (the model) returns varied, sometimes wrong text, so you add validation, evaluation sets and guardrails around it."
sensei_tip: "Treat the model client like any remote dependency behind an interface: timeouts, retries, a fake for tests. The new part is measuring quality over many examples instead of asserting one answer."
---

| Java habit | In an LLM feature |
| --- | --- |
| `assertEquals(expected, actual)` | a golden set scored as a pass rate |
| a typed DTO from a REST call | structured output validated against a schema |
| business rules in services | still in code; the model handles only the fuzzy language part |
| a `RestClient` call | a model API call, billed per token |
| Spring AI, LangChain4j | frameworks over the same parts: model, context, tools, evals |
