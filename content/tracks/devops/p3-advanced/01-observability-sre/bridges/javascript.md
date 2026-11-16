---
from: javascript
status: draft
concept: "Emit metrics, structured logs and traces from the service"
from_way: "console.log in the browser devtools, and the Network tab's waterfall."
native_way: "pino writes JSON logs, prom-client exposes metrics, and OpenTelemetry's Node.js SDK draws the same waterfall across services: a trace."
sensei_tip: "Load the OpenTelemetry SDK before anything else (node --import) or auto-instrumentation misses the modules already loaded."
---

| JavaScript world | Observability |
| --- | --- |
| `console.log` | pino JSON logs |
| the Network waterfall | a distributed trace |
| Web Vitals | latency percentiles |
| `performance.now()` | spans and histograms |
