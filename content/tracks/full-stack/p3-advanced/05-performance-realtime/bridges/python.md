---
from: python
status: reviewed
concept: "Async Python for live updates"
from_way: "Polling an endpoint every second from the browser because WSGI cannot hold connections."
native_way: "ASGI (FastAPI, Starlette, Django Channels) for SSE and WebSockets, Redis pub/sub for fan-out, and asyncio.gather for parallel calls."
sensei_tip: "Django Channels uses a channel layer (usually Redis) exactly for the problem of the event happening on another worker."
---

| Python | Performance and real-time |
| --- | --- |
| `StreamingResponse` with `text/event-stream` | Server-Sent Events |
| Django Channels + Redis channel layer | WebSockets with fan-out |
| `asyncio.gather` | parallel calls |
| `select_related` / `prefetch_related` | fewer queries |
