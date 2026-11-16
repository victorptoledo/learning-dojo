---
from: go
status: draft
concept: "Ranges with identity"
from_way: "html/template uses {{range .Items}} and {{if}}; on the server there are no client events, handlers are HTTP routes."
native_way: "In React, map replaces range, ternaries and && replace if, and client events call functions passed as props."
sensei_tip: "A key is like the map key you would use to look an item up between two versions of the list: an id, never the loop index."
---

| Go | React |
| --- | --- |
| `{{range .Items}}<li>{{.Title}}</li>{{end}}` | `items.map((i) => <li key={i.id}>{i.title}</li>)` |
| `{{if .User}}...{{else}}...{{end}}` | `user ? ... : ...` |
| `http.HandleFunc("/save", save)` | `onClick={save}` on the client |
| `func(id string)` callback | `onDelete: (id: string) => void` |
| `slices.DeleteFunc` | `items.filter(...)` |
| Map lookup by id | `key={item.id}` |
