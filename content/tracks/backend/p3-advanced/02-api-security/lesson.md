# API Security: The OWASP API Top 10

APIs are the front door to your data, and attackers call them directly: no browser, no UI limits,
scripts that try every id. The **OWASP API Security Top 10** lists the risks seen most often in real
breaches. Most of them are not exotic: they are missing checks in ordinary code.

## The risks, and what stops them

### 1. Broken object-level authorization (BOLA)

`GET /invoices/1043` returns an invoice. The attacker changes it to `1044` and gets someone else's.
The most common API vulnerability.

- Check, for **every object**, that the caller may access it: load by id **and** owner
  (`findByIdAndOwnerId`), or check after loading.
- Answer `404` for objects the caller may not see. Unguessable ids (UUIDs) help but are not a
  substitute for the check.

### 2. Broken authentication

Weak passwords, no rate limit on login, tokens that never expire, JWTs accepted with `alg: none` or
an unverified signature. Use proven libraries and an identity provider, short-lived tokens, rate
limits and MFA on login.

### 3. Broken object property-level authorization (mass assignment, excessive data)

- **Mass assignment**: binding the request straight onto an entity lets a client send
  `"role": "ADMIN"` or `"priceCents": 1`. Bind to **input records** that contain only what the client
  may set.
- **Excessive data exposure**: returning the whole entity (password hash, internal notes) and hoping
  the client hides it. Return **output records** with only what the caller may see.

### 4. Unrestricted resource consumption

No limits on request size, page size, upload size, query complexity or request rate lets one
client exhaust memory, CPU, database or your cloud bill. Set limits everywhere: maximum page size,
body size, timeouts, rate limits per client (`429`).

### 5. Broken function-level authorization

The admin endpoint is hidden from the UI but reachable: `DELETE /admin/users/7`. Enforce roles on
the server for every endpoint, deny by default (`anyRequest().authenticated()`, then allow).

### 6. Unrestricted access to sensitive business flows

Buying all the concert tickets with a bot, creating thousands of accounts for a referral bonus.
Detect and limit automated abuse of valuable flows: rate limits per account and device, CAPTCHAs,
purchase limits.

### 7. Server-side request forgery (SSRF)

An endpoint fetches a URL the client provides (an avatar, a webhook). The attacker gives
`http://169.254.169.254/` (the cloud metadata service) or an internal admin address. Allow-list
destinations, block private and link-local ranges, and do not follow redirects blindly.

### 8. Security misconfiguration

Debug endpoints and actuator open to the world, stack traces in errors, permissive CORS, missing TLS,
default credentials, verbose headers. Harden defaults and review configuration like code.

### 9. Improper inventory management

Old API versions and forgotten test hosts still running, unpatched, with production data. Keep an
inventory of what is deployed, retire old versions, and document every endpoint.

### 10. Unsafe consumption of APIs

Trusting data from third-party APIs more than user input: no validation, no timeouts, following
their redirects. Treat external responses as untrusted input.

## Input: validate, bind, encode

- **Validate** every input against an allow-list (types, lengths, formats, ranges) at the edge.
- **Bind** values as parameters in SQL, never concatenate (SQL injection). The same idea applies to
  LDAP, OS commands and templates.
- **Encode** output for its context (HTML, JSON, logs) so data never becomes code.

## Secrets

- Never in the repository, never in logs, never in error messages or URLs.
- From the environment or a secret manager; rotate them; give each service only what it needs.
- Scan commits and images for leaked keys.

## Transport and browsers

- **TLS everywhere**, including between internal services where possible; HSTS for browsers.
- **CORS** controls which browser origins may call your API: list the allowed origins, never reflect
  any origin together with `Allow-Credentials`.
- Cookies: `Secure`, `HttpOnly`, `SameSite`.

## Make it part of the process

Threat-model new features ("what could an attacker do with this endpoint?"), add security tests
(another user's id answers 404, oversized pages answer 400, a missing role answers 403), run
dependency scanning, and keep libraries patched.

## Summary

- Check object ownership on every request (BOLA), and roles on every function.
- Bind input records, return output records: no mass assignment, no excessive data.
- Limit sizes, pages, rates and time; protect valuable flows from automation.
- Block SSRF with allow-lists; harden configuration; know what is deployed; distrust third-party data.
- Validate, bind parameters, encode output; keep secrets out of code and logs; TLS and strict CORS.
