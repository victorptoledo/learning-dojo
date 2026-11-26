import assert from "node:assert/strict";
import { describe, test } from "node:test";
import { Readable } from "node:stream";
import { mapConcurrent } from "../concurrent.ts";
import { firstError, lines, parseEntries, report, windows, type LineError, type LogEntry } from "../logs.ts";

const wait = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

/** An async source that counts pulls and records whether it was closed. */
function tracked<T>(items: readonly T[], delayMs = 0) {
  const state = { pulled: 0, closed: false };
  async function* source(): AsyncGenerator<T, void, undefined> {
    try {
      for (const item of items) {
        if (delayMs > 0) await wait(delayMs);
        state.pulled += 1;
        yield item;
      }
    } finally {
      state.closed = true;
    }
  }
  return { source: source(), state };
}

const entry = (iso: string, level: LogEntry["level"]): LogEntry => ({ at: new Date(iso), level, message: "m" });

describe("lines", () => {
  test("joins lines split across chunks and drops \\r\\n", async () => {
    const chunks = ["first li", "ne\r\nsecond\n", "\nthird"];
    assert.deepEqual(await Array.fromAsync(lines(chunks)), ["first line", "second", "", "third"]);
  });

  test("does not add an empty line after a final newline", async () => {
    assert.deepEqual(await Array.fromAsync(lines(["a\nb\n"])), ["a", "b"]);
    assert.deepEqual(await Array.fromAsync(lines([])), []);
  });

  test("decodes UTF-8 bytes even when a character is split between chunks", async () => {
    const bytes = new TextEncoder().encode("café\nnaïve\n");
    const cut = 4; // inside the two bytes of "é"
    const chunks = [bytes.slice(0, cut), bytes.slice(cut)];
    assert.deepEqual(await Array.fromAsync(lines(chunks)), ["café", "naïve"]);
  });

  test("reads a Node.js stream", async () => {
    const stream = Readable.from([Buffer.from("x\ny"), Buffer.from("z\n")]);
    assert.deepEqual(await Array.fromAsync(lines(stream)), ["x", "yz"]);
  });
});

describe("parseEntries", () => {
  test("parses entries and reports bad lines with their numbers", async () => {
    const errors: LineError[] = [];
    const text = [
      "2026-11-26T10:00:00Z info service started",
      "",
      "2026-11-26T10:00:01.250Z warn disk at 91%",
      "not a log line",
      "2026-11-26T10:00:02Z fatal oops",
      "2026-13-45T99:00:00Z info bad date",
      "2026-11-26T10:00:03Z error  boom ",
    ];
    const entries = await Array.fromAsync(parseEntries(text, (error) => errors.push(error)));
    assert.deepEqual(
      entries.map((e) => [e.at.toISOString(), e.level, e.message]),
      [
        ["2026-11-26T10:00:00.000Z", "info", "service started"],
        ["2026-11-26T10:00:01.250Z", "warn", "disk at 91%"],
        ["2026-11-26T10:00:03.000Z", "error", " boom "],
      ],
    );
    assert.deepEqual(
      errors.map((error) => error.line),
      [4, 5, 6],
    );
    assert.equal(errors[0]?.text, "not a log line");
  });
});

describe("windows", () => {
  test("counts levels in aligned windows and skips empty ones", async () => {
    const result = await Array.fromAsync(
      windows(
        [
          entry("2026-11-26T10:00:05Z", "info"),
          entry("2026-11-26T10:00:59Z", "error"),
          entry("2026-11-26T10:01:00Z", "info"),
          entry("2026-11-26T10:05:30Z", "warn"),
        ],
        60_000,
      ),
    );
    assert.deepEqual(
      result.map((w) => [w.start.toISOString(), w.counts]),
      [
        ["2026-11-26T10:00:00.000Z", { debug: 0, info: 1, warn: 0, error: 1 }],
        ["2026-11-26T10:01:00.000Z", { debug: 0, info: 1, warn: 0, error: 0 }],
        ["2026-11-26T10:05:00.000Z", { debug: 0, info: 0, warn: 1, error: 0 }],
      ],
    );
  });

  test("yields a window as soon as the next one starts", async () => {
    const { source, state } = tracked([
      entry("2026-11-26T10:00:05Z", "info"),
      entry("2026-11-26T10:01:05Z", "info"),
      entry("2026-11-26T10:01:06Z", "info"),
    ]);
    const first = await windows(source, 60_000).next();
    assert.equal(first.done, false);
    assert.equal(state.pulled, 2, "the first window is complete when the second entry arrives");
  });

  test("rejects entries out of order and a bad size", async () => {
    await assert.rejects(
      Array.fromAsync(windows([entry("2026-11-26T10:05:00Z", "info"), entry("2026-11-26T10:00:00Z", "info")], 60_000)),
      RangeError,
    );
    await assert.rejects(Array.fromAsync(windows([], 0)), RangeError);
  });
});

describe("report and firstError", () => {
  test("report puts the stages together", async () => {
    const chunks = ["2026-11-26T10:00:01Z info a\n2026-11-26T10:00:0", "2Z error b\noops\n2026-11-26T10:00:11Z debug c"];
    const result = await report(chunks, 10_000);
    assert.deepEqual(
      result.windows.map((w) => [w.start.toISOString(), w.counts]),
      [
        ["2026-11-26T10:00:00.000Z", { debug: 0, info: 1, warn: 0, error: 1 }],
        ["2026-11-26T10:00:10.000Z", { debug: 1, info: 0, warn: 0, error: 0 }],
      ],
    );
    assert.deepEqual(result.errors, [{ line: 3, text: "oops" }]);
  });

  test("firstError stops reading the source and closes it", async () => {
    const { source, state } = tracked([
      "2026-11-26T10:00:00Z info a\n",
      "2026-11-26T10:00:01Z error found it\n",
      "2026-11-26T10:00:02Z info b\n",
      "2026-11-26T10:00:03Z error later\n",
    ]);
    const found = await firstError(source);
    assert.equal(found?.message, "found it");
    assert.equal(state.pulled, 2);
    assert.equal(state.closed, true);
    assert.equal(await firstError(["2026-11-26T10:00:00Z info a"]), undefined);
  });
});

describe("mapConcurrent", () => {
  test("keeps source order and never runs more than concurrency calls", async () => {
    let running = 0;
    let most = 0;
    const delays = [40, 5, 25, 1, 15, 30];
    const results = await Array.fromAsync(
      mapConcurrent(
        delays,
        async (ms) => {
          running += 1;
          most = Math.max(most, running);
          await wait(ms);
          running -= 1;
          return ms * 2;
        },
        { concurrency: 3 },
      ),
    );
    assert.deepEqual(results, [80, 10, 50, 2, 30, 60]);
    assert.equal(most, 3);
  });

  test("runs calls at the same time", async () => {
    const started = Date.now();
    await Array.fromAsync(mapConcurrent([1, 2, 3, 4], () => wait(60), { concurrency: 4 }));
    assert.ok(Date.now() - started < 200, "four 60 ms calls at once should take about 60 ms");
  });

  test("pulls from the source only as fast as results are consumed", async () => {
    const { source, state } = tracked([1, 2, 3, 4, 5, 6, 7, 8, 9, 10]);
    const results = mapConcurrent(source, async (n) => n, { concurrency: 2 });
    await results.next();
    await wait(20);
    assert.ok(state.pulled <= 3, `pulled ${state.pulled} items for one result with concurrency 2`);
    await results.return(undefined);
  });

  test("an early stop aborts the calls in flight and closes the source", async () => {
    const { source, state } = tracked([1, 2, 3, 4, 5]);
    const aborted: number[] = [];
    const results = mapConcurrent(
      source,
      (n, signal) =>
        new Promise<number>((resolve, reject) => {
          if (n === 1) {
            resolve(n);
            return;
          }
          signal.addEventListener("abort", () => {
            aborted.push(n);
            reject(signal.reason);
          });
        }),
      { concurrency: 3 },
    );
    for await (const value of results) {
      assert.equal(value, 1);
      break;
    }
    assert.deepEqual(aborted.sort(), [2, 3]);
    assert.equal(state.closed, true);
  });

  test("a failing call fails the whole run and aborts the others", async () => {
    const aborted: number[] = [];
    const run = Array.fromAsync(
      mapConcurrent(
        [1, 2, 3],
        (n, signal) =>
          new Promise<number>((resolve, reject) => {
            if (n === 1) {
              setTimeout(() => reject(new Error("call 1 failed")), 10);
              return;
            }
            signal.addEventListener("abort", () => {
              aborted.push(n);
              reject(signal.reason);
            });
          }),
        { concurrency: 3 },
      ),
    );
    await assert.rejects(run, /call 1 failed/);
    assert.deepEqual(aborted.sort(), [2, 3]);
  });

  test("a later failure does not become an unhandled rejection", async () => {
    let unhandled = 0;
    const count = () => {
      unhandled += 1;
    };
    process.on("unhandledRejection", count);
    try {
      const run = Array.fromAsync(
        mapConcurrent(
          [1, 2],
          async (n) => {
            if (n === 2) throw new Error("fast failure");
            await wait(30);
            return n;
          },
          { concurrency: 2 },
        ),
      );
      await assert.rejects(run, /fast failure/);
      await wait(20);
      assert.equal(unhandled, 0);
    } finally {
      process.off("unhandledRejection", count);
    }
  });

  test("an aborted signal stops the run with its reason", async () => {
    const controller = new AbortController();
    const reason = new Error("shutting down");
    const results: number[] = [];
    await assert.rejects(
      (async () => {
        for await (const value of mapConcurrent(
          [1, 2, 3, 4],
          (n, signal) =>
            new Promise<number>((resolve, reject) => {
              const timer = setTimeout(() => resolve(n), 10);
              signal.addEventListener("abort", () => {
                clearTimeout(timer);
                reject(signal.reason);
              });
            }),
          { concurrency: 1, signal: controller.signal },
        )) {
          results.push(value);
          if (value === 2) controller.abort(reason);
        }
      })(),
      (error) => error === reason,
    );
    assert.deepEqual(results, [1, 2]);
    await assert.rejects(
      Array.fromAsync(mapConcurrent([1], async (n) => n, { concurrency: 1, signal: AbortSignal.abort(reason) })),
      (error) => error === reason,
    );
  });

  test("rejects a bad concurrency", async () => {
    await assert.rejects(Array.fromAsync(mapConcurrent([1], async (n) => n, { concurrency: 0 })), RangeError);
  });
});
