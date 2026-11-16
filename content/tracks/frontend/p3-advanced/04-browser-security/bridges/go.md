---
from: go
status: draft
concept: "Security on the client side"
from_way: "`html/template` escapes by context; net/http sets cookies with HttpOnly and SameSite fields."
native_way: "Client-side code has no contextual escaping by default: textContent and URL validation play that role."
sensei_tip: "html/template refuses javascript: URLs in href by default: do the same in the browser with new URL() and a protocol check."
---

| Go | In the browser |
| --- | --- |
| `html/template` escaping | `textContent` |
| `template.HTML(s)` | `innerHTML` (dangerous) |
| `http.Cookie{HttpOnly: true, SameSite: http.SameSiteLaxMode}` | Cookie attributes |
| A middleware setting headers | CSP and the other security headers |
