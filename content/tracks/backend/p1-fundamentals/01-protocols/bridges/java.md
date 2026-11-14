---
from: java
status: draft
concept: "Client-server communication"
from_way: "Writing framework route handlers without looking at the HTTP messages underneath: 200 for everything, state changed in GET handlers, no caching headers."
native_way: "Methods chosen by safety and idempotency, status codes as part of the contract, Cache-Control and ETags for caches and lost-update protection, HTTP/2 and HTTP/3 underneath the same semantics."
sensei_tip: "Use WebSockets only for true two-way real time; for one-way server notifications, Server-Sent Events are simpler and more reliable."
---

| Habit | HTTP-aware |
| --- | --- |
| `GET /orders/42/cancel` | `POST /orders/42/cancellation` |
| `200 {"error": "not found"}` | `404` with a Problem Details body |
| Always fetching the full article | `If-None-Match` → `304` |
| Last write wins | `If-Match` → `412` on a stale update |
