---
from: go
status: reviewed
concept: "html/template pages first"
from_way: "Go services serve JSON only, and any page is somebody else's SPA."
native_way: "html/template renders fast, safe pages for content and forms; add JavaScript where interaction needs it."
sensei_tip: "html/template escapes per context automatically; template.HTML and template.URL switch that off, so grep for them in reviews."
---

| Go | Rendering practice |
| --- | --- |
| `tmpl.Execute(w, data)` | server-rendered page |
| re-execute with errors and values, `w.WriteHeader(422)` | the form again with field errors |
| `http.Redirect(w, r, "/", http.StatusSeeOther)` | Post/Redirect/Get |
| `template.HTML(s)` | raw HTML: review it |
