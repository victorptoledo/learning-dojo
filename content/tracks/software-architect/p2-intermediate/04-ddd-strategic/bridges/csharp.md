---
from: csharp
status: draft
concept: "Bounded contexts and context maps"
from_way: "One `Company.Domain` project with a `Customer` class used by every feature, and generated service references used in business logic."
native_way: "A project or folder per bounded context with its own model, an ACL class in `Infrastructure` that maps generated clients (NSwag, Refit) into the context's records, and ids shared between contexts."
sensei_tip: "Refit and NSwag clients generate types in the other side's language. Keep them `internal` to an ACL project, map them to your records, and the compiler protects your domain."
---

| C# / .NET | Strategic DDD idea |
| --- | --- |
| a project per context | bounded context |
| records named in the experts' terms | ubiquitous language in code |
| an `internal` mapper over a Refit client | anti-corruption layer |
| a published OpenAPI or a contracts NuGet package | open host service with published language |
| a small `SharedKernel` project | shared kernel |
| NetArchTest: only the ACL references the client | the ACL enforced |
