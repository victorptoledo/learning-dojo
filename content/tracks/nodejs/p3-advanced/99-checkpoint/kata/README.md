# Checkpoint 3: Job Runner

A team wants to run batches of small jobs on a shared machine: hash some report files, call a
few command-line tools. The jobs come in a JSON **manifest** that someone else wrote, so the
manifest is untrusted input. Write the runner. It brings the phase together: CPU-bound work on
worker threads, other programs in child processes, and the security rules that keep a
manifest from doing more than it should.

```json
{
  "jobs": [
    { "id": "q1-sum", "kind": "digest", "file": "reports/q1.csv", "rounds": 1000 },
    { "id": "hello", "kind": "tool", "tool": "echo", "args": ["hello", "world"] }
  ]
}
```

`runner.ts` gives the types, `TOOLS`, `LIMITS`, `ManifestError` and `SecurityError`. Write the
five functions there and the worker in `digest-worker.ts`. `main.ts` runs a manifest with
`npm start -- jobs.json <root>`.

## parseManifest(text)

Returns a `Manifest` with **new** objects that have only the known fields, or throws
`ManifestError`:

- The text must be JSON. No key anywhere may be `__proto__`, `constructor` or `prototype`
  (prototype pollution).
- The top level is an object with a `jobs` list of 1 to `LIMITS.maxJobs` jobs.
- Every job is an object with an `id` matching `/^[a-z0-9][a-z0-9-]{0,31}$/`, unique in the
  manifest, and a `kind`:
  - `"digest"`: `file` is a non-empty string; `rounds` is a whole number from 1 to
    `LIMITS.maxRounds`, 1 when missing.
  - `"tool"`: `tool` is one of the names in `TOOLS` (its **own** keys: `toString` is not a
    tool); `args` is a list of at most `LIMITS.maxArgs` strings, `[]` when missing. No
    argument may contain a NUL byte or **start with `-`**, so a value can never become an
    option of the program.
  - Any other kind is an error.

## resolveInside(root, file)

Resolves `file` against `root` and returns its **real** path (with symbolic links followed).
It throws `SecurityError` when the file would be outside `root`: an absolute path, a NUL
byte, a `..` that climbs out (checked **before** touching the disk), or a symbolic link
inside `root` that points outside. A missing file rejects with the system error (`ENOENT`).
Compare paths with `path.relative`, not `startsWith`: `/srv/root-old` starts with `/srv/root`.

## digestInWorker(file, rounds) and digest-worker.ts

A digest is CPU-bound on purpose: `sha256(content)`, then `sha256` of the previous 32 raw
bytes, `rounds` times in all, as hex. `chainedSha256(content, rounds)` in the worker does the
math. `digestInWorker` starts a worker from `DIGEST_WORKER` with `{ file, rounds }` as
`workerData`. The worker reads the file, computes and posts the hex string. The promise
resolves with it, or rejects with the worker's error (a missing file, for example). The main
thread must stay free: a test checks that its timers keep firing during two digests of
`LIMITS.maxRounds` rounds.

## runTool(tool, args, options?)

Starts `TOOLS[tool]`: its command, its fixed arguments and then `args`.

- **No shell**, and an environment with **only `PATH`**: the runner's secrets stay in the
  runner.
- Resolves with stdout without trailing white space when the tool exits with 0.
- Rejects with `Error("<tool> exited with code <code>: <first line of stderr, trimmed>")`
  otherwise.
- After `options.timeoutMs` (default `LIMITS.toolTimeoutMs`) the child gets `SIGKILL` and the
  promise rejects with `Error("<tool> timed out after <ms> ms")`.
- When stdout passes `LIMITS.maxOutputBytes`, the child gets `SIGKILL` and the promise rejects
  with `Error("<tool> printed more than <n> bytes")`.

## runJobs(manifest, { root, concurrency? })

Runs every job, **at most `concurrency`** (default 2) at the same time, and resolves with one
`JobResult` per job **in manifest order**. A job that fails becomes
`{ id, ok: false, error: <message> }` and does not stop the others; `runJobs` itself does not
reject because of a job. A digest job resolves its file with `resolveInside` first.

Run the tests with `npm test` after `npm install`. There are 18 tests: 1 in
`tests/visible.test.ts` and 17 hidden ones (one of them is skipped on Windows).
