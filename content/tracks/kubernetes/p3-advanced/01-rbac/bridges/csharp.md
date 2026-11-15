---
from: csharp
status: draft
concept: "Role-based access, enforced by the platform"
from_way: "Azure AD groups assigned to app roles and [Authorize(Roles = ...)] attributes, with managed identities for services."
native_way: "AKS maps Entra ID groups to RoleBindings, and Workload Identity links a ServiceAccount to a managed identity for Azure resources."
sensei_tip: "With Azure Workload Identity, a Pod gets Azure tokens through its ServiceAccount, so no client secret is stored in the cluster at all."
---

| .NET / Azure world | Kubernetes |
| --- | --- |
| `[Authorize(Roles = "Admin")]` | a Role and a RoleBinding |
| an Entra ID group | a group subject in a binding |
| a managed identity | a ServiceAccount (+ Workload Identity) |
| Azure RBAC scopes | namespaces and cluster scope |
