First the queue itself, kept on disk so a crash loses nothing. Export from `queue.ts` (the
starter declares the types):

| Export | Does |
| --- | --- |
| `openQueue(dir)` | Creates `dir` if needed, replays its journal and returns the `Queue` |
| `JOURNAL` | `"journal.ndjson"`, the journal's file name inside `dir` |
| `Queue` | A class that extends `EventEmitter`, with the methods below |

| Method | Does |
| --- | --- |
| `add(type, payload, { priority }?)` | A new `queued` job: the next id (`"1"`, `"2"`, ...), `priority` 0 by default, `attempts` 0, `createdAt` = `Date.now()`, the payload copied. Emits `"added"` with the job once it is saved |
| `take()` | The queued job with the **highest priority**, the **oldest** first among equals; it becomes `running` and its `attempts` goes up by one. `undefined` when none is queued |
| `complete(id, result)` | A running job becomes `done` with its result |
| `fail(id, error)` | A running job becomes `failed` with the error message |
| `get(id)`, `list(state?)` | A job (`undefined` if unknown), and every job or those in one state, in the order they were added |
| `compact()` | Rewrites the journal (below) |
| `close()` | Waits for the writes in progress, then closes the file; after that every change rejects |

`complete` and `fail` reject for an unknown id or a job that is not running. Every method
that changes a job resolves **after** the change is written. Jobs (and payloads and results)
are plain JSON. `get` and `list` hand out copies: changing what they return changes nothing
in the queue.

## The journal

- Every change appends **one line** of JSON to `dir/journal.ndjson`. Opening the queue reads
  it from the top: a later line about a job wins over an earlier one. Writing a full copy of
  the job on each change is the simplest way.
- Writes go out **one at a time, in order** (chain them on a promise), so lines never mix even
  when fifty `add` calls run at once. Twenty `take` calls at once never get the same job:
  decide which job, and mark it, before the first `await`.
- **Crash:** a job that was `running` when the process died is `queued` again on open, with
  its `attempts` kept. The tests run a child process that adds jobs, takes one and kills
  itself with `SIGKILL`.
- **Torn write:** a crash in the middle of a write can leave a last line without its `\n`.
  Ignore it, and cut it off the file before appending, or the next line would be glued to it.
- Ids are never reused: after a restart, the next id follows the highest one in the journal.
- `compact()` rewrites the journal with one line per job: write a temporary file in the same
  folder, then `rename` it over the journal, so a crash leaves either the old journal or the
  new one. Afterwards the folder holds only `journal.ndjson`.

The operating system keeps what a killed process wrote, so the tests do not need `fsync`;
surviving a power cut would (`filehandle.sync()`), at a cost on every write.
