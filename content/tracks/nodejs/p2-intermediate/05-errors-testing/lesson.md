# Error Handling, Configuration and Testing with node:test

Production Node.js code fails in two very different ways, and handling them the same way is
the root of most "the server crashed" and "the server hung" incidents. This lesson sorts
errors out, makes configuration fail early, and tests all of it with the test runner built
into Node.js.

## Two kinds of errors

| | Operational errors | Programmer errors |
| --- | --- | --- |
| What | Expected failures of a correct program | Bugs |
| Examples | Timeout, `ECONNREFUSED`, `ENOENT`, invalid user input, 404 from a service | `undefined is not a function`, a wrong argument type, a broken invariant |
| Handle by | Retrying, answering 4xx/503, falling back, reporting to the user | **Crashing**, then fixing the code |

You cannot "handle" a bug at runtime: the program is already in a state you did not plan
for. Log it, let the process exit, and let the supervisor (systemd, Docker, Kubernetes)
start a fresh one.

### Errors with context

```ts
class NotFoundError extends Error {
  override name = "NotFoundError";
}

try {
  await db.query(sql);
} catch (error) {
  throw new Error(`loading order ${id} failed`, { cause: error });   // keep the original
}

if ((error as NodeJS.ErrnoException).code === "ECONNRESET") { /* retry */ }
```

Throw `Error` objects (they have a stack), add context with `cause`, check `code` for system
errors and `instanceof` for your own classes.

### The last-resort handlers

```ts
process.on("uncaughtException", (error) => {
  logger.fatal({ error }, "uncaught exception");
  process.exit(1);                         // log, then exit: never continue
});
process.on("unhandledRejection", (reason) => {
  logger.fatal({ reason }, "unhandled rejection");
  process.exit(1);
});
```

These are for **logging before exiting**, not for keeping a broken process alive. An empty
handler turns every crash into a silent, half-working server.

## Configuration

Read configuration from the environment once, at startup, validate it, and stop at once when
it is wrong:

```ts
import { z } from "zod";

const Config = z.object({
  PORT: z.coerce.number().int().min(1).max(65_535).default(3000),
  DATABASE_URL: z.url(),
  LOG_LEVEL: z.enum(["debug", "info", "warn", "error"]).default("info"),
});

const parsed = Config.safeParse(process.env);
if (!parsed.success) {
  console.error("invalid configuration:\n" + z.prettifyError(parsed.error));
  process.exit(1);                                   // fail fast, before listening
}
export const config = parsed.data;                  // typed: { PORT: number, ... }
```

A service that starts with a missing `DATABASE_URL` and fails on the first request is much
harder to diagnose than one that refuses to start. (Without zod, a small hand-written check
works the same way.)

## Testing with node:test

Node.js ships a test runner: no Jest or Mocha needed.

```ts
// tests/price.test.ts
import assert from "node:assert/strict";
import { describe, test, beforeEach, mock } from "node:test";
import { priceWithTax } from "../src/price.ts";

describe("priceWithTax", () => {
  test("adds 20% tax", () => {
    assert.equal(priceWithTax(100), 120);
  });

  test("rejects negative prices", () => {
    assert.throws(() => priceWithTax(-1), RangeError);
  });

  test("async code", async () => {
    await assert.rejects(loadPrice("missing"), { name: "NotFoundError" });
  });
});
```

```bash
node --test                                  # finds **/*.test.ts, **/test/** ... and runs them
node --test --watch                          # rerun on change
node --test --experimental-test-coverage     # line, branch and function coverage
node --test --test-name-pattern="tax"        # only matching tests
```

- Use **`node:assert/strict`**: `equal` is `===` and `deepEqual` compares structure strictly.
- Hooks: `before`, `after`, `beforeEach`, `afterEach` (per `describe`).
- `test.skip`, `test.todo`, `{ only: true }` with `--test-only`.
- Test files run in parallel, each in its own process.

### Mocks and fake timers

```ts
test("retries once after 30 seconds", async (t) => {
  t.mock.timers.enable({ apis: ["setTimeout"] });
  const send = t.mock.fn(async () => {
    if (send.mock.callCount() === 0) throw new TimeoutError("slow");   // first call fails
    return "sent";
  });
  const result = sendReminder(user, { send });
  await Promise.resolve();                    // let the first attempt fail
  t.mock.timers.tick(30_000);                 // no real waiting
  assert.equal(await result, "sent");
  assert.equal(send.mock.callCount(), 2);
});
```

`t.mock.fn()` records calls (`mock.calls[i].arguments`), `t.mock.method(object, "name")`
replaces a method, and both are restored when the test ends. **Inject dependencies** (pass the
email client, the clock, the logger) and most code becomes easy to test without mocking
modules.

### Testing an HTTP server

Start the real server on port 0, call it with `fetch`, close it afterwards:

```ts
let base = "";
before(async () => {
  server = createApp();
  await new Promise<void>((resolve) => server.listen(0, "127.0.0.1", resolve));
  base = `http://127.0.0.1:${(server.address() as AddressInfo).port}`;
});
after(() => server.close());

test("GET /health", async () => {
  const response = await fetch(`${base}/health`);
  assert.equal(response.status, 200);
});
```

This tests routing, headers and serialisation together, and runs in milliseconds. Libraries
such as supertest, or Fastify's `inject()`, do the same without a socket.

## Summary

- Operational errors: handle them. Programmer errors: crash and fix.
- Add context with `cause`; check `code` and `instanceof`.
- `uncaughtException` and `unhandledRejection` handlers log and exit; they never swallow.
- Validate configuration at startup and fail fast.
- `node:test` + `node:assert/strict`: hooks, `t.mock.fn`, `t.mock.timers`, coverage, watch;
  test servers on port 0 with `fetch`.

## Further reading

- Node.js API, "Errors": <https://nodejs.org/api/errors.html>
- Node.js API, "Test runner": <https://nodejs.org/api/test.html>
- Node.js API, "Mocking" in the test runner: <https://nodejs.org/api/test.html#mocking>
- Node.js API, "Assert": <https://nodejs.org/api/assert.html>
- Node.js API, "Event: uncaughtException" (warning about using it correctly): <https://nodejs.org/api/process.html#warning-using-uncaughtexception-correctly>
- Node.js, "Using Node.js's test runner": <https://nodejs.org/en/learn/test-runner/using-test-runner>
- Joyent (archived), "Error Handling in Node.js": <https://web.archive.org/web/20220126012729/https://www.joyent.com/node-js/production/design/errors>
- zod documentation: <https://zod.dev/>
