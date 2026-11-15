---
from: javascript
status: draft
concept: "Placement rules for replicas"
from_way: "A PaaS chooses machines for you, and you pick a region in a dashboard."
native_way: "In Kubernetes you can state placement in the Pod spec: zones, node types, and which Pods must not share a node."
sensei_tip: "For a Next.js or API service, spread replicas over zones with maxSkew 1 and add a PDB; that alone survives most cloud zone incidents and node upgrades."
---

| PaaS habit | Kubernetes |
| --- | --- |
| choosing a region | node labels `topology.kubernetes.io/region` and `zone` |
| "at least 2 instances" | replicas + a PodDisruptionBudget |
| a machine size per service | requests plus affinity to an instance type |
| automatic placement | the default scheduler scoring |
