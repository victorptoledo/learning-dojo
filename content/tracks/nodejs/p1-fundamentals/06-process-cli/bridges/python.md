---
from: python
status: draft
concept: "Command-line programs"
from_way: "sys.argv and argparse, os.environ, print and sys.stderr, sys.exit(code), signal.signal, and console_scripts entry points."
native_way: "process.argv and util.parseArgs, process.env, console.log and console.error, process.exitCode, process.on(signal), and the bin field of package.json."
sensei_tip: "parseArgs is smaller than argparse: it parses but does not generate help or convert types. sys.exit raises SystemExit; process.exit just stops, so prefer process.exitCode."
---

| Python | Node.js |
| --- | --- |
| `argparse` | `util.parseArgs` |
| `os.environ.get("PORT")` | `process.env.PORT` |
| `sys.stdin` lines | `readline` over `process.stdin` |
| `sys.exit(2)` | `process.exitCode = 2` |
| `signal.signal(SIGTERM, h)` | `process.on("SIGTERM", h)` |
| `[project.scripts]` | `"bin"` in package.json |
