# Processes, Environment and Command-Line Programs

Every Node.js program is an operating system process, and `process` is its control panel:
arguments, environment, standard streams, exit code and signals. Getting these right makes a
program behave well in shells, pipelines, containers and CI jobs.

## Arguments

`process.argv` holds the full command line: the path to `node`, the path to your script, then
the arguments. Parse them with **`util.parseArgs`**, built in since Node.js 18.3:

```ts
import { parseArgs } from "node:util";

const { values, positionals } = parseArgs({
  args: process.argv.slice(2),
  allowPositionals: true,
  strict: true,                        // unknown options throw a TypeError
  options: {
    output: { type: "string", short: "o" },
    verbose: { type: "boolean", short: "v", default: false },
    tag: { type: "string", multiple: true },          // --tag a --tag b
  },
});
// node tool.ts -v --output out.json --tag x in.csv
// values: { verbose: true, output: "out.json", tag: ["x"] }, positionals: ["in.csv"]
```

Values of `type: "string"` are strings: convert and validate numbers yourself. In strict mode
catch the `TypeError` and print a usage message instead of a stack trace. For big CLIs with
subcommands, libraries such as commander or yargs add help generation.

## Environment variables

`process.env` is an object of strings (or `undefined`): configuration, secrets, the `PATH`.

```ts
const port = Number(process.env.PORT ?? 3000);
const debug = process.env.DEBUG === "1";
```

- Every value is a string: `"false"` is truthy, `"0"` is not `0`. Parse explicitly.
- Read and validate them **once at startup** into a typed config object, and fail fast when
  a required one is missing.
- `node --env-file=.env app.ts` loads a `.env` file (since Node.js 20.6; also
  `--env-file-if-exists` and `process.loadEnvFile()`), so the dotenv package is optional.
  Never commit a `.env` with real secrets.

## Standard streams

| Stream | For |
| --- | --- |
| `process.stdout` | The program's **output**: data another program may read |
| `process.stderr` | Messages for humans: progress, warnings, errors |
| `process.stdin` | Input: a file redirected with `<`, or the output of a pipe |

`console.log` writes to stdout and `console.error` to stderr. Keeping data on stdout and
everything else on stderr is what lets `tool | jq` and `tool > out.json` work.

stdin is a readable stream. Read it all, or line by line:

```ts
import { text } from "node:stream/consumers";
import { createInterface } from "node:readline";

const all = await text(process.stdin);                  // small inputs

for await (const line of createInterface({ input: process.stdin, crlfDelay: Infinity })) {
  console.log(line.toUpperCase());                       // large inputs, one line at a time
}
```

`process.stdin.isTTY` and `process.stdout.isTTY` tell you whether a human is at a terminal
(prompt, colours) or a pipe (plain output). For questions, `readline/promises` has
`rl.question("Name? ")`.

## Exit codes

The exit code tells the shell, CI or the orchestrator what happened: **0** means success,
anything else a failure. Conventions: `1` for a general error, `2` for wrong usage, and
`128 + n` when signal `n` killed the process (130 for Ctrl+C, 143 for SIGTERM).

```ts
process.exitCode = 1;    // preferred: the process exits with 1 when the loop is empty
process.exit(1);         // immediate: pending writes and callbacks may be lost
```

`process.exit()` stops at once, and output to a pipe that is still being written can be cut
off. Set `process.exitCode` and let the program finish, unless you really must stop now.

The process exits by itself when the event loop has nothing left to do. A forgotten
`setInterval` or open socket keeps it alive; `timer.unref()` tells Node.js not to wait for a
timer.

## Signals

Signals come from the operating system: `SIGINT` (Ctrl+C), `SIGTERM` (`kill`, Docker,
Kubernetes), `SIGHUP` (terminal closed). Node.js's default for `SIGINT` and `SIGTERM` is to
exit. Listening replaces that default, so your handler must end the program:

```ts
process.once("SIGINT", () => {
  console.error("\ninterrupted, cleaning up");
  cleanup().finally(() => process.exit(130));
});
```

Phase 4 turns this into a full graceful shutdown. On Windows, `SIGINT` from Ctrl+C works; most
other signals are emulated or missing.

Writing to a pipe whose reader has gone (`tool | head -1`) makes the next write fail with
`EPIPE`. A CLI should treat that as "the reader is done" and exit quietly:

```ts
process.stdout.on("error", (error: NodeJS.ErrnoException) => {
  if (error.code === "EPIPE") process.exit(0);
  throw error;
});
```

## Publishing a command

```json
{ "name": "csv2json", "type": "module", "bin": { "csv2json": "./bin/csv2json.js" } }
```

The file starts with a shebang, `#!/usr/bin/env node`. `npm install -g` (or `npx csv2json`)
puts the command on the `PATH`. Published packages usually ship compiled `.js`, because Node.js
does not strip types of files inside `node_modules`.

## Summary

- `util.parseArgs` in strict mode for arguments; validate values yourself.
- `process.env` holds strings: parse and validate once; `--env-file` loads `.env`.
- Data to stdout, messages to stderr; read stdin as a stream or with `readline`.
- Exit codes matter: `process.exitCode` over `process.exit()`.
- Handle `SIGINT`/`SIGTERM` and `EPIPE`; publish commands with `bin` and a shebang.

## Further reading

- Node.js API, "Process": <https://nodejs.org/api/process.html>
- Node.js API, `util.parseArgs()`: <https://nodejs.org/api/util.html#utilparseargsconfig>
- Node.js API, `--env-file`: <https://nodejs.org/api/cli.html#--env-filefile>
- Node.js API, "Signal events": <https://nodejs.org/api/process.html#signal-events>
- Node.js API, "Readline": <https://nodejs.org/api/readline.html>
- Node.js, "How to read environment variables from Node.js": <https://nodejs.org/en/learn/command-line/how-to-read-environment-variables-from-nodejs>
- The Twelve-Factor App, "Config": <https://12factor.net/config>
- GNU Bash manual, "Exit Status": <https://www.gnu.org/software/bash/manual/html_node/Exit-Status.html>
