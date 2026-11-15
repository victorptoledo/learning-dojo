---
from: java
status: draft
concept: "An edge proxy configured by objects"
from_way: "Spring Cloud Gateway or Zuul routes defined in application.yaml, deployed and scaled as one more service."
native_way: "An Ingress controller or a Gateway API implementation is the edge proxy; routes are Kubernetes objects each team applies next to its Service."
sensei_tip: "Behind a proxy, set server.forward-headers-strategy=framework so Spring builds redirects and links from X-Forwarded-Proto and Host."
---

| Java world | Kubernetes |
| --- | --- |
| Spring Cloud Gateway routes | HTTPRoute rules |
| a route predicate `Path=/api/**` | `PathPrefix /api` |
| a weight predicate for canaries | weighted `backendRefs` |
| a keystore for TLS | a `kubernetes.io/tls` Secret |
