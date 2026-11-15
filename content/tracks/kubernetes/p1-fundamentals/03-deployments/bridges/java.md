---
from: java
status: draft
concept: "Rolling, revisioned releases"
from_way: "A release script takes servers out of the load balancer one by one, deploys the new WAR and puts them back."
native_way: "A Deployment does the same with Pods: a new ReplicaSet grows while the old one shrinks, paced by readiness, and old revisions stay for rollback."
sensei_tip: "JVM start-up makes readiness essential: point the readiness probe at Spring Boot's /actuator/health/readiness so traffic waits for the context to load."
---

| Java world | Kubernetes |
| --- | --- |
| a release version in Maven | an image tag in the Pod template |
| the previous WAR kept on disk | an old ReplicaSet (`revisionHistoryLimit`) |
| draining a node from the load balancer | `maxUnavailable` and readiness |
| redeploying the previous version | `kubectl rollout undo` |
