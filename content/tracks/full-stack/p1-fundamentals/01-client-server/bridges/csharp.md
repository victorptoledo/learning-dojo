---
from: csharp
status: reviewed
concept: "CORS policies and cookie options in ASP.NET Core"
from_way: "builder.Services.AddCors with AllowAnyOrigin().AllowAnyHeader() to make the error disappear."
native_way: "A named policy WithOrigins(...).AllowCredentials(), and CookieOptions with Secure, HttpOnly and SameSite set explicitly."
sensei_tip: "AllowAnyOrigin and AllowCredentials together throw at start-up in ASP.NET Core, for the same reason browsers refuse them."
---

| ASP.NET Core | Browser effect |
| --- | --- |
| `policy.WithOrigins("https://app.example.com").AllowCredentials()` | credentialed CORS for one origin |
| `options.Cookie.SameSite = SameSiteMode.Lax` | no cookie on cross-site POSTs |
| `options.Cookie.HttpOnly = true` | hidden from scripts |
| `Results.Redirect(url)` | 302; use 303 for PRG after POST |
