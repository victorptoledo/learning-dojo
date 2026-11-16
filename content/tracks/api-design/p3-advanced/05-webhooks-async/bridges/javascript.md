---
from: javascript
status: draft
concept: "Webhooks and asynchronous APIs"
from_way: "Express handlers that do all the processing before answering the webhook."
native_way: "Receivers that verify the signature over the raw body (`express.raw()`), store the event id, answer 200 and process from a queue (BullMQ)."
sensei_tip: "`express.json()` consumes and re-encodes the body; use `express.raw({ type: 'application/json' })` on the webhook route so the HMAC sees the original bytes."
---

| JavaScript / TypeScript | Async concept |
| --- | --- |
| `express.raw()` | raw body for verification |
| `crypto.createHmac('sha256', secret)` | signature |
| `crypto.timingSafeEqual` | constant-time verification |
| BullMQ | process after acknowledging |
| event id in a unique index | deduplication |
