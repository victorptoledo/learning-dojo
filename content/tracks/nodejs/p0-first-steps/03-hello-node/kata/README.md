# Hello, Node

Write a small command-line program the way real Node.js tools are written: options parsed
with `parseArgs` from `node:util`, a `--help`, errors on stderr and a meaningful **exit code**.

```text
$ node bin.ts --lang pt --times 2 Ada Linus
Olá, Ada!
Olá, Linus!
Olá, Ada!
Olá, Linus!
$ node bin.ts --loud Ada
error: Unknown option '--loud' ...
usage: hello [--lang en|pt|es] [--times N] [--shout] <name...>
$ echo $?
2
```

Write three functions in `cli.ts`. `bin.ts` connects `main` to the real process and is
already done.

| Function | Does |
| --- | --- |
| `greeting(name, lang)` | `"Hello, Ada!"`, `"Olá, Ada!"` or `"Hola, Ada!"` (see `WORDS`) |
| `parseOptions(argv, defaultLang?)` | Parses the arguments with `parseArgs` in **strict** mode and returns `{ ok: true, options }` or `{ ok: false, error }`. It never throws |
| `main(argv, io)` | Runs the program with the given `Io` and returns the exit code |

## Options

| Option | Short | Meaning |
| --- | --- | --- |
| `--lang <en\|pt\|es>` | `-l` | The language. Without it, `defaultLang` (unless it is empty), else `en` |
| `--times <N>` | `-t` | Repeat every greeting N times, N a whole number from 1 to 5 (default 1) |
| `--shout` | `-s` | Upper-case every line |
| `--help` | `-h` | Print `USAGE` to stdout and exit with 0; no name needed |

The other arguments are the names: at least one is required (unless `--help`). With `--times 2`
the program prints every name once, then every name again.

## main

- Calls `parseOptions(argv, io.env.HELLO_LANG)`: the environment variable `HELLO_LANG` sets
  the default language.
- On an error it writes **two lines to stderr**: `error: <message>`, then `USAGE`. Nothing goes
  to stdout, and the exit code is `2` (the usual code for "wrong usage").
- Otherwise it writes the greetings, one per line, to stdout and returns `0`.

`main` never touches `process` itself: it gets an `Io` instead. That keeps it easy to test,
and `bin.ts` passes the real `process.env`, `process.stdout` and `process.stderr`. Notice that
`bin.ts` sets `process.exitCode` instead of calling `process.exit()`, so output is never cut off.

Run the tests with `npm test` after `npm install`, or try the program with
`node bin.ts Ada`. There are 12 tests: 1 in `tests/visible.test.ts` and 11 hidden ones.
