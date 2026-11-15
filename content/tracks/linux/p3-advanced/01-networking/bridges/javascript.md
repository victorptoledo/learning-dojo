---
from: javascript
status: draft
concept: "Connecting to other services"
from_way: "fetch or axios errors such as ECONNREFUSED, ETIMEDOUT and ENOTFOUND in Node."
native_way: "Each Node error code maps onto one layer you can test from the shell."
sensei_tip: "ENOTFOUND is DNS, ECONNREFUSED is nothing listening, ETIMEDOUT is dropped packets. A server that listens on localhost only answers on 127.0.0.1: bind to 0.0.0.0 in containers."
---

| JavaScript / TypeScript | Linux |
| --- | --- |
| ENOTFOUND | getent hosts / dig |
| ECONNREFUSED | ss -tlnp on the server |
| ETIMEDOUT | a firewall or security group |
| server.listen(3000, 'localhost') | 127.0.0.1:3000 in ss |
| DevTools Network tab | curl -v |
