---
from: csharp
status: draft
concept: "Let the browser enforce security for you"
from_way: "ASP.NET Core with app.UseCors(p => p.AllowAnyOrigin()) and no CSP."
native_way: "UseHsts and UseHttpsRedirection, a header middleware for CSP and friends, named CORS policies with WithOrigins, and cookie policies."
sensei_tip: "`AllowAnyOrigin()` cannot be combined with `AllowCredentials()` in ASP.NET Core; developers who hit that error sometimes reflect the origin with `SetIsOriginAllowed(_ => true)`, which is worse."
---

| C# | Hardened responses |
| --- | --- |
| SetIsOriginAllowed(_ => true) | WithOrigins("https://app.example.com") |
| no UseHsts() | app.UseHsts() outside Development |
| no CSP | a header middleware with a nonce |
| Kestrel Server header | AddServerHeader = false |
| cookies without SameSite | CookiePolicyOptions |
