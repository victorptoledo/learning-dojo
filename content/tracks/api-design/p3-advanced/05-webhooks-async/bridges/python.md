---
from: python
status: draft
concept: "Webhooks and asynchronous APIs"
from_way: "Calling the customer's URL from the request handler with `requests.post` and no timeout."
native_way: "A task queue (Celery, RQ, Dramatiq) delivering signed webhooks with retries; receivers verify with `hmac.compare_digest` and enqueue the work."
sensei_tip: "Verify the signature over the **raw** request body (`await request.body()`), not over re-serialised JSON: key order and spacing change the bytes."
---

| Python | Async concept |
| --- | --- |
| Celery task with `autoretry_for` and backoff | delivery with retries |
| `hmac.new(secret, msg, sha256)` | signature |
| `hmac.compare_digest` | constant-time verification |
| `status_code=202` + job resource | long-running operation |
| `standardwebhooks` package | reference implementation |
