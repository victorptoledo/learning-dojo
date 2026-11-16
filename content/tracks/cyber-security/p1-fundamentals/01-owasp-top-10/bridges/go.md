---
from: go
status: draft
concept: "Authorise every object, not just every user"
from_way: "Handlers that parse the id from the path and run `SELECT ... WHERE id = $1`."
native_way: "Queries that include the caller's id, a policy function called by every handler, and an SSRF-safe http.Client."
sensei_tip: "A custom `net.Dialer` with a `Control` function can reject private and link-local addresses at connect time, which also covers redirects and DNS rebinding."
---

| Go | OWASP Top 10 practice |
| --- | --- |
| WHERE id = $1 | WHERE id = $1 AND owner_id = $2 |
| auth middleware only | authorisation per object |
| http.Get(userURL) | a client with a checking Dialer and CheckRedirect |
| encoding/gob on input | decode into plain structs only |
| panics shown to clients | recover, log, generic 500 |
