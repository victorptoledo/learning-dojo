---
from: go
status: draft
concept: "Operating the platform, not only the app"
from_way: "Your service follows Go's release policy (two supported versions) and you rebuild on every patch."
native_way: "The cluster follows a similar cadence: three minor versions a year, about 14 months of support, upgrades one minor at a time."
sensei_tip: "Argo CD, Flux, Velero, Kyverno and Karpenter are all Go projects built on client-go: when a tool misbehaves, its reconcile loop is readable."
---

| Go world | Kubernetes |
| --- | --- |
| the Go release policy | the Kubernetes support window |
| `go vet` in CI | kubeconform and policy checks in CI |
| `go.mod` pinning | pinned chart versions and image digests |
| a release tag | a Git commit applied by GitOps |
