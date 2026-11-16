---
from: javascript
status: draft
concept: "Who you are, whether it is still you, and what you may do"
from_way: "JWTs in localStorage, read by any script, and Express routes checking req.user.role inline."
native_way: "HttpOnly, Secure, SameSite session cookies, session regeneration at login, CSRF defences and a central policy."
sensei_tip: "With express-session, call `req.session.regenerate()` after a successful login; without it the pre-login session id survives (session fixation)."
---

| JavaScript / TypeScript | Secure practice |
| --- | --- |
| token in localStorage | HttpOnly cookie |
| no regenerate() at login | req.session.regenerate() |
| GET /logout that only clears the client | destroy the server session |
| req.user.role checks inline | a policy module or CASL-style rules |
| Object.assign(user, req.body) | pick allowed fields |
