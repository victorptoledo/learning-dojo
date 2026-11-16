---
from: go
status: draft
concept: "Events as messages"
from_way: "Handlers registered per route or per channel; no UI tree."
native_way: "The DOM is a tree, and events walk up it; registering one handler on a parent is like routing by prefix."
sensei_tip: "Delegation is a mux: one handler at the top inspects the target and dispatches, like http.ServeMux does with paths."
---

| Go | In the browser |
| --- | --- |
| `mux.HandleFunc("/tasks/", h)` | One listener on the list |
| Inspecting `r.URL.Path` | `event.target.closest("[data-action]")` |
| `html/template` escaping | `textContent` |
| `ctx` cancellation | `AbortController` signal on a listener |
