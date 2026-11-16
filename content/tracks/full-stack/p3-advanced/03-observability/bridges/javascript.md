---
from: javascript
status: reviewed
concept: "Seeing both the browser and Node.js"
from_way: "console.log on the server and console.error in the browser, neither collected anywhere."
native_way: "pino JSON logs with the trace id, the OpenTelemetry Node.js SDK for spans and metrics, OpenTelemetry or Sentry in the browser, and web-vitals for real-user metrics."
sensei_tip: "AsyncLocalStorage lets a Node.js logger find the current trace id without passing it through every function."
---

| JavaScript | Observability |
| --- | --- |
| `console.log` | pino or OpenTelemetry logs in JSON |
| `@opentelemetry/sdk-node` | spans with `traceparent` propagation |
| `window.onerror` | error reporting with source maps |
| `web-vitals` + `sendBeacon` | real-user monitoring |
