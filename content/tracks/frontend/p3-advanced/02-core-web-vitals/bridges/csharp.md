---
from: csharp
status: draft
concept: "User-facing SLOs"
from_way: "Application Insights request durations and dependency timings."
native_way: "Core Web Vitals add the client's view; Application Insights' browser SDK and similar RUM tools collect them."
sensei_tip: "Startup time of a desktop app is LCP; UI hangs are INP; controls that jump while loading are CLS."
---

| C# | In the browser |
| --- | --- |
| Request duration | TTFB, the first part of LCP |
| UI hang detection | Long tasks and INP |
| Layout jumping while data loads | CLS |
| Application Insights | RUM in the browser |
