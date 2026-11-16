---
from: javascript
status: draft
concept: "Running data in production"
from_way: "Uptime checks and error tracking for the web app."
native_way: "SLOs and alerts for the data behind dashboards, with incidents handled like outages."
sensei_tip: "Show data freshness in the product (\"updated at 06:12\") and a banner during incidents: users trust numbers more when they can see how fresh they are."
---

| JavaScript / TypeScript | Data reliability |
| --- | --- |
| Sentry / error tracking | pipeline failure alerts |
| an uptime check | a freshness check |
| a status page | communicating a data incident |
| snapshot tests | comparing model outputs |
| bundle size budgets | cost budgets per team |
