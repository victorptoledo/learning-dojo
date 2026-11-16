---
from: go
status: draft
concept: "State and persistence"
from_way: "Server-side state in a database, maybe gorilla/sessions with cookies."
native_way: "The client holds its own state per origin; the URL is the most durable and shareable piece."
sensei_tip: "A Go handler reads r.URL.Query(); the page reads URLSearchParams: keeping filters in the URL lets both sides agree on state for free."
---

| Go | In the browser |
| --- | --- |
| `r.URL.Query().Get("q")` | `URLSearchParams.get("q")` |
| `http.Cookie{HttpOnly: true}` | The right place for a session |
| `encoding/json` to a file | `localStorage` with JSON |
| bbolt or SQLite | IndexedDB |
