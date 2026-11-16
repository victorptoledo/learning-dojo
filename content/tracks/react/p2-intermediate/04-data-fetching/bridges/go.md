---
from: go
status: draft
concept: "Requests bound to a context"
from_way: "http.NewRequestWithContext ties a request to a context; cancel() aborts it; you check resp.StatusCode yourself."
native_way: "fetch takes an AbortSignal; the effect cleanup calls abort(); you check response.ok yourself, just like resp.StatusCode."
sensei_tip: "AbortController is context.WithCancel: create it in the effect, pass its signal to fetch, and call abort in the cleanup like a deferred cancel()."
---

| Go | React |
| --- | --- |
| `ctx, cancel := context.WithCancel(...)` | `const controller = new AbortController()` |
| `http.NewRequestWithContext(ctx, ...)` | `fetch(url, { signal: controller.signal })` |
| `defer cancel()` | `return () => controller.abort()` |
| `if resp.StatusCode != 200` | `if (!response.ok)` |
| `errgroup` for parallel calls | `Promise.all` |
| A cache with TTL | TanStack Query `staleTime` |
