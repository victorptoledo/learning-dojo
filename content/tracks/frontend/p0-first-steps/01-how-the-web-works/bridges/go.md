---
from: go
status: draft
concept: "The browser as a client runtime"
from_way: "`net/http` handlers and `html/template` render pages on the server."
native_way: "The browser receives the HTML, builds the DOM and runs scripts; your Go server becomes one origin among the requests the page makes."
sensei_tip: "`html/template` escapes output to prevent XSS: in the browser you keep that discipline with textContent instead of innerHTML (Phase 3)."
---

| Go | In the browser |
| --- | --- |
| `http.HandleFunc` | The response the page receives |
| `html/template` | The HTML document |
| `http.Get` | `fetch` |
| `log.Println` | `console.log` |
