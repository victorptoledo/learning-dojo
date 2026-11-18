// Helpers for the contract tests: sinks the test controls, a service on a real port, and raw
// node:http requests that send the body in the chunks the test chooses.
import { request } from "node:http";
import type { AddressInfo } from "node:net";
import { after } from "node:test";
import { setTimeout as sleep } from "node:timers/promises";
import { createService, type ServiceOptions, type Sink } from "../service.ts";

export interface TestSink extends Sink {
  /** Every batch written so far, in order. */
  batches: string[][];
  /** Calls to write(), finished or not. */
  calls: number;
  /** The most calls that were running at the same time. */
  maxConcurrent: number;
  /** While closed, write() waits; open() lets the waiting writes finish. */
  close(): void;
  open(): void;
  /** The next `count` writes reject. */
  failNext(count: number): void;
}

export function testSink(): TestSink {
  let gate: Promise<void> = Promise.resolve();
  let release: () => void = () => {};
  let running = 0;
  let failures = 0;
  const sink: TestSink = {
    batches: [],
    calls: 0,
    maxConcurrent: 0,
    async write(lines) {
      sink.calls++;
      running++;
      sink.maxConcurrent = Math.max(sink.maxConcurrent, running);
      try {
        await gate;
        await sleep(1);
        if (failures > 0) {
          failures--;
          throw new Error("disk full");
        }
        sink.batches.push([...lines]);
      } finally {
        running--;
      }
    },
    close() {
      gate = new Promise<void>((resolve) => (release = resolve));
    },
    open() {
      release();
      gate = Promise.resolve();
    },
    failNext(count) {
      failures = count;
    },
  };
  return sink;
}

const services: { server: import("node:http").Server }[] = [];
after(() => {
  for (const { server } of services) {
    server.closeAllConnections();
    server.close();
  }
});

export async function startService(options: Partial<ServiceOptions> = {}) {
  const sink = (options.sink as TestSink | undefined) ?? testSink();
  const service = createService({ ...options, sink });
  services.push(service);
  await new Promise<void>((resolve) => service.server.listen(0, "127.0.0.1", resolve));
  const port = (service.server.address() as AddressInfo).port;
  return { service, sink, port, base: `http://127.0.0.1:${port}` };
}

export interface Reply {
  status: number;
  headers: import("node:http").IncomingHttpHeaders;
  text: string;
  json: any;
}

/** POST /events with the body sent as these chunks, `gapMs` apart. */
export function postChunks(
  port: number,
  chunks: (string | Buffer)[],
  { gapMs = 0, contentType = "application/x-ndjson", path = "/events" } = {},
): Promise<Reply> {
  return new Promise((resolve, reject) => {
    const req = request({ host: "127.0.0.1", port, path, method: "POST", agent: false, headers: { "content-type": contentType } }, (res) => {
      let text = "";
      res.setEncoding("utf8");
      res.on("data", (chunk: string) => (text += chunk));
      res.on("end", () => {
        let json: unknown;
        try {
          json = JSON.parse(text);
        } catch {
          json = undefined;
        }
        resolve({ status: res.statusCode ?? 0, headers: res.headers, text, json });
      });
    });
    req.on("error", reject);
    void (async () => {
      for (const chunk of chunks) {
        req.write(chunk);
        if (gapMs > 0) await sleep(gapMs);
      }
      req.end();
    })();
  });
}

export const post = (port: number, body: string, options?: { contentType?: string; path?: string }) =>
  postChunks(port, [body], options);

/** An event line (without the newline). */
export function event(type: string, extra: Record<string, unknown> = {}): string {
  return JSON.stringify({ type, time: "2026-03-01T12:00:00.000Z", ...extra });
}

/** n event lines, each ending with a newline: "e0", "e1"... */
export function events(n: number, prefix = "e"): string {
  let out = "";
  for (let i = 0; i < n; i++) out += `${event(`${prefix}${i}`)}\n`;
  return out;
}

/** The event types written to the sink, in order. */
export function writtenTypes(sink: TestSink): string[] {
  return sink.batches.flat().map((line) => (JSON.parse(line) as { type: string }).type);
}

/** Waits until check() is true, or fails after ms. */
export async function until(check: () => boolean, ms: number, what: string): Promise<void> {
  const deadline = Date.now() + ms;
  while (!check()) {
    if (Date.now() > deadline) throw new Error(`timed out waiting for ${what}`);
    await sleep(10);
  }
}
