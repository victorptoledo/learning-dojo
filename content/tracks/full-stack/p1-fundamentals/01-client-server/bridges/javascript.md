---
from: javascript
status: reviewed
concept: "Why the browser blocks what Node.js allows"
from_way: "The same fetch works in a Node.js script and fails in the browser with a CORS error, which feels arbitrary."
native_way: "The browser protects the user's cookies with the same-origin policy; the server must opt in with CORS headers, and Node.js has no such policy."
sensei_tip: "In Vite or Next.js dev servers, a proxy for /api makes the browser see one origin: no CORS in development, and the same setup can ship to production."
---

| JavaScript | What happens |
| --- | --- |
| `fetch(url, { credentials: "include" })` | cookies sent cross-origin, server must allow credentials |
| `document.cookie` | cannot see `HttpOnly` cookies |
| `fetch` with `Content-Type: application/json` cross-origin | triggers an OPTIONS preflight |
| `server.proxy` in Vite | one origin, no CORS |
