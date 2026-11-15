---
from: java
status: draft
concept: "Placement rules for replicas"
from_way: "An operations team decides which application server gets which application, and keeps two instances in different racks by hand."
native_way: "Pods declare placement rules (affinity, spread, tolerations) and the scheduler applies them to every replica, every time."
sensei_tip: "JVM services with large heaps need memory requests close to real usage; otherwise the scheduler packs too many on one node and they get OOM killed under load."
---

| Java world | Kubernetes |
| --- | --- |
| a cluster of app servers in two racks | topology spread across zones |
| a dedicated server for batch jobs | a tainted node pool plus tolerations and affinity |
| "never both instances on one host" | Pod anti-affinity on `kubernetes.io/hostname` |
| a maintenance window plan | a PodDisruptionBudget |
