# Authentication: Sessions, JWT, OAuth2 and OIDC

Every protected request answers two questions. **Authentication**: who is this? **Authorization**:
may they do this? This lesson covers how identity is proved and carried, the formats you will
meet (sessions, JWTs), and the protocols that let one login work across many services (OAuth2 and
OpenID Connect).

## Passwords

If you store passwords at all:

- hash them with an **adaptive, salted** algorithm: **argon2id** or **bcrypt** (never MD5, SHA-1 or
  plain SHA-256: they are fast, which helps attackers try billions of guesses);
- compare in constant time (the libraries do);
- rate-limit and lock out login attempts, and offer a second factor.

Often the better answer is to **not** store passwords: delegate login to an identity provider.

## Sessions or tokens

After login the client needs something to send on every request.

| | Server session | Self-contained token (JWT) |
| --- | --- | --- |
| What the client holds | an opaque random id in a cookie | a signed token with claims |
| Where the state lives | server side (memory, Redis, database) | inside the token |
| Lookup per request | yes | no: verify the signature |
| Revoke immediately | yes: delete the session | hard: wait for expiry, or keep a deny list |
| Fits | browser apps on one domain | APIs, mobile apps, service-to-service |

Neither is "more secure" by itself. Sessions are simple and revocable; tokens avoid a shared store
and work across services. Many systems use both: a session cookie for the web app, short-lived
tokens for APIs.

Cookies carrying sessions need `HttpOnly` (no JavaScript access), `Secure` (HTTPS only) and
`SameSite=Lax` or `Strict` (CSRF protection).

## JSON Web Tokens

A JWT is three Base64url parts separated by dots: `header.payload.signature`.

```text
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9         {"alg":"HS256","typ":"JWT"}
.eyJzdWIiOiJhZGEiLCJleHAiOjE3OTk5OTk5OTl9    {"sub":"ada","exp":1799999999}
.Qm9hcmQtc2lnbmF0dXJlLWJ5dGVz...             HMAC-SHA256(header.payload, key)
```

- **Claims**: `sub` (who), `exp` (expiry, seconds since the epoch), `iat` (issued at), `iss`
  (issuer), `aud` (intended audience), plus your own (`roles`, `scope`).
- **Signature**: `HS256` uses one shared secret (HMAC); `RS256`/`ES256` sign with a private key and
  verify with a public key, so verifiers cannot create tokens. Issuers publish their public keys as
  a **JWKS** document.

Verifying a token means, in order:

1. the header names an algorithm you **expect** (reject `none`, and never let the token choose the
   algorithm);
2. the **signature** matches, computed with your key and compared in constant time;
3. `exp` is in the future (allow a little clock skew), `iss` and `aud` are what you expect.

> [!WARNING]
> The payload is only **encoded**, not encrypted: anyone can read it. Never put secrets or
> sensitive personal data in a JWT. Keep tokens short-lived (minutes), and use refresh tokens to get
> new ones.

## OAuth2 and OpenID Connect

**OAuth2** lets a client get an **access token** for an API, issued by an **authorization server**,
without ever seeing the user's password. **OpenID Connect** (OIDC) adds an **ID token** that tells
the client who the user is.

The flow for apps acting for a user is the **authorization code flow with PKCE**:

```text
1. App → browser → Authorization server: /authorize?client_id=...&code_challenge=...
2. User logs in there (password, MFA, social login) and consents.
3. Authorization server → app: redirect with a short-lived code.
4. App → Authorization server: /token with the code and the PKCE code_verifier.
5. Authorization server → app: access token (+ ID token, + refresh token).
6. App → API: Authorization: Bearer <access token>.
```

PKCE proves that the app redeeming the code is the one that started the flow, which protects
public clients (SPAs, mobile apps) that cannot keep a secret.

For **service-to-service** calls without a user, the **client credentials** flow gives a service a
token for itself. The old "implicit" and "password" flows are deprecated.

Your API, the **resource server**, only validates access tokens (signature, `exp`, `iss`, `aud`,
scopes) and never handles passwords.

## Authorization models

- **RBAC** (role-based): users have roles (`ADMIN`, `SUPPORT`), roles grant permissions. Simple,
  coarse.
- **Scopes** in tokens: what the client may do on the user's behalf (`orders:read`).
- **ABAC / ownership** rules: decided by attributes of the user and the object ("the owner of the
  order", "same tenant"). Most real APIs need these on top of roles, checked in the service for
  every object.

## Summary

- Hash passwords with argon2id or bcrypt, or better, delegate login to an identity provider.
- Sessions: opaque id, state on the server, easy revocation. JWTs: signed claims, no lookup, hard to
  revoke, readable by anyone.
- Verify a JWT by expected algorithm, signature, `exp`, `iss` and `aud`; keep it short-lived.
- OAuth2 authorization code + PKCE for users, client credentials for services; OIDC adds identity.
- Combine roles and scopes with per-object ownership checks.
