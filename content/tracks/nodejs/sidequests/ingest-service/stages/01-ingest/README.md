Take events in, line by line, and hand them to the sink in batches.

```ts
export interface Sink { write(lines: readonly string[]): Promise<void> }
export interface ServiceOptions { sink: Sink; batchSize?: number; flushMs?: number; maxLineBytes?: number }
export interface Service { server: Server; flush(): Promise<void> }
export function createService(options: ServiceOptions): Service;
export function fileSink(dir: string): Sink;
```

## POST /events

- The body is NDJSON: content type `application/x-ndjson`, otherwise `415`. Other methods
  on `/events` are a `405` with `Allow: POST`; other paths a `404`. Error bodies are JSON
  `{ "error": "..." }`.
- **Stream it.** Read the body chunk by chunk and cut it into lines on `\n` (a `\r` before it
  is dropped; the last line may have no `\n`). Cut the **bytes** before decoding: a chunk can
  end in the middle of a line, or in the middle of a UTF-8 character. Never hold the whole
  body in memory: there is no size limit, an upload can be gigabytes.
- **A line** longer than `maxLineBytes` (default 65536) is rejected, and you do not keep its
  bytes while skipping to its end. A blank line is skipped. Any other line must be a JSON
  object with `type`, a string of 1 to 100 characters, and `time`, a string `Date.parse`
  understands. A valid line is **accepted**: its event goes into the open batch as compact
  JSON (`JSON.stringify(JSON.parse(line))`). Otherwise it is **rejected**.
- The answer is `202` with `{ "accepted": n, "rejected": n, "errors": [{ "line": 2, "message": "..." }] }`:
  line numbers count from 1 and include blank lines, and `errors` lists the first 10
  rejected lines (the messages are yours).

## Batches

- Accepted events, from every request, go into one open batch. When it holds `batchSize`
  events (default 100), it goes to the sink **at once**, and a new batch opens.
- An open batch is also written `flushMs` (default 1000) after its first event, full or not.
- `sink.write(lines)` gets one batch: the events, in the order they arrived. **One write at a
  time**: the next batch waits for the previous write to finish.
- `flush()` sends the open batch (if it has events) and resolves when every batch handed to the
  sink is written.

## fileSink(dir)

Writes each batch to `dir/batch-000001.ndjson`, `batch-000002.ndjson`, ... (six digits), one
event per line, each ending with `\n`. It creates `dir` if needed, numbers after the batch
files already there (a restarted service does not overwrite them; other files are ignored),
and writes through a temporary file renamed into place, so a reader never sees half a batch.
