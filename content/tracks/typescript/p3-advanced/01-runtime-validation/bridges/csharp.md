---
from: csharp
status: draft
concept: "Model validation at the edge"
from_way: "ASP.NET Core model binding deserialises into typed classes and DataAnnotations or FluentValidation report errors per property."
native_way: "Nothing binds or checks automatically; a zod schema parses unknown input into a typed value and reports issues with paths."
sensei_tip: "Think of a zod schema as model binding and FluentValidation in one object, plus the class definition for free through z.infer."
---

| C# | TypeScript |
| --- | --- |
| `[Required, EmailAddress] string Email` | `email: z.email()` |
| `ModelState.IsValid` | `result.success` |
| `JsonSerializer.Deserialize<User>(json)` | `User.parse(JSON.parse(json))` |
| `IOptions<T>` with `ValidateOnStart()` | A config schema parsed at start-up |
| `readonly record struct UserId(string Value)` | `.brand<"UserId">()` |
