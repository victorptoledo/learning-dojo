# API Keys, OAuth 2.x and Scopes

Authentication answers *who is calling*; authorisation answers *what they may do*. For an API
designer the questions are which credentials the API accepts, how they travel, how fine-grained
permissions are expressed, and what the API answers when they are missing or insufficient.

> The Backend track's "Authentication: Sessions, JWT, OAuth2 and OIDC"
> (`backend.p1.auth-security`) explains the protocols and tokens; the Spring Boot track's
> "Security" (`spring-boot.p3.security`) implements them. This lesson is about the API surface.

## API keys

A long random secret that identifies a **client application** (not a user).

- Send it in a header: `Authorization: Bearer sk_live_...` or a dedicated `Api-Key:` header;
  **never** in the query string (it ends up in logs, browser history and `Referer`).
- Make keys recognisable and scannable: a prefix (`sk_live_`, `ghp_`) lets secret scanners find
  leaked keys in public repositories.
- Store only a hash, show the key once, allow several active keys per client so they can
  **rotate** without downtime, and let clients revoke them.
- Good for server-to-server integrations and for identifying who to rate-limit or bill. Not
  for acting on behalf of an end user, and never embed one in a mobile or browser app.

## OAuth 2.x: delegated access

OAuth lets a user grant an application **limited** access to their data without giving it their
password. The application receives an **access token** (opaque or a JWT) from an
**authorisation server** and sends it to the API (the **resource server**):

```http
GET /v1/calendars/primary/events HTTP/1.1
Authorization: Bearer eyJhbGciOiJFUzI1NiIs...
```

| Grant (flow) | For | Notes |
| --- | --- | --- |
| Authorization Code **+ PKCE** | web, mobile and single-page apps acting for a user | the only user flow OAuth 2.1 keeps; PKCE is required for every client |
| Client Credentials | a service acting as itself (machine to machine) | no user involved |
| Device Authorization (RFC 8628) | TVs, CLIs without a browser | the user approves on another device |
| ~~Implicit~~, ~~Password~~ | | removed in OAuth 2.1 and discouraged by the OAuth Security BCP (RFC 9700) |

**OpenID Connect** adds identity (an ID token, the `userinfo` endpoint) on top of OAuth. Use it
for *login*; use OAuth access tokens for *API calls*.

## Scopes: designing permissions

A **scope** is a named permission the client asks for and the user approves:
`orders:read`, `orders:write`, `payments:refund`.

- Name scopes after **resources and actions**, consistently: `resource:action`.
- Keep reads and writes apart; put dangerous actions (refunds, deletes, admin) in their own
  scopes so clients can ask for the least they need.
- Not too fine (users will not read forty checkboxes), not too coarse (`full_access`).
- Scopes limit what the *client* may do; the user's own permissions still apply. A token with
  `orders:read` must not read another tenant's orders: check **object-level authorisation** on
  every request (OWASP API1:2023, "Broken Object Level Authorization", see
  `backend.p3.api-security`).
- Document the required scopes per operation; OpenAPI does it with `security` requirements
  (next lessons).

## Answers on the wire

| Situation | Status | Headers |
| --- | --- | --- |
| no token, malformed or expired token | `401` | `WWW-Authenticate: Bearer error="invalid_token"` |
| valid token, missing scope | `403` | `WWW-Authenticate: Bearer error="insufficient_scope", scope="orders:write"` |
| valid token, object belongs to someone else | `404` (to avoid leaking existence) or `403` | |

RFC 6750 defines those `error` values; problem details bodies can repeat them for humans.

## Tokens: what the API should check

For a JWT access token: signature (keys from the authorisation server's JWKS), `iss`, `aud`
(this API, so a token for another API is rejected), `exp`/`nbf`, then scopes and the subject.
Prefer short-lived access tokens (minutes) with refresh tokens over long-lived ones; consider
sender-constrained tokens (DPoP, RFC 9449, or mTLS) for high-value APIs.

## Further reading

- RFC 6749, "The OAuth 2.0 Authorization Framework": <https://www.rfc-editor.org/rfc/rfc6749>
- OAuth 2.1 draft (IETF OAuth working group): <https://datatracker.ietf.org/doc/draft-ietf-oauth-v2-1/>
- RFC 9700, "Best Current Practice for OAuth 2.0 Security": <https://www.rfc-editor.org/rfc/rfc9700>
- RFC 7636, "Proof Key for Code Exchange (PKCE)": <https://www.rfc-editor.org/rfc/rfc7636>
- RFC 6750, "Bearer Token Usage": <https://www.rfc-editor.org/rfc/rfc6750>
- OWASP API Security Top 10 (2023): <https://owasp.org/API-Security/editions/2023/en/0x11-t10/>
- Google Cloud, "Best practices for managing API keys": <https://cloud.google.com/docs/authentication/api-keys-best-practices>
