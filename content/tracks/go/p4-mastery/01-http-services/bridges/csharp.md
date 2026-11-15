---
from: csharp
status: draft
concept: "ASP.NET Core minimal APIs"
from_way: "app.MapGet(\"/todos/{id:int}\", ...) with typed parameters, Results.Ok and Results.NotFound, middleware with app.Use, Kestrel limits in configuration, IHostApplicationLifetime for shutdown and IHttpClientFactory."
native_way: "mux.HandleFunc(\"GET /todos/{id}\", ...) with string path values you parse, explicit status writes, middleware wrapping handlers, http.Server fields for limits, srv.Shutdown and one shared http.Client."
sensei_tip: "Minimal APIs and net/http after Go 1.22 look alike. The main difference is that route constraints like {id:int} become a strconv call and a 400 in your handler."
---

| ASP.NET Core | Go net/http |
| --- | --- |
| `app.MapGet("/todos/{id:int}", h)` | `mux.HandleFunc("GET /todos/{id}", h)` + `strconv.Atoi` |
| `Results.Created($"/todos/{id}", todo)` | `Location` header + `writeJSON(w, 201, todo)` |
| `Results.NotFound()` | `w.WriteHeader(http.StatusNotFound)` |
| `app.Use(async (ctx, next) => ...)` | `func(next http.Handler) http.Handler` |
| Kestrel `Limits.KeepAliveTimeout` | `http.Server{IdleTimeout: ...}` |
| `IHostApplicationLifetime` | `signal.NotifyContext` + `srv.Shutdown` |
| `IHttpClientFactory` | one `*http.Client` with a `Timeout` |
| `WebApplicationFactory` | `httptest.NewServer` / `NewRecorder` |
