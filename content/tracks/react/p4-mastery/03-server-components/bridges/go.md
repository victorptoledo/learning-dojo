---
from: go
status: draft
concept: "Templates on the server, islands on the client"
from_way: "A Go server renders html/template pages and serves a small JavaScript bundle for interactive islands; handlers process form posts."
native_way: "Server Components are the server-rendered parts, Client Components are the islands, and server functions are generated handlers for form posts and calls."
sensei_tip: "The RSC model is close to server templates plus islands, which Go developers know well. Treat every server function as an http.HandlerFunc: authenticate and validate first."
---

| Go | React frameworks |
| --- | --- |
| `tmpl.Execute` with DB data | async Server Component |
| JS island on the page | Client Component |
| `http.HandleFunc("POST /cart", ...)` | `"use server"` function |
| Middleware auth | Auth inside the server function |
| Pre-generated static files | SSG |
| Flushing the response in chunks | Streaming SSR |
