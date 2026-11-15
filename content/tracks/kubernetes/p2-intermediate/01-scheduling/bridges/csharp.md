---
from: csharp
status: draft
concept: "Placement rules for replicas"
from_way: "Availability sets and zones are chosen per VM in Azure, and Windows workloads need Windows hosts."
native_way: "Pods choose nodes by labels (OS, zone, pool) and spread themselves with topology constraints."
sensei_tip: "In mixed AKS clusters, .NET Framework containers must land on Windows nodes: use nodeSelector kubernetes.io/os: windows, and the Windows pool is usually tainted."
---

| Azure / .NET world | Kubernetes |
| --- | --- |
| availability zones per VM | topology spread on `topology.kubernetes.io/zone` |
| a Windows Server host | `kubernetes.io/os: windows` |
| a dedicated VM for a service | a tainted node pool |
| update domains | a PodDisruptionBudget |
