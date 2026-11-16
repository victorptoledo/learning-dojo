---
from: javascript
status: draft
concept: "Pay per token, wait per token"
from_way: "Bundle size and render time, with CDN caching for repeated requests."
native_way: "Token budgets per call, prefix caching, cheaper models for easy work, streaming for perceived speed and retries with backoff and jitter."
sensei_tip: "Libraries like p-limit and p-retry cover concurrency and backoff in Node; for the browser, stream through your server so the key and the rate limiting stay on your side."
---

| JavaScript | Cost and latency |
| --- | --- |
| CDN or HTTP cache | answer caching |
| `p-retry` | backoff with jitter |
| `p-limit` | concurrency limit |
| `Promise.all` | parallel calls |
| streaming responses | perceived latency |
