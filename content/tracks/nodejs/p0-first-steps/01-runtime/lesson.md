# The Node.js Runtime: V8, libuv and Running TypeScript

JavaScript was born in the browser, where it could not open a file or a socket. In 2009 Ryan
Dahl took Chrome's JavaScript engine, **V8**, put it in a program with a C library for
asynchronous I/O, **libuv**, and added a standard library for the operating system: files,
sockets, processes, cryptography. That program is **Node.js**. It is not a language and not a
framework: it is a **runtime** that runs JavaScript (and, since Node.js 23.6, TypeScript)
outside the browser.

## The pieces

```text
┌──────────────────────── your code (JavaScript / TypeScript) ───────────────────────┐
│   node:fs   node:http   node:stream   node:crypto   node:child_process   ...        │  standard library
├──────────────────────────────────────┬─────────────────────────────────────────────┤
│ V8: parses, compiles (JIT) and runs  │ libuv: event loop, non-blocking sockets,    │
│ JavaScript, manages memory (GC)      │ timers, a thread pool for files, DNS, crypto │
└──────────────────────────────────────┴─────────────────────────────────────────────┘
                                 operating system (epoll, kqueue, IOCP)
```

- **V8** compiles JavaScript to machine code just in time and collects garbage. It knows
  nothing about files or the network.
- **libuv** runs the **event loop** and talks to the operating system. Network I/O uses the
  kernel's non-blocking APIs (epoll on Linux, kqueue on macOS, IOCP on Windows). Work the
  operating system cannot do asynchronously, such as most file operations, DNS lookups and
  some crypto, runs on libuv's small **thread pool** (4 threads by default,
  `UV_THREADPOOL_SIZE`).
- The **bindings and the standard library** (`node:fs`, `node:http` ...) connect the two: you
  call `readFile`, Node.js asks libuv, and libuv calls your callback when the data is there.

## One thread, many connections

Your JavaScript runs on **one main thread**. That sounds like a weakness, but most backend
work is **waiting**: for the database, for another service, for the disk. A
thread-per-request server (classic Java servlets, PHP-FPM) keeps a whole thread, with its
stack, blocked during every wait. Node.js instead starts the I/O, registers a callback and
moves on to the next request. When the answer arrives, the event loop runs the callback.

```js
import { createServer } from "node:http";

createServer(async (req, res) => {
  const user = await db.findUser(req.url);  // the thread is free while the database works
  res.end(JSON.stringify(user));
}).listen(3000);
```

The flip side: **while your code computes, nothing else runs**. A loop that takes 2 seconds to
parse a huge JSON blocks every other request for 2 seconds. Node.js is a great fit for
I/O-bound services (APIs, gateways, real-time apps, tooling) and a poor fit for long CPU-bound
work on the main thread. For that there are **worker threads**, **child processes** and running
several processes (phases 3 and 4).

| Good fit | Think twice |
| --- | --- |
| REST and GraphQL APIs, BFFs, gateways | Video encoding, heavy image processing on the main thread |
| WebSockets, chat, streaming | Scientific computing, large in-memory number crunching |
| CLIs, build tools, scripts | Work that needs shared-memory multi-threading everywhere |

## Releases: Current and LTS

Node.js has a new major version twice a year. **Even** majors (20, 22, 24, 26) become **Long
Term Support** (LTS) releases in October and get fixes for about 30 months; odd majors never
do. Use an **Active or Maintenance LTS** release in production and check the schedule before
starting a project. This track uses **Node.js 26**.

```bash
node --version          # v26.x
node                    # the REPL: try 1 + 1, .help, .exit
node app.js             # run a file
node --watch app.js     # rerun when a file changes (no nodemon needed)
node --run test         # run the "test" script of package.json, faster than npm run
```

Install Node.js from nodejs.org, the package manager of your system, or a version manager
such as **nvm**, **fnm** or **Volta**, which let each project pin its own version.

## Running TypeScript

Node.js 26 runs `.ts` files directly with **type stripping**: it removes the type annotations
and runs what is left, with no build step.

```ts
// greet.ts
function greet(name: string): string {
  return `Hello, ${name}!`;
}
console.log(greet("Ada"));
```

```bash
node greet.ts           # Hello, Ada!
```

Two things to remember:

1. **Stripping is not checking.** A type error does not stop `node greet.ts`. Type checking is
   a separate step: `tsc --noEmit` (or your editor). The katas of this track run `tsc` first.
2. **Only erasable syntax works.** Annotations, interfaces, `type` and generics are removed by
   deleting text. Syntax that generates code, such as `enum`, `namespace` with values and
   constructor parameter properties, is rejected. The `erasableSyntaxOnly` option of `tsc`
   reports it before Node does.

Imports name the real file, with its extension: `import { greet } from "./greet.ts"`.

## Globals that are not in the browser

| Global | What it is |
| --- | --- |
| `process` | The running process: `argv`, `env`, `exitCode`, `stdout`, signals |
| `Buffer` | Binary data (phase 2) |
| `import.meta.dirname`, `import.meta.filename` | This module's folder and file (ES modules) |
| `globalThis` | The global object (`window` does not exist) |

Many web APIs exist too: `fetch`, `URL`, `AbortController`, `TextEncoder`, `structuredClone`,
`setTimeout`, Web Streams and `crypto.randomUUID()`.

## Summary

- Node.js = V8 (runs JavaScript) + libuv (event loop, async I/O, thread pool) + a standard
  library for the operating system.
- One thread runs your code; I/O waits do not block it, CPU work does.
- Use an even-numbered LTS release in production; `node --watch` and `node --run` cover common
  tooling needs.
- `node file.ts` strips types without checking them: run `tsc` as well.

## Further reading

- Node.js, "Introduction to Node.js": <https://nodejs.org/en/learn/getting-started/introduction-to-nodejs>
- Node.js, "Node.js Releases": <https://nodejs.org/en/about/previous-releases>
- Node.js, "Running TypeScript Natively": <https://nodejs.org/en/learn/typescript/run-natively>
- Node.js API, "Modules: TypeScript": <https://nodejs.org/api/typescript.html>
- libuv documentation, "Design overview": <https://docs.libuv.org/en/v1.x/design.html>
- V8 documentation: <https://v8.dev/docs>
- roadmap.sh, Node.js: <https://roadmap.sh/nodejs>
