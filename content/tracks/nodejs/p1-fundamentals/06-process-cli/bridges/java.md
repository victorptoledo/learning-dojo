---
from: java
status: draft
concept: "The process and its environment"
from_way: "String[] args in main (or picocli), System.getenv, System.out and System.err, System.exit(code), and shutdown hooks."
native_way: "process.argv with util.parseArgs, process.env, process.stdout and stderr, process.exitCode, and signal listeners on process."
sensei_tip: "System.exit in Java runs shutdown hooks; process.exit in Node.js stops right away and can lose buffered output. Prefer process.exitCode and let the event loop drain."
---

| Java | Node.js |
| --- | --- |
| `main(String[] args)` | `process.argv.slice(2)` |
| picocli | `util.parseArgs`, commander |
| `System.getenv("PORT")` | `process.env.PORT` |
| `System.err.println` | `console.error` |
| `System.exit(2)` | `process.exitCode = 2` |
| `Runtime.addShutdownHook` | `process.on("SIGTERM", ...)` |
