---
from: javascript
status: draft
concept: "From one application to a system"
from_way: "One Node.js process with an event loop; an await feels instant on localhost."
native_way: "Many instances across zones; every await on the network has a latency distribution, and the slowest dependency sets your tail latency."
sensei_tip: "Open the browser's Network tab on a site far away: DNS, TLS and the first byte often cost more than the page itself. That is why the edge (CDN) comes first in this track."
---

| JavaScript habit | System design view |
| --- | --- |
| `await fetch(url)` | a round trip: set `AbortSignal.timeout(ms)` |
| one Express server | many instances behind a load balancer |
| Lighthouse score | latency budget per component |
| `localStorage` | client-side caching, the first cache layer |
| `npm start` on a laptop | a deployment across zones and regions |
