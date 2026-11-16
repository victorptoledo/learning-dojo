---
from: go
status: reviewed
concept: "Sessions, CSRF and OIDC in Go web apps"
from_way: "A JWT returned to the front end on login and parsed by a middleware on every request."
native_way: "A server-side session (scs, gorilla/sessions) in an HttpOnly cookie, CSRF protection (http.CrossOriginProtection or gorilla/csrf), and golang.org/x/oauth2 with go-oidc for the provider."
sensei_tip: "Go 1.25 added http.CrossOriginProtection, which refuses cross-origin unsafe requests from Sec-Fetch-Site and Origin: a cheap first CSRF layer."
---

| Go | Across the stack |
| --- | --- |
| `scs.SessionManager` with `Cookie.HttpOnly` | server-side session |
| `http.NewCrossOriginProtection()` | origin-based CSRF defence |
| `oauth2.Config.Exchange` with a PKCE verifier | code flow, tokens kept server-side |
| ownership checks in handlers | enforce what the UI hides |
