```go
type Config struct {
	Upstream string // the base URL of the upstream, such as "http://127.0.0.1:8081"
}
```

- `New` fails with an error when `Upstream` is not an absolute `http` or `https` URL.
- Every request goes to the upstream with the same method, path, query string, headers and
  body. The upstream's status, headers and body come back unchanged.
- The proxy adds the client's IP (taken from the connection, `r.RemoteAddr`) to
  `X-Forwarded-For`, after any value the client sent.
- When the upstream cannot be reached, the answer is `502` as a problem document.
