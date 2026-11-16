---
from: python
status: draft
concept: "Context variables"
from_way: "contextvars.ContextVar follows asyncio tasks; structlog binds context to log lines; prometheus_client and OpenTelemetry instrument the app."
native_way: "AsyncLocalStorage follows async flows; pino's mixin or child loggers add context; prom-client and OpenTelemetry instrument the app."
sensei_tip: "AsyncLocalStorage is Node's ContextVar. Where structlog uses contextvars.bind_contextvars, a pino mixin reads the store on every line."
---

| Python | Node.js |
| --- | --- |
| `ContextVar("request_id")` | `new AsyncLocalStorage()` |
| `var.set(x)` / `var.get()` | `als.run(x, fn)` / `als.getStore()` |
| structlog | pino |
| `prometheus_client` | prom-client |
| `opentelemetry-instrument python app.py` | `node --import ./telemetry.ts app.ts` |
