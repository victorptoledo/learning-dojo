---
from: csharp
status: draft
concept: "The declarative reconciliation loop"
from_way: "A BackgroundService polls a database every minute and fixes what it finds, or a Windows service recovery setting restarts a crashed process."
native_way: "Each Kubernetes controller is a long-running loop that watches the API and closes the gap between spec and status."
sensei_tip: "The KubernetesClient library (and KubeOps) let you write controllers in C# as hosted services: a watch, a work queue and an idempotent reconcile method."
---

| .NET world | Kubernetes |
| --- | --- |
| an `IHostedService` loop | a controller |
| a SQL table of desired settings | objects stored in etcd |
| Windows service recovery | the kubelet restarting containers |
| Service Fabric cluster manager | the control plane |
