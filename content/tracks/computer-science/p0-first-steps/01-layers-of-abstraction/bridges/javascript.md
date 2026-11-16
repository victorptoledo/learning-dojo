---
from: javascript
status: draft
concept: "The layers under a JavaScript program"
from_way: "Seeing the browser or Node.js as a black box that runs JavaScript."
native_way: "V8 parses your code, interprets bytecode (Ignition) and compiles hot functions (TurboFan); one event-loop thread runs your code, while libuv and the OS handle I/O."
sensei_tip: "In Node.js, `--cpu-prof` and `--prof` show where time goes across your code, V8 and native code: the layers made visible."
---

| In JavaScript / TypeScript | Underneath |
| --- | --- |
| `node app.js` | a process running V8 and libuv |
| `number` | IEEE 754 binary64 (or a small integer V8 tags internally) |
| `await fetch()` | a socket and an event-loop callback |
| `Worker` | a second OS thread with its own V8 isolate |
| TypeScript types | erased by the compiler before anything runs |
