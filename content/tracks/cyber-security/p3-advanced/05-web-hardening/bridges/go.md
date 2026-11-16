---
from: go
status: draft
concept: "Let the browser enforce security for you"
from_way: "net/http handlers that set only Content-Type, with a CORS middleware echoing the Origin header."
native_way: "A middleware that sets the security headers and a per-request nonce, a CORS check against a fixed set of origins, and http.Cookie with all attributes."
sensei_tip: "Write the security headers in one middleware wrapping your mux, generate the nonce with crypto/rand per request, and pass it to html/template through the request context."
---

| Go | Hardened responses |
| --- | --- |
| w.Header().Set("Access-Control-Allow-Origin", origin) | check origin against an allow-list first |
| no headers | one middleware for HSTS, CSP, nosniff |
| tls.Config defaults on old Go | MinVersion TLS 1.2 |
| http.Cookie{Name, Value} | Secure, HttpOnly, SameSite set |
| Server header from a proxy | removed at the edge |
