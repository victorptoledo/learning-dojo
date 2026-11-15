# HTTP Services with net/http and ServeMux Patterns

Go's standard library has a production-grade HTTP server and client. Since Go 1.22 its
router, `http.ServeMux`, matches **methods and path wildcards**, so most services need no web
framework at all.

## Handlers

Everything is built on one interface:

```go
type Handler interface {
	ServeHTTP(w http.ResponseWriter, r *http.Request)
}
```

`http.HandlerFunc` turns an ordinary function into a Handler, so a handler is usually just a
function or a method:

```go
func (s *Server) health(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "text/plain")
	w.WriteHeader(http.StatusOK)     // optional for 200; must come before the body
	fmt.Fprintln(w, "ok")
}
```

Set headers, then the status, then write the body: once the body starts, the status and
headers are sent and cannot change. Each request runs in **its own goroutine**, so handlers
share state only through properly synchronised fields.

## Routing patterns (Go 1.22)

```go
mux := http.NewServeMux()
mux.HandleFunc("GET /todos", s.listTodos)
mux.HandleFunc("POST /todos", s.createTodo)
mux.HandleFunc("GET /todos/{id}", s.getTodo)
mux.HandleFunc("DELETE /todos/{id}", s.deleteTodo)
mux.HandleFunc("GET /files/{path...}", s.serveFile)  // {name...} matches the rest of the path
mux.HandleFunc("GET /{$}", s.home)                   // {$}: exactly "/", not every path
```

- A pattern is `[METHOD ][HOST]/PATH`. `GET` also matches `HEAD`.
- `r.PathValue("id")` returns the wildcard's value (always a string: parse and validate it).
- When two patterns match, the **most specific** wins (`/todos/latest` beats
  `/todos/{id}`); patterns that conflict without one being more specific panic at
  registration, so mistakes show up at start-up.
- A path that matches with the wrong method gets **405 Method Not Allowed** with an `Allow`
  header; an unknown path gets 404.
- A pattern ending in `/` (without `{$}`) matches the whole subtree.

## JSON in and out

```go
type createTodo struct {
	Title string `json:"title"`
}

func (s *Server) createTodo(w http.ResponseWriter, r *http.Request) {
	r.Body = http.MaxBytesReader(w, r.Body, 1<<20)      // refuse bodies over 1 MiB
	var in createTodo
	if err := json.NewDecoder(r.Body).Decode(&in); err != nil {
		writeError(w, http.StatusBadRequest, "invalid JSON body")
		return
	}
	if strings.TrimSpace(in.Title) == "" {
		writeError(w, http.StatusBadRequest, "title is required")
		return
	}
	todo := s.store.Add(in.Title)
	w.Header().Set("Location", fmt.Sprintf("/todos/%d", todo.ID))
	writeJSON(w, http.StatusCreated, todo)
}

func writeJSON(w http.ResponseWriter, status int, v any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	json.NewEncoder(w).Encode(v)
}
```

Go 1.27 adds `encoding/json/v2`, which is stricter by default (it rejects duplicate keys and
invalid UTF-8) and faster; `encoding/json` keeps its behaviour and now runs on the same
engine.

## Middleware

Middleware is a function that wraps a Handler and returns a Handler:

```go
func logRequests(logger *slog.Logger, next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		start := time.Now()
		next.ServeHTTP(w, r)
		logger.Info("request", "method", r.Method, "path", r.URL.Path, "duration", time.Since(start))
	})
}

handler := logRequests(logger, recoverPanics(mux))
```

Typical middleware: logging, panic recovery, request IDs, authentication, CORS, timeouts
(`http.TimeoutHandler`), metrics. To record the status code, wrap the `ResponseWriter` in a
small struct that remembers what `WriteHeader` received.

## The server, its timeouts and graceful shutdown

`http.ListenAndServe(":8080", mux)` has **no timeouts**, so a slow client can hold a
connection forever. In production, configure an `http.Server`:

```go
srv := &http.Server{
	Addr:              ":8080",
	Handler:           handler,
	ReadHeaderTimeout: 5 * time.Second,
	ReadTimeout:       10 * time.Second,
	WriteTimeout:      30 * time.Second,
	IdleTimeout:       120 * time.Second,
}

ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
defer stop()

go func() {
	if err := srv.ListenAndServe(); err != nil && !errors.Is(err, http.ErrServerClosed) {
		log.Fatal(err)
	}
}()

<-ctx.Done()                                    // SIGTERM from Kubernetes, or Ctrl+C
shutdownCtx, cancel := context.WithTimeout(context.Background(), 20*time.Second)
defer cancel()
srv.Shutdown(shutdownCtx)                       // stop accepting, finish in-flight requests
```

## The client

`http.Get` uses `http.DefaultClient`, which **has no timeout**. Create one client, reuse it
(it pools connections), set a timeout and pass contexts:

```go
client := &http.Client{Timeout: 10 * time.Second}
req, _ := http.NewRequestWithContext(ctx, http.MethodGet, url, nil)
resp, err := client.Do(req)
if err != nil {
	return err
}
defer resp.Body.Close()                         // always, or connections leak
if resp.StatusCode != http.StatusOK {
	return fmt.Errorf("GET %s: %s", url, resp.Status)
}
```

## Testing handlers

`net/http/httptest` makes handlers easy to test without a network:

```go
req := httptest.NewRequest(http.MethodGet, "/todos/1", nil)
rec := httptest.NewRecorder()
server.ServeHTTP(rec, req)
if rec.Code != http.StatusOK { ... }
```

`httptest.NewServer` starts a real server on a local port for client tests.

## Summary

- A Handler is one method; HandlerFunc adapts functions; middleware wraps Handlers.
- ServeMux patterns route by method, host and path wildcards (`{id}`, `{path...}`, `{$}`),
  with `r.PathValue`, 405s and most-specific matching.
- Decode JSON with limits, validate, answer with status codes and JSON errors.
- Configure server timeouts and shut down gracefully on SIGTERM; give clients timeouts and
  close response bodies.
- Test with httptest.

## Further reading

- Routing Enhancements for Go 1.22 (Go blog): <https://go.dev/blog/routing-enhancements>
- Package net/http: <https://pkg.go.dev/net/http>
- Package net/http/httptest: <https://pkg.go.dev/net/http/httptest>
- Tutorial, "Developing a RESTful API with Go and Gin" (for comparison): <https://go.dev/doc/tutorial/web-service-gin>
- Package encoding/json/v2: <https://pkg.go.dev/encoding/json/v2>
- roadmap.sh, Go topic "net/http (standard)": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/golang/content/nethttp-standard@NMLmbKjfMSqz-Iz2CooCl.md>
