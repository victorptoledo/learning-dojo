---
from: go
status: draft
concept: "An LLM feature is a service with one probabilistic dependency"
from_way: "Small services with explicit dependencies, an http.Client with timeouts and table tests with exact expectations."
native_way: "The same service, with a model client interface, structured output decoded and validated, and table-driven evals scored as pass rates."
sensei_tip: "Go's habits fit well: an interface for the model client, context.Context deadlines on every call, and table tests that grow into your golden set runner."
---

| Go habit | In an LLM feature |
| --- | --- |
| table tests with `want` | golden set cases scored as a pass rate |
| `json.Unmarshal` into a struct | decode, validate fields, then repair or retry |
| `context.WithTimeout` | a deadline on every model call |
| business rules in handlers | still in code; the model handles fuzzy language |
| provider SDKs (Anthropic, OpenAI) | thin clients; you own the loop, retries and checks |
