---
from: java
status: draft
concept: "Non-blocking operations"
from_way: "Sending emails and building reports inside the same synchronous HTTP request."
native_way: "The endpoint enqueues a job in milliseconds and answers `202 Accepted`; background workers do the work without blocking the API."
sensei_tip: "Make background jobs idempotent: if a message is delivered twice, the end result must be the same."
---
