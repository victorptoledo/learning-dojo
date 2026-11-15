---
from: csharp
status: draft
concept: "Reliability as a number"
from_way: "Application Insights or OpenTelemetry with dashboards and threshold alerts."
native_way: "SLOs and error budgets on top of the telemetry."
sensei_tip: "Application Insights availability and request metrics are SLIs once you define good versus total; set the SLO and alert on the burn rate."
---

| C# | In practice |
| --- | --- |
| Application Insights requests | an availability or latency SLI |
| threshold alerts | burn-rate alerts |
| System.Diagnostics.Metrics histograms | latency distributions |
| incident tickets | blameless postmortems |
