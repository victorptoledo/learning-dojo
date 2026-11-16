---
from: python
status: draft
concept: "Emit metrics, structured logs and traces from the service"
from_way: "logging.info with f-strings and the odd print, read with grep on the server."
native_way: "prometheus_client exposes metrics, structlog writes JSON events, and opentelemetry-instrument traces Flask, Django, FastAPI, requests and SQLAlchemy."
sensei_tip: "With gunicorn workers, prometheus_client needs multiprocess mode, or each scrape sees one worker's numbers."
---

| Python world | Observability |
| --- | --- |
| `logging` with f-strings | structured logs (structlog) |
| a counter in a global dict | a Prometheus Counter |
| `time.time()` around a call | a histogram or a span |
| Sentry | errors plus traces |
