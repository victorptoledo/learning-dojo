# Tokens and API Security: JWT, OAuth 2.0 and Key Rotation

APIs authenticate every request with a **token**. There are two kinds:

| Kind | What the token is | Validating it | Revoking it |
| --- | --- | --- | --- |
| **Opaque** | a random reference (`secrets.token_urlsafe(32)`) | look it up in a store | delete it from the store |
| **Self-contained** | signed claims (JWT, PASETO, your own format) | check the signature and claims locally | hard: it stays valid until it expires |

Self-contained tokens save a lookup per request and work across services, at the price of
revocation. So keep them **short-lived** (5–15 minutes) and pair them with a **refresh
token** (opaque, stored, rotated at every use, revocable) to get new ones.

## JWT, and how it goes wrong

A JWT (RFC 7519) is `base64url(header).base64url(payload).base64url(signature)`:

```json
{"alg": "ES256", "kid": "2026-11", "typ": "JWT"}
{"iss": "https://auth.example.com", "aud": "orders-api", "sub": "42", "exp": 1794700900, "iat": 1794700000}
```

It is **signed, not encrypted**: anyone can read the payload, so never put secrets in it.
The classic vulnerabilities all come from trusting the token to say how to verify itself:

- **`alg: none`**: some libraries accepted unsigned tokens. Pin the algorithms you accept.
- **Algorithm confusion**: a server expecting RS256 is sent an HS256 token whose HMAC key is
  the server's *public* key, which the attacker knows. Pinning the algorithm per key stops it.
- **Weak HMAC secrets**: HS256 tokens can be brute-forced offline from one sample. Use 256
  random bits, or asymmetric keys.
- **Missing claim checks**: always check `exp` (and `nbf`) with a small clock skew, `iss`, and
  `aud`, so a token issued for one API cannot be replayed against another.
- **`kid` and `jku` injection**: the key id selects a key from *your* key set; never use it as
  a file path or SQL value, and never fetch keys from a URL the token names.

RFC 8725 (*JWT Best Current Practices*) lists these rules; follow it or use a well-maintained
library configured with explicit algorithms, issuer and audience.

## OAuth 2.0 and OpenID Connect

**OAuth 2.0** (RFC 6749, and the OAuth 2.1 consolidation) lets a client get an **access
token** for an API without handling the user's password. **OpenID Connect** adds an **ID
token** that tells the client who logged in.

| Situation | Flow |
| --- | --- |
| Web, mobile or single-page app acting for a user | **Authorization code with PKCE** through the browser |
| Service calling a service, no user | **Client credentials** |
| TV, CLI without a browser | Device authorization grant |
| Implicit and password grants | deprecated: they put tokens in URLs and passwords in clients |

PKCE (RFC 7636) binds the code to a secret the client generated, so an intercepted code is
useless. Validate the `state` parameter (CSRF), register exact redirect URIs, and request
minimal **scopes**. For browser apps, the **backend-for-frontend** pattern keeps tokens on the
server and gives the browser only an `HttpOnly` session cookie.

## Key rotation

Every signing key will need to be replaced: on a schedule, when people leave, after a leak.
Design for it from day one:

1. Each token names its key (`kid`), and verifiers hold a **set** of keys by id (published
   as a JWKS at a well-known URL for asymmetric keys).
2. **Add** the new key to every verifier first, then start **signing** with it.
3. Keep the old key for verification until the longest-lived token signed with it expires,
   then **remove** it. After a leak, remove it at once and accept that users log in again.

## API security beyond tokens

The **OWASP API Security Top 10 (2023)** shows that most API breaches are authorisation
bugs, not crypto: **broken object level authorisation** (BOLA, the API name for IDOR),
broken authentication, **broken object property level authorisation** (mass assignment and
excessive data exposure), **unrestricted resource consumption** (no rate limits or page size
limits), broken function level authorisation (admin endpoints), unrestricted access to
sensitive business flows (bots buying all the tickets), SSRF, misconfiguration, poor
inventory (forgotten `v1` endpoints) and unsafe consumption of third-party APIs. Defences:
the access control rules of phase 1, rate limits and quotas per client, response schemas
that list exactly the fields returned, and an inventory of every API version that is live.

## Summary

- Opaque tokens revoke easily; signed tokens must be short-lived, with rotating refresh tokens.
- Validate JWTs completely: pinned algorithm, signature, `exp`/`nbf`, `iss`, `aud`, known `kid`.
- OAuth: authorization code with PKCE for users, client credentials for services; OIDC for identity.
- Rotate keys with key ids: add, switch, retire.
- Most API breaches are authorisation and resource limits (OWASP API Top 10).

## Further reading

- RFC 8725, *JSON Web Token Best Current Practices*: <https://www.rfc-editor.org/rfc/rfc8725>
- RFC 9700, *Best Current Practice for OAuth 2.0 Security*: <https://www.rfc-editor.org/rfc/rfc9700>
- RFC 7636, *Proof Key for Code Exchange (PKCE)*: <https://www.rfc-editor.org/rfc/rfc7636>
- OWASP JSON Web Token Cheat Sheet for Java: <https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_for_Java_Cheat_Sheet.html>
- OWASP API Security Top 10 (2023): <https://owasp.org/API-Security/editions/2023/en/0x11-t10/>
- OpenID Connect Core 1.0: <https://openid.net/specs/openid-connect-core-1_0.html>
