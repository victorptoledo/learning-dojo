---
from: java
status: draft
concept: "Deny by default, allow exactly what is needed"
from_way: "Spring Security grants roles with `@PreAuthorize(\"hasRole('ADMIN')\")`, and the app often connects as one all-powerful database user."
native_way: "IAM decides every API call from policies: explicit deny first, then allow, otherwise implicit deny; each workload gets its own role with exact actions and ARNs."
sensei_tip: "On EKS use IRSA or EKS Pod Identity and on ECS a task role, so each Spring Boot service gets its own role; never a shared access key in `application.yml`."
---

| Java world | AWS |
| --- | --- |
| `hasAuthority("orders:read")` | `"Action": "dynamodb:GetItem"` on one table ARN |
| a JAAS realm | an IAM role and its trust policy |
| `AccessDeniedException` | `AccessDenied` (implicit or explicit deny) |
| the security config of one app | identity, resource and boundary policies combined |
