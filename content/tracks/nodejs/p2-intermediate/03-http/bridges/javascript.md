---
from: javascript
status: draft
concept: "The other side of fetch"
from_way: "You call fetch from the browser and read response.status and response.json()."
native_way: "You also write the server: node:http receives the request as a stream, and you decide status, headers and body; fetch is the same API for calling other services."
sensei_tip: "Everything you check in a browser's network tab (status, content-type, CORS headers, keep-alive) is now something your code sets. And server-side fetch needs a timeout the browser never forced on you."
---

| Browser | Node.js server |
| --- | --- |
| `fetch(url)` | `fetch(url, { signal: AbortSignal.timeout(ms) })` |
| `response.json()` | Read `req`, `JSON.parse`, validate |
| CORS errors | `access-control-allow-origin` headers you send |
| Service Worker `fetch` event | `createServer` request listener |
