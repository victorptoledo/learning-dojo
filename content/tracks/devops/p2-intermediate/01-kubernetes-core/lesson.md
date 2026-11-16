# Kubernetes for Operations: Workloads, Networking and Day-2 Work

Kubernetes is where most DevOps teams run containers. You describe the desired state of your
services in objects; controllers keep the real world matching it, restarting, rescheduling and
replacing what fails. This lesson is the operator's subset. The **Kubernetes** track covers
each object in depth, with katas checked against the real schemas.

## The reconcile loop

You never tell Kubernetes "start three containers". You `apply` an object whose `spec` says
"there should be three replicas of this Pod", the API server stores it in etcd, and a
**controller** watches it and acts until the `status` matches the `spec`, forever. When a node
dies, the controller sees two replicas and creates a third. The same idea runs through
configuration management, Terraform and GitOps: declare, compare, converge.

```text
kubectl apply ──► API server ──► etcd
                      ▲  │ watch
          status ─────┘  ▼
              controllers (Deployment, ReplicaSet, ...) ──► scheduler ──► kubelet on a node
```

## Running a stateless service

```yaml
apiVersion: apps/v1
kind: Deployment
metadata: { name: orders, labels: { app: orders } }
spec:
  replicas: 3
  selector: { matchLabels: { app: orders } }
  strategy:
    type: RollingUpdate
    rollingUpdate: { maxUnavailable: 0, maxSurge: 1 }   # never fewer than 3 ready Pods
  template:
    metadata: { labels: { app: orders } }
    spec:
      containers:
        - name: orders
          image: ghcr.io/acme/orders:3f2c9e1
          ports: [{ name: http, containerPort: 8080 }]
          envFrom: [{ configMapRef: { name: orders-config } }]
          env:
            - name: DB_PASSWORD
              valueFrom: { secretKeyRef: { name: orders-db, key: password } }
          readinessProbe: { httpGet: { path: /ready, port: http } }
          livenessProbe: { httpGet: { path: /live, port: http }, periodSeconds: 10 }
          resources:
            requests: { cpu: 250m, memory: 256Mi }
            limits: { memory: 256Mi }
```

A **Deployment** manages ReplicaSets, which manage Pods. Changing the Pod template (a new
image tag) starts a **rolling update**: new Pods come up, become ready, and old ones are
removed, within `maxSurge` and `maxUnavailable`.

| Setting | Why operators care |
| --- | --- |
| **readinessProbe** | a Pod gets traffic only when ready; without it, rollouts send users to Pods that are still starting |
| **livenessProbe** | restarts a stuck container; keep it cheap and do not check dependencies in it |
| **requests** | what the scheduler reserves; the basis for bin-packing and autoscaling |
| **limits** | memory over the limit is OOM-killed; CPU over the limit is throttled |
| **image by tag or digest** | tie every Pod to an exact build |

## Networking

A **Service** gives a stable virtual IP and DNS name (`orders.payments.svc.cluster.local`) to
the Pods its selector matches, and only to the ready ones. North-south traffic enters through
an **Ingress** or, the newer standard, the **Gateway API**: platform teams own a `Gateway`
(listeners, TLS), application teams attach `HTTPRoute`s to it.

```yaml
apiVersion: gateway.networking.k8s.io/v1
kind: HTTPRoute
metadata: { name: orders }
spec:
  parentRefs: [{ name: public, namespace: infra }]
  hostnames: [orders.example.com]
  rules:
    - backendRefs: [{ name: orders, port: 80 }]
```

**NetworkPolicies** restrict which Pods may talk to which; by default everything can reach
everything.

## Configuration

**ConfigMaps** hold configuration, **Secrets** hold credentials (base64-encoded, not encrypted
by default: enable encryption at rest and restrict who can read them, or use an external
secrets operator that syncs from a vault). Both arrive as environment variables or mounted
files. Environment variables are read once at start, so a change needs a rollout; generated
names with a hash suffix (Kustomize, Helm) make that automatic.

## Day-2 commands

```bash
kubectl get pods -l app=orders -o wide           # where, how ready, how many restarts
kubectl describe pod orders-7d9c-x2k1            # events: scheduling, pulls, probe failures, OOMKilled
kubectl logs orders-7d9c-x2k1 --previous         # the logs of the crashed container
kubectl get events --sort-by=.lastTimestamp -n payments
kubectl rollout status deploy/orders             # wait for a rollout
kubectl rollout history deploy/orders
kubectl rollout undo deploy/orders               # back to the previous ReplicaSet
kubectl top pods                                 # usage against requests (needs metrics-server)
kubectl debug -it orders-7d9c-x2k1 --image=busybox --target=orders
```

Common states: `Pending` (cannot be scheduled: requests too big, taints, no volume),
`ImagePullBackOff` (wrong tag or missing pull secret), `CrashLoopBackOff` (the process keeps
exiting: read `--previous` logs), `OOMKilled` (raise the limit or fix the leak).

In a GitOps setup (next lesson), `rollout undo` is an emergency tool: the real rollback is a
revert in Git, or the controller will put the bad version back.

## Packaging

Raw manifests multiply per environment. **Kustomize** patches a shared base with overlays per
environment; **Helm** templates a chart with values and tracks installed releases. Both are
covered in the Kubernetes track; this track uses plain manifests.

## Summary

- Kubernetes stores desired state and controllers reconcile reality to it continuously.
- Deployments roll out new Pod templates; probes, requests and limits make rollouts safe and
  scheduling predictable.
- Services give stable names to ready Pods; Gateway API routes bring traffic in.
- ConfigMaps and Secrets configure Pods; protect Secrets.
- `get`, `describe`, `logs --previous`, `events` and `rollout` are the day-2 toolkit.

## Further reading

- Kubernetes concepts: <https://kubernetes.io/docs/concepts/>
- Deployments and rolling updates: <https://kubernetes.io/docs/concepts/workloads/controllers/deployment/>
- Gateway API: <https://gateway-api.sigs.k8s.io/>
- Debugging Pods: <https://kubernetes.io/docs/tasks/debug/debug-application/debug-pods/>
- *Kubernetes Up & Running*, 3rd edition, Brendan Burns, Joe Beda, Kelsey Hightower and Lachlan Evenson (O'Reilly, 2022)
