---
from: java
status: draft
concept: "Role-based access, enforced by the platform"
from_way: "Spring Security roles guard your endpoints, and a shared database user with broad rights runs every service."
native_way: "RBAC guards the Kubernetes API itself: every workload has its own ServiceAccount, and Roles grant it exactly the verbs it needs."
sensei_tip: "Spring Cloud Kubernetes reads ConfigMaps through the API, so its ServiceAccount needs get/list/watch on configmaps; nothing more, and never secrets unless you use that feature."
---

| Java world | Kubernetes |
| --- | --- |
| `@PreAuthorize("hasRole('ADMIN')")` | a Role bound to a subject |
| a technical user per service | a ServiceAccount per workload |
| roles granted to LDAP groups | RoleBindings to OIDC groups |
| a JAAS realm | the API server's authenticators |
