---
from: csharp
status: draft
concept: "Securing endpoints with Spring Security"
from_way: "ASP.NET Core authentication schemes, [Authorize] policies and Identity."
native_way: "Spring Security's filter chain, request matchers and method security."
sensei_tip: "app.UseAuthentication() with policies maps to a SecurityFilterChain; [Authorize(Roles = \"Admin\")] is @PreAuthorize(\"hasRole('ADMIN')\")."
---

| C# | Spring Boot 4 |
| --- | --- |
| [Authorize] | authenticated() rule |
| [Authorize(Roles = "Admin")] | hasRole("ADMIN") |
| AddJwtBearer | oauth2ResourceServer().jwt() |
| PasswordHasher<T> | PasswordEncoder |
| User.Identity.Name | authentication.getName() |
