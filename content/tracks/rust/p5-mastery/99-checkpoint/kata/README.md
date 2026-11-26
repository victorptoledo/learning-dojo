# Final Exam 🥋: Health Probe

`probe` is the small program an operator runs at 3 a.m. to see which services are alive. It reads
its settings from the environment, checks every target **at the same time** under a timeout,
writes structured log lines and exits with a code a script can act on.

There is no Tokio here (the grading container is offline and the crate has no dependencies), so
part 1 is the runtime itself, built from the standard library as in *Futures by Hand*.

## Part 1: a runtime

- `block_on(future)`: runs a future on the current thread. While it is pending the thread must
  **park**, not spin: a test counts the polls.
- `sleep(duration) -> Sleep`: ready once the duration has passed, **without blocking** the thread
  that polls it. When polled again before the deadline, it must wake the waker of the **latest**
  poll (a test polls it with two different wakers).
- `join_all(futures) -> JoinAll<F>`: polls all the futures, returns their outputs in **input
  order**, and never polls a future again after it returned `Ready`. An empty list is ready at
  once.
- `timeout(duration, future) -> Timeout<F>`, with `Output = Result<F::Output, Elapsed>`:
  - polls the work **first**, so a ready future wins even `Duration::ZERO`;
  - when the time is up, returns `Err(Elapsed)` and **drops** the work right away (that is the
    cancellation).

The structs in the starter only have the fields needed to compile: add the ones you need. Storing
futures as `Pin<Box<F>>` keeps `Pin` simple.

## Part 2: configuration and errors

`load_config(lookup)` reads three variables through `lookup` (in a real `main`,
`|var| std::env::var(var).ok()`):

| Variable | Rule | Default |
| --- | --- | --- |
| `PROBE_TARGETS` | comma-separated names, trimmed, empty pieces ignored; at least one, no duplicates | required |
| `PROBE_TIMEOUT_MS` | a whole number, trimmed, from 1 to 60000 | 500 |
| `PROBE_LOG` | `error`, `warn`, `info` or `debug`, any case | `info` |

Errors and their `Display`, where `value` is the raw value as read:

| Error | Message |
| --- | --- |
| `Missing { var }` | `PROBE_TARGETS is not set` |
| `Invalid { var, value, reason }` | `PROBE_TIMEOUT_MS=0 is invalid: must be between 1 and 60000` |
| `NotANumber { var, value, source }` | `PROBE_TIMEOUT_MS=fast is not a number` |

The reasons are `no targets`, `duplicate target api`, `must be between 1 and 60000` and
`expected error, warn, info or debug`. `NotANumber` keeps the `ParseIntError` and returns it
from `Error::source`; the others have no source.

`error_chain(error)` joins an error and all its causes with `": "`:
`PROBE_TIMEOUT_MS=fast is not a number: invalid digit found in string`.

## Part 3: the probe

`probe(config, check)` calls `check(target)` for every target, wraps each future in
`timeout(config.timeout, ...)`, runs them **all at once** with `join_all` and `block_on`, and
returns the results in config order:

| The check | Outcome | Log line |
| --- | --- | --- |
| `Ok(ms)` | `Up { ms }` | `level=info target=api msg="up in 12 ms"` |
| `Err(reason)` | `Down { reason }` | `level=error target=db msg="down: connection refused"` |
| too slow | `TimedOut` | `level=warn target=cache msg="timed out after 200 ms"` |

- Before each target's line comes `level=debug target=api msg="checking"`.
- After all targets, a summary without a target: `level=info msg="3 of 3 targets up"` when all
  are up, otherwise `level=warn msg="1 of 3 targets up"`.
- Only lines at or above the configured level are kept: `error` keeps only errors, `debug`
  keeps everything.
- Inside `msg`, escape `\` as `\\`, `"` as `\"` and a newline as `\n`, so every entry stays one
  line.

`run(lookup, check) -> (exit code, log lines)` is the whole `main`: `0` when every target is up,
`1` when one is not, and `2` with a single line `level=error msg="<error chain>"` when the
configuration is wrong.

> The final exam reviews the advanced and mastery phases: futures and the waker contract, `Pin`,
> cancellation by drop, `Arc<dyn Fn>` shared callbacks, typed errors with a source chain,
> configuration from the environment with validation, structured logs and exit codes.
