---
from: python
status: draft
concept: "Tokens that are short-lived, fully validated and rotatable"
from_way: "jwt.decode(token, options={\"verify_signature\": False}) to read claims, or decode without algorithms=."
native_way: "PyJWT or joserfc with an explicit algorithms list, audience and issuer, keys from a JWKS client, and short expiry."
sensei_tip: "`jwt.decode(token, key, algorithms=[\"ES256\"], audience=\"orders-api\", issuer=ISSUER)` in PyJWT validates exp, aud and iss in one call; leaving out `audience` raises an error if the token has one, which is a feature."
---

| Python | Safer token practice |
| --- | --- |
| verify_signature False | decode with key and algorithms |
| algorithms not given | algorithms=["ES256"] |
| HS256 with 'secret' | ES256 or EdDSA, or 256 random bits |
| tokens in query strings | Authorization header only |
| static key in settings | PyJWKClient and kid |
