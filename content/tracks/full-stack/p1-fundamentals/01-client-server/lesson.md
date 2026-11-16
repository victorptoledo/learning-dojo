# HTTP Between Browser and Server

The Backend Developer track covers HTTP from the server's side. A full stack developer also has
to know what the **browser** adds to each request and enforces on each answer, because that is
where most integration bugs appear: a cookie that is not sent, a CORS error, a cached answer that
should not be.

## Methods and status codes, seen from the client

| Method | Safe | Idempotent | Typical use from a page |
| --- | --- | --- | --- |
| `GET` | yes | yes | load a page or data; links, prefetchers and caches may repeat it |
| `POST` | no | no | create, submit a form, run an action |
| `PUT` | no | yes | replace a resource |
| `PATCH` | no | not necessarily | change some fields |
| `DELETE` | no | yes | remove |

Never change state on `GET`: mail scanners and link previews follow links. And pick statuses
the UI can act on:

| Status | The UI should... |
| --- | --- |
| `200`, `201`, `204` | show the result (`201` with a `Location`, `204` with no body) |
| `303` | follow it (browsers do it for you) |
| `304` | use its cached copy (the next lessons) |
| `400`, `422` | show the message, ideally under the field it belongs to |
| `401` | send the user to log in |
| `403` | explain that they may not do it |
| `404` | show "not found", not a crash |
| `409`, `412` | someone else changed it: reload and let the user decide |
| `429` | wait (`Retry-After`) and retry |
| `5xx` | show a generic error with a reference the user can report |

## Cookies

The server sets a cookie with `Set-Cookie`; the browser sends it back with every request to
that site in a `Cookie` header, from any page, including requests started by other sites. The
attributes decide when:

```http
Set-Cookie: sid=9f3c...; Path=/; Secure; HttpOnly; SameSite=Lax; Max-Age=86400
```

- `Secure`: only over HTTPS.
- `HttpOnly`: invisible to JavaScript (`document.cookie`), so an XSS bug cannot read it.
- `SameSite=Lax` (the browsers' default): not sent on cross-site subresource requests or
  cross-site POSTs, but sent when the user follows a link to your site. `Strict` is stricter;
  `None` (which needs `Secure`) sends it everywhere and is only for real cross-site use.
- `Domain` and `Path` limit where it is sent; without `Domain`, only the exact host gets it.
- `Max-Age` / `Expires`: without them it is a session cookie, gone when the browser closes.

## The same-origin policy and CORS

An **origin** is scheme + host + port: `https://app.example.com` and `https://api.example.com`
are different origins. The **same-origin policy** lets a page *send* requests anywhere but only
*read* answers from its own origin. **CORS** is how a server tells the browser "this other
origin may read my answers":

```http
Access-Control-Allow-Origin: https://app.example.com
Access-Control-Allow-Credentials: true
Vary: Origin
```

Requests that are not "simple" (a `PUT`, a JSON `Content-Type`, a custom header) first send a
**preflight**:

```http
OPTIONS /api/profile
Origin: https://app.example.com
Access-Control-Request-Method: PUT
Access-Control-Request-Headers: content-type
```

and the browser only sends the real request if the answer allows that method and those headers.

Rules that save hours:

- CORS is enforced by **browsers only**. `curl` and server-to-server calls ignore it. It is not
  an access control for your API; authentication is.
- With cookies (`fetch(url, { credentials: "include" })`), the server must answer with the exact
  origin, never `*`, plus `Access-Control-Allow-Credentials: true`.
- A "CORS error" hides the real status. Look at the network tab: the failing request is often
  the preflight, or a 500 without CORS headers.

## Avoid CORS when you can

The simplest CORS setup is none. Serve the front end and the API from **one origin**: a reverse
proxy (nginx, a CDN rule, your framework's dev server proxy) sends `/api/*` to the API and
everything else to the front end. Cookies then stay first-party, there is no preflight, and
`SameSite=Lax` just works. Use cross-origin APIs when you really have several front ends on
different domains or a public API.

## Summary

- Keep `GET` safe and pick statuses the UI can act on.
- Cookie attributes decide when the browser sends a cookie: use `Secure; HttpOnly; SameSite=Lax`
  for sessions.
- The same-origin policy blocks reading cross-origin answers; CORS headers relax it, with
  preflights for non-simple requests and the exact origin when credentials are involved.
- One origin with a reverse proxy avoids CORS entirely.

## Further reading

- MDN, "HTTP request methods": <https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Methods>
- MDN, "Using HTTP cookies": <https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/Cookies>
- MDN, "Cross-Origin Resource Sharing (CORS)": <https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/CORS>
- MDN, "Same-origin policy": <https://developer.mozilla.org/en-US/docs/Web/Security/Same-origin_policy>
- RFC 9110, "HTTP Semantics": <https://www.rfc-editor.org/rfc/rfc9110>
- In the dojo: the API Design track's *HTTP Semantics* and *Choosing HTTP Status Codes*, and the Frontend track's *Browser Security*.
