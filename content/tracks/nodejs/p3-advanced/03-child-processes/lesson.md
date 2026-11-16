# Child Processes: spawn, exec and Pipes

Some work belongs to other programs: `git`, `ffmpeg`, `pg_dump`, `convert`, a Python model.
`node:child_process` starts them, streams their input and output, and tells you how they
ended. It is also where one of the most common Node.js vulnerabilities lives: **command
injection**.

## Four functions

| Function | Shell? | Output | Use it for |
| --- | --- | --- | --- |
| `spawn(cmd, args, options)` | No (unless `shell: true`) | Streams | The general tool: long-running programs, large output, input to write |
| `execFile(cmd, args, options, cb)` | No | Buffered (up to `maxBuffer`, 1 MiB) | Short programs with small output |
| `exec(commandString, options, cb)` | **Yes** | Buffered | Quick scripts with fixed command strings only |
| `fork(modulePath, args)` | No | Streams + an IPC channel | Another Node.js process you send messages to |

Each has a `*Sync` version (`spawnSync`, `execFileSync`) that blocks the event loop: fine in
scripts and build tools, never in a server.

```ts
import { execFile, spawn } from "node:child_process";
import { promisify } from "node:util";

const run = promisify(execFile);
const { stdout } = await run("git", ["log", "-1", "--format=%H"], { cwd: repo });

const child = spawn("ffmpeg", ["-i", input, "-vf", "scale=640:-1", output]);
child.stderr.on("data", (chunk) => progress(chunk.toString()));
child.on("error", (error) => console.error("could not start ffmpeg", error));   // e.g. ENOENT
child.on("close", (code, signal) => console.log("ffmpeg ended", code, signal));
```

## Command injection

`exec` hands its string to a shell (`/bin/sh -c` or `cmd.exe /c`), and the shell interprets
`;`, `|`, `&&`, `$(...)`, backticks and quotes:

```ts
exec(`convert ${name} thumb.png`);   // name = "a.png; rm -rf ~" runs rm -rf ~
```

With `spawn` or `execFile` and an **argument array**, there is no shell: each array element
reaches the program as exactly one argument, whatever it contains. That is the fix.
Additionally:

- Validate inputs that become arguments (an allow list, a pattern), and pass `--` before
  user-supplied values when the program supports it, so a value starting with `-` is not read
  as an option.
- Generate file names on the server instead of trusting the client's.
- Avoid `shell: true`. On Windows, `.bat` and `.cmd` files need a shell; Node.js refuses to
  run them without `shell: true` (a past injection fix), so prefer real executables.

## stdio and pipes

`stdio` configures the child's stdin, stdout and stderr:

| Value | Meaning |
| --- | --- |
| `"pipe"` (default) | A stream in the parent: `child.stdin`, `child.stdout`, `child.stderr` |
| `"inherit"` | Share the parent's stream (output goes straight to your terminal or log) |
| `"ignore"` | Connect to `/dev/null` |

The pipes are ordinary Node.js streams, so `pipeline` connects them, with backpressure:

```ts
import { pipeline } from "node:stream/promises";

const dump = spawn("pg_dump", ["--format=plain", databaseUrl]);
const gzip = spawn("gzip", ["-c"]);
await pipeline(dump.stdout, gzip.stdin);     // pg_dump | gzip
await pipeline(gzip.stdout, createWriteStream("backup.sql.gz"));
```

Always read (or ignore) a child's stdout and stderr: a pipe has a small kernel buffer, and a
child that fills it **blocks** until someone reads. And close its stdin (`child.stdin.end()`)
when you have nothing to send, or a program that reads stdin waits forever.

## How a child ends

- `"exit"` fires when the process ends; `"close"` fires later, when its stdio streams have
  also closed. Use `"close"` when you need all the output.
- `code` is the exit code (0 = success), or `null` when a **signal** ended it, and then
  `signal` says which (`"SIGTERM"`, `"SIGKILL"`).
- `"error"` means it could not be started (`ENOENT`: no such program), could not be killed,
  or a message could not be sent. It may come without `"exit"`.

Never assume success: check the code. A non-zero exit with a useful stderr is the normal way
programs report failure.

## Timeouts and killing

```ts
const child = spawn("convert", args, {
  timeout: 30_000,             // sends killSignal (default SIGTERM) after 30 s
  signal: request.signal,      // abort() kills the child too
});
```

`SIGTERM` asks a program to stop; it may clean up, or ignore it. `SIGKILL` cannot be caught or
ignored. A robust wrapper sends `SIGTERM`, waits a grace period, then `SIGKILL`. On Windows,
signals are emulated and `kill()` always terminates the process.

Children that outlive the parent are **orphans**; a killed shell (`exec`, `shell: true`) can
leave its own children running, which is one more reason to avoid the shell. `detached: true`
plus `child.unref()` deliberately starts a background process that survives the parent.

## Environment and working directory

`cwd` sets the child's working folder and `env` its **whole** environment, so spread
`process.env` when you only add a variable:

```ts
spawn("npm", ["run", "build"], { cwd: project, env: { ...process.env, NODE_ENV: "production" } });
```

Do not pass secrets the child does not need: every child gets a copy of the environment.

## fork and IPC

`fork("worker.ts")` starts another Node.js process with an IPC channel: `child.send(msg)` and
`process.on("message")` in the child, with JSON-like serialisation. It isolates crashes and
memory better than worker threads, at a higher start cost. The `cluster` module is built on
it.

## Summary

- `spawn`/`execFile` with an argument array: no shell, no injection. `exec` only with fixed
  strings.
- stdio streams are normal streams: pipe them, always read them, close stdin.
- Check `code` and `signal` on `"close"`; handle `"error"` (ENOENT).
- Time out with SIGTERM then SIGKILL; kill children when their request is cancelled.
- `env` replaces the whole environment; `fork` adds an IPC channel.

## Further reading

- Node.js API, "Child process": <https://nodejs.org/api/child_process.html>
- Node.js API, "Spawning .bat and .cmd files on Windows": <https://nodejs.org/api/child_process.html#spawning-bat-and-cmd-files-on-windows>
- OWASP, "Command Injection": <https://owasp.org/www-community/attacks/Command_Injection>
- OWASP, "OS Command Injection Defense Cheat Sheet": <https://cheatsheetseries.owasp.org/cheatsheets/OS_Command_Injection_Defense_Cheat_Sheet.html>
- `signal(7)`, Linux manual page: <https://man7.org/linux/man-pages/man7/signal.7.html>
- `pipe(7)`, Linux manual page (pipe capacity): <https://man7.org/linux/man-pages/man7/pipe.7.html>
