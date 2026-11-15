# Deployments, ReplicaSets, Rollouts and Rollbacks

You almost never create Pods yourself. A **Deployment** describes the Pods you want and how
many, and replaces them safely when the description changes. It is the default way to run a
stateless service.

## Deployment → ReplicaSet → Pods

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: orders
  labels: { app: orders }
spec:
  replicas: 3
  selector:
    matchLabels: { app: orders }        # which Pods belong to this Deployment
  template:                             # the Pod to create
    metadata:
      labels: { app: orders }           # must satisfy the selector
    spec:
      containers:
        - name: orders
          image: ghcr.io/acme/orders:1.4.0
          ports: [{ name: http, containerPort: 8080 }]
```

The Deployment does not create Pods directly. It creates a **ReplicaSet** for each version of
the template, and the ReplicaSet keeps exactly `replicas` Pods matching its selector. Names
show the chain: `orders` → `orders-6d8f9c7b5` → `orders-6d8f9c7b5-x2k4p`, where the middle
part is the **pod-template-hash** label added to keep versions apart.

The **selector must match the template's labels**, or the API rejects the Deployment. The
selector is also **immutable** after creation: choose stable labels (`app`, `component`) and
keep version labels out of it.

## Rolling updates

Change anything in `spec.template` (image, env, a label, resources) and the Deployment starts
a **rollout**: it creates a new ReplicaSet and moves replicas from the old one to the new one.
Changing `replicas` only scales; it is not a rollout.

```yaml
spec:
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1              # at most replicas + 1 Pods during the update
      maxUnavailable: 0        # never fewer than replicas ready Pods
  minReadySeconds: 10          # a new Pod must stay ready 10 s to count as available
  progressDeadlineSeconds: 600 # report the rollout as failed after 10 minutes without progress
  revisionHistoryLimit: 5      # keep 5 old ReplicaSets for rollbacks
```

Both values can be numbers or percentages (the default is 25 % each). `maxUnavailable: 0`
with `maxSurge: 1` is the cautious choice for small services: capacity never drops.

The pace is set by **readiness**: a new Pod counts only once its readiness probe passes
(next lessons). If the new version never becomes ready, the rollout stalls with the old Pods
still serving, and after `progressDeadlineSeconds` the Deployment's `Progressing` condition
turns false. Without a readiness probe, Kubernetes treats a started container as ready and
happily replaces healthy Pods with broken ones.

The other strategy, `Recreate`, deletes all old Pods before creating new ones: downtime, but
never two versions at once (useful when they cannot coexist, for example during some schema
changes).

## Watching and undoing

```powershell
kubectl set image deploy/orders orders=ghcr.io/acme/orders:1.5.0   # quick test; prefer Git
kubectl rollout status deploy/orders     # waits, and fails if the rollout fails
kubectl rollout history deploy/orders    # revisions (kept old ReplicaSets)
kubectl rollout undo deploy/orders                 # back to the previous revision
kubectl rollout undo deploy/orders --to-revision=3
kubectl rollout pause deploy/orders      # stop mid-way (a manual canary)
kubectl rollout resume deploy/orders
kubectl rollout restart deploy/orders    # new Pods, same spec (for example to reload a Secret)
```

`rollout undo` scales the old ReplicaSet back up: it is fast because the old Pod template
is still there. If you deploy from Git, also revert the commit, or the next apply brings the
bad version back. Annotate changes with `kubernetes.io/change-cause` to make history readable.

`kubectl rollout status` exits non-zero on failure, so CI pipelines use it to wait for and
verify a deploy.

## Beyond rolling updates

**Blue-green** (run both versions, switch a Service selector) and **canary** (send a small
share of traffic to the new version) need more than a Deployment: two Deployments and a
Service or Gateway route, or tools such as Argo Rollouts and Flagger (Phase 4).

## Summary

- A Deployment owns one ReplicaSet per template version; ReplicaSets own the Pods.
- The selector must match the template labels and cannot change later.
- Changing the Pod template starts a rolling update, paced by readiness, `maxSurge` and
  `maxUnavailable`; changing replicas only scales.
- `kubectl rollout status | history | undo | pause | restart` watch and control rollouts.
- A stalled rollout keeps the old Pods serving and is reported after `progressDeadlineSeconds`.

## Further reading

- Deployments: <https://kubernetes.io/docs/concepts/workloads/controllers/deployment/>
- ReplicaSet: <https://kubernetes.io/docs/concepts/workloads/controllers/replicaset/>
- Performing a rolling update (tutorial): <https://kubernetes.io/docs/tutorials/kubernetes-basics/update/update-intro/>
- Deployment API reference: <https://kubernetes.io/docs/reference/kubernetes-api/workload-resources/deployment-v1/>
