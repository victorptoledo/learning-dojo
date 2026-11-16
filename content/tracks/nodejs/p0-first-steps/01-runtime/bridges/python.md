---
from: python
status: draft
concept: "An interpreter with a built-in event loop"
from_way: "CPython runs one thread at a time (the GIL); async code needs asyncio.run() to start an event loop, and most libraries are blocking by default."
native_way: "Node.js always runs an event loop, and the standard library is asynchronous by default; a JIT (V8) makes plain JavaScript fast."
sensei_tip: "Node.js feels like a Python program that is always inside asyncio.run(): the loop exists from the first line, and every I/O API has an async form, so there is no 'sync library in async code' trap unless you use the *Sync functions."
---

| Python | Node.js |
| --- | --- |
| CPython interpreter | V8 with a JIT |
| `asyncio.run(main())` | The loop is always running |
| `python -i` / REPL | `node` (REPL) |
| `pyenv`, `uv python` | `nvm`, `fnm`, Volta |
| Type hints ignored at runtime | Types stripped at runtime, checked by `tsc` |
| `multiprocessing` for CPU work | worker threads or child processes |
