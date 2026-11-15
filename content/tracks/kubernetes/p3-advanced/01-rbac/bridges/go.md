---
from: go
status: draft
concept: "Role-based access, enforced by the platform"
from_way: "A service authenticates to infrastructure with a long-lived key read from a file."
native_way: "client-go's rest.InClusterConfig() uses the Pod's ServiceAccount token, and RBAC decides what that identity may do."
sensei_tip: "Generate RBAC from code with kubebuilder markers (// +kubebuilder:rbac:groups=apps,resources=deployments,verbs=get;list;watch) so the Role always matches what the controller calls."
---

| Go world | Kubernetes |
| --- | --- |
| `rest.InClusterConfig()` | the mounted ServiceAccount token |
| a static API key | avoid: use projected tokens |
| a permission check in a handler | the RBAC authoriser |
| kubebuilder RBAC markers | generated ClusterRoles |
