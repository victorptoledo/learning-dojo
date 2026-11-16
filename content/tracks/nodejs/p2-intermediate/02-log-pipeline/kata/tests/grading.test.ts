import assert from "node:assert/strict";
import { createReadStream, createWriteStream } from "node:fs";
import { mkdtemp, readFile, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import path from "node:path";
import { Readable, Writable } from "node:stream";
import { describe, test } from "node:test";
import { setTimeout as sleep } from "node:timers/promises";
import { countByLevel, filterLevel, filterLog, parseEntries, splitLines, toNdjson, type LogEntry } from "../logs.ts";

const entry = (level: string, msg: string, extra: object = {}) =>
  JSON.stringify({ time: "2026-01-01T00:00:00Z", level, msg, ...extra });

async function collect<T>(stream: AsyncIterable<T>): Promise<T[]> {
  const out: T[] = [];
  for await (const item of stream) {
    out.push(item);
  }
  return out;
}

/** Feeds the chunks into the transform and collects what comes out. */
async function run<T>(transform: NodeJS.ReadWriteStream, chunks: unknown[]): Promise<T[]> {
  return collect<T>(Readable.from(chunks, { objectMode: true }).pipe(transform) as AsyncIterable<T>);
}

describe("splitLines", () => {
  test("splits lines across chunks and keeps a last line without newline", async () => {
    const lines = await run<string>(splitLines(), [Buffer.from("one\ntw"), Buffer.from("o\nthree\n\nfour")]);
    assert.deepEqual(lines, ["one", "two", "three", "", "four"]);
  });

  test("removes \\r from Windows line ends", async () => {
    assert.deepEqual(await run<string>(splitLines(), [Buffer.from("a\r\nb\r"), Buffer.from("\nc\r\n")]), ["a", "b", "c"]);
  });

  test("decodes a UTF-8 character split across two chunks", async () => {
    const bytes = Buffer.from("café\nolá\n");
    const lines = await run<string>(splitLines(), [bytes.subarray(0, 4), bytes.subarray(4, 9), bytes.subarray(9)]);
    assert.deepEqual(lines, ["café", "olá"]);
  });
});

describe("parseEntries", () => {
  test("turns valid lines into entries, keeps only the three fields and reports the rest", async () => {
    const invalid: [number, string][] = [];
    const lines = [
      entry("info", "started", { pid: 7 }),
      "",
      "not json",
      entry("fatal", "unknown level"),
      JSON.stringify({ level: "warn", msg: "no time" }),
      "42",
      entry("error", "boom"),
    ];
    const entries = await run<LogEntry>(parseEntries((n, line) => invalid.push([n, line])), lines);
    assert.deepEqual(entries, [
      { time: "2026-01-01T00:00:00Z", level: "info", msg: "started" },
      { time: "2026-01-01T00:00:00Z", level: "error", msg: "boom" },
    ]);
    assert.deepEqual(
      invalid.map(([n]) => n),
      [3, 4, 5, 6],
    );
    assert.equal(invalid[0]?.[1], "not json");
  });
});

describe("filterLevel and toNdjson", () => {
  test("keeps the level and the ones above it", async () => {
    const all: LogEntry[] = (["debug", "info", "warn", "error"] as const).map((level) => ({ time: "t", level, msg: level }));
    const kept = await run<LogEntry>(filterLevel("warn"), all);
    assert.deepEqual(
      kept.map((each) => each.level),
      ["warn", "error"],
    );
  });

  test("writes one JSON line per entry", async () => {
    const out = await run<string | Buffer>(toNdjson(), [{ time: "t", level: "info", msg: "hi" }]);
    assert.equal(out.map(String).join(""), '{"time":"t","level":"info","msg":"hi"}\n');
  });
});

describe("filterLog", () => {
  test("filters a file into another file", async () => {
    const dir = await mkdtemp(path.join(tmpdir(), "logs-"));
    const source = path.join(dir, "app.log");
    const target = path.join(dir, "errors.log");
    await writeFile(source, [entry("info", "a"), "garbage", entry("error", "b"), entry("warn", "c"), entry("debug", "d")].join("\n"));
    const result = await filterLog(createReadStream(source, { highWaterMark: 7 }), createWriteStream(target), "warn");
    assert.deepEqual(result, { kept: 2, invalid: 1 });
    assert.equal(
      await readFile(target, "utf8"),
      `${JSON.stringify({ time: "2026-01-01T00:00:00Z", level: "error", msg: "b" })}\n${JSON.stringify({ time: "2026-01-01T00:00:00Z", level: "warn", msg: "c" })}\n`,
    );
    await rm(dir, { recursive: true, force: true });
  });

  test("respects backpressure: a slow writer slows the reader down", async () => {
    const total = 20_000;
    let produced = 0;
    const input = new Readable({
      read() {
        produced += 1;
        this.push(produced <= total ? `${entry("error", `line ${produced}`)}\n` : null);
      },
    });
    let writes = 0;
    let producedAtWrite20 = 0;
    const output = new Writable({
      highWaterMark: 64,
      write(_chunk, _encoding, callback) {
        writes += 1;
        if (writes === 20) {
          producedAtWrite20 = produced;
        }
        if (writes <= 20) {
          setTimeout(callback, 2); // slow at first
        } else {
          callback();
        }
      },
    });
    const result = await filterLog(input, output, "info");
    assert.equal(result.kept, total);
    assert.ok(producedAtWrite20 < total / 4, `the reader ran ahead: ${producedAtWrite20} lines read after 20 writes`);
  });

  test("rejects with the input's error", async () => {
    const input = new Readable({
      read() {
        this.destroy(new Error("disk failure"));
      },
    });
    const output = new Writable({
      write(_chunk, _encoding, callback) {
        callback();
      },
    });
    await assert.rejects(filterLog(input, output, "info"), { message: "disk failure" });
    await sleep(1);
    assert.equal(output.destroyed, true);
  });
});

describe("countByLevel", () => {
  test("counts every level, zero included", async () => {
    async function* entries(): AsyncGenerator<LogEntry> {
      yield { time: "t", level: "info", msg: "a" };
      yield { time: "t", level: "error", msg: "b" };
      yield { time: "t", level: "info", msg: "c" };
    }
    assert.deepEqual(await countByLevel(entries()), { debug: 0, info: 2, warn: 0, error: 1 });
  });
});
