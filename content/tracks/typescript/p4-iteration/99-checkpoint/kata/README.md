# Checkpoint 4: Log Stream Report

A service writes its logs as lines of `<ISO time> <level> <message>`:

```text
2026-11-26T10:00:01Z info service started
2026-11-26T10:00:01.250Z warn disk at 91%
```

Build a report over a log that may be far bigger than memory, as a pipeline of **async
generators**: chunks become lines, lines become entries, entries become time windows. Every
source may be an async iterable (a Node.js stream, a `fetch` body) or a plain iterable (an
array in a test).

## logs.ts

| Member | Does |
| --- | --- |
| `LEVELS`, `Level` | `LEVELS` is `as const`, and `Level` is the union **derived** from it |
| `lines(chunks)` | Chunks of text or UTF-8 bytes in, lines out, without `\n` or `\r\n`. A line may be split across chunks, and **so may a multi-byte character**. An empty line in the middle is a line; a final newline does not add one |
| `parseEntries(lines, onError?)` | One `LogEntry` (`{ at: Date, level, message }`) per valid line. The time is an ISO date-time ending in `Z` (with optional milliseconds) that `Date` accepts; the level is one of `LEVELS`; the message is everything after the second space (not empty, kept as written). Blank lines are skipped silently; any other bad line goes to `onError({ line, text })` with its **1-based line number** (blank lines count) and is skipped |
| `windows(entries, sizeMs)` | Tumbling windows: `{ start, counts }` with `start` aligned on a multiple of `sizeMs` since the epoch and a count for **every** level. Windows without entries are not yielded. A window is yielded **as soon as** an entry of a later window arrives. An entry that belongs before the current window throws a `RangeError`, and so does a `sizeMs` that is not a positive integer |
| `report(chunks, windowMs)` | Runs the whole pipeline and returns `{ windows, errors }` |
| `firstError(chunks)` | The first entry of level `error`, or `undefined`. It **stops reading** the source as soon as it finds it, which closes the source |

## concurrent.ts

`mapConcurrent(source, fn, { concurrency, signal? })` is an async generator that calls
`fn(item, signal)` on each item with **at most `concurrency` calls running**, and yields the
results **in source order**:

- It pulls an item from the source only to start a call, so it never reads more than
  `concurrency` items ahead of what has been consumed.
- When a call rejects, the run throws that error. When the run stops for any reason (an error,
  the consumer breaking out of its loop, the end), the signal given to the calls still running
  is **aborted** and the source is **closed**. A call that rejects while an earlier one is still
  awaited must not become an unhandled rejection.
- When `options.signal` is aborted (before or during the run), the run throws
  `signal.reason` and aborts the calls in flight.
- A `concurrency` that is not a positive integer throws a `RangeError`.

Run the tests with `npm test`. There are 22 tests: 1 in `tests/visible.test.ts` and 21 hidden
ones, 3 of them type-level.
