---
from: javascript
status: draft
concept: "Automatic elasticity"
from_way: "A serverless platform scales functions per request without you configuring anything."
native_way: "In Kubernetes you choose the signal: the HPA on CPU or requests per second, KEDA on events, with explicit minimum and maximum replicas."
sensei_tip: "Node.js is single-threaded, so one Pod rarely uses more than one core: request about one CPU or less, and let the HPA add Pods rather than raising limits."
---

| Node.js / serverless world | Kubernetes |
| --- | --- |
| automatic per-request scaling | HPA or KEDA with a chosen metric |
| scale to zero | KEDA `minReplicaCount: 0` |
| cold starts | Pod start and readiness time |
| concurrency limits | `maxReplicas` |
