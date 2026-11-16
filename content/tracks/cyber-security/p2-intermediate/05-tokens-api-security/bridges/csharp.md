---
from: csharp
status: draft
concept: "Tokens that are short-lived, fully validated and rotatable"
from_way: "TokenValidationParameters with ValidateAudience = false and a symmetric key from appsettings."
native_way: "AddJwtBearer with Authority (OIDC discovery and key rotation built in), ValidAudience, and short-lived tokens."
sensei_tip: "With `options.Authority` set, the JWT bearer handler downloads and refreshes the signing keys from the issuer's metadata, so key rotation at the issuer needs no change in your API."
---

| C# | Safer token practice |
| --- | --- |
| ValidateAudience = false | ValidAudience = "orders-api" |
| IssuerSigningKey from config | Authority with discovery and JWKS |
| ClockSkew of 5 minutes (default) | a smaller skew if needed |
| tokens in URLs for SignalR | only when required, never logged |
| role claims for everything | policies with scopes and resources |
