---
from: javascript
status: draft
concept: "From one server to many"
from_way: "Node's cluster module or pm2 instances on one host."
native_way: "Horizontal scaling across machines with stateless services."
sensei_tip: "pm2 cluster mode scales across cores; the same rules (no local state, sticky sessions avoided) let you scale across machines."
---

| JavaScript / TypeScript | In practice |
| --- | --- |
| pm2 -i max | instances behind a load balancer |
| in-memory store | Redis |
| socket.io sticky sessions | a shared adapter or a broker |
| bigger instance | more instances |
