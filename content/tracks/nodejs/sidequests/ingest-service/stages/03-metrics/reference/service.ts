// An event ingestion service: NDJSON streamed in over HTTP, validated line by line, grouped in
// batches and handed to a sink one batch at a time, with backpressure and metrics.
import { mkdir, readdir, rename, writeFile } from "node:fs/promises";
import { createServer, type IncomingMessage, type Server, type ServerResponse } from "node:http";
import path from "node:path";
import { monitorEventLoopDelay } from "node:perf_hooks";

/** Where full batches go: one call per batch, never two at once. */
export interface Sink {
  write(lines: readonly string[]): Promise<void>;
}

export interface ServiceOptions {
  sink: Sink;
  batchSize?: number;
  flushMs?: number;
  maxLineBytes?: number;
  maxPending?: number;
}

export interface Stats {
  /** Accepted events not written yet: the open batch plus the pending ones. */
  bufferedEvents: number;
  /** Batches handed to the sink and not written yet (the one being written included). */
  pendingBatches: number;
  batchesWritten: number;
}

export interface Service {
  server: Server;
  flush(): Promise<void>;
  stats(): Stats;
}

const BATCH_NAME = /^batch-(\d{6})\.ndjson$/;

/** Writes each batch to dir/batch-NNNNNN.ndjson, numbered after the files already there. */
export function fileSink(dir: string): Sink {
  let next: Promise<number> | undefined;
  const firstNumber = async () => {
    await mkdir(dir, { recursive: true });
    const numbers = (await readdir(dir)).map((name) => BATCH_NAME.exec(name)?.[1]).filter((n) => n !== undefined).map(Number);
    return Math.max(0, ...numbers) + 1;
  };
  return {
    async write(lines) {
      next = (next ?? firstNumber()).then((n) => n + 1);
      const number = (await next) - 1;
      const name = `batch-${String(number).padStart(6, "0")}.ndjson`;
      const temp = path.join(dir, `.${name}.tmp`);
      await writeFile(temp, lines.map((line) => `${line}\n`).join(""));
      await rename(temp, path.join(dir, name));
    },
  };
}

interface LineError {
  line: number;
  message: string;
}

/** Why a line is not a valid event, or the event's normalised JSON text. */
function parseEvent(text: string): { ok: true; json: string } | { ok: false; message: string } {
  let value: unknown;
  try {
    value = JSON.parse(text);
  } catch {
    return { ok: false, message: "not valid JSON" };
  }
  if (typeof value !== "object" || value === null || Array.isArray(value)) {
    return { ok: false, message: "not a JSON object" };
  }
  const event = value as Record<string, unknown>;
  if (typeof event.type !== "string" || event.type.length < 1 || event.type.length > 100) {
    return { ok: false, message: "type must be a string of 1 to 100 characters" };
  }
  if (typeof event.time !== "string" || Number.isNaN(Date.parse(event.time))) {
    return { ok: false, message: "time must be a date and time" };
  }
  return { ok: true, json: JSON.stringify(event) };
}

const BUCKETS = [0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1, 2.5, 5, 10];

class Metrics {
  events = { accepted: 0, rejected: 0 };
  requests = new Map<string, number>();
  buckets = BUCKETS.map(() => 0);
  durationSum = 0;
  durationCount = 0;
  batchesWritten = 0;

  request(route: string, status: number): void {
    const key = `route="${route}",status="${status}"`;
    this.requests.set(key, (this.requests.get(key) ?? 0) + 1);
  }

  duration(seconds: number): void {
    BUCKETS.forEach((bound, index) => {
      if (seconds <= bound) this.buckets[index] = (this.buckets[index] ?? 0) + 1;
    });
    this.durationSum += seconds;
    this.durationCount++;
  }
}

export function createService(options: ServiceOptions): Service {
  const sink = options.sink;
  const batchSize = options.batchSize ?? 100;
  const flushMs = options.flushMs ?? 1000;
  const maxLineBytes = options.maxLineBytes ?? 65536;
  const maxPending = options.maxPending ?? 4;

  const metrics = new Metrics();
  const loopDelay = monitorEventLoopDelay({ resolution: 10 });
  loopDelay.enable();

  let batch: string[] = [];
  let flushTimer: NodeJS.Timeout | undefined;
  let pending = 0;
  let pendingEvents = 0;
  let writing: Promise<void> = Promise.resolve();
  let slotWaiters: (() => void)[] = [];

  const releaseSlot = () => {
    const waiters = slotWaiters;
    slotWaiters = [];
    for (const wake of waiters) wake();
  };

  async function write(lines: string[]): Promise<void> {
    await sink.write(lines);
    metrics.batchesWritten++;
  }

  /** Hands a batch to the sink, waiting first while maxPending batches are pending. */
  async function handOff(lines: string[]): Promise<void> {
    while (pending >= maxPending) {
      await new Promise<void>((resolve) => slotWaiters.push(resolve));
    }
    pending++;
    pendingEvents += lines.length;
    const done = () => {
      pending--;
      pendingEvents -= lines.length;
      releaseSlot();
    };
    writing = writing
      .then(() => write(lines))
      .then(done, done);
  }

  function takeBatch(): string[] {
    clearTimeout(flushTimer);
    flushTimer = undefined;
    const full = batch;
    batch = [];
    return full;
  }

  async function addEvent(json: string): Promise<void> {
    batch.push(json);
    if (batch.length >= batchSize) {
      await handOff(takeBatch());
    } else if (flushTimer === undefined) {
      flushTimer = setTimeout(() => {
        flushTimer = undefined;
        if (batch.length > 0) void handOff(takeBatch());
      }, flushMs);
    }
  }

  async function flush(): Promise<void> {
    if (batch.length > 0) await handOff(takeBatch());
    let current: Promise<void>;
    do {
      current = writing;
      await current;
    } while (current !== writing);
  }

  const stats = (): Stats => ({
    bufferedEvents: batch.length + pendingEvents,
    pendingBatches: pending,
    batchesWritten: metrics.batchesWritten,
  });

  function send(res: ServerResponse, status: number, body: unknown, headers: Record<string, string> = {}): void {
    res.writeHead(status, { "content-type": "application/json", ...headers });
    res.end(JSON.stringify(body));
  }

  async function ingest(req: IncomingMessage, res: ServerResponse): Promise<number> {
    const type = (req.headers["content-type"] ?? "").split(";")[0]?.trim().toLowerCase();
    if (type !== "application/x-ndjson") {
      send(res, 415, { error: "send application/x-ndjson" });
      return 415;
    }
    if (pending >= maxPending) {
      send(res, 503, { error: "too busy, retry later" }, { "retry-after": "1" });
      return 503;
    }
    let lineNo = 0;
    let accepted = 0;
    let rejected = 0;
    const errors: LineError[] = [];
    let parts: Buffer[] = [];
    let size = 0;
    let tooLong = false;
    const endLine = async () => {
      lineNo++;
      const long = tooLong;
      const text = long ? "" : Buffer.concat(parts).toString("utf8").replace(/\r$/, "");
      parts = [];
      size = 0;
      tooLong = false;
      let message: string;
      if (long) {
        message = `longer than ${maxLineBytes} bytes`;
      } else {
        if (text.trim() === "") return;
        const parsed = parseEvent(text);
        if (parsed.ok) {
          accepted++;
          metrics.events.accepted++;
          await addEvent(parsed.json);
          return;
        }
        message = parsed.message;
      }
      rejected++;
      metrics.events.rejected++;
      if (errors.length < 10) errors.push({ line: lineNo, message });
    };
    for await (const chunk of req as AsyncIterable<Buffer>) {
      let start = 0;
      for (;;) {
        const newline = chunk.indexOf(10, start);
        const end = newline === -1 ? chunk.length : newline;
        if (!tooLong) {
          size += end - start;
          if (size > maxLineBytes) {
            tooLong = true;
            parts = [];
          } else if (end > start) {
            parts.push(chunk.subarray(start, end));
          }
        }
        if (newline === -1) break;
        await endLine();
        start = newline + 1;
      }
    }
    if (size > 0 || tooLong) await endLine();
    send(res, 202, { accepted, rejected, errors });
    return 202;
  }

  function metricsText(): string {
    const out: string[] = [];
    const family = (name: string, type: string, help: string) => out.push(`# HELP ${name} ${help}`, `# TYPE ${name} ${type}`);
    family("ingest_events_total", "counter", "Events received, by result.");
    out.push(`ingest_events_total{result="accepted"} ${metrics.events.accepted}`);
    out.push(`ingest_events_total{result="rejected"} ${metrics.events.rejected}`);
    family("ingest_requests_total", "counter", "HTTP requests, by route and status.");
    for (const [labels, count] of metrics.requests) out.push(`ingest_requests_total{${labels}} ${count}`);
    family("ingest_request_duration_seconds", "histogram", "Time to answer POST /events.");
    BUCKETS.forEach((bound, index) => out.push(`ingest_request_duration_seconds_bucket{le="${bound}"} ${metrics.buckets[index]}`));
    out.push(`ingest_request_duration_seconds_bucket{le="+Inf"} ${metrics.durationCount}`);
    out.push(`ingest_request_duration_seconds_sum ${metrics.durationSum}`);
    out.push(`ingest_request_duration_seconds_count ${metrics.durationCount}`);
    family("ingest_batches_written_total", "counter", "Batches the sink wrote.");
    out.push(`ingest_batches_written_total ${metrics.batchesWritten}`);
    family("ingest_pending_batches", "gauge", "Batches waiting for the sink.");
    out.push(`ingest_pending_batches ${pending}`);
    family("ingest_buffered_events", "gauge", "Accepted events not written yet.");
    out.push(`ingest_buffered_events ${stats().bufferedEvents}`);
    family("ingest_event_loop_delay_max_seconds", "gauge", "Longest event loop delay since the last scrape.");
    const max = Number.isFinite(loopDelay.max) && loopDelay.count > 0 ? loopDelay.max / 1e9 : 0;
    loopDelay.reset();
    out.push(`ingest_event_loop_delay_max_seconds ${max}`);
    return `${out.join("\n")}\n`;
  }

  async function handle(req: IncomingMessage, res: ServerResponse): Promise<void> {
    const started = process.hrtime.bigint();
    const url = new URL(req.url ?? "/", "http://localhost");
    const method = req.method ?? "GET";
    let route = "other";
    let status: number;
    try {
      if (url.pathname === "/events") {
        route = "/events";
        if (method === "POST") {
          status = await ingest(req, res);
          metrics.duration(Number(process.hrtime.bigint() - started) / 1e9);
        } else {
          send(res, 405, { error: "method not allowed" }, { allow: "POST" });
          status = 405;
        }
      } else if (url.pathname === "/metrics" && method === "GET") {
        route = "/metrics";
        res.writeHead(200, { "content-type": "text/plain; version=0.0.4; charset=utf-8" });
        res.end(metricsText());
        status = 200;
      } else {
        send(res, 404, { error: "not found" });
        status = 404;
      }
    } catch {
      status = 500;
      if (!res.headersSent) send(res, 500, { error: "internal error" });
      else res.destroy();
    }
    if (!req.complete) req.resume();
    metrics.request(route, status);
  }

  const server = createServer((req, res) => {
    void handle(req, res);
  });

  return {
    server,
    flush,
    stats,
  };
}
