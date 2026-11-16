---
from: python
status: draft
concept: "API keys, OAuth 2.x and scopes"
from_way: "A shared secret compared with `==` in every view."
native_way: "FastAPI `Security(oauth2_scheme, scopes=[...])` with `SecurityScopes`, PyJWT or Authlib verifying signature, `iss`, `aud` and `exp`; `hmac.compare_digest` for API keys."
sensei_tip: "Compare secrets with `hmac.compare_digest`, never `==`: a normal string comparison leaks timing information."
---

| Python | Auth concept |
| --- | --- |
| `OAuth2AuthorizationCodeBearer` | OAuth scheme |
| `SecurityScopes` | required scopes |
| `jwt.decode(..., audience=...)` | audience check |
| `hmac.compare_digest` | safe key comparison |
| `HTTPException(401, headers={"WWW-Authenticate": ...})` | the challenge |
