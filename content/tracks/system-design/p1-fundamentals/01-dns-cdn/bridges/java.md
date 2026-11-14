---
from: java
status: draft
concept: "Routing and caching at the edge"
from_way: "Picturing every request hitting the application server directly."
native_way: "Most static requests never reach the backend: CDN points of presence serve them, routed by anycast."
sensei_tip: "Set `Cache-Control` headers and invalidate the cache by tag or webhook to cut infrastructure cost."
---
