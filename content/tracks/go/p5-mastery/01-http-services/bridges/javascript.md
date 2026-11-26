---
from: javascript
status: draft
concept: "Express or Fastify servers"
from_way: "app.get with :id parameters in req.params, express.json() middleware, middleware with (req, res, next), res.status(201).json(obj), and fetch for outgoing calls."
native_way: "ServeMux patterns with {id} wildcards read by r.PathValue, explicit JSON decoding, middleware that wraps a Handler, headers then status then body, and http.Client."
sensei_tip: "In Express, middleware calls next(); in Go, middleware calls next.ServeHTTP(w, r) inside the function it returns, so code after that line runs after the handler, like an onion."
---

| Express | Go net/http |
| --- | --- |
| `app.get("/todos/:id", h)` | `mux.HandleFunc("GET /todos/{id}", h)` |
| `req.params.id` | `r.PathValue("id")` |
| `app.use(express.json())` + `req.body` | `json.NewDecoder(r.Body).Decode(&in)` |
| `res.status(201).json(todo)` | `writeJSON(w, http.StatusCreated, todo)` |
| `(req, res, next) => { ...; next() }` | `func(next http.Handler) http.Handler` |
| `app.listen(8080)` | `srv := &http.Server{Addr: ":8080", ...}; srv.ListenAndServe()` |
| supertest | `httptest.NewRecorder()` |
| `fetch(url, { signal })` | `client.Do(req)` with `NewRequestWithContext` |
