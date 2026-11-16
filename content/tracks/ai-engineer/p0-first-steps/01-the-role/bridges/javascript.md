---
from: javascript
status: draft
concept: "An LLM feature is a service with one probabilistic dependency"
from_way: "Front ends and Node services where fetch returns predictable JSON and tests assert exact values."
native_way: "A Node or browser feature that streams model output to the user, validates structured results with a schema and measures quality with example sets."
sensei_tip: "Your UI skills matter here: streaming text, showing sources, letting users correct or reject an answer. Good AI features are designed for the model being wrong sometimes."
---

| JavaScript habit | In an LLM feature |
| --- | --- |
| `expect(x).toBe(y)` | a pass rate over a golden set |
| zod-validated `fetch` JSON | structured output validated with zod, then repaired or retried |
| a spinner while loading | streaming tokens as they arrive |
| business logic in the client | exact rules stay in code; the model handles fuzzy language |
| Vercel AI SDK, LangChain.js | helpers over the same parts: model, context, tools, evals |
