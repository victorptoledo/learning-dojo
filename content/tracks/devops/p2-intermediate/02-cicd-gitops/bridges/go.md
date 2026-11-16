---
from: go
status: draft
concept: "Promote one artefact; let the environment pull its state"
from_way: "A make deploy target runs kubectl apply with your own kubeconfig."
native_way: "CI never touches the cluster; Argo CD or Flux (both Go programs) reconcile the cluster from Git."
sensei_tip: "If you write a controller yourself, use controller-runtime: GitOps tools are the same watch-and-reconcile loop."
---

| Go world | GitOps |
| --- | --- |
| `make deploy` | a merged pull request |
| your kubeconfig | the controller's in-cluster identity |
| a reconcile loop | Argo CD sync |
| `git tag v1.4.0` | the version in the config repo |
