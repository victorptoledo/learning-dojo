---
from: java
status: draft
concept: "Declare the service, controllers keep it running"
from_way: "An application server cluster with a load balancer in front, and an admin console to redeploy WARs node by node."
native_way: "A Deployment rolls Pods node by node by itself, a Service load-balances over ready Pods, and probes hit Spring Boot's actuator health groups."
sensei_tip: "Map readinessProbe to /actuator/health/readiness and livenessProbe to /actuator/health/liveness; never put the database check in liveness."
---

| Java world | Kubernetes |
| --- | --- |
| a Tomcat cluster | a Deployment's replicas |
| the load balancer pool | a Service's endpoints |
| `/actuator/health` | readiness and liveness probes |
| `-Xmx` | memory limit (with MaxRAMPercentage) |
