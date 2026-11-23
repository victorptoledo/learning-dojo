The cluster's nodes are upgraded every week: each node is drained, and its Pods are evicted
and rescheduled. Make sure that never takes the shop down, and that when the cluster is full,
the right Pods win.

**Disruption budgets** (for example in `pdb.yaml`):

- `web`: a PodDisruptionBudget `web` with `minAvailable: 2`;
- `api`: a PodDisruptionBudget `api` with `maxUnavailable: 1`;
- both select their own Pods (and only them) and set `unhealthyPodEvictionPolicy:
  AlwaysAllow`, so a Pod that is already failing never blocks a drain;
- no budget may allow **zero** disruptions when its workload runs at its minimum scale:
  `minAvailable` equal to the replica count (or `maxUnavailable: 0`) blocks every drain
  forever, and the upgrade waits on a human. That includes `db`: a single-instance database
  is protected by its priority, its volume and its backups, not by a budget that forbids
  draining its node.

**Spreading.** `web` and `api` spread their Pods with two `topologySpreadConstraints`, each
with a `labelSelector` that matches the workload's own Pods and `matchLabelKeys:
[pod-template-hash]` (so the old and new Pods of a rollout are counted apart):

- across zones: `topologyKey: topology.kubernetes.io/zone`, `maxSkew: 1`,
  `whenUnsatisfiable: DoNotSchedule` (losing a zone loses at most a third);
- across nodes: `topologyKey: kubernetes.io/hostname`, `maxSkew: 1`,
  `whenUnsatisfiable: ScheduleAnyway` (a preference, so a small cluster can still schedule).

**Priority.** A PriorityClass `shop-critical` with `value: 100000`, `globalDefault: false`,
`preemptionPolicy: PreemptLowerPriority` and a `description`. `api` and `db` use it
(`priorityClassName`); `web` does not (its pages can wait a minute; orders cannot).

> [!TIP]
> `kubectl get pdb -n shop` shows `ALLOWED DISRUPTIONS`. If it says 0 while every Pod is
> healthy, the next drain will hang.
