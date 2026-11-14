---
from: java
status: draft
concept: "Reliability as a number"
from_way: "Uptime judged by whether anyone complained, alerts on every CPU spike, and incidents debugged live in production before anyone rolled back."
native_way: "SLIs and SLOs per user journey, an error budget that decides priorities, burn-rate alerts on symptoms, mitigation first, and blameless postmortems."
sensei_tip: "Write down one SLO for your most important endpoint this week; the conversation it starts is worth more than any dashboard."
---

| Before | SRE |
| --- | --- |
| "The server is up" | 99.9% of checkout requests succeed over 30 days |
| Average response time | p95 and p99 latency |
| Alert on CPU > 80% | alert on error budget burn rate |
| "Who broke it?" | what made the mistake easy, and who owns the fix |
