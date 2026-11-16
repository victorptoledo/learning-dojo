# EventEmitter and Event-Driven Code

Half of Node.js's API is built on one class: **`EventEmitter`**. HTTP servers emit
`"request"`, streams emit `"data"` and `"end"`, child processes emit `"exit"`, `process` emits
`"SIGTERM"`. Knowing its rules (synchronous, the special `"error"` event, listener leaks) is
knowing how most of Node.js behaves.

## The API

```ts
import { EventEmitter } from "node:events";

const bus = new EventEmitter();

function onOrder(id: string, total: number) {
  console.log(`order ${id}: ${total}`);
}

bus.on("order", onOrder);                       // every time
bus.once("ready", () => console.log("ready"));  // the first time only, then removed
bus.emit("order", "o-1", 42);                   // calls the listeners with these arguments
bus.emit("ready");
bus.off("order", onOrder);                      // removeListener: needs the same function
bus.listenerCount("order");                     // 0
```

`emit` returns `true` if the event had listeners. Listeners run **in the order they were
added**. A class becomes an emitter by extending it: `class Downloader extends EventEmitter`.

## Typed events

`@types/node` makes `EventEmitter` generic over an **event map**: event names to tuples of
arguments. TypeScript then checks every `emit` and `on`:

```ts
interface DownloadEvents {
  progress: [percent: number];
  done: [path: string];
  error: [error: Error];
}

class Downloader extends EventEmitter<DownloadEvents> {
  start() {
    this.emit("progress", 50);
    this.emit("done", "/tmp/file");    // this.emit("done", 1) is a type error
  }
}

new Downloader().on("progress", (percent) => console.log(percent.toFixed(0)));  // percent: number
```

## Listeners run synchronously

`emit` calls every listener **right away, on the same stack**, and returns when they are done.
Nothing is queued. Two consequences:

1. A slow listener slows the emitter. A listener that throws makes `emit` throw.
2. **Emitting from a constructor (or any method a caller has just called) is too early**: the
   caller has not added its listeners yet.

```ts
class Cache extends EventEmitter {
  constructor(key: string) {
    super();
    if (memory.has(key)) {
      this.emit("hit", memory.get(key));               // nobody is listening yet: lost
      process.nextTick(() => this.emit("hit", memory.get(key)));   // fixed
    }
  }
}
```

An API that is sometimes synchronous and sometimes asynchronous (nicknamed "releasing
Zalgo") is a source of bugs: always deliver results asynchronously. `process.nextTick` is
the tool Node.js itself uses for this.

## The "error" event

`"error"` is special: **if it is emitted and there is no listener, `emit` throws the error**.
In a server that usually means the process stops with "Unhandled 'error' event".

```ts
const socket = connect(5432, "db.internal");
socket.on("error", (error) => logger.warn({ error }, "database connection failed"));
```

Every emitter that can fail (sockets, streams, servers, child processes) needs an `"error"`
listener. In your own classes, use `"error"` for real failures, and other events (or return
values) for expected outcomes such as a failed job in a queue.

## Listener leaks

Adding a listener per request to a long-lived emitter, and never removing it, is a memory
leak: each closure keeps its request alive. Node.js warns when an event gets more than 10
listeners:

```text
MaxListenersExceededWarning: Possible EventEmitter memory leak detected.
11 close listeners added to [Socket]. MaxListeners is 10.
```

The fix is almost never `setMaxListeners(100)`. Use `once` when you only need one event,
remove listeners with `off` when you are done, or tie them to a **signal**:

```ts
function watchConfig(signal: AbortSignal) {
  config.on("change", reload);
  signal.addEventListener("abort", () => config.off("change", reload), { once: true });
}

const controller = new AbortController();
watchConfig(controller.signal);
controller.abort();          // the listener is gone
```

## Events as promises and async iterators

`node:events` turns events into promises and loops:

```ts
import { once, on } from "node:events";

await once(server, "listening");                    // resolves with the event's arguments
const [code] = await once(child, "exit");

// rejects if "error" comes first; with a signal, stops waiting
await once(queue, "drain", { signal: AbortSignal.timeout(5_000) });

for await (const [message] of on(socket, "message", { signal })) {
  handle(message);                                  // until the signal aborts
}
```

`once` also listens to `"error"` for you and removes both listeners when it settles.

## EventTarget

The web's `EventTarget` and `Event` classes exist in Node.js too (`AbortSignal` is one). They
use `addEventListener` with an options object (`{ once: true, signal }`), and listeners get an
`Event` object instead of arguments. Use `EventEmitter` for Node.js-style code and
`EventTarget` when you share code with the browser.

## Summary

- `on`, `once`, `off`, `emit`; listeners run synchronously, in order.
- Type events with an event map: `EventEmitter<{ name: [args] }>`.
- Never emit before the caller can listen: defer with `process.nextTick`.
- An unhandled `"error"` event throws: every emitter that can fail needs a listener.
- Remove what you add; the MaxListeners warning usually means a leak.
- `events.once` and `events.on` give promises and async iterators, with `AbortSignal`.

## Further reading

- Node.js API, "Events": <https://nodejs.org/api/events.html>
- Node.js API, "Error events": <https://nodejs.org/api/events.html#error-events>
- Node.js API, `events.once()`: <https://nodejs.org/api/events.html#eventsonceemitter-name-options>
- Node.js, "The Node.js Event emitter": <https://nodejs.org/en/learn/asynchronous-work/the-nodejs-event-emitter>
- Isaac Z. Schlueter, "Designing APIs for Asynchrony": <https://blog.izs.me/2013/08/designing-apis-for-asynchrony/>
- MDN, `EventTarget`: <https://developer.mozilla.org/en-US/docs/Web/API/EventTarget>
