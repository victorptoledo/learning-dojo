---
from: go
status: draft
concept: "Command-line tools"
from_way: "os.Args and the flag package, os.Getenv, os.Stdout and os.Stderr, os.Exit(code), and signal.NotifyContext for Ctrl+C."
native_way: "process.argv and util.parseArgs, process.env, process.stdout and stderr, process.exitCode, and process.on(\"SIGINT\")."
sensei_tip: "Go's os.Exit skips deferred functions; Node's process.exit skips pending writes. In both, return the code from main and exit once at the edge, as the bin.ts files of the katas do."
---

| Go | Node.js |
| --- | --- |
| `flag.String("o", "", "...")` | `parseArgs({ options: { o: { type: "string" } } })` |
| `os.Getenv("PORT")` | `process.env.PORT` |
| `fmt.Fprintln(os.Stderr, ...)` | `console.error(...)` |
| `os.Exit(code)` | `process.exitCode = code` |
| `signal.NotifyContext(ctx, os.Interrupt)` | `process.once("SIGINT", ...)` |
| `bufio.Scanner` over stdin | `readline` over `process.stdin` |
