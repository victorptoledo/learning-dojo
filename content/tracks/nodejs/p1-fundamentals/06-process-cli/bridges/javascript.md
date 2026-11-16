---
from: javascript
status: draft
concept: "A program that talks to a shell"
from_way: "Browser code has no arguments, no environment and no exit code; input comes from the page and output goes to the DOM."
native_way: "A Node.js program gets argv and env, reads stdin, writes stdout and stderr, exits with a code and receives signals."
sensei_tip: "console.log and console.error look the same in a browser console, but in Node.js they go to different streams, and pipelines depend on the difference."
---

| Browser | Node.js |
| --- | --- |
| URL query parameters | `process.argv` + `parseArgs` |
| Build-time env (`import.meta.env`) | `process.env` at runtime |
| `prompt()` | `readline/promises` `question()` |
| Closing the tab | Signals: `SIGINT`, `SIGTERM` |
| No exit status | `process.exitCode` |
