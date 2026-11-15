---
from: javascript
status: draft
concept: "Spreading load and owning keys"
from_way: "Node's cluster module sharing a port between workers, or PM2 running several processes."
native_way: "A balancer in front spreads requests across machines by health and load; keyed tiers use consistent hashing so changes move few keys."
sensei_tip: "Socket.IO and WebSocket servers keep connection state; long-lived connections defeat round robin over time. Balance on connect with least connections and rebalance by asking clients to reconnect gradually."
---

| JavaScript | Load balancing concept |
| --- | --- |
| `cluster` / PM2 instances | many backends behind one entry point |
| `ioredis` Cluster client | client-side key placement (hash slots) |
| `server.close()` on SIGTERM | draining before shutdown |
| `/healthz` route | instance health check |
| sticky sessions for Socket.IO | stateful balancing, best avoided |
