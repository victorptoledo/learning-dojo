---
from: javascript
status: draft
concept: "Tokens that are short-lived, fully validated and rotatable"
from_way: "jsonwebtoken.verify(token, secret) without options, and tokens kept in localStorage."
native_way: "jose's jwtVerify with a remote JWKS, explicit algorithms, issuer and audience, and a backend-for-frontend that keeps tokens out of the browser."
sensei_tip: "`jwtVerify(token, createRemoteJWKSet(url), { issuer, audience, algorithms: ['ES256'] })` from the `jose` package covers the RFC 8725 checks and caches keys by kid."
---

| JavaScript / TypeScript | Safer token practice |
| --- | --- |
| jwt.verify(token, secret) | jwtVerify with issuer, audience, algorithms |
| jwt.decode() to trust claims | only after verification |
| localStorage tokens | BFF with HttpOnly session cookie |
| implicit flow in the SPA | authorization code with PKCE |
| no rate limiting | per-client limits and quotas |
