---
from: java
status: draft
concept: "Operating what you ship"
from_way: "Spring Boot Actuator, Micrometer metrics, centralized logging, blue-green deploys and feature toggles like Unleash."
native_way: "The frontend's equivalents: error tracking with source maps, RUM of Web Vitals, synthetic journeys, atomic deploys and feature flags in the client."
sensei_tip: "Your SRE habits transfer: SLOs, alerts on error budgets, canary releases. The difference is that the 'servers' are your users' browsers."
---

| Java | In the browser |
| --- | --- |
| Actuator health checks | Synthetic journeys |
| Micrometer metrics | RUM with the web-vitals library |
| Logback error logs | `window` error and unhandledrejection reports |
| Unleash / Togglz | Frontend feature flags |
