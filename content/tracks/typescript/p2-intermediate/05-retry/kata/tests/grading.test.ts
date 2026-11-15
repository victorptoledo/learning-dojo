import assert from "node:assert/strict";
import { describe, test } from "node:test";
import { mapLimit, retry, RetryError, sleep, TimeoutError, toError, withTimeout } from "../retry.ts";

describe("toError", () => {
  test("keeps errors and wraps other values", () => {
    const original = new TypeError("bad");
    assert.equal(toError(original), original);
    assert.equal(toError("boom").message, "boom");
    assert.ok(toError("boom") instanceof Error);
    assert.equal(toError(42).message, "42");
    assert.equal(toError({ code: 7 }).message, '{"code":7}');
    assert.equal(toError(undefined).message, "undefined");
  });
});

describe("sleep", () => {
  test("waits about ms", async () => {
    const started = performance.now();
    await sleep(30);
    assert.ok(performance.now() - started >= 25);
  });

  test("rejects with the reason when aborted during the wait", async () => {
    const controller = new AbortController();
    const waiting = sleep(10_000, controller.signal);
    controller.abort(new Error("stop"));
    await assert.rejects(waiting, { message: "stop" });
  });

  test("rejects at once when the signal is already aborted", async () => {
    await assert.rejects(sleep(10_000, AbortSignal.abort(new Error("early"))), { message: "early" });
  });
});

describe("withTimeout", () => {
  test("resolves with the task's value", async () => {
    assert.equal(await withTimeout(async () => 7, 1000), 7);
  });

  test("rejects with a TimeoutError and aborts the signal", async () => {
    let seen: AbortSignal | undefined;
    await assert.rejects(
      withTimeout((signal) => {
        seen = signal;
        return sleep(10_000, signal);
      }, 20),
      (error: unknown) => {
        assert.ok(error instanceof TimeoutError);
        assert.equal(error.message, "timed out after 20 ms");
        assert.equal(error.name, "TimeoutError");
        return true;
      },
    );
    assert.equal(seen?.aborted, true);
  });

  test("passes the task's own error through", async () => {
    await assert.rejects(
      withTimeout(async () => {
        throw new RangeError("inner");
      }, 1000),
      RangeError,
    );
  });
});

describe("retry", () => {
  const failing = (message: string) => async (): Promise<never> => {
    throw new Error(message);
  };

  test("throws a RetryError with every error after the last attempt", async () => {
    let attempt = 0;
    await assert.rejects(
      retry(
        async () => {
          attempt += 1;
          throw new Error(`fail ${attempt}`);
        },
        { attempts: 3, delayMs: 1 },
      ),
      (error: unknown) => {
        assert.ok(error instanceof RetryError);
        assert.equal(error.attempts, 3);
        assert.deepEqual(
          error.errors.map((each) => toError(each).message),
          ["fail 1", "fail 2", "fail 3"],
        );
        assert.equal(toError(error.cause).message, "fail 3");
        return true;
      },
    );
  });

  test("waits with exponential backoff between attempts", async () => {
    const waits: number[] = [];
    await assert.rejects(
      retry(failing("x"), {
        attempts: 4,
        delayMs: 100,
        wait: async (ms) => {
          waits.push(ms);
        },
      }),
      RetryError,
    );
    assert.deepEqual(waits, [100, 200, 400]);
  });

  test("uses the given factor", async () => {
    const waits: number[] = [];
    await assert.rejects(
      retry(failing("x"), {
        attempts: 3,
        delayMs: 10,
        factor: 3,
        wait: async (ms) => {
          waits.push(ms);
        },
      }),
      RetryError,
    );
    assert.deepEqual(waits, [10, 30]);
  });

  test("stops at once when shouldRetry says no", async () => {
    const fatal = new TypeError("fatal");
    let calls = 0;
    await assert.rejects(
      retry(
        async () => {
          calls += 1;
          throw fatal;
        },
        { attempts: 5, delayMs: 1, shouldRetry: (error) => !(error instanceof TypeError) },
      ),
      (error: unknown) => error === fatal,
    );
    assert.equal(calls, 1);
  });

  test("one attempt means no wait", async () => {
    const waits: number[] = [];
    await assert.rejects(
      retry(failing("once"), {
        attempts: 1,
        delayMs: 50,
        wait: async (ms) => {
          waits.push(ms);
        },
      }),
      RetryError,
    );
    assert.deepEqual(waits, []);
  });
});

describe("mapLimit", () => {
  test("keeps the order of the items", async () => {
    const delays = [30, 5, 20, 1];
    const results = await mapLimit(delays, 2, async (ms, index) => {
      await sleep(ms);
      return `${index}:${ms}`;
    });
    assert.deepEqual(results, ["0:30", "1:5", "2:20", "3:1"]);
  });

  test("never runs more than limit at a time", async () => {
    let running = 0;
    let peak = 0;
    await mapLimit([...Array(10).keys()], 3, async () => {
      running += 1;
      peak = Math.max(peak, running);
      await sleep(5);
      running -= 1;
    });
    assert.equal(peak, 3);
  });

  test("rejects with the first error and starts no new calls", async () => {
    const started: number[] = [];
    await assert.rejects(
      mapLimit([1, 2, 3, 4, 5, 6], 2, async (n) => {
        started.push(n);
        await sleep(5);
        if (n === 2) {
          throw new Error("two");
        }
        return n;
      }),
      { message: "two" },
    );
    await sleep(30);
    assert.ok(started.length <= 3, `started ${started.join(",")}`);
  });

  test("handles no items", async () => {
    assert.deepEqual(await mapLimit([], 4, async () => 1), []);
  });
});
