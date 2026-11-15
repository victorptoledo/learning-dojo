---
from: csharp
status: draft
concept: "Identity security"
from_way: "ASP.NET Core Identity, cookie authentication and AddJwtBearer."
native_way: "The same building blocks chosen per client type."
sensei_tip: "AddJwtBearer validates issuer, audience and lifetime by default when you configure TokenValidationParameters; never turn those checks off to make a test pass."
---

| C# | In practice |
| --- | --- |
| Identity cookies | server-side sessions |
| AddJwtBearer | token verification |
| TokenValidationParameters | iss, aud, exp, signature |
| PasswordHasher | PBKDF2; argon2 or bcrypt also fine |
