---
from: go
status: draft
concept: "API keys, OAuth 2.x and scopes"
from_way: "Middleware that checks `r.URL.Query().Get(\"key\")`."
native_way: "Middleware reading `Authorization: Bearer`, verifying JWTs with a JWKS (for example `github.com/lestrrat-go/jwx`), putting claims in the `context`, and scope checks per handler."
sensei_tip: "`subtle.ConstantTimeCompare` compares API key hashes safely; store only `sha256` of keys."
---

| Go | Auth concept |
| --- | --- |
| `strings.CutPrefix(h, "Bearer ")` | reading the token |
| JWKS-based verification | signature, iss, aud, exp |
| `context.WithValue(ctx, claimsKey, c)` | claims for handlers |
| `subtle.ConstantTimeCompare` | safe key comparison |
| `w.Header().Set("WWW-Authenticate", ...)` | the challenge |
