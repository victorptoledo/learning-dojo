---
from: java
status: draft
concept: "Identity security"
from_way: "Configuring security filters through XML or tangled bean chains, storing passwords hashed with MD5, and long-lived tokens full of user data."
native_way: "Delegated login with OAuth2/OIDC, argon2id or bcrypt when passwords are stored, short-lived signed JWTs verified by expected algorithm, expiry, issuer and audience, and per-object authorization."
sensei_tip: "Never put sensitive data in a JWT payload: it is only Base64-encoded and anyone can read it."
---

| Habit | Modern practice |
| --- | --- |
| `MessageDigest.getInstance("MD5")` for passwords | argon2id / bcrypt |
| One JWT valid for 30 days | access token of minutes + refresh token |
| Trusting the `alg` in the header | the verifier fixes the algorithm |
| Password grant in the mobile app | authorization code + PKCE |
