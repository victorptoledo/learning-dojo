---
from: python
status: draft
concept: "State and persistence"
from_way: "Django sessions, a database, and shelve or SQLite for local data."
native_way: "Client-side storage is per origin and can vanish; the URL carries shareable state, and the server stays authoritative."
sensei_tip: "localStorage is like a shelve file anyone can edit: json.loads it in a try block and never keep secrets in it."
---

| Python | In the browser |
| --- | --- |
| `request.session` | An HttpOnly cookie plus server state |
| `shelve` / a JSON file | `localStorage` with `JSON.stringify` |
| SQLite | IndexedDB |
| `request.GET["q"]` | `new URLSearchParams(location.search).get("q")` |
