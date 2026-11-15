---
from: python
status: draft
concept: "Service discovery by name"
from_way: "Hosts and ports in a settings file, or a Consul lookup, and an nginx upstream block listing the workers."
native_way: "A Service gives a stable DNS name; the set of Pods behind it is updated automatically from labels and readiness."
sensei_tip: "Keep connection pools short-lived or use HTTP keep-alive limits: kube-proxy balances connections, not requests, so one long-lived connection always hits the same Pod."
---

| Python world | Kubernetes |
| --- | --- |
| `PAYMENTS_HOST=10.0.3.7` | `http://payments.billing` |
| nginx `upstream { server ...; }` | EndpointSlices |
| Consul service registration | labels and a selector |
| `requests.Session()` pooling | connection-level balancing to keep in mind |
