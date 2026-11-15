---
from: go
status: draft
concept: "context since Go 1.7"
from_way: "Done channels passed by hand, golang.org/x/net/context, cancellation without a reason, goroutines started in AfterFunc-like helpers by hand, a detached context built from scratch for background work, and signal.Notify plus a channel for shutdown."
native_way: "WithCancelCause and Cause (1.20), AfterFunc, WithoutCancel, WithDeadlineCause and WithTimeoutCause (1.21), t.Context in tests (1.24) and signal.NotifyContext with a cause naming the signal (1.26)."
sensei_tip: "When a cancellation error in your logs says only \"context canceled\", switch that code to WithCancelCause and log context.Cause(ctx): the next incident will tell you why."
---

| Older Go | Go 1.27 |
| --- | --- |
| `done := make(chan struct{})` passed everywhere | `ctx context.Context` |
| `golang.org/x/net/context` | `context` (1.7) |
| `cancel()` with no reason | `cancel(err)` with `WithCancelCause`, read by `context.Cause` |
| a goroutine waiting on `<-ctx.Done()` to clean up | `context.AfterFunc(ctx, cleanup)` |
| copying values into a new `Background()` context | `context.WithoutCancel(ctx)` |
| `signal.Notify(ch, os.Interrupt)` + a select | `signal.NotifyContext` (cause names the signal since 1.26) |
| `context.Background()` in tests | `t.Context()` |
