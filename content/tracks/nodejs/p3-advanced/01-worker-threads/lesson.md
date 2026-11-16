# Worker Threads for CPU-Bound Work

The event loop is great at waiting and terrible at computing: every millisecond of CPU work
on the main thread delays every other request. **Worker threads** (`node:worker_threads`) run
JavaScript on other threads, each with its own event loop and V8 instance, so CPU work can use
other cores while the main thread keeps answering.

## When (not) to use them

| Work | Where |
| --- | --- |
| I/O: database, HTTP calls, files | The main thread. Workers do not make I/O faster; libuv already does it in the background |
| Short CPU work (< a few ms per request) | The main thread |
| Long CPU work: image or PDF processing, compression, hashing large data, parsing huge documents, number crunching | **Worker threads** |
| Running another program (ffmpeg, git, Python) | Child processes (next lesson) |
| Using all cores for an HTTP server | Several processes: a load balancer, Kubernetes replicas or the `cluster` module |

## A first worker

```ts
// main.ts
import { Worker } from "node:worker_threads";

const worker = new Worker(new URL("./fib.worker.ts", import.meta.url), { workerData: 40 });
worker.on("message", (result: number) => console.log("fib(40) =", result));
worker.on("error", (error) => console.error("worker failed", error));
worker.on("exit", (code) => console.log("worker exited with", code));
```

```ts
// fib.worker.ts
import { parentPort, workerData } from "node:worker_threads";

const fib = (n: number): number => (n < 2 ? n : fib(n - 1) + fib(n - 2));
parentPort?.postMessage(fib(workerData as number));
```

- The worker is a separate module with its own globals. It can import other modules, use
  `fs`, `fetch` and timers, but it **shares no JavaScript objects** with the main thread.
- `workerData` is a value passed at start; `parentPort.postMessage` and `worker.postMessage`
  send messages both ways; `isMainThread` tells the two sides apart.
- Node.js 26 runs `.ts` worker files directly, like any other module.
- Inside a worker, `process.exit()` ends only that worker.

## What travels between threads

Messages are copied with the **structured clone** algorithm: objects, arrays, `Map`, `Set`,
`Date`, typed arrays, errors. Functions, class instances' methods and sockets cannot be
cloned.

Copying a 50 MB buffer costs time and doubles the memory. **Transfer** it instead: ownership
moves to the other thread and the sender's copy becomes empty.

```ts
const pixels = new Uint8Array(50_000_000);
worker.postMessage({ pixels }, [pixels.buffer]);   // transferred, not copied
pixels.byteLength;                                 // 0 on this side now
```

A `SharedArrayBuffer` is memory that **both** threads see at the same time. Use it with
`Atomics` (`Atomics.add`, `Atomics.wait`, `Atomics.notify`) to avoid data races. It is
powerful and rarely needed: messages are simpler and safer.

## Errors and lifetimes

- An exception thrown in a worker and not caught there emits `"error"` on the `Worker`, then
  `"exit"` with code 1. Without an `"error"` listener on the main side, it crashes the main
  process too.
- `worker.terminate()` stops a worker; its promise resolves with the exit code.
- A busy worker keeps the process alive; `worker.unref()` lets the process exit anyway.
- `resourceLimits` (`maxOldGenerationSizeMb` ...) cap a worker's heap, so a runaway task
  fails alone instead of taking the process down.

Prefer answering an error **as a message** (`{ ok: false, error }`) for failures you expect,
and keep `"error"`/`"exit"` for real crashes.

## Pools

Starting a worker takes tens of milliseconds and some memory (a new V8 isolate). One worker
per request wastes that and, under load, creates thousands of threads. Real programs keep a
**pool**: a fixed number of workers (about `os.availableParallelism()`), a queue of tasks, and
each free worker takes the next task.

```ts
import Piscina from "piscina";

const pool = new Piscina({ filename: new URL("./resize.worker.ts", import.meta.url).href });
const thumbnail = await pool.run({ image, width: 200 });
```

Piscina and workerpool are well-tested libraries; in the kata you write a small pool yourself,
to see the queue, the message matching and crash handling from inside. Also **bound the
queue**: a pool that accepts unlimited tasks just moves the overload from the CPU to memory.
Answer 503 (or 429) when it is full.

## Workers vs processes vs cluster

| | Worker threads | Child processes | `cluster` / several processes |
| --- | --- | --- | --- |
| Isolation | Same process, separate heaps | Separate processes | Separate processes |
| Start cost | Tens of ms | More (a whole runtime) | More |
| Communication | `postMessage`, transfer, shared memory | stdio, IPC messages | Shared server port |
| A crash | Can be caught; memory limits per worker | Isolated | Isolated |
| Typical use | CPU work inside a service | Other programs, untrusted code | Using every core for an HTTP server |

In containers, the usual way to use more cores is **more replicas** rather than `cluster`.

## Summary

- Worker threads are for CPU-bound JavaScript, not for I/O.
- Communicate with `postMessage` (structured clone); transfer big buffers; share memory only
  with `SharedArrayBuffer` + `Atomics`.
- Listen to `"error"` and `"exit"`; terminate workers you no longer need.
- Reuse workers in a bounded pool (Piscina or your own).

## Further reading

- Node.js API, "Worker threads": <https://nodejs.org/api/worker_threads.html>
- Node.js, "Don't Block the Event Loop (or the Worker Pool)": <https://nodejs.org/en/learn/asynchronous-work/dont-block-the-event-loop>
- MDN, "The structured clone algorithm": <https://developer.mozilla.org/en-US/docs/Web/API/Web_Workers_API/Structured_clone_algorithm>
- MDN, `SharedArrayBuffer` and `Atomics`: <https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Atomics>
- Piscina documentation: <https://piscinajs.dev/>
- Node.js API, "Cluster": <https://nodejs.org/api/cluster.html>
