---
from: javascript
status: draft
concept: "Authorise every object, not just every user"
from_way: "Express routes with an auth middleware that loads `req.params.id` straight from the database."
native_way: "Queries scoped to `req.user.id`, a central policy helper, and SSRF-safe fetching for any URL a user provides."
sensei_tip: "Node's `fetch` follows redirects by default; pass `redirect: 'manual'` and validate each hop, and resolve DNS yourself before connecting if you must fetch user URLs."
---

| JavaScript / TypeScript | OWASP Top 10 practice |
| --- | --- |
| db.invoice.findUnique({ where: { id } }) | where: { id, ownerId: req.user.id } |
| isAuthenticated middleware only | authorisation per object |
| fetch(req.body.url) | allow-list, IP check, redirect: 'manual' |
| res.status(500).send(err.stack) | generic errors, logged details |
| unpinned dependencies | A03: lockfile and reviewed updates |
