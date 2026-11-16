---
from: csharp
status: draft
concept: "Reliability is a target with a budget"
from_way: "Azure Monitor alerts on every metric someone once found interesting."
native_way: "Two or three user-facing SLOs, with burn-rate alerts that page, and everything else as dashboards or tickets."
sensei_tip: "Application Insights availability tests are a good external SLI source; combine them with request success rates from inside."
---

| C# world | SRE |
| --- | --- |
| a metric alert per counter | symptom-based alerts |
| availability tests | a synthetic SLI |
| action groups | Alertmanager routes |
| a support SLA | the contract behind the SLO |
