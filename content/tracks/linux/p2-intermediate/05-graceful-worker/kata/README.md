# Graceful Worker

Write `worker.sh [--once] QUEUE DONE`. `QUEUE` and `DONE` are directories. Every regular
file in `QUEUE` whose name does not start with a dot is a **job**.

## Processing

- Jobs are processed one at a time, in name order (byte order).
- Processing a job writes its content **in upper case** to `DONE/NAME`, then deletes
  `QUEUE/NAME`, then prints `done NAME`.
- The result must appear **atomically**: write it to `DONE/.NAME.tmp`, then `mv` it to
  `DONE/NAME`. A reader must never see a half-written file under the real name.
- After each job, and while the queue is empty, the worker sleeps `WORKER_DELAY` seconds
  (an environment variable, default `1`; the tests use fractions such as `0.2`).
- With `--once` the worker exits with status `0` as soon as the queue is empty. Without it,
  it keeps looking for new jobs until it is stopped.

## One worker at a time

- At start, the worker writes its PID (`$$`) to `QUEUE/.worker.pid`, and it deletes that
  file when it exits, however it exits.
- If `QUEUE/.worker.pid` exists and that process is alive, print
  `worker.sh: already running (pid N)` on stderr and exit `1`, without touching the file.
- If the file exists but the process is gone (a **stale** pid file), print
  `worker.sh: removing stale pid file` on stderr and carry on.

## Stopping

On `SIGTERM` or `SIGINT` the worker **finishes the job it is doing**, does not start
another, prints `stopping` and exits `0`. It must react within one `WORKER_DELAY`, even
while it sleeps.

## Errors

Not exactly two directories (after an optional `--once`): `usage: worker.sh [--once] QUEUE DONE`
on stderr, exit `2`. A `QUEUE` or `DONE` that is not a directory:
`worker.sh: not a directory: PATH` on stderr, exit `2`.

Tools: `$$`, `kill -0`, `trap '...' TERM INT`, `trap '...' EXIT`, `sleep N & wait $!`,
`find -maxdepth 1 -type f ! -name '.*'`, `tr`, `mv`, `${WORKER_DELAY:-1}`.

There are 9 tests: 1 in `tests/visible.bats` and 8 hidden ones, plus ShellCheck.
