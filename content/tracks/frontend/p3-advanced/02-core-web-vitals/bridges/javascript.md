---
from: javascript
status: draft
concept: "Measuring in the browser"
from_way: "performance.now() and console.time around code in Node.js."
native_way: "PerformanceObserver gives standardized entries: largest-contentful-paint, event, layout-shift and longtask."
sensei_tip: "Do not reinvent the metrics: the web-vitals library implements the official definitions, edge cases included."
---

| JavaScript / Node.js | In the browser |
| --- | --- |
| `console.time(label)` | `performance.mark()` / `measure()` |
| `process.hrtime()` | `performance.now()` |
| Benchmarking a function | Watching INP of real interactions |
| Log shipping | `navigator.sendBeacon` to a RUM endpoint |
