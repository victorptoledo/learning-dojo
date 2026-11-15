---
from: python
status: draft
concept: "Placement rules for replicas"
from_way: "Celery workers for heavy tasks run on bigger machines chosen in the Ansible inventory."
native_way: "Node labels and affinity send heavy workers to big nodes, and taints keep everything else off them."
sensei_tip: "Give model-serving or pandas-heavy workers their own node pool with a taint, so a memory spike cannot evict the web tier."
---

| Python world | Kubernetes |
| --- | --- |
| an Ansible inventory group `[gpu]` | node labels such as `pool: gpu` |
| `celery -Q heavy` on big hosts | affinity + tolerations for the heavy workers |
| two web hosts in different datacentres | topology spread constraints |
| a planned reboot of hosts | a drain that respects a PodDisruptionBudget |
