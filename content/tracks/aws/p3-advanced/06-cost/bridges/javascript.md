---
from: javascript
status: draft
concept: "Architecture is cost: measure it, attribute it, optimise it"
from_way: "A Vercel or Heroku plan with a fixed tier per month."
native_way: "Every request, GB and hour is billed; architecture choices like NAT gateways and data transfer show up directly."
sensei_tip: "Bundle and tree-shake Lambda code and pick arm64: smaller packages start faster and Graviton Lambda costs about 20 % less per GB-second."
---

| Node.js world | AWS |
| --- | --- |
| a hosting plan tier | Budgets on actual and forecast spend |
| bandwidth limits | data transfer charges |
| always-on dynos | functions and tasks that scale to zero |
| a CDN add-on | CloudFront cutting origin transfer |
