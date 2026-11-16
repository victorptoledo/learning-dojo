---
from: csharp
status: draft
concept: "API keys, OAuth 2.x and scopes"
from_way: "Basic auth over every endpoint with a shared password."
native_way: "`AddAuthentication().AddJwtBearer(...)`, authorization policies with `RequireClaim(\"scope\", ...)`, and resource-based authorisation (`IAuthorizationService`) for object checks."
sensei_tip: "Resource-based authorisation (`AuthorizeAsync(User, order, \"OwnerPolicy\")`) is how ASP.NET Core expresses object-level checks; scopes alone are not enough."
---

| C# / ASP.NET Core | Auth concept |
| --- | --- |
| `AddJwtBearer(o => o.Audience = ...)` | token validation |
| `RequireClaim("scope", "orders:write")` | scope policy |
| `.RequireAuthorization("policy")` | per endpoint |
| `IAuthorizationService.AuthorizeAsync(user, resource, ...)` | object-level check |
| `JwtBearerEvents.OnChallenge` | WWW-Authenticate details |
