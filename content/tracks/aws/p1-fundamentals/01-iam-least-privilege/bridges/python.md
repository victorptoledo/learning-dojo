---
from: python
status: draft
concept: "Deny by default, allow exactly what is needed"
from_way: "A Django project gives staff users `is_superuser` and the app one database login with every privilege."
native_way: "IAM policies list exact actions on exact ARNs; a role per workload gets temporary credentials; anything not allowed is denied."
sensei_tip: "When boto3 raises `ClientError` with `AccessDenied`, read the message: it names the action and resource missing from the policy, so add exactly that and nothing wider."
---

| Python world | AWS |
| --- | --- |
| `@permission_required("orders.view_order")` | an Allow on one action and resource |
| Django groups | IAM groups, or permission sets in IAM Identity Center |
| `botocore.exceptions.ClientError` | the deny you must read, not silence |
| a superuser | `AdministratorAccess`: avoid it for workloads |
