---
from: javascript
status: draft
concept: "See what the model saw"
from_way: "Front-end analytics, console logs and APM tools for Node."
native_way: "Server-side traces of every model call with tokens, cost and prompt version, linked to the feedback buttons in the UI."
sensei_tip: "The Vercel AI SDK and several Node model SDKs emit OpenTelemetry spans; send the user's thumbs up or down to the server with the trace id so feedback lands on the right request."
---

| JavaScript | LLM observability |
| --- | --- |
| analytics events | feedback tied to a trace id |
| `@opentelemetry/api` | spans for model and tool calls |
| APM (Datadog, New Relic) | latency and error dashboards |
| `console.log` | structured, redacted server logs |
| A/B flags | comparing prompt versions |
