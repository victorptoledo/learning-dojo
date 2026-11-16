---
from: javascript
status: draft
concept: "Reliability is a target with a budget"
from_way: "Lighthouse scores and Core Web Vitals thresholds (LCP under 2.5 s for 75% of visits)."
native_way: "That is already an SLO: a good-event threshold and a target percentage. Back-end services get the same, with budgets and burn-rate alerts."
sensei_tip: "Core Web Vitals use the 75th percentile of real users; pick your SLO percentile from what users feel, not what is easy to measure."
---

| JavaScript world | SRE |
| --- | --- |
| LCP under 2.5 s | a latency SLI threshold |
| 75% of page loads | an SLO target |
| real user monitoring | measuring close to the user |
| a performance budget | an error budget |
