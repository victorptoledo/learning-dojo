---
from: javascript
status: draft
concept: "API keys, OAuth 2.x and scopes"
from_way: "API keys in `?apiKey=` and JWTs decoded without verifying."
native_way: "`jose` (`jwtVerify` with a remote JWKS, issuer and audience), scope checks per route, `crypto.timingSafeEqual` for API keys."
sensei_tip: "`jsonwebtoken.decode()` does **not** verify anything; use `jwtVerify` from `jose` with `createRemoteJWKSet`."
---

| JavaScript / TypeScript | Auth concept |
| --- | --- |
| `createRemoteJWKSet(url)` | authorisation server keys |
| `jwtVerify(token, jwks, { issuer, audience })` | token validation |
| `payload.scope.split(' ')` | scopes |
| `crypto.timingSafeEqual` | safe key comparison |
| `res.set('WWW-Authenticate', 'Bearer error="insufficient_scope"')` | 403 challenge |
