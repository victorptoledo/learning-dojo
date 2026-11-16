---
from: javascript
status: draft
concept: "Metrics, logs and traces tell you what you cannot see"
from_way: "`console.log` strings and an APM agent in production."
native_way: "JSON logs to CloudWatch Logs, metrics and alarms in CloudWatch, traces through X-Ray or OpenTelemetry."
sensei_tip: "Lambda's Node.js runtime supports JSON log format natively (`LoggingConfig: { LogFormat: JSON }`), so `console.log` objects become searchable fields."
---

| Node.js world | AWS |
| --- | --- |
| `console.log("user", id)` | a structured JSON log entry |
| pino with a transport | pino to stdout, collected by CloudWatch |
| an APM dashboard | CloudWatch dashboards and Application Signals |
| prom-client | EMF or `PutMetricData` |
