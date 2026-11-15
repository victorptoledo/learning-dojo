---
from: java
status: draft
concept: "Containing failure"
from_way: "Making one Spring Boot service robust with retries, circuit breakers and health checks."
native_way: "Making the whole system contain failure: SLOs and error budgets, zones and cells, shuffle sharding, multi-region recovery and drills."
sensei_tip: "Micrometer timers with SLO buckets (Timer.builder(...).serviceLevelObjectives(Duration.ofMillis(300))) give you the SLI directly: the share of requests under the threshold, ready for burn-rate alerts."
---

| Java / Spring | Resilience concept |
| --- | --- |
| Micrometer `serviceLevelObjectives(...)` | an SLI histogram |
| Spring Cloud / feature flags (OpenFeature Java SDK) | graceful degradation switches |
| a tenant id → cell lookup in a filter | cell routing |
| Resilience4j `Bulkhead` per tenant | isolation inside one service |
| Chaos Monkey for Spring Boot | failure injection for drills |
