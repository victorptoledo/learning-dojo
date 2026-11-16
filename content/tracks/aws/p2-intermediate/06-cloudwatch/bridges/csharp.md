---
from: csharp
status: draft
concept: "Metrics, logs and traces tell you what you cannot see"
from_way: "Serilog to files or Seq, and Application Insights for metrics and traces."
native_way: "CloudWatch Logs for structured logs, CloudWatch metrics and alarms, X-Ray or OpenTelemetry for traces."
sensei_tip: "Use Serilog's JSON formatter to stdout and the OpenTelemetry .NET SDK with the AWS exporters; `System.Diagnostics.Metrics` maps cleanly to CloudWatch through ADOT."
---

| .NET world | AWS |
| --- | --- |
| Application Insights | CloudWatch and X-Ray |
| Serilog sinks | stdout to CloudWatch Logs |
| KQL queries | Logs Insights queries |
| Azure Monitor alerts | CloudWatch alarms |
