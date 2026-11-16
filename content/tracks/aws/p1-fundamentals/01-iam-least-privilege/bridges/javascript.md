---
from: javascript
status: draft
concept: "Deny by default, allow exactly what is needed"
from_way: "An Express app checks `req.user.role === \"admin\"` and uses one access key with `*` permissions for every AWS call."
native_way: "Each workload gets an IAM role scoped to the actions and ARNs it calls; trust policies say who may assume it."
sensei_tip: "In GitHub Actions use `aws-actions/configure-aws-credentials` with OIDC and a role whose trust policy pins `token.actions.githubusercontent.com:sub` to your repository and branch."
---

| Node.js world | AWS |
| --- | --- |
| an RBAC middleware | IAM policy evaluation on every call |
| a JWT's `scope` claim | the actions in an Allow statement |
| `process.env.AWS_ACCESS_KEY_ID` in CI | OIDC federation to a role |
| a 403 from your API | an `AccessDenied` error from AWS |
