# Web Hardening: Security Headers, Cookies, CORS and TLS

Browsers have strong security features, but most are **opt-in**: the server has to ask for
them in response headers. A handful of headers, set once in a reverse proxy or middleware,
block whole classes of attacks.

## The headers

| Header | Recommended value | Stops |
| --- | --- | --- |
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains` | downgrade to HTTP and SSL stripping after the first visit |
| `Content-Security-Policy` | see below | most XSS, clickjacking (`frame-ancestors`), data exfiltration |
| `X-Content-Type-Options` | `nosniff` | the browser guessing a script out of an uploaded "image" |
| `Referrer-Policy` | `strict-origin-when-cross-origin` (or `no-referrer`) | URLs with tokens or ids leaking to other sites |
| `Permissions-Policy` | `camera=(), microphone=(), geolocation=()` | embedded or injected code using powerful features |
| `X-Frame-Options` | `DENY` (legacy; CSP `frame-ancestors` replaces it) | clickjacking in old browsers |
| `Cross-Origin-Opener-Policy` | `same-origin` | other windows getting a handle to yours |

Remove headers that only help attackers: `Server: nginx/1.18.0`, `X-Powered-By: PHP/7.4`
advertise exactly which CVEs to try.

**HSTS** tells the browser to use HTTPS only for this host for `max-age` seconds. Start with
a short max-age, then raise it to at least a year; `includeSubDomains` covers subdomains;
`preload` (and submitting to the preload list) protects even the first visit, but is hard to
undo, so add it only when every subdomain is HTTPS.

## Content Security Policy

A strict, nonce-based policy:

```text
Content-Security-Policy: default-src 'self'; script-src 'nonce-{random}' 'strict-dynamic'; object-src 'none'; base-uri 'none'; frame-ancestors 'none'; form-action 'self'
```

- Generate a fresh random **nonce** per response and put it on every legitimate
  `<script nonce="...">`; injected scripts do not know it.
- `'unsafe-inline'` and `'unsafe-eval'` in `script-src` undo most of the protection; so do
  wildcards (`*`, `https:`) and allow-listed CDNs that host user content or JSONP endpoints.
- `frame-ancestors 'none'` (or `'self'`) controls who may frame your pages (clickjacking).
- Roll out with `Content-Security-Policy-Report-Only` and a `report-to` endpoint first, fix
  what breaks, then enforce.

## Cookies

`Set-Cookie: __Host-session=...; Path=/; Secure; HttpOnly; SameSite=Lax`, as in phase 1. Two
more rules: `SameSite=None` requires `Secure` (browsers reject it otherwise), and a cookie
without `Domain` is sent only to the exact host that set it, which is what you want.

## CORS

The **same-origin policy** stops a page on `evil.test` from *reading* responses from your
API. **CORS** headers relax it for origins you choose. The common mistakes:

- reflecting the request's `Origin` back in `Access-Control-Allow-Origin` together with
  `Access-Control-Allow-Credentials: true`: any site can now read your users' data;
- allowing `null` (sandboxed iframes and local files send it) or matching origins with a
  loose regex (`example.com.evil.test`, `notexample.com`);
- thinking CORS is access control: it only tells *browsers* what to expose. `curl` ignores
  it, so the API must still authenticate and authorise every request.

Use a fixed allow-list of exact origins, send `Vary: Origin`, and avoid credentials in CORS
if the API can use tokens instead.

## TLS configuration

TLS 1.2 and 1.3 only; your server's or framework's modern defaults (Mozilla's SSL
Configuration Generator gives tested settings for nginx, Apache, HAProxy and others);
certificates renewed automatically with ACME; OCSP stapling where supported. Check public
sites with SSL Labs' server test and the headers with a scanner, and add both to a periodic
job. The next kata is that header check, written by you.

## Summary

- HSTS, CSP, nosniff, Referrer-Policy and Permissions-Policy are cheap and block whole attack classes.
- A strict CSP uses per-response nonces, no `unsafe-inline`, `object-src 'none'`, `base-uri`, `frame-ancestors`.
- Cookies: `Secure; HttpOnly; SameSite`, `__Host-` prefix; `SameSite=None` needs `Secure`.
- CORS: exact allow-lists, never a reflected origin with credentials; it is not access control.
- Modern TLS defaults, automated certificates, regular scans.

## Further reading

- OWASP HTTP Headers Cheat Sheet: <https://cheatsheetseries.owasp.org/cheatsheets/HTTP_Headers_Cheat_Sheet.html>
- OWASP Secure Headers Project: <https://owasp.org/www-project-secure-headers/>
- RFC 6797, *HTTP Strict Transport Security (HSTS)*: <https://www.rfc-editor.org/rfc/rfc6797>
- W3C Content Security Policy Level 3: <https://www.w3.org/TR/CSP3/>
- MDN, Cross-Origin Resource Sharing (CORS): <https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/CORS>
- Mozilla SSL Configuration Generator: <https://ssl-config.mozilla.org/>
