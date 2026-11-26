# Checkpoint 3: Advanced

This checkpoint closes the **Advanced** phase, the concurrency phase. It has two parts, and
you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): a generic batcher. Many goroutines add items,
   one goroutine owns the batch and flushes it by size or by time, `Add` waits while a flush
   runs and gives up when its context ends, `Close` is safe to call twice and while others
   add, and the end of a context stops everything without leaking a goroutine.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Goroutines and the Scheduler | Who waits for the goroutines I start? How would I notice one that never returns? |
| Channels and select | Who closes a channel, and why never the receiver? What does a nil channel do in a `select`? |
| Worker Pool and Pipeline | How do I keep order when work runs in parallel? When is it safe to close an output channel? |
| sync and the Memory Model | Which operations create happens-before? When is an atomic enough, and when do I need a mutex? |
| Context | Which context does cleanup run with after the parent was cancelled? Who calls `cancel`? |
| Parallel Fetch | How do I stop the other goroutines on the first failure without leaking them? |

> [!TIP]
> The kata runs under the race detector and inside `testing/synctest` bubbles. A bubble has
> a fake clock (a `time.Sleep(time.Hour)` returns at once once every goroutine is blocked)
> and fails the test when a goroutine is still blocked at the end, so a leak is a failing
> test, not a slow one.
