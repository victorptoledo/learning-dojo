# Pods: Containers, Init Containers, Sidecars, Labels and Namespaces

Kubernetes never runs a bare container. It runs **Pods**: one or more containers that are
scheduled together on one node and share a network identity and storage. Everything else in
this track (Deployments, Jobs, StatefulSets) is a way to create and replace Pods.

## What a Pod is

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: web
  labels: { app: web, tier: frontend }
spec:
  containers:
    - name: web
      image: nginx:1.29
      ports:
        - { name: http, containerPort: 80 }
```

Containers in one Pod share:

- the **network namespace**: one IP address, one port space. They reach each other on
  `localhost` and must not listen on the same port;
- **volumes** declared in the Pod, which each container mounts where it wants;
- a **lifecycle**: they are scheduled, started, stopped and replaced together, on one node.

Each container keeps its own file system (from its image), process tree and resource
limits. One Pod is the unit of scaling: three replicas means three copies of the whole Pod.
That is why most Pods have **one** main container; put two together only when they cannot
work apart.

## Init containers and sidecars

**Init containers** run before the app containers, one at a time, in order, and each must
exit successfully. Use them for one-off preparation: waiting for a dependency, running a
migration, rendering a configuration file into a shared volume.

**Sidecars** run next to the app for the whole Pod life: a log shipper, a proxy, a
certificate refresher. Since Kubernetes 1.29 a sidecar is declared as an init container with
`restartPolicy: Always`: it starts before the app, keeps running, and stops after it.

```yaml
spec:
  initContainers:
    - name: migrate
      image: ghcr.io/acme/orders-migrate:1.4.0
    - name: log-shipper
      image: fluent/fluent-bit:4.1
      restartPolicy: Always            # a native sidecar
      volumeMounts: [{ name: logs, mountPath: /var/log/app }]
  containers:
    - name: app
      image: ghcr.io/acme/orders:1.4.0
      volumeMounts: [{ name: logs, mountPath: /var/log/app }]
  volumes:
    - name: logs
      emptyDir: {}                     # lives as long as the Pod
```

## The Pod lifecycle

A Pod's `status.phase` is one of `Pending` (accepted, not all containers running: being
scheduled or pulling images), `Running`, `Succeeded`, `Failed` or `Unknown` (the node stopped
reporting). Each container has its own state: `Waiting` (with a reason such as
`ContainerCreating`, `ImagePullBackOff` or `CrashLoopBackOff`), `Running` or `Terminated`
(with an exit code and a reason such as `OOMKilled`).

`spec.restartPolicy` (`Always`, `OnFailure`, `Never`) tells the kubelet what to do when a
container exits. Restarts back off exponentially, up to five minutes: that waiting state is
**CrashLoopBackOff**, the most famous status in Kubernetes.

Stopping is graceful: on deletion the kubelet runs any `preStop` hook, sends **SIGTERM** to
each container's PID 1, waits up to `terminationGracePeriodSeconds` (30 by default) and then
sends SIGKILL. Meanwhile the Pod is removed from Service endpoints. Your process must handle
SIGTERM: finish in-flight requests, then exit.

Pods are **mortal and disposable**: a Pod is never moved or resurrected, only replaced by a
new one with a new name and IP. Never rely on a Pod's IP.

## Labels, selectors and annotations

**Labels** are key/value pairs that identify objects: `app: web`, `tier: frontend`,
`env: prod`. **Selectors** pick objects by label, and this is how everything is wired
together: a Deployment finds its Pods, a Service its backends, a NetworkPolicy its targets.

```powershell
kubectl get pods -l app=web,tier=frontend
kubectl get pods -l 'env in (staging, prod)' --show-labels
```

The recommended labels `app.kubernetes.io/name`, `/instance`, `/version`, `/component` and
`/part-of` are understood by many tools. **Annotations** hold non-identifying data that tools
read: a build commit, a change reason, settings for an Ingress controller. You cannot select
by annotation.

## Namespaces

A **namespace** is a scope for names and a unit for access control, quotas and policies.
`kubectl get pods -n payments`; `default` is used when none is given, and `kube-system`
holds the cluster's own components. Namespaces are not a security boundary on their own:
add RBAC, NetworkPolicies and Pod Security (Phase 3). Some objects are cluster-wide: nodes,
PersistentVolumes, ClusterRoles, namespaces themselves.

## Summary

- A Pod is a group of containers sharing a network namespace, volumes and a lifecycle.
- Init containers prepare, in order; sidecars (`restartPolicy: Always`) run alongside.
- Phases and container states tell you where a Pod is; CrashLoopBackOff is a restart backoff.
- Termination is SIGTERM, a grace period, then SIGKILL; Pods are replaced, never moved.
- Labels and selectors connect objects; annotations carry metadata; namespaces scope names.

## Further reading

- Pods: <https://kubernetes.io/docs/concepts/workloads/pods/>
- Pod lifecycle: <https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle/>
- Sidecar containers: <https://kubernetes.io/docs/concepts/workloads/pods/sidecar-containers/>
- Labels and selectors: <https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/>
- Pod API reference: <https://kubernetes.io/docs/reference/kubernetes-api/workload-resources/pod-v1/>
