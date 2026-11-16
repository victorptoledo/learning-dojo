# Browser Security: Same-Origin Policy, XSS, CSP, CORS and Cookies

The browser runs code from strangers all day, so it isolates sites from each other. Frontend
security is about not breaking that isolation yourself: never letting attacker-controlled data
become code, configuring the policies that limit damage, and handling credentials correctly.

## The same-origin policy

Scripts from one origin (scheme + host + port) cannot read the responses, the DOM or the
storage of another origin. A page on `https://evil.example` can **send** a request to
`https://bank.example` (a form post, an image), but cannot **read** the answer. Everything
below either defends that boundary or relaxes it on purpose.

## Cross-site scripting (XSS)

XSS is attacker-controlled data running as script **inside your origin**: it can read the page,
call your APIs with the user's cookies and change what the user sees.

| Kind | Where the payload comes from |
| --- | --- |
| Stored | Saved data (a comment, a profile name) shown to other users |
| Reflected | The request (a `?q=` parameter echoed into the page) |
| DOM-based | Client code moves data from a source (`location.hash`, `postMessage`, storage) to a sink |

The sinks to fear: `innerHTML`, `outerHTML`, `insertAdjacentHTML`, `document.write`,
`eval`, `new Function`, `setTimeout("string")`, and URL attributes (`href`, `src`) that accept
`javascript:` URLs. Defenses, in order:

1. **Treat data as text.** `textContent`, `createElement`, `setAttribute` for non-URL
   attributes. Frameworks escape `{value}` by default; the escape hatches
   (`dangerouslySetInnerHTML`, `v-html`, `[innerHTML]`) bring the risk back.
2. **Validate URLs** before putting them in `href` or `src`: parse with `new URL()` and allow
   only `http:` and `https:` (and maybe `mailto:`).
3. **Sanitize** when you truly must render user HTML (rich-text comments): DOMPurify, or the
   upcoming `setHTML()` Sanitizer API, never a home-made regex.
4. **Content-Security-Policy** as a safety net, and **Trusted Types** to lock the sinks.

## Content Security Policy (CSP)

A response header that tells the browser what the page may load and run:

```http
Content-Security-Policy: default-src 'self'; script-src 'self'; object-src 'none';
  base-uri 'none'; form-action 'self'; frame-ancestors 'none';
  require-trusted-types-for 'script'
```

| Directive | Does |
| --- | --- |
| `default-src 'self'` | Fallback for every fetch type: same origin only |
| `script-src 'self'` | Scripts from your origin; **no inline scripts or `onclick=` attributes**, no `eval` |
| `script-src 'nonce-R4nd0m' 'strict-dynamic'` | A "strict CSP": only scripts with the per-response nonce, and what they load |
| `object-src 'none'` | No plugins |
| `base-uri 'none'` | An injected `<base>` cannot redirect relative URLs |
| `frame-ancestors 'none'` | Nobody may frame you: stops clickjacking (replaces `X-Frame-Options`) |
| `require-trusted-types-for 'script'` | String assignments to sinks like `innerHTML` throw unless they come from a Trusted Types policy |
| `report-to` | Where browsers send violation reports |

Roll out with `Content-Security-Policy-Report-Only` first, read the reports, then enforce.
`'unsafe-inline'` and `'unsafe-eval'` switch off most of the protection.

## CORS: reading cross-origin on purpose

When `https://app.example` calls `fetch("https://api.example/data")`, the browser sends the
request with an `Origin` header and only lets the page read the response if the server
answers with `Access-Control-Allow-Origin: https://app.example` (or `*` for public data).

- "Non-simple" requests (a JSON `content-type`, methods other than GET/HEAD/POST, custom
  headers) are first **preflighted** with an `OPTIONS` request.
- Credentials (cookies) need `credentials: "include"` in fetch and
  `Access-Control-Allow-Credentials: true` with an exact origin (never `*`).
- CORS is enforced by **browsers** to protect users; it does not protect your API from
  `curl`. A CORS error is fixed on the server, not in the frontend.

## Cookies and CSRF

```http
Set-Cookie: session=abc123; Secure; HttpOnly; SameSite=Lax; Path=/; Max-Age=86400
```

- `HttpOnly`: scripts cannot read it, so XSS cannot steal the session.
- `Secure`: HTTPS only. `__Host-` prefixed names add origin locking.
- `SameSite=Lax` (the default in modern browsers): not sent on cross-site subrequests and
  posts, which blocks most **CSRF** (a foreign site making the user's browser perform actions).
  `Strict` is stricter; `None` (with `Secure`) is for deliberate cross-site use.
- For state-changing requests, add a CSRF token or check the `Origin` header on the server.
- Tokens in `localStorage` are readable by any XSS; prefer HttpOnly cookies for sessions.

## More headers worth sending

| Header | Effect |
| --- | --- |
| `Strict-Transport-Security: max-age=63072000; includeSubDomains` | HTTPS only, even for typed `http://` |
| `X-Content-Type-Options: nosniff` | No MIME sniffing: a text file is never run as a script |
| `Referrer-Policy: strict-origin-when-cross-origin` | Do not leak full URLs to other sites |
| `Permissions-Policy: camera=(), geolocation=()` | Turn off powerful features you do not use |
| `Cross-Origin-Opener-Policy: same-origin` | Isolate your window from pages it opens or that open it |

## Third parties and the supply chain

Every script you include runs with your origin's full power. Self-host or pin what you can,
use **Subresource Integrity** (`<script src integrity="sha384-...">`) for CDN files, keep
dependencies updated, and give links that open new tabs `rel="noopener"` (the default for
`target="_blank"` in modern browsers) so the new page cannot control yours.

## Summary

- The same-origin policy is the base; XSS defeats it from inside, so never turn data into code.
- Text APIs, URL validation and real sanitizers prevent XSS; CSP and Trusted Types contain it.
- CORS lets a server allow cross-origin reads; it is configured on the server.
- Sessions live in Secure, HttpOnly, SameSite cookies; CSRF needs SameSite plus tokens or
  Origin checks.
- Send the security headers, and treat third-party scripts as part of your code.

## Further reading

- OWASP, "Cross Site Scripting Prevention Cheat Sheet": <https://cheatsheetseries.owasp.org/cheatsheets/Cross_Site_Scripting_Prevention_Cheat_Sheet.html>
- MDN, "Content Security Policy (CSP)": <https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/CSP>
- W3C, Content Security Policy Level 3: <https://www.w3.org/TR/CSP3/>
- MDN, "Trusted Types API": <https://developer.mozilla.org/en-US/docs/Web/API/Trusted_Types_API>
- MDN, "Cross-Origin Resource Sharing (CORS)": <https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/CORS>
- MDN, "Using HTTP cookies": <https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/Cookies>
- web.dev, "Mitigate cross-site scripting with a strict CSP": <https://web.dev/articles/strict-csp>
- OWASP, "HTTP Security Response Headers Cheat Sheet": <https://cheatsheetseries.owasp.org/cheatsheets/HTTP_Headers_Cheat_Sheet.html>
