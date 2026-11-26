---
from: python
status: draft
concept: "FastAPI or Flask services"
from_way: "Decorated route functions, path parameters parsed by type hints, Pydantic models validating JSON, middleware classes, uvicorn or gunicorn running workers, and httpx as the client."
native_way: "Handlers registered with method and wildcard patterns, JSON decoded into structs and validated by hand, middleware wrapping handlers, one process with a goroutine per request, and http.Client."
sensei_tip: "FastAPI validates for you from type hints; in Go, validation is code you write after Decode. Keep it in one function per request type so handlers stay short."
---

| FastAPI | Go net/http |
| --- | --- |
| `@app.get("/todos/{id}")` | `mux.HandleFunc("GET /todos/{id}", h)` |
| `def get(id: int)` | `id, err := strconv.Atoi(r.PathValue("id"))` |
| `body: CreateTodo` (Pydantic) | `json.NewDecoder(r.Body).Decode(&in)` + checks |
| `raise HTTPException(404)` | `writeError(w, http.StatusNotFound, "...")` and `return` |
| `@app.middleware("http")` | `func(next http.Handler) http.Handler` |
| `uvicorn --workers 4` | one binary; a goroutine per request |
| `TestClient(app)` | `httptest.NewRecorder()` |
| `httpx.Client(timeout=10)` | `&http.Client{Timeout: 10 * time.Second}` |
