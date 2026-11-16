---
from: javascript
status: draft
concept: "A model call is a remote call with a richer result"
from_way: "await fetch(url).then(r => r.json()), checking r.ok, and EventSource for server-sent events."
native_way: "The official TypeScript SDKs (@anthropic-ai/sdk, openai) with typed responses, async-iterable streams and typed error classes."
sensei_tip: "EventSource only supports GET, so model streams are read with the SDK's async iterator or fetch plus a stream reader. Never call the API from the browser with your key; stream through your own backend route."
---

| JavaScript | Model API |
| --- | --- |
| `r.ok` | `stop_reason` and HTTP status both matter |
| `fetch` + `response.body.getReader()` | `for await (const event of stream)` |
| `EventSource` | SSE, consumed on the server |
| `process.env.KEY` | the key on the server only |
| msw or nock | a fake client with scripted responses |
