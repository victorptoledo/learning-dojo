---
from: python
status: draft
concept: "From one application to a system"
from_way: "One Django or FastAPI process where a function call is cheap and the database is \"just there\"."
native_way: "Many processes on many machines, where each call to the database or another service is a network round trip with its own latency and failure modes."
sensei_tip: "A Gunicorn server with 4 sync workers and 250 ms requests serves at most about 16 req/s per instance (Little's law). Do that arithmetic before you choose the number of workers or instances."
---

| Python habit | System design view |
| --- | --- |
| `requests.get(url)` | a round trip that needs a timeout |
| one `uvicorn` process | N instances behind a load balancer |
| `timeit` | latency percentiles under load (p50, p99) |
| SQLite in development | a networked, replicated database in production |
| `functools.lru_cache` | a shared cache tier (Redis, Memcached) |
