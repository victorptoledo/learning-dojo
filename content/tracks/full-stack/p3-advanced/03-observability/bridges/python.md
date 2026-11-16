---
from: python
status: reviewed
concept: "OpenTelemetry and structured logs in Python"
from_way: "logging.info with f-strings, and print debugging in production."
native_way: "structlog or JSON formatters with the trace id, opentelemetry-instrument for Django, FastAPI and the database driver, and prometheus-client histograms."
sensei_tip: "opentelemetry-instrument python app.py instruments the framework, HTTP clients and psycopg without code changes; start there."
---

| Python | Observability |
| --- | --- |
| `logging.info(f"...")` | structured JSON with `trace_id` |
| `opentelemetry-instrument` | automatic spans and propagation |
| `prometheus_client.Histogram` | latency with buckets |
| Sentry SDK | errors with traces |
