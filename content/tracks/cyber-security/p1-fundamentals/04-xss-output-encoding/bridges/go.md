---
from: go
status: draft
concept: "Encode for the context the data lands in"
from_way: "text/template or fmt.Fprintf to write HTML responses."
native_way: "html/template, which escapes by context (HTML, attributes, JavaScript, URLs) automatically, and bluemonday for user HTML."
sensei_tip: "html/template filters unsafe URLs in href to `#ZgotmplZ`; seeing that in your output means a `javascript:` value was blocked, so do not wrap it in template.URL to make it go away."
---

| Go | XSS-safe way |
| --- | --- |
| text/template for HTML | html/template |
| fmt.Fprintf(w, "<p>%s</p>", s) | a template, or html.EscapeString |
| template.HTML(userInput) | bluemonday.UGCPolicy().Sanitize |
| regexp.MatchString("^...$") | anchored pattern, Go has no trailing-newline quirk |
| no CSP | set the header in middleware |
