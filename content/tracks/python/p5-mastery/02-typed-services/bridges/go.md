---
from: go
status: draft
concept: "net/http handlers versus FastAPI endpoints"
from_way: "http.HandlerFunc with ServeMux patterns (GET /orders/{id}), json.Decoder plus manual validation, context.Context carrying request values, middleware chains, and httptest for tests."
native_way: "Typed endpoint functions where FastAPI decodes and validates, Pydantic models, Depends instead of context values and middleware for per-request dependencies, and TestClient for tests."
sensei_tip: "Go 1.22's ServeMux patterns look like FastAPI paths, but in Go you parse r.PathValue(\"id\") yourself. In FastAPI the type hint does the parsing, the validation and the 422."
---

| Go (net/http) | FastAPI |
| --- | --- |
| `mux.HandleFunc("GET /orders/{id}", h)` | `@app.get("/orders/{order_id}")` |
| `strconv.Atoi(r.PathValue("id"))` | `order_id: int` |
| `json.NewDecoder(r.Body).Decode(&in)` + checks | `order: OrderIn` |
| `http.Error(w, "not found", 404)` | `raise HTTPException(status_code=404)` |
| values in `r.Context()` from middleware | `Depends(current_user)` |
| `httptest.NewRecorder()` | `TestClient(app)` |
| `envconfig` / `os.Getenv` | `pydantic_settings.BaseSettings` |
