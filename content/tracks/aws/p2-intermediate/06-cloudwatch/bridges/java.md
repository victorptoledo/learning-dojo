---
from: java
status: draft
concept: "Metrics, logs and traces tell you what you cannot see"
from_way: "Micrometer metrics go to Prometheus, Logback writes text logs to files, and a Zipkin server collects traces."
native_way: "CloudWatch collects metrics and logs; X-Ray or OpenTelemetry traces requests; alarms page on-call through SNS."
sensei_tip: "Log JSON with logstash-logback-encoder, export metrics with Micrometer's CloudWatch registry or EMF, and trace with the ADOT Java agent instead of hand-written spans."
---

| Java world | AWS |
| --- | --- |
| Micrometer timers | CloudWatch metrics with percentiles |
| Logback file appender | stdout to CloudWatch Logs |
| Zipkin or Jaeger | X-Ray via OpenTelemetry |
| Grafana alert rules | CloudWatch alarms to SNS |
