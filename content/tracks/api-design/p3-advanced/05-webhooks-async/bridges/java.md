---
from: java
status: draft
concept: "Webhooks and asynchronous APIs"
from_way: "A synchronous `restTemplate.postForObject(customerUrl, event)` inside the business transaction."
native_way: "Events written to an outbox table in the same transaction, a worker that delivers them with retries and HMAC signatures, and `202 Accepted` resources for long jobs."
sensei_tip: "Compute the signature with `javax.crypto.Mac` (`HmacSHA256`) over the raw body bytes; and verify with `MessageDigest.isEqual`, which compares in constant time."
---

| Java / Spring | Async concept |
| --- | --- |
| outbox table + scheduled worker | reliable delivery |
| `Mac.getInstance("HmacSHA256")` | signature |
| `MessageDigest.isEqual` | constant-time verification |
| `ResponseEntity.accepted().location(uri)` | 202 for long jobs |
| Spring Cloud Stream / Kafka | internal events |
