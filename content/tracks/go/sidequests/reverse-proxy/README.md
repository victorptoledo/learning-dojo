# Sidequest: Reverse proxy with rate limiting

Build the piece that sits in front of every serious API: a reverse proxy. Clients talk to it,
it talks to the real service (the *upstream*), and on the way it protects the upstream from
greedy clients and slow answers.

The module is `dojo/proxy`, a library package with one fixed entry point:

```go
func New(cfg Config) (http.Handler, error)
```

`Config` starts with one field and grows a few each stage (keep the names the stage READMEs
give). The contract tests start fake upstreams with `httptest.NewServer`, send requests
through your handler and check what each side saw. `net/http/httputil.ReverseProxy` is a
fine base for the forwarding; the limits, timeouts and routing around it are yours. The
race detector runs on every check, and a rate limiter is shared state.

Errors follow RFC 9457 (problem details): status code, `Content-Type:
application/problem+json`, and a body such as

```json
{"type": "about:blank", "title": "Bad Gateway", "status": 502, "detail": "..."}
```

where `title` is the standard text of the status (`http.StatusText`).
