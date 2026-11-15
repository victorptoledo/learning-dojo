---
from: javascript
status: draft
concept: "Containing failure"
from_way: "Error boundaries in the UI and try/catch around fetches."
native_way: "Error boundaries for the whole system: cells and shuffle shards for tenants, degraded modes, regions that can take over."
sensei_tip: "A React error boundary is graceful degradation in miniature: a broken widget renders a placeholder instead of taking down the page. Apply the same idea to services: a failing recommendation call returns an empty list."
---

| JavaScript | Resilience concept |
| --- | --- |
| React error boundaries | degrade one part, keep the page |
| `prom-client` histograms | SLIs |
| feature flags (OpenFeature JS SDK) | switching features off under stress |
| a tenant → cell map at the edge | cell routing |
| k6 load tests | checking SLOs at 2× load |
