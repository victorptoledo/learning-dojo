# Log Pipeline

A service writes one JSON object per line (NDJSON) to its log. The file is 3 GB, so reading it
with `readFile` is not an option. Write a filter that streams it in small chunks:

```text
bytes ──▶ splitLines ──▶ parseEntries ──▶ filterLevel ──▶ toNdjson ──▶ bytes
          (strings)      (LogEntry)       (LogEntry)      (strings)
```

```text
{"time":"2026-01-01T10:00:00Z","level":"info","msg":"started","pid":7}
{"time":"2026-01-01T10:00:01Z","level":"error","msg":"database down"}
```

Write six functions in `logs.ts`. Each of the first four returns a new `Transform` stream.

| Function | In | Out |
| --- | --- | --- |
| `splitLines()` | `Buffer` (or string) chunks | One string per line (object mode), without `\n` or `\r\n`. A line, and even a UTF-8 character, may be split across two chunks (use a `StringDecoder`). A last line without `\n` is kept; empty lines in the middle are kept as `""` |
| `parseEntries(onInvalid?)` | Lines | `LogEntry` objects with only `time`, `level` and `msg`. Blank lines are skipped silently. A line that is not JSON, or not an object with a string `time`, a level from `LEVELS` and a string `msg`, is skipped and reported with `onInvalid(lineNumber, line)`, where the first line is 1 and blank lines count |
| `filterLevel(min)` | Entries | Only the entries of level `min` or above (`debug` < `info` < `warn` < `error`) |
| `toNdjson()` | Entries | `JSON.stringify(entry) + "\n"` for each entry |

| Function | Does |
| --- | --- |
| `filterLog(input, output, min)` | Connects `input`, the four transforms and `output` with `pipeline` from `node:stream/promises` and resolves with `{ kept, invalid }`: entries written and invalid lines. If any stream fails, it rejects with that error |
| `countByLevel(source)` | Reads entries with `for await` and counts each level: `{ debug, info, warn, error }`, zeros included |

## Backpressure

One test feeds 20,000 lines into a writer that is slow at first and checks that the reader
**slows down with it**: after 20 writes, most of the input must still be unread. Streams
connected with `pipeline` (or `pipe`) do this for you: when a buffer is full, `write()` returns
`false` and the stream above stops reading until `"drain"`. Code that collects everything into
an array first, or calls `write()` without looking at its result, breaks it, and memory grows
with the size of the file.

Use `pipeline`, not `.pipe()`: `.pipe()` does not forward errors, so a failed read leaves the
other streams open and the promise never settles. `pipeline` destroys every stream and
rejects.

Run the tests with `npm test` after `npm install`. There are 11 tests: 1 in
`tests/visible.test.ts` and 10 hidden ones.
