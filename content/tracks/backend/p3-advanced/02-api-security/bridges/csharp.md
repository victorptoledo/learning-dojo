---
from: csharp
status: draft
concept: "Security in ordinary code"
from_way: "Binding request bodies straight to EF entities."
native_way: "Request DTOs, ownership checks and limits."
sensei_tip: "Binding to the entity lets clients set any property; bind to a record with only the editable fields and map it. ASP.NET Core's rate limiting middleware covers the rest."
---

| C# | In practice |
| --- | --- |
| [FromBody] User user (entity) | [FromBody] UpdateUserRequest |
| FindAsync(id) | query filtered by the current user |
| AddRateLimiter | rate limits |
| returning the entity | a response DTO |
