---
from: java
status: draft
concept: "See what the model saw"
from_way: "Micrometer metrics, structured logs with MDC and OpenTelemetry traces across services."
native_way: "The same stack, with model-call spans carrying gen_ai attributes (model, tokens), prompt versions and retrieval details, plus quality metrics from feedback and online evals."
sensei_tip: "Spring AI emits Micrometer observations for chat and embedding calls, including token usage; export them through OpenTelemetry and add your prompt version as a low-cardinality tag."
---

| Java | LLM observability |
| --- | --- |
| Micrometer `Timer` | latency per model and route |
| `Counter` | tokens and cost |
| MDC request id | conversation id in every log |
| OpenTelemetry Java agent | spans for HTTP, database and model calls |
| log masking | redaction of prompts |
