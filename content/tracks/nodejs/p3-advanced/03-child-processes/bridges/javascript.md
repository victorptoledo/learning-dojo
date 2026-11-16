---
from: javascript
status: draft
concept: "Something the browser can never do"
from_way: "Browser code cannot start programs; the nearest things are Web Workers and calling a server."
native_way: "child_process starts any program the user may run, with full access to the machine, which is exactly why input must never reach a shell."
sensei_tip: "Treat every value that ends up in a child process argument like HTML that ends up in innerHTML: escape by structure (an argument array), never by string building."
---

| Idea | Node.js |
| --- | --- |
| Run a program | `spawn(cmd, args)` |
| Run another JS file in isolation | `fork(file)` or a worker |
| Read its output | `for await (const chunk of child.stdout)` |
| Stop it | `child.kill()` or an `AbortSignal` |
