---
from: javascript
status: draft
concept: "Let the browser enforce security for you"
from_way: "Express with app.use(cors()) and the X-Powered-By: Express header on every response."
native_way: "helmet for security headers with a nonce CSP, cors with an origin allow-list, and app.disable('x-powered-by')."
sensei_tip: "`helmet()` sets sensible defaults for HSTS, nosniff, frame protection and more in one line; its CSP still needs your script sources or nonces to fit your app."
---

| JavaScript / TypeScript | Hardened responses |
| --- | --- |
| app.use(cors()) | cors({ origin: ['https://app.example.com'] }) |
| X-Powered-By: Express | app.disable('x-powered-by') |
| no headers | helmet() |
| res.cookie('sid', v) | add secure, httpOnly, sameSite |
| inline scripts everywhere | nonces or external files |
