---
from: go
status: draft
concept: "Tokens that are short-lived, fully validated and rotatable"
from_way: "jwt.Parse with a keyfunc that returns the key without checking token.Method."
native_way: "golang-jwt with jwt.WithValidMethods, WithAudience and WithIssuer, keys selected by kid from a JWKS cache."
sensei_tip: "`jwt.Parse(token, keyfunc, jwt.WithValidMethods([]string{\"EdDSA\"}), jwt.WithAudience(\"orders-api\"), jwt.WithIssuer(iss))` pins everything the token should not decide."
---

| Go | Safer token practice |
| --- | --- |
| keyfunc ignoring token.Method | jwt.WithValidMethods(...) |
| no audience check | jwt.WithAudience("orders-api") |
| one []byte secret in code | a JWKS keyed by kid |
| tokens valid for days | minutes, refresh tokens rotated |
| no per-client limits | rate limiting middleware |
