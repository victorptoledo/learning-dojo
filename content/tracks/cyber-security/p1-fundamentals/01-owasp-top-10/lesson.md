# OWASP Top 10: the Most Common Web Application Risks

The **OWASP Top 10** is a consensus list of the most critical risks to web applications, built
from vulnerability data contributed by many organisations and a community survey. It is not a
complete standard (that is the OWASP ASVS), but it is the shared vocabulary of application
security: interviewers, auditors and scanners all speak it.

## The 2025 list

| # | Category | What it looks like in code |
| --- | --- | --- |
| A01 | **Broken Access Control** | `GET /invoices/42` returns another customer's invoice; a user calls an admin endpoint; SSRF (now part of this category) |
| A02 | **Security Misconfiguration** | debug mode on, default credentials, verbose error pages, a public storage bucket, missing security headers |
| A03 | **Software Supply Chain Failures** | an unpinned dependency, a compromised package, an unprotected build pipeline |
| A04 | **Cryptographic Failures** | passwords hashed with MD5, data sent over plain HTTP, a hard-coded key |
| A05 | **Injection** | SQL built with string concatenation, `os.system("ping " + host)`, XSS |
| A06 | **Insecure Design** | a password reset that answers "what is your pet's name", no rate limit on a costly operation |
| A07 | **Authentication Failures** | no protection against credential stuffing, sessions that never expire, weak recovery |
| A08 | **Software or Data Integrity Failures** | deserialising untrusted objects, auto-updates without signature checks |
| A09 | **Security Logging and Alerting Failures** | failed logins not logged, logs nobody watches, no alert on an obvious attack |
| A10 | **Mishandling of Exceptional Conditions** | an error path that skips the permission check, failing open, leaking stack traces |

Each later lesson of this track goes deeper into one or two of these. This one covers the
first: access control, and SSRF inside it.

## A01: Broken access control

**Authentication** answers *who are you?*; **authorisation** (access control) answers *may you
do this, to this object?*. Most breaches of this kind happen when an app checks the first and
forgets the second.

```python
# Vulnerable: any logged-in user can read any invoice by changing the id (an IDOR,
# insecure direct object reference).
@app.get("/invoices/<int:invoice_id>")
@login_required
def invoice(invoice_id):
    return db.one("SELECT * FROM invoices WHERE id = ?", invoice_id)

# Fixed: the query itself enforces ownership, so there is no path that forgets it.
@app.get("/invoices/<int:invoice_id>")
@login_required
def invoice(invoice_id):
    row = db.one("SELECT * FROM invoices WHERE id = ? AND owner_id = ?", invoice_id, current_user.id)
    if row is None:
        abort(404)   # do not reveal whether the invoice exists
    return row
```

Rules that prevent it:

- **Deny by default.** Every endpoint requires authentication and an explicit permission;
  public endpoints are the reviewed exception.
- **Check on the server, on every request.** Hiding a button is not access control.
- **Check ownership of the object,** not only the role. Random UUIDs make ids harder to guess
  but are not a control on their own.
- **Centralise** the checks (a policy layer, middleware, row-level security) instead of
  repeating `if` statements in each handler.
- **Log denials** and alert when one user triggers many of them.

## SSRF: making the server fetch for the attacker

**Server-side request forgery** happens when the app fetches a URL that the user controls:
link previews, "import from URL", webhooks, PDF renderers. The server sits inside your
network, so the attacker can make it reach things they cannot:

- the cloud **metadata service** at `http://169.254.169.254/`, which can hand out the
  instance's credentials,
- admin interfaces on `localhost` or internal hosts (`http://10.0.0.5:8080/admin`),
- other protocols (`file:///etc/passwd`, `gopher://`) if the client library supports them.

Defend in layers:

1. Allow only `https` (and maybe `http`) and, when possible, an **allow-list of hosts**.
2. **Resolve the name and check the IP** against private, loopback and link-local ranges,
   then connect to that same IP (otherwise DNS rebinding swaps the answer between check and
   use). Do not follow redirects, or check each hop again.
3. Block the egress at the network level: the service that fetches URLs runs where it cannot
   reach the metadata endpoint or internal subnets. On AWS, require **IMDSv2**, which needs a
   session token that a simple forged GET cannot get.
4. Never return the raw response to the user; return only what the feature needs.

> [!WARNING]
> A regular expression on the URL string is not an SSRF defence: `http://2130706433/`,
> `http://[::1]/`, `http://localhost.example.com` (a DNS name pointing to 127.0.0.1) and
> redirects all get past naive checks.

## Summary

- The OWASP Top 10:2025 lists ten risk categories; access control is first.
- Authentication is not authorisation: check ownership on the server, on every request,
  deny by default, and log denials.
- SSRF turns your server into the attacker's proxy; defend with allow-lists, IP checks after
  resolution, no redirects and egress controls.

## Further reading

- OWASP Top 10:2025: <https://owasp.org/Top10/>
- OWASP Authorization Cheat Sheet: <https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html>
- OWASP SSRF Prevention Cheat Sheet: <https://cheatsheetseries.owasp.org/cheatsheets/Server_Side_Request_Forgery_Prevention_Cheat_Sheet.html>
- OWASP Application Security Verification Standard (ASVS): <https://owasp.org/www-project-application-security-verification-standard/>
- AWS, *Use IMDSv2*: <https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/configuring-instance-metadata-service.html>
