# The Control Plane: API Server, etcd, Scheduler and Controller Manager

A Kubernetes cluster has two halves. The **control plane** decides what should run where and
stores that decision. The **nodes** run the containers. Everything talks through one door,
the API server, and nothing is told what to do directly: every component watches the state
it cares about and acts on the difference.

## The components

```text
            kubectl, CI, controllers, operators
                         │  HTTPS (REST + watch)
 ┌───────────── control plane ─────────────┐
 │  kube-apiserver  ◀──▶  etcd             │
 │       ▲   ▲                             │
 │  kube-scheduler   kube-controller-manager│   (+ cloud-controller-manager)
 └───────┼─────────────────────────────────┘
         │
 ┌──── node ────┐   ┌──── node ────┐
 │ kubelet      │   │ kubelet      │
 │ kube-proxy   │   │ kube-proxy   │
 │ containerd   │   │ containerd   │
 └──────────────┘   └──────────────┘
```

| Component | Job |
| --- | --- |
| **kube-apiserver** | the only entry point. Authenticates, authorises (RBAC), runs admission (defaults, policies), validates, and reads and writes objects in etcd. Serves *watches*: streams of changes |
| **etcd** | a consistent, distributed key-value store holding every object. It uses the **Raft** consensus algorithm, so it runs as 3 or 5 members and needs a majority to accept writes |
| **kube-scheduler** | watches for Pods without a node and picks one (Phase 2) |
| **kube-controller-manager** | runs dozens of controllers: Deployment, ReplicaSet, Job, node lifecycle, EndpointSlice, ServiceAccount… |
| **cloud-controller-manager** | talks to the cloud: load balancers for Services, node addresses, routes |
| **kubelet** | the agent on each node: starts the Pods bound to it through the container runtime (CRI), runs probes, reports status |
| **kube-proxy** | programs each node's packet rules (iptables, IPVS or nftables) so Service IPs reach Pods |
| **container runtime** | containerd or CRI-O, which run the OCI images with runc |

Add-ons complete a cluster: **CoreDNS** for names, a **CNI plugin** (Calico, Cilium,
Flannel…) for Pod networking, and usually a metrics server.

## From apply to a running container

1. `kubectl apply` sends the Deployment to the API server.
2. The API server authenticates you, checks RBAC, runs admission controllers, validates the
   object and stores it in etcd. It answers at once: the work has not started yet.
3. The **Deployment controller**, watching Deployments, sees a new one and creates a
   **ReplicaSet**.
4. The **ReplicaSet controller** sees a ReplicaSet wanting 3 Pods with 0 existing, and
   creates 3 Pod objects. They have no `nodeName` yet.
5. The **scheduler** sees unscheduled Pods, filters and scores nodes, and writes a binding.
6. The **kubelet** on each chosen node sees a Pod bound to it, pulls the image and starts the
   containers through containerd, then reports the status back to the API server.

No component calls another. Each one **watches** the API for the objects it owns and writes
its results back. That loose coupling is why you can add your own controllers (Phase 4).

## The reconciliation loop

Every controller runs the same loop:

```text
loop:
    desired := read spec from the API
    actual  := observe the world (Pods, nodes, a cloud load balancer…)
    if actual != desired:
        act to reduce the difference
    write what you observed into status
```

Controllers are **level-triggered**: they react to the current state, not to a single event.
If a controller crashes and misses ten events, it reads the state again when it comes back
and still converges. That is why Kubernetes heals itself after outages: there is nothing to
replay, only a difference to close.

`metadata.generation` counts changes to `spec`; `status.observedGeneration` says which
generation a controller has acted on. When they are equal, the controller has caught up.

## When a node dies

The kubelet sends heartbeats (Lease objects). When they stop, the **node lifecycle
controller** marks the node `NotReady` and taints it. After a grace period (five minutes by
default) its Pods are evicted; the ReplicaSet controller sees fewer Pods than desired and
creates new ones, which the scheduler places elsewhere. A bare Pod with no controller is
not replaced: always run workloads through a controller.

## Managed and self-managed

On EKS, GKE or AKS the provider runs the control plane (etcd included) and you manage the
nodes, or not even those. With kubeadm or k3s you run everything yourself, and etcd backups,
certificates and upgrades are your job (Phase 4).

## Summary

- The API server is the single door; etcd stores all state; everything else watches.
- The scheduler assigns Pods to nodes; controllers turn high-level objects into lower ones.
- The kubelet makes a node's Pods real through the container runtime and reports status.
- Controllers reconcile continuously and level-triggered, so the cluster converges after
  failures instead of replaying them.
- Only Pods managed by a controller are recreated when a node is lost.

## Further reading

- Kubernetes components: <https://kubernetes.io/docs/concepts/overview/components/>
- Cluster architecture: <https://kubernetes.io/docs/concepts/architecture/>
- Controllers: <https://kubernetes.io/docs/concepts/architecture/controller/>
- Nodes and heartbeats: <https://kubernetes.io/docs/concepts/architecture/nodes/>
- etcd and Raft: <https://etcd.io/docs/v3.6/learning/>
