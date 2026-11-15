---
from: javascript
status: draft
concept: "Identity security"
from_way: "Passport, express-session and jsonwebtoken, and tokens kept in localStorage."
native_way: "Sessions or tokens chosen by threat model, with PKCE for public clients."
sensei_tip: "Tokens in localStorage are readable by any injected script. Prefer HttpOnly cookies for browsers, and the authorization code flow with PKCE for SPAs and mobile apps."
---

| JavaScript / TypeScript | In practice |
| --- | --- |
| jsonwebtoken.verify(token, key) | verify with algorithms, issuer and audience |
| localStorage token | HttpOnly Secure cookie |
| passport-oauth2 | authorization code + PKCE |
| bcrypt | slow password hashing |
