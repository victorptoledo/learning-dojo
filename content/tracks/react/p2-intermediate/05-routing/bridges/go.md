---
from: go
status: draft
concept: "A mux on the client"
from_way: "http.ServeMux matches \"GET /products/{id}\" and r.PathValue(\"id\") reads the parameter; r.URL.Query() reads the query string."
native_way: "React Router matches \"products/:productId\" in the browser; useParams and useSearchParams read the same pieces, and layouts nest with Outlet."
sensei_tip: "Your Go server still matters: serve index.html for every app route (the SPA fallback) and keep authorisation checks there."
---

| Go | React Router |
| --- | --- |
| `mux.HandleFunc("GET /products/{id}", h)` | `{ path: "products/:productId" }` |
| `r.PathValue("id")` | `useParams().productId` |
| `r.URL.Query().Get("page")` | `searchParams.get("page")` |
| Middleware wrapping handlers | Layout routes wrapping children |
| `http.Redirect(w, r, "/login", 303)` | `redirect("/login")` |
| `http.FileServer` with fallback | SPA fallback to `index.html` |
