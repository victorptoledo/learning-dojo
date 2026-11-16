---
from: go
status: draft
concept: "User-facing SLOs"
from_way: "Histograms of handler latency and percentiles in Prometheus."
native_way: "The same idea measured in the browser: p75 of LCP, INP and CLS per page and device type."
sensei_tip: "A fast Go handler is only TTFB: the user still waits for the image, the scripts and the main thread."
---

| Go | In the browser |
| --- | --- |
| `http.Handler` latency histogram | LCP and its TTFB part |
| pprof CPU profile | The Performance panel |
| Prometheus alert on p99 | An alert on p75 INP |
| Synthetic checks | Lighthouse in CI |
