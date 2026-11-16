---
from: csharp
status: draft
concept: "Authorise every object, not just every user"
from_way: "[Authorize] controllers that call `_db.Invoices.Find(id)`."
native_way: "Resource-based authorisation (`IAuthorizationService.AuthorizeAsync(User, invoice, \"Owner\")`) or queries filtered by the user's id."
sensei_tip: "ASP.NET Core's resource-based authorisation handlers receive the loaded object, which is exactly what URL or role policies cannot see."
---

| C# | OWASP Top 10 practice |
| --- | --- |
| _db.Invoices.Find(id) | Where(i => i.Id == id && i.OwnerId == userId) |
| [Authorize(Roles = "User")] | resource-based authorization handler |
| HttpClient.GetAsync(userUrl) | allow-list, IP check, no auto-redirect |
| BinaryFormatter | A08: removed, use System.Text.Json DTOs |
| UseDeveloperExceptionPage everywhere | only in Development |
