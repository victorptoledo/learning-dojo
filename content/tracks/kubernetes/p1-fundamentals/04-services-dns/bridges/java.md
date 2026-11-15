---
from: java
status: draft
concept: "Service discovery by name"
from_way: "Spring Cloud with Eureka: every instance registers itself and clients ask the registry and balance with a client-side load balancer."
native_way: "The platform does discovery: a Service name resolves in DNS and kube-proxy balances connections over the ready Pods; the app only needs a URL."
sensei_tip: "On Kubernetes you can drop Eureka and Ribbon-style client balancing: call http://orders.shop and let the Service do it. JVM DNS caching (networkaddress.cache.ttl) is harmless here because the Service IP never changes."
---

| Spring Cloud | Kubernetes |
| --- | --- |
| Eureka registry | EndpointSlices maintained by a controller |
| `@LoadBalanced RestTemplate` | the Service virtual IP |
| instance health in Eureka | readiness of each Pod |
| `lb://orders` | `http://orders.shop` |
