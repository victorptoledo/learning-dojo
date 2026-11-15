# Promises, async/await and Error Handling

JavaScript runs your code on **one thread** with an event loop: while a request waits for the
network, other callbacks run. Promises represent those future results, and `async`/`await`
lets you write them as straight-line code. TypeScript types both the values and, as far as it
can, the errors. This lesson covers both, plus what TypeScript cannot know about errors.

## Promises and async functions

```ts
async function loadUser(id: string): Promise<User> {
  const response = await fetch(`https://api.example.com/users/${id}`);
  if (!response.ok) {
    throw new Error(`HTTP ${response.status} for user ${id}`);
  }
  return (await response.json()) as User;  // json() is Promise<any>: validate it (phase 3)
}
```

- An `async` function **always** returns a promise: `Promise<User>` here, even though the body
  returns a `User`. Throwing inside it rejects the promise.
- `await` unwraps a promise; its type is `Awaited<T>`.
- The type parameter of `Promise<T>` describes success only. **There is no error type**: a
  promise can reject with anything.

### Sequential vs concurrent

```ts
const a = await loadUser("a");      // waits
const b = await loadUser("b");      // then waits again: twice as slow

const [c, d] = await Promise.all([loadUser("c"), loadUser("d")]);  // both at once
```

| Combinator | Resolves when | Rejects when | Result type |
| --- | --- | --- | --- |
| `Promise.all` | All resolve | The first rejects (the others keep running!) | A tuple or array of values |
| `Promise.allSettled` | All settle | Never | `{ status: "fulfilled"; value } \| { status: "rejected"; reason }` |
| `Promise.race` | The first settles | The first settles with a rejection | The first value |
| `Promise.any` | The first resolves | All reject (`AggregateError`) | The first value |

`Promise.all` on a thousand items starts a thousand requests at once. Limit concurrency with a
small pool (you will write one in the kata).

### Floating promises

Forgetting `await` is the classic async bug:

```ts
async function save(user: User) {
  db.insert(user);       // not awaited: errors become unhandled rejections, order is lost
}
```

TypeScript does not report this (a `Promise` is a valid expression statement). The lint rule
`@typescript-eslint/no-floating-promises` does; use it. Node.js terminates the process on an
unhandled rejection by default, which is the right behaviour, but you want the bug found
earlier.

## Cancellation with AbortSignal

Promises cannot be cancelled; the standard way to stop work is an `AbortSignal`, accepted by
`fetch`, timers in `node:timers/promises`, streams and many libraries:

```ts
const controller = new AbortController();
const timer = setTimeout(() => controller.abort(new Error("too slow")), 5_000);
try {
  const response = await fetch(url, { signal: controller.signal });
} finally {
  clearTimeout(timer);
}

await fetch(url, { signal: AbortSignal.timeout(5_000) });  // the short form
```

Write your own long-running functions to accept a `signal` and check `signal.aborted` or
listen to its `"abort"` event. The value given to `abort(reason)` is available as
`signal.reason`.

## Errors

### catch gives you unknown

JavaScript can `throw` anything: an `Error`, a string, `undefined`. With `strict`
(`useUnknownInCatchVariables`) a catch variable is `unknown`, so you must narrow it:

```ts
try {
  await loadUser(id);
} catch (error) {
  if (error instanceof HttpError && error.status === 404) {
    return null;
  }
  const message = error instanceof Error ? error.message : String(error);
  logger.error({ message }, "loading the user failed");
  throw error;   // rethrow what you cannot handle
}
```

Rules that keep error handling sane:

- Throw only `Error` objects (or subclasses), never strings: they carry a stack trace.
- Catch only what you can handle; rethrow the rest. Never swallow errors silently.
- Add context with `cause` instead of losing the original:
  `throw new Error("cannot load the invoice", { cause: error })`.
- Clean up in `finally`, or use `using` / `await using` declarations (TypeScript 5.2,
  ECMAScript explicit resource management) for objects with `[Symbol.dispose]` or
  `[Symbol.asyncDispose]`.

### Custom error classes

```ts
export class HttpError extends Error {
  override name = "HttpError";
  readonly status: number;

  constructor(status: number, message: string, options?: ErrorOptions) {
    super(message, options);
    this.status = status;
  }
}
```

`instanceof` works on them, and a `name` makes logs readable. `AggregateError` carries several
errors at once (used by `Promise.any`).

### Errors as values

Exceptions are invisible in signatures: nothing in `loadUser(id): Promise<User>` says it may
fail with a 404. For **expected** failures, such as validation, "not found" or a business rule,
return them as values with a discriminated union:

```ts
type Result<T, E = string> = { ok: true; value: T } | { ok: false; error: E };

function parsePort(text: string): Result<number> {
  const port = Number(text);
  return Number.isInteger(port) && port > 0 && port < 65_536
    ? { ok: true, value: port }
    : { ok: false, error: `invalid port: ${text}` };
}

const result = parsePort(input);
if (!result.ok) {
  return badRequest(result.error);   // the compiler forces this check before .value
}
listen(result.value);
```

A sensible split: `Result` for failures the caller is expected to handle; exceptions for bugs
and for infrastructure failures (network, disk) that bubble up to a top-level handler.
Libraries such as neverthrow or Effect build on this idea; a five-line `Result` type is often
enough.

## Summary

- `async` functions return promises; `Promise<T>` types only success; `Awaited<T>` unwraps.
- Run independent work concurrently with `Promise.all`, collect outcomes with `allSettled`,
  and limit concurrency for large batches. Lint for floating promises.
- Cancel with `AbortSignal`; clean up in `finally` or with `using`.
- `catch` variables are `unknown`: narrow, add `cause`, rethrow what you cannot handle.
- Custom error classes for exceptions; `Result` unions for expected failures.

## Further reading

- MDN, "Using promises": <https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide/Using_promises>
- MDN, `AbortSignal`: <https://developer.mozilla.org/en-US/docs/Web/API/AbortSignal>
- MDN, `Error.prototype.cause`: <https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Error/cause>
- TypeScript 5.2 release notes, `using` declarations: <https://www.typescriptlang.org/docs/handbook/release-notes/typescript-5-2.html>
- TSConfig reference, `useUnknownInCatchVariables`: <https://www.typescriptlang.org/tsconfig/#useUnknownInCatchVariables>
- Node.js, "The Node.js Event Loop": <https://nodejs.org/en/learn/asynchronous-work/event-loop-timers-and-nexttick>
- roadmap.sh, TypeScript topic "unknown": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/typescript/content/unknown@O2TYbd9i_OQwQk2dKh-ly.md>
