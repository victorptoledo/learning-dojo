---
from: csharp
status: draft
concept: "Deny by default, allow exactly what is needed"
from_way: "ASP.NET Core policies (`[Authorize(Policy = \"CanEdit\")]`) protect the app, while it talks to SQL Server as `sa`."
native_way: "IAM policies grant exact API actions on exact ARNs to roles; explicit denies win; nothing is allowed by default."
sensei_tip: "Treat IAM like authorization policies in code: one role per service, generated or reviewed in pull requests, and validated with Access Analyzer in CI."
---

| .NET world | AWS |
| --- | --- |
| `AuthorizationPolicyBuilder` | a policy document with statements |
| a Windows service account | an IAM role assumed by the workload |
| `[AllowAnonymous]` | `"Principal": "*"`: read it twice |
| claims transformation | session tags and `aws:PrincipalTag` conditions |
