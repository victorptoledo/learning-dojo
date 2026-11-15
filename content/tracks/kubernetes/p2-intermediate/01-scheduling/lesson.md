# Scheduling: Node Selection, Affinity, Taints and Topology Spread

The scheduler decides where every Pod runs. Most of the time its defaults are fine. When they
are not (GPU nodes, zones, noisy neighbours, critical services) you steer it with a few
fields in the Pod spec and on the nodes.

## How the scheduler decides

For each Pod without a node, the scheduler runs two phases:

1. **Filter**: drop nodes that cannot run the Pod: not enough free requested CPU or memory,
   a `nodeSelector` or required affinity that does not match, a taint the Pod does not
   tolerate, a port conflict, a volume in another zone.
2. **Score**: rank the remaining nodes (spread replicas, balance usage, prefer nodes with the
   image, preferred affinities) and bind the Pod to the best one.

When no node passes the filter, the Pod stays **Pending** and an event explains why:

```text
0/6 nodes are available: 3 Insufficient memory, 3 node(s) had untolerated taint {gpu: true}.
```

`kubectl describe pod` shows it; the cluster autoscaler reads the same signal to add nodes.

## nodeSelector and node affinity

Nodes carry labels: well-known ones such as `kubernetes.io/hostname`,
`kubernetes.io/arch`, `topology.kubernetes.io/zone` and `node.kubernetes.io/instance-type`,
plus your own (`pool: gpu`).

```yaml
spec:
  nodeSelector:
    kubernetes.io/arch: arm64          # simple, hard requirement
  affinity:
    nodeAffinity:
      requiredDuringSchedulingIgnoredDuringExecution:
        nodeSelectorTerms:
          - matchExpressions:
              - { key: pool, operator: In, values: [general, burst] }
      preferredDuringSchedulingIgnoredDuringExecution:
        - weight: 50
          preference:
            matchExpressions:
              - { key: node.kubernetes.io/instance-type, operator: In, values: [m7g.large] }
```

**Required** rules filter (and can leave a Pod Pending); **preferred** rules only add score.
"IgnoredDuringExecution" means a running Pod is not evicted if node labels change later.

## Pod affinity and anti-affinity

Rules relative to **other Pods**: "run near the cache" (affinity) or "never two replicas on
one node" (anti-affinity), within a **topology key** such as a hostname or a zone.

```yaml
  affinity:
    podAntiAffinity:
      preferredDuringSchedulingIgnoredDuringExecution:
        - weight: 100
          podAffinityTerm:
            topologyKey: kubernetes.io/hostname
            labelSelector:
              matchLabels: { app: api }
```

Required anti-affinity with more replicas than nodes leaves the extra Pods Pending; prefer
the preferred form or topology spread for replicas.

## Topology spread constraints

The modern, precise way to spread replicas:

```yaml
  topologySpreadConstraints:
    - maxSkew: 1
      topologyKey: topology.kubernetes.io/zone
      whenUnsatisfiable: DoNotSchedule      # hard; ScheduleAnyway is soft
      labelSelector:
        matchLabels: { app: api }
    - maxSkew: 1
      topologyKey: kubernetes.io/hostname
      whenUnsatisfiable: ScheduleAnyway
      labelSelector:
        matchLabels: { app: api }
```

`maxSkew: 1` means the replica counts of any two zones differ by at most one: 6 replicas in
3 zones become 2/2/2, so losing a zone loses a third of the capacity, not all of it.

## Taints and tolerations

Affinity attracts Pods to nodes; **taints** repel them. A taint on a node keeps away every
Pod that does not **tolerate** it:

```powershell
kubectl taint nodes gpu-1 gpu=true:NoSchedule
```

```yaml
  tolerations:
    - { key: gpu, operator: Equal, value: "true", effect: NoSchedule }
```

Effects: `NoSchedule` (new Pods stay away), `PreferNoSchedule` (soft), `NoExecute` (running
Pods are evicted too, optionally after `tolerationSeconds`). A toleration **allows** a Pod on
a tainted node but does not send it there: combine it with affinity or a selector for
dedicated pools. Kubernetes itself taints nodes, for example
`node.kubernetes.io/not-ready:NoExecute` when a node stops reporting.

## Priority, preemption and disruption budgets

A **PriorityClass** gives Pods a priority; when a high-priority Pod cannot be scheduled, the
scheduler may **preempt** (evict) lower-priority Pods to make room. System components use
`system-cluster-critical`. Use priorities sparingly.

Voluntary disruptions (a node drain for an upgrade, an autoscaler removing a node) respect a
**PodDisruptionBudget**:

```yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: api
spec:
  minAvailable: 4          # or maxUnavailable: 1
  selector:
    matchLabels: { app: api }
```

`kubectl drain` evicts Pods one by one and waits whenever an eviction would break the
budget. A PDB does not protect against crashes or node failures, and a budget that allows
zero disruptions blocks upgrades forever.

## Summary

- The scheduler filters nodes that cannot run a Pod, then scores the rest; Pending events
  say which filter failed.
- nodeSelector and node affinity choose nodes by label; required filters, preferred scores.
- Pod (anti-)affinity and topology spread place replicas relative to each other.
- Taints repel and tolerations permit; dedicated pools need both a taint and affinity.
- PriorityClasses decide who is preempted; PodDisruptionBudgets limit voluntary evictions.

## Further reading

- Kubernetes scheduler: <https://kubernetes.io/docs/concepts/scheduling-eviction/kube-scheduler/>
- Assigning Pods to nodes: <https://kubernetes.io/docs/concepts/scheduling-eviction/assign-pod-node/>
- Taints and tolerations: <https://kubernetes.io/docs/concepts/scheduling-eviction/taint-and-toleration/>
- Pod topology spread constraints: <https://kubernetes.io/docs/concepts/scheduling-eviction/topology-spread-constraints/>
- Disruptions and PodDisruptionBudgets: <https://kubernetes.io/docs/concepts/workloads/pods/disruptions/>
