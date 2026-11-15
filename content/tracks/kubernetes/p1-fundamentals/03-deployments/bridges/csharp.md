---
from: csharp
status: draft
concept: "Rolling, revisioned releases"
from_way: "An Azure DevOps release pipeline swaps a staging slot into production, or updates a scale set instance by instance."
native_way: "A Deployment rolls Pods from the old ReplicaSet to a new one, paced by readiness, and can undo by scaling the old one back."
sensei_tip: "Map ASP.NET Core health checks to /health/ready and /health/live with tags so the readiness probe can include dependencies and the liveness probe does not."
---

| .NET world | Kubernetes |
| --- | --- |
| a VM scale set rolling upgrade | a rolling update |
| a slot swap | blue-green with two Deployments and a Service switch |
| the previous release in the pipeline | `rollout history` |
| a manual approval gate | `kubectl rollout pause` |
