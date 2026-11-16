---
from: go
status: draft
concept: "Validation at the edge"
from_way: "`r.ParseForm()` and hand-written checks, or a validator package with struct tags."
native_way: "The browser checks the declared constraints first; FormData packs the values the same way r.Form sees them."
sensei_tip: "What the browser sends is exactly r.PostForm; validate it in the handler as you always did, the HTML attributes only save the user a round trip."
---

| Go | In the browser |
| --- | --- |
| `r.FormValue("email")` | `new FormData(form).get("email")` |
| `validate:"required,email"` tags | `required type="email"` |
| `url.Values` | `URLSearchParams` |
| A 422 with field errors | Messages tied with `aria-describedby` |
