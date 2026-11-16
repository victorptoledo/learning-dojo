---
from: python
status: draft
concept: "Reliability is a target with a budget"
from_way: "An uptime robot pings the home page every minute and emails when it fails."
native_way: "The SLI counts every real request; the SLO and its burn-rate alerts tell you when users suffer, not when one ping fails."
sensei_tip: "Define histogram buckets in prometheus_client to include your SLO threshold exactly (for example 0.3 s), or the SLI is an approximation."
---

| Python world | SRE |
| --- | --- |
| an uptime ping | a synthetic probe (one SLI source) |
| an error email per exception | a burn-rate alert |
| `Histogram(buckets=...)` | the latency SLI's threshold |
| a TODO to fix reliability | an error budget policy |
