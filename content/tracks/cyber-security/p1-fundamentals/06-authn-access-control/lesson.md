# Authentication, Sessions and Access Control

Three separate questions, each with its own failures:

1. **Authentication**: is this really Ada?
2. **Session management**: is this request still from the Ada who logged in?
3. **Access control** (authorisation): may Ada do *this*, to *this object*?

## Authentication

**Factors** are something you know (a password), something you have (a phone, a security
key) and something you are (a fingerprint, unlocking a key). **MFA** combines two kinds.
Ranked from weakest: SMS codes (SIM swapping), TOTP apps, push approvals (fatigue attacks),
and **phishing-resistant** WebAuthn credentials (security keys and **passkeys**), which are
bound to the site's origin so a fake site cannot use them.

A password login still needs care:

- **Store** passwords with a slow, salted KDF (phase 2), never reversibly.
- **Generic errors**: "invalid username or password" for both cases, and the same response
  time, so the form does not reveal which accounts exist (*user enumeration*). Apply the same
  to sign-up and "forgot password" pages.
- **Rate limit** per account *and* per source, with growing delays. Bots try leaked
  username/password pairs from other breaches (**credential stuffing**) or one common password
  against many accounts (**password spraying**). Permanent lockouts let attackers lock
  out real users, so prefer backoff, CAPTCHAs after failures, and alerts.
- **Check new passwords** against breached-password lists (your first kata).
- **Recovery** is an authentication path too: a random, single-use, short-lived reset token
  (store only its hash), sent to the verified address; never email a password; never
  "security questions".

## Sessions

After login the server issues a **session id**: a long random value (at least 128 bits from
a CSPRNG, such as `secrets.token_urlsafe(32)`) that maps to server-side state, or a signed
token (phase 2). Protect it like a password:

```text
Set-Cookie: __Host-sid=Yx7...; Path=/; Secure; HttpOnly; SameSite=Lax; Max-Age=28800
```

| Attribute | Stops |
| --- | --- |
| `Secure` | the cookie being sent over plain HTTP |
| `HttpOnly` | JavaScript (and XSS) reading it |
| `SameSite=Lax` / `Strict` | the browser sending it on cross-site sub-requests and form posts (most CSRF) |
| `__Host-` prefix | the cookie being set for other subdomains or without `Secure` |

Rules: **rotate** the session id at login and at privilege changes (otherwise an attacker who
planted an id before login, *session fixation*, shares the logged-in session); expire after
idle time and after an absolute lifetime; destroy the server-side session at logout;
re-authenticate for sensitive actions (changing the email, the password or MFA).

**CSRF** (cross-site request forgery): another site makes the victim's browser submit a form
to yours, and the browser adds the cookie. Defences: `SameSite` cookies, **anti-CSRF tokens**
in forms (frameworks do this), checking `Origin` / `Sec-Fetch-Site` headers, and never
changing state on `GET`.

## Access control

| Model | Decision based on | Fits |
| --- | --- | --- |
| **RBAC** | the user's roles (`admin`, `support`) | coarse permissions: who may open the admin area |
| **ABAC** / policy-based | attributes of user, object and context (`owner_id`, amount, tenant, time) | "support may refund up to 100 € on orders of their region" |
| **ReBAC** | relationships (Ada is an editor of folder X, which contains doc Y) | sharing models like Google Drive (Zanzibar-style systems) |

Whatever the model:

- **Deny by default**, and enforce on the server for every request, including APIs used only
  by your own front end.
- Check **object-level** access (ownership, tenant) in the data query, not only the role.
- **Centralise** the decision in one policy layer (framework authorisation handlers, a
  policy engine such as OPA or Cedar) and test it with a table of user × action × object.
- Watch for **privilege escalation** through mass assignment: a profile update that also
  accepts `{"role": "admin"}` because the model binds every field.

## Summary

- MFA, ideally phishing-resistant passkeys; generic errors and rate limits on login and recovery.
- Session ids: random, in `Secure; HttpOnly; SameSite` cookies, rotated at login, expired, destroyed at logout.
- CSRF: SameSite plus tokens or Origin checks; no state changes on GET.
- Access control: deny by default, object-level checks, one central policy, tested.

## Further reading

- NIST SP 800-63B-4, *Digital Identity Guidelines: Authentication and Authenticator Management*: <https://pages.nist.gov/800-63-4/sp800-63b.html>
- OWASP Authentication Cheat Sheet: <https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html>
- OWASP Session Management Cheat Sheet: <https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html>
- OWASP Cross-Site Request Forgery Prevention Cheat Sheet: <https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html>
- W3C Web Authentication (WebAuthn) Level 3: <https://www.w3.org/TR/webauthn-3/>
