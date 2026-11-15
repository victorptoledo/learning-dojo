---
from: python
status: draft
concept: "Identity security"
from_way: "Django sessions or FastAPI with OAuth2PasswordBearer and python-jose for JWTs."
native_way: "Choosing sessions, tokens or OAuth2/OIDC flows deliberately, with safe storage and verification."
sensei_tip: "Validate every JWT field that matters: signature with a fixed algorithm, exp, iss and aud. Libraries do it only when you pass the expected values."
---

| Python | In practice |
| --- | --- |
| Django session cookie | a server-side session |
| python-jose decode | verify signature, exp, iss, aud |
| Authlib | OAuth2 / OIDC client or server |
| passlib bcrypt | a slow password hash (bcrypt, argon2) |
