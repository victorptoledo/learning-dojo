---
from: csharp
status: draft
concept: "Thinking in threats, not only in features"
from_way: "[Authorize] on some controllers and the rest open by default."
native_way: "A fallback policy that requires authentication everywhere, with [AllowAnonymous] as the reviewed exception."
sensei_tip: "In ASP.NET Core, setting `options.FallbackPolicy` to a policy that requires an authenticated user turns secure by default into one line."
---

| C# | Security mindset |
| --- | --- |
| [Authorize] added endpoint by endpoint | fallback policy, deny by default |
| a connection string with sa | a least-privilege SQL login |
| the developer exception page in production | secure defaults per environment |
| secrets in appsettings.json | user secrets locally, a vault in production |
| catch around authorization handlers | fail closed |
