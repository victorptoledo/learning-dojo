# Why Kubernetes, Local Clusters and kubectl

With Docker you can run any service in a container. Then production asks harder questions:
which of my ten servers should this container run on? Who restarts it when the server dies
at 3 a.m.? How do I ship version 2 without downtime, and how do the other services find it
when it moves? A **container orchestrator** answers these questions for a whole fleet, and
**Kubernetes** (often written **k8s**) is the one the industry settled on.

## What an orchestrator does

| Problem | What Kubernetes does |
| --- | --- |
| Where should it run? | the **scheduler** places containers on nodes with enough free CPU and memory |
| It crashed | the **kubelet** restarts the container; a lost node's work is recreated elsewhere |
| A new version | a **Deployment** replaces Pods gradually and can roll back |
| How do others find it? | a **Service** gives a stable name and virtual IP that load-balances over the replicas |
| More traffic | change `replicas`, or let an **autoscaler** do it |
| Configuration and secrets | **ConfigMaps** and **Secrets** injected as variables or files |

Kubernetes was open-sourced by Google in 2014, building on its internal Borg system, and is
maintained by the Cloud Native Computing Foundation. Every major cloud sells it as a managed
service (EKS, GKE, AKS), and the same manifests run on a laptop.

## Declarative, not imperative

With `docker run` you give **orders**: start this, now. With Kubernetes you write down the
**desired state** and hand it to the API:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: web
spec:
  replicas: 3            # "I want three copies"
  selector:
    matchLabels: { app: web }
  template:
    metadata:
      labels: { app: web }
    spec:
      containers:
        - name: web
          image: nginx:1.29
```

**Controllers** then compare that desired state with what actually runs and act on the
difference, forever. Kill a Pod and a new one appears, because "three" is still the goal.
This **reconciliation loop** is the central idea of the whole track (Phase 1 opens it up).

Every object you write has the same shape: `apiVersion` and `kind` (what it is), `metadata`
(name, namespace, labels) and `spec` (what you want). The cluster adds `status` (what is).

## A local cluster

You need a cluster to try things. Good local options:

| Option | What it is |
| --- | --- |
| **Docker Desktop** | Settings → Kubernetes → Enable. A one-node cluster; `kubectl` is installed with it |
| **kind** | "Kubernetes in Docker": each node is a container, great for CI and multi-node tests |
| **minikube** | a cluster in a VM or container, with add-ons such as an Ingress controller |

```powershell
kubectl version            # client and server versions: the cluster answers
kubectl get nodes          # one node, Ready
kubectl config get-contexts
kubectl config use-context docker-desktop
```

A **context** in `~/.kube/config` joins a cluster, a user and a default namespace. Check it
before every command that changes something; applying to production when you meant your
laptop is a classic mistake.

## kubectl, the everyday commands

```powershell
kubectl apply -f web.yaml                  # create or update what the file declares
kubectl get pods -o wide                   # list, with node and IP
kubectl get deploy,svc                     # several kinds at once
kubectl describe pod web-7d9f...           # details and recent events: the first debugging step
kubectl logs deploy/web                    # what a container printed
kubectl exec -it deploy/web -- sh          # a shell inside a container
kubectl port-forward svc/web 8080:80       # reach a Service from your machine
kubectl delete -f web.yaml                 # remove what the file declares
kubectl explain deployment.spec.strategy   # the API reference, served by the cluster
```

Imperative shortcuts exist (`kubectl create deployment`, `kubectl run`, `kubectl scale`), and
they are handy for experiments. For anything that matters, keep the YAML in Git and use
`kubectl apply`, so the repository is the record of what should run. `kubectl diff -f`
shows what an apply would change before you make it.

## How the dojo checks Kubernetes katas

The katas in this track need **no cluster**. You write manifests (and later Kustomize
overlays and a Helm chart) in the kata folder. On every save, the dojo runs the `dojo-k8s`
container, with no network, which:

1. renders your files (as they are, with `kustomize build` or with `helm template`),
2. validates every object with **kubeconform** against the schemas of Kubernetes 1.36, so a
   misspelt field or a wrong type fails like a compile error,
3. runs bats tests over the rendered objects: does the Service select the Pods, is the
   container non-root, are probes and resources set?

Build the image once with `dojo runner build`. A live cluster is still the best playground
for the lessons: apply the reference solutions and watch what happens.

## Summary

- An orchestrator schedules, restarts, updates, connects and scales containers across nodes.
- You declare the desired state; controllers reconcile the actual state towards it.
- Every object has `apiVersion`, `kind`, `metadata` and `spec`; the cluster reports `status`.
- Docker Desktop, kind or minikube give you a local cluster; `kubectl` talks to its API.
- Check your context; keep manifests in Git and `kubectl apply` them.

## Further reading

- Kubernetes overview: <https://kubernetes.io/docs/concepts/overview/>
- Objects in Kubernetes: <https://kubernetes.io/docs/concepts/overview/working-with-objects/>
- kubectl quick reference: <https://kubernetes.io/docs/reference/kubectl/quick-reference/>
- kind quick start: <https://kind.sigs.k8s.io/docs/user/quick-start/>
- Kubernetes roadmap (roadmap.sh): <https://roadmap.sh/kubernetes>
