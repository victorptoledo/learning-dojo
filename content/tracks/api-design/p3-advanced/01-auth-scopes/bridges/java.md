---
from: java
status: draft
concept: "API keys, OAuth 2.x and scopes"
from_way: "A custom filter that reads a token from a query parameter and checks it against a table."
native_way: "Spring Security's OAuth2 resource server: `oauth2ResourceServer(o -> o.jwt(...))`, `@PreAuthorize(\"hasAuthority('SCOPE_orders:write')\")`, and object checks in the service."
sensei_tip: "Spring Security maps each `scope` claim to an authority `SCOPE_<scope>`; the `aud` claim needs an explicit validator (`JwtClaimValidator`) unless you set `spring.security.oauth2.resourceserver.jwt.audiences`."
---

| Java / Spring | Auth concept |
| --- | --- |
| `oauth2ResourceServer().jwt()` | resource server |
| `SCOPE_orders:write` | scope as authority |
| `@PreAuthorize` | operation-level check |
| `spring...jwt.audiences` | audience check |
| `BearerTokenAuthenticationEntryPoint` | 401 with WWW-Authenticate |
