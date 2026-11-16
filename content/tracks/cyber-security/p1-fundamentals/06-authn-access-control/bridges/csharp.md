---
from: csharp
status: draft
concept: "Who you are, whether it is still you, and what you may do"
from_way: "Custom authentication cookies and roles checked with User.IsInRole inside actions."
native_way: "ASP.NET Core Identity or OIDC, cookie options with SecurePolicy and SameSite, antiforgery tokens, and policy-based authorisation."
sensei_tip: "ASP.NET Core's `[ValidateAntiForgeryToken]` (or AutoValidateAntiforgeryToken globally) and Identity's lockout options cover CSRF and online guessing with configuration, not code."
---

| C# | Secure practice |
| --- | --- |
| custom cookie with user id | Identity or cookie auth handler |
| [IgnoreAntiforgeryToken] on forms | AutoValidateAntiforgeryToken |
| User.IsInRole in each action | policies and requirements |
| no lockout | Identity lockout and rate limiting |
| binding the entity in actions | view models with allowed fields |
