# Callbacks, Promises and async/await in Node.js

Node.js has been asynchronous since day one, and its async style has changed three times:
callbacks, then promises, then `async`/`await`. Modern code uses the last one, but the other
two are everywhere in libraries and older code, and the rules about errors come from them.

## Error-first callbacks

The original Node.js convention: the last argument is a callback, and its **first** parameter
is an error (or `null`).

```js
import { readFile } from "node:fs";

readFile("config.json", "utf8", (err, text) => {
  if (err) {
    console.error("cannot read the config:", err.message);
    return;
  }
  console.log(JSON.parse(text));
});
```

The problems appear when callbacks nest:

- **Pyramid of doom**: each step indents the next one.
- **Errors do not travel**: a `throw` inside a callback is not caught by any `try` around
  `readFile`; that stack is long gone. It becomes an uncaught exception and stops the process.
- Easy to forget `return` after handling an error, or to call the callback twice.

## Promises

A promise is a value for a result that is not there yet. Most Node.js APIs have a promise form:

```ts
import { readFile } from "node:fs/promises";
import { setTimeout as sleep } from "node:timers/promises";
import { promisify } from "node:util";
import { execFile } from "node:child_process";

const text = await readFile("config.json", "utf8");
await sleep(100);

const execFileAsync = promisify(execFile);          // any error-first callback API
const { stdout } = await execFileAsync("git", ["rev-parse", "HEAD"]);
```

`util.promisify(fn)` turns an error-first function into one that returns a promise. Prefer the
`/promises` modules (`node:fs/promises`, `node:dns/promises`, `node:stream/promises`,
`node:timers/promises`) when they exist.

## async/await

```ts
async function loadDashboard(userId: string) {
  const user = await db.getUser(userId);                       // needed by the next two
  const [orders, recommendations] = await Promise.all([        // independent: run together
    db.getOrders(user.id),
    db.getRecommendations(user.id),
  ]);
  return { user, orders, recommendations };
}
```

- An `async` function always returns a promise; `throw` inside it rejects that promise.
- `try`/`catch` works around `await`: errors travel again.
- `await` in sequence when a step needs the previous result; `Promise.all` when it does not.

| Combinator | Resolves | Rejects |
| --- | --- | --- |
| `Promise.all` | When all resolve | At the first rejection (the others keep running) |
| `Promise.allSettled` | When all settle, with each outcome | Never |
| `Promise.race` | With the first to settle | If the first to settle rejects |
| `Promise.any` | With the first to resolve | When all reject (`AggregateError`) |

`Promise.all(items.map(fetchOne))` on 10,000 items opens 10,000 connections at once. Limit
concurrency with a small worker pool or a library such as `p-limit` (you write one in the
next kata).

## Unhandled rejections

A promise that rejects with nobody listening is an **unhandled rejection**. Since Node.js 15
the default (`--unhandled-rejections=throw`) is to raise it as an uncaught exception, which
**ends the process** with an error. That is the right default: a lost error is worse than a
crash. The classic causes:

```ts
items.forEach(async (item) => { await save(item); });   // forEach ignores the promises
save(item);                                              // forgot await: a floating promise
```

Use `for ... of` with `await`, or `await Promise.all(items.map(save))`. The lint rule
`@typescript-eslint/no-floating-promises` finds the rest.

## Cancellation with AbortSignal

Promises cannot be cancelled, but the work behind them can, with an `AbortSignal`. Node.js
APIs accept one: `fetch`, `fs.readFile`, `timers/promises`, `events.once`, `child_process`,
streams.

```ts
const response = await fetch(url, { signal: AbortSignal.timeout(2_000) });  // fails after 2 s

const controller = new AbortController();
process.once("SIGINT", () => controller.abort());
await sleep(60_000, undefined, { signal: controller.signal });    // ends at Ctrl+C
```

An aborted operation rejects with an `AbortError` (`error.name === "AbortError"`; a timeout
gives a `TimeoutError`). Accept a `signal` in your own long-running functions too.

## Async iteration

Many Node.js objects are **async iterables**: streams, `fs.opendir`, `readline` interfaces,
`events.on(emitter, "data")`. `for await` reads them one item at a time, and an async
generator (`async function*`) produces them:

```ts
import { opendir } from "node:fs/promises";

async function* filesIn(folder: string) {
  for await (const entry of await opendir(folder)) {
    if (entry.isFile()) yield entry.name;
  }
}

for await (const name of filesIn(".")) {
  console.log(name);
}
```

## Summary

- Error-first callbacks: `(err, value)`; a `throw` inside a callback is not catchable by the
  caller.
- Use the promise APIs (`node:*/promises`, `util.promisify`) and `async`/`await` with
  `try`/`catch`.
- Sequential `await` for dependent steps, `Promise.all`/`allSettled` for independent ones,
  limited concurrency for large batches.
- An unhandled rejection ends the process: never leave a promise floating.
- Cancel with `AbortSignal`; consume streams of values with `for await`.

## Further reading

- Node.js, "Discover Promises in Node.js": <https://nodejs.org/en/learn/asynchronous-work/discover-promises-in-nodejs>
- Node.js, "JavaScript Asynchronous Programming and Callbacks": <https://nodejs.org/en/learn/asynchronous-work/javascript-asynchronous-programming-and-callbacks>
- Node.js API, `util.promisify()`: <https://nodejs.org/api/util.html#utilpromisifyoriginal>
- Node.js API, `--unhandled-rejections`: <https://nodejs.org/api/cli.html#--unhandled-rejectionsmode>
- Node.js API, "Timers Promises API": <https://nodejs.org/api/timers.html#timers-promises-api>
- MDN, "Using promises": <https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide/Using_promises>
- MDN, `for await...of`: <https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Statements/for-await...of>
