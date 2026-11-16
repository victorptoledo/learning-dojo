---
from: go
status: draft
concept: "Networks, load balancers and bills are part of the design"
from_way: "net/http listens on :8080 and clients connect directly."
native_way: "Clients reach a load balancer by DNS; the load balancer health-checks your Pods and drains them during deploys, across zones."
sensei_tip: "Set http.Server timeouts and keep-alive limits; idle connections held by a load balancer behave differently from browser clients."
---

| Go world | Cloud |
| --- | --- |
| `ListenAndServe(":8080")` | a target behind a load balancer |
| `net.Resolver` | VPC DNS and private zones |
| `Server.Shutdown` | connection draining |
| `http.Transport` limits | connection reuse across NAT |
