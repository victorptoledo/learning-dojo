---
from: go
status: draft
concept: "Metrics, logs and traces tell you what you cannot see"
from_way: "`log.Printf` text logs and a Prometheus `/metrics` endpoint."
native_way: "`slog` JSON logs to CloudWatch Logs, metrics through EMF or the CloudWatch agent, traces via OpenTelemetry to X-Ray."
sensei_tip: "Use `slog.New(slog.NewJSONHandler(os.Stdout, nil))` and add the request id as an attribute; OpenTelemetry's Go SDK instruments aws-sdk-go-v2 with `otelaws`."
---

| Go world | AWS |
| --- | --- |
| `log.Printf` | `slog` JSON lines |
| Prometheus scrape | CloudWatch agent or Amazon Managed Prometheus |
| pprof in production | traces and metrics first, pprof on demand |
| Alertmanager | CloudWatch alarms to SNS |
