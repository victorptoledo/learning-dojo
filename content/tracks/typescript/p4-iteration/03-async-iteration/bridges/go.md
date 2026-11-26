---
from: go
status: draft
concept: "Channels, goroutines and context"
from_way: "A goroutine sends values on a channel, range over the channel receives them, a buffered channel or a worker pool limits work, and context.Context cancels."
native_way: "An async generator yields values, for await receives them, a pool of pending promises limits work, and an AbortSignal cancels; everything runs on one thread."
sensei_tip: "An unbuffered channel is the closest thing to for await: the producer blocks until the consumer is ready. In TypeScript the producer does not even run until asked, so there is no goroutine to leak."
---

| Go | TypeScript |
| --- | --- |
| `ch := make(chan T)` + goroutine | `async function*` |
| `for v := range ch` | `for await (const v of gen)` |
| `bufio.Scanner` over a file | `readline` over `createReadStream`, or your own `lines()` |
| `ctx, cancel := context.WithCancel(...)` | `const controller = new AbortController()` |
| `<-ctx.Done()` | `signal.aborted` / the `"abort"` event |
| A worker pool with ordered results | `mapConcurrent(source, fn, { concurrency })` |
| `defer f.Close()` | `finally { await file.close() }` |
