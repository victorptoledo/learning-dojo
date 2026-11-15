# Retry

Write five async helpers in `retry.ts`. The error classes `TimeoutError` and `RetryError` are
given.

| Function | Does |
| --- | --- |
| `toError(value: unknown)` | Turns any thrown value into an `Error`: an `Error` is returned as it is, a string becomes the message, an object becomes its `JSON.stringify`, anything else `String(value)` |
| `sleep(ms, signal?)` | Resolves after `ms`. If the signal is already aborted, rejects at once with `signal.reason`; if it is aborted during the wait, clears the timer and rejects with `signal.reason` |
| `withTimeout(task, ms)` | Calls `task(signal)` with a fresh `AbortController`'s signal. After `ms` it aborts the signal and rejects with `new TimeoutError("timed out after <ms> ms")`. Otherwise it settles like the task and clears its timer |
| `retry(task, options)` | Calls `task(1)`, `task(2)`, ... up to `options.attempts` times until one resolves |
| `mapLimit(items, limit, fn)` | Maps every item with `fn(item, index)`, never more than `limit` calls at a time; resolves with the results **in item order**; rejects with the first error and starts no new calls after it |

## retry

- Between attempts it waits `delayMs`, then `delayMs * factor`, then `delayMs * factor²`, ...
  (`factor` defaults to 2). No wait after the last attempt.
- It waits with `options.wait` when given (the tests use it to record the delays), otherwise
  with `sleep`.
- When `shouldRetry(error)` returns `false`, it rethrows **that error as it is**, at once.
- When every attempt fails, it throws `new RetryError(errors)`, with every error in order.

In TypeScript a `catch (error)` variable is `unknown` (with `strict`), so the code has to
narrow it before using it. That is what `toError` is for.

Run the tests with `npm test` after `npm install`. There are 17 tests: 1 in
`tests/visible.test.ts` and 16 hidden ones.
