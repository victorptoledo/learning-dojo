# Run Command

Backends call other programs all the time: `git`, `ffmpeg`, `pg_dump`, a Python script. The
quick way, `exec("convert " + userFile)`, has three classic bugs: it goes through a shell (so
`userFile = "x; rm -rf ~"` is a disaster), it buffers unlimited output, and a hung child hangs
the caller forever. Write the safe version on top of `spawn`.

Write three functions in `run.ts`. The types and `CommandError` are given.

## run(command, args?, options?)

Starts the command with `spawn(command, args, ...)` **without a shell**, so every argument
reaches the child exactly as written. It resolves with a `RunResult` when the child has exited
and its output is read (the `"close"` event), **whatever the exit code**. It rejects only when
the program cannot start, with the system error (for example `code: "ENOENT"`).

| Option | Meaning |
| --- | --- |
| `cwd`, `env` | The child's working folder and its whole environment (default: this process's) |
| `input` | Written to the child's stdin, which is then closed. **Without `input`, stdin is closed at once**, so a child that reads stdin sees the end instead of waiting forever |
| `timeoutMs` | After this, `timedOut` becomes true and the child gets `SIGTERM`. If it is still alive `killGraceMs` later (default 500), it gets `SIGKILL` |
| `maxOutputBytes` | stdout and stderr each keep at most this many bytes (default 1 MiB); the rest is read and dropped, and `truncated` becomes true. Keep reading, or the child blocks on a full pipe |

`code` is the exit code, or `null` when a signal ended the child (`signal` says which).
Clear your timers when the child exits: a timer left behind keeps the process alive.

## runOrThrow(command, args?, options?)

Resolves with stdout when the command exited with code 0. Otherwise it rejects with a
`CommandError` carrying the result, with one of these messages:

- `<command> timed out after <timeoutMs> ms`
- `<command> was killed by <signal>`
- `<command> exited with code <code>: <first line of stderr, trimmed>`

## pipeCommands(first, second)

The shell's `first | second`: start both, stream the first child's stdout into the second
child's stdin (with `pipeline`), and resolve with `{ stdout, codes }`: the second's stdout and
both exit codes. If the second stops reading early, the write fails with `EPIPE`: that is
normal (think `yes | head -1`), not an error.

The tests run small `node -e "..."` scripts as children, so they work the same on any
system. Run them with `npm test` after `npm install`. There are 15 tests: 1 in
`tests/visible.test.ts` and 14 hidden ones (one of them is skipped on Windows).
