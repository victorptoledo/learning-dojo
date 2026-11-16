---
from: go
status: reviewed
concept: "html/template on one side, the DOM on the other"
from_way: "Go developers render pages with html/template and seldom think about what the browser does next."
native_way: "The browser parses that HTML into a DOM, submits forms by itself, and fetch reports HTTP errors as successful promises."
sensei_tip: "html/template escapes by context (text, attributes, URLs); in the browser you must choose textContent yourself."
---

| Go | Browser |
| --- | --- |
| `{{.Name}}` in html/template | `el.textContent = name` |
| `template.HTML(x)` | `el.innerHTML = x` |
| `if resp.StatusCode >= 400` | `if (!response.ok)` |
| `r.ParseForm()` | the form's urlencoded body |
