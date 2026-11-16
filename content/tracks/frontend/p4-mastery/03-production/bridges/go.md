---
from: go
status: draft
concept: "Operating what you ship"
from_way: "Prometheus, structured logs with slog, health endpoints and rolling deploys."
native_way: "For the frontend: errors and Web Vitals sent from browsers with sendBeacon, synthetic journeys, static deploys to a CDN and flags."
sensei_tip: "A small Go endpoint that receives sendBeacon reports is a perfectly good RUM collector: the hard part is choosing percentiles and alerts."
---

| Go | In the browser |
| --- | --- |
| `slog` error logs | Browser error reports |
| Prometheus histograms | RUM percentiles of LCP and INP |
| Health endpoint | Synthetic Playwright journeys |
| `golang.org/x/text` for i18n | `Intl` |
