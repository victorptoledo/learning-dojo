---
from: go
status: draft
concept: "HTTP services before and after Go 1.22"
from_way: "gorilla/mux or chi only to get methods and path parameters, `if r.Method != http.MethodGet` checks at the top of handlers, strings.TrimPrefix to read IDs, http.ListenAndServe without timeouts, and httputil.ReverseProxy with a Director."
native_way: "Method and wildcard patterns in ServeMux (1.22), r.PathValue, 405s with Allow headers, 307 trailing-slash redirects (1.26), ReverseProxy.Rewrite instead of Director (deprecated in 1.26), and encoding/json/v2 (1.27)."
sensei_tip: "Most gorilla/mux routers port mechanically: \"/todos/{id}\" with .Methods(\"GET\") becomes \"GET /todos/{id}\", and mux.Vars(r)[\"id\"] becomes r.PathValue(\"id\")."
---

| Older Go | Go 1.27 |
| --- | --- |
| `r.HandleFunc("/todos/{id}", h).Methods("GET")` (gorilla) | `mux.HandleFunc("GET /todos/{id}", h)` |
| `mux.Vars(r)["id"]` | `r.PathValue("id")` |
| `if r.Method != http.MethodPost { 405 }` | the method in the pattern |
| `strings.TrimPrefix(r.URL.Path, "/todos/")` | `{id}` and `{path...}` wildcards |
| `ReverseProxy{Director: ...}` | `ReverseProxy{Rewrite: ...}` |
| `encoding/json` only | `encoding/json/v2` (stricter, faster) |
