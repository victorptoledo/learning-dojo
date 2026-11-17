One proxy in front of several services. New type and field:

```go
type Route struct {
	Prefix      string // such as "/api/users"
	Upstream    string
	StripPrefix bool   // send "/api/users/7" upstream as "/7"
}

	Routes []Route
```

- A request goes to the route with the **longest** `Prefix` that matches its path, either
  the whole path or up to a `/`: `/api/users` matches `/api/users` and `/api/users/7`, not
  `/api/usersettings`. With `StripPrefix`, the prefix is removed from the path sent upstream
  (an empty path becomes `/`).
- `Upstream` in `Config`, when set, takes the requests no route matches; with routes it may be
  empty (with neither, `New` still fails). When no route matches and there is no default, the
  answer is `404` as a problem document.
- `GET /healthz` is answered by the proxy itself, never forwarded: `200`,
  `Content-Type: application/json`, body `{"status":"ok"}`.
- `New` fails when a route's upstream is not a valid URL. Rate limits and timeouts apply to
  every route.
