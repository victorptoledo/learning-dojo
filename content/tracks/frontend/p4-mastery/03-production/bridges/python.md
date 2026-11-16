---
from: python
status: draft
concept: "Operating what you ship"
from_way: "Sentry for Django, Prometheus metrics, gunicorn rolling restarts."
native_way: "Sentry's browser SDK, RUM, Baseline-based support policies and CDN deploys cover the client side."
sensei_tip: "The same Sentry project can take browser errors: tag both with the release and you can follow an incident across server and client."
---

| Python | In the browser |
| --- | --- |
| `sentry_sdk.init()` | Sentry browser SDK with source maps |
| Prometheus histograms | RUM percentiles |
| Rolling restart | Atomic CDN deploy |
| `gettext` and Babel | `Intl` and translation catalogs |
