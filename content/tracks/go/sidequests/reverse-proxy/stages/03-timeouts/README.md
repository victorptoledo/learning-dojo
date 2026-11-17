A slow upstream must not pile up waiting clients. New field:

```go
	Timeout time.Duration // how long a request may take upstream; 0 = no limit
```

- When the upstream has not answered within `Timeout`, the client gets `504` as a problem
  document, at once (not when the upstream finally answers).
- When the client goes away (its request's context is cancelled), the request to the upstream
  is cancelled too, so the upstream can stop working on it.

Both come from the same tool: the request's `context.Context`.
