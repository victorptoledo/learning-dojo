---
from: python
status: draft
concept: "Role-based access, enforced by the platform"
from_way: "Django groups and permissions protect the admin, while the deploy server holds one all-powerful SSH key."
native_way: "Kubernetes RBAC is the permission system of the platform: Roles are permission sets, bindings are group memberships, scoped to namespaces."
sensei_tip: "The Python client loads in-cluster credentials with config.load_incluster_config(); check what that ServiceAccount may do with kubectl auth can-i --list before shipping."
---

| Python world | Kubernetes |
| --- | --- |
| a Django `Permission` | a rule: verb + resource |
| a Django `Group` | a Role (or ClusterRole) |
| adding a user to a group | a RoleBinding |
| a superuser | `cluster-admin` (avoid it) |
