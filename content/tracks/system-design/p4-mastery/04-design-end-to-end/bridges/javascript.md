---
from: javascript
status: draft
concept: "From components to a whole system"
from_way: "Building a front end and a Node API that talk over REST."
native_way: "Designing every hop from the device to storage: edge, connections, APIs, streams, data stores, and their failure modes."
sensei_tip: "Real-time features (chat, live tracking) mean long-lived WebSocket connections on stateful gateways; plan reconnection with jitter and resync by sequence number from day one."
---

| JavaScript habit | End-to-end design habit |
| --- | --- |
| `fetch` to one API | edge, gateways and services on the request path |
| Socket.IO rooms | stateful gateways and a presence registry |
| optimistic UI | explicit consistency choices per feature |
| Lighthouse and RUM | SLIs measured from real users |
| an architecture slide | ADRs and a design review checklist |
