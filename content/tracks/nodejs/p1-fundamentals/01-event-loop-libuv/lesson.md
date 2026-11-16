# The Node.js Event Loop and libuv

Everything asynchronous in Node.js, from a timer to an HTTP request, ends with a callback that
the **event loop** runs on the main thread. Knowing its order is the difference between
"Node.js is fast" and a server that freezes under load for no visible reason.

## The loop

When `node app.ts` starts, it runs your module's top-level code. Then it enters the event loop,
implemented by libuv, and keeps turning while there is something to wait for: a timer, an open
socket, a pending file read. When nothing is left, the process exits.

Each turn goes through **phases**, and each phase has a queue of callbacks:

```text
   ┌──────────────────────────┐
┌─▶│ timers                   │  setTimeout and setInterval callbacks that are due
│  └────────────┬─────────────┘
│  ┌────────────▼─────────────┐
│  │ pending callbacks        │  some system callbacks deferred from the last turn
│  └────────────┬─────────────┘
│  ┌────────────▼─────────────┐
│  │ poll                     │  wait for I/O and run its callbacks (sockets, files, ...)
│  └────────────┬─────────────┘
│  ┌────────────▼─────────────┐
│  │ check                    │  setImmediate callbacks
│  └────────────┬─────────────┘
│  ┌────────────▼─────────────┐
└──┤ close callbacks          │  "close" events, e.g. socket.on("close")
   └──────────────────────────┘
```

The **poll** phase is where the process spends most of its life: it waits (blocking in the
kernel, using no CPU) until I/O is ready or the next timer is due, then runs the I/O
callbacks.

## The queues between callbacks

Two more queues are not phases. They are drained **after every single callback**, before the
loop moves on:

1. the **nextTick queue** (`process.nextTick`), first;
2. the **microtask queue**: promise reactions (`.then`, the code after `await`) and
   `queueMicrotask`.

```ts
import { readFile } from "node:fs";

readFile(import.meta.filename, () => {     // we are in the poll phase
  setTimeout(() => console.log("timeout"), 0);
  setImmediate(() => console.log("immediate"));
  process.nextTick(() => console.log("nextTick"));
  Promise.resolve().then(() => console.log("promise"));
  console.log("sync");
});
// sync, nextTick, promise, immediate, timeout
```

- `sync` first: the current callback always runs to the end.
- `nextTick`, then `promise`: the queues are drained before anything else.
- `immediate` before `timeout`: after poll comes **check**; timers come in the next turn.

From the main module (not inside an I/O callback), `setTimeout(fn, 0)` vs `setImmediate(fn)`
is **not deterministic**: it depends on how fast the process got to the loop. Inside an I/O
callback, `setImmediate` always wins.

> `setTimeout(fn, 0)` actually means "at least 1 ms". A timer is a minimum delay, never a
> promise to run on time: if the loop is busy, the timer runs late.

### nextTick vs microtasks vs setImmediate

| API | Runs | Use it for |
| --- | --- | --- |
| `process.nextTick(fn)` | Right after the current callback, before promises | Rarely: emitting an event after the caller had a chance to add listeners |
| `queueMicrotask(fn)` / promises | After the nextTick queue | Normal async code |
| `setImmediate(fn)` | In the check phase of this turn | Yielding to I/O between chunks of work |

Because the nextTick and microtask queues are drained **completely**, a callback that keeps
scheduling another `nextTick` (or a promise chain that never waits for real I/O) **starves**
the loop: timers and I/O never run. `setImmediate` does not have this problem: callbacks added
during the check phase wait for the next turn.

## The thread pool

Network I/O is handled by the kernel's non-blocking APIs. Other work has no portable
non-blocking API, so libuv runs it on a **thread pool** of 4 threads by default:

- most `fs` operations,
- `dns.lookup` (used by `http.get` and `fetch` to resolve host names),
- CPU-heavy `crypto` (`pbkdf2`, `scrypt`, `randomBytes` for large sizes) and `zlib`.

With 4 threads, the fifth concurrent `pbkdf2` call waits for a free one. You can raise the size
with the `UV_THREADPOOL_SIZE` environment variable (set before the process starts, up to
1024), but a bigger pool does not give you more cores.

## Don't block the loop

Every millisecond your JavaScript computes is a millisecond no other request is served. The
usual suspects:

| Blocking | Instead |
| --- | --- |
| `readFileSync`, `execSync` ... in request handlers | The async versions (fine at startup) |
| `crypto.pbkdf2Sync`, `zlib.gzipSync` | The callback or promise versions (thread pool) |
| `JSON.parse` / `JSON.stringify` of tens of megabytes | Streaming, smaller payloads, a worker |
| Regular expressions with catastrophic backtracking | Safer patterns, input length limits |
| Long loops over big arrays | Chunks with `setImmediate` between them, or a worker thread |

Measure instead of guessing:

```ts
import { monitorEventLoopDelay } from "node:perf_hooks";

const delay = monitorEventLoopDelay({ resolution: 20 });
delay.enable();
setInterval(() => {
  console.log(`event loop delay p99: ${(delay.percentile(99) / 1e6).toFixed(1)} ms`);
  delay.reset();
}, 10_000).unref();   // unref: this timer alone does not keep the process alive
```

A healthy service has a delay of a few milliseconds. Hundreds of milliseconds mean something
blocks the loop; a CPU profile (`node --cpu-prof`) shows what.

## Summary

- The loop turns through timers → pending → poll → check → close; poll waits for I/O.
- After **every** callback, the nextTick queue and then the microtask queue are drained.
- Inside an I/O callback, `setImmediate` runs before `setTimeout(..., 0)`.
- Files, DNS lookups, and heavy crypto and zlib use libuv's thread pool (4 threads).
- Never block the loop: async APIs, chunking with `setImmediate`, worker threads; measure the
  delay with `monitorEventLoopDelay`.

## Further reading

- Node.js, "The Node.js Event Loop": <https://nodejs.org/en/learn/asynchronous-work/event-loop-timers-and-nexttick>
- Node.js, "Don't Block the Event Loop (or the Worker Pool)": <https://nodejs.org/en/learn/asynchronous-work/dont-block-the-event-loop>
- Node.js, "Understanding process.nextTick()": <https://nodejs.org/en/learn/asynchronous-work/understanding-processnexttick>
- Node.js API, `perf_hooks.monitorEventLoopDelay()`: <https://nodejs.org/api/perf_hooks.html#perf_hooksmonitoreventloopdelayoptions>
- libuv documentation, "Design overview": <https://docs.libuv.org/en/v1.x/design.html>
- Node.js API, `UV_THREADPOOL_SIZE`: <https://nodejs.org/api/cli.html#uv_threadpool_sizesize>
