---
from: java
status: draft
concept: "Tokens that are short-lived, fully validated and rotatable"
from_way: "A homemade JWT filter that parses the token with a library default and checks only the signature."
native_way: "Spring Security's OAuth2 resource server with a JWKS URI, which validates signature, exp, nbf and issuer, plus an audience validator you add."
sensei_tip: "Spring's `JwtDecoders.fromIssuerLocation(issuer)` validates the issuer for you, but not the audience: add a `JwtClaimValidator` for `aud` or another API's tokens will be accepted."
---

| Java | Safer token practice |
| --- | --- |
| Jwts.parser() with a static secret | NimbusJwtDecoder with a JWKS URI |
| no aud check | a JwtClaimValidator for aud |
| 30-day access tokens | minutes, plus rotating refresh tokens |
| password grant from the mobile app | authorization code with PKCE |
| one signing key forever | kid and a JWKS with rotation |
