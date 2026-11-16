---
from: go
status: draft
concept: "Deny by default, allow exactly what is needed"
from_way: "A Go service reads a long-lived access key from a flag and uses it for every AWS call."
native_way: "The service runs with a role; its policy names the exact actions and ARNs; credentials are temporary and refreshed by the SDK."
sensei_tip: "Use `errors.As(err, &apiErr)` with `smithy.APIError` and log `apiErr.ErrorCode()`; an `AccessDenied` code tells you the policy, not the code, needs a change."
---

| Go world | AWS |
| --- | --- |
| a shared `-aws-key` flag | a role per service |
| `casbin` rules | IAM statements: effect, action, resource, condition |
| `smithy.APIError` code `AccessDenied` | an implicit or explicit deny |
| `stscreds.NewAssumeRoleProvider` | assuming a role from code |
