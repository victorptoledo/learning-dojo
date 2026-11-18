// An event ingestion service: NDJSON streamed in over HTTP, validated line by line, grouped in
// batches and handed to a sink one batch at a time, with backpressure.
import { mkdir, readdir, rename, writeFile } from "node:fs/promises";
import { createServer, type IncomingMessage, type Server, type ServerResponse } from "node:http";
import path from "node:path";

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

export function createService(options: ServiceOptions): Service {
  const sink = options.sink;
  const batchSize = options.batchSize ?? 100;
  const flushMs = options.flushMs ?? 1000;
  const maxLineBytes = options.maxLineBytes ?? 65536;
  const maxPending = options.maxPending ?? 4;

  let batch: string[] = [];
  let flushTimer: NodeJS.Timeout | undefined;
  let writing: Promise<void> = Promise.resolve();
  let pending = 0;
  let pendingEvents = 0;
  let batchesWritten = 0;
  let slotWaiters: (() => void)[] = [];

  const releaseSlot = () => {
    const waiters = slotWaiters;
    slotWaiters = [];
    for (const wake of waiters) wake();
  };

  /** Queues a batch for the sink, one write at a time, waiting while maxPending are pending. */
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
      .then(() => sink.write(lines))
      .then(() => {
        batchesWritten++;
        done();
      }, done);
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

  const stats = (): Stats => ({ bufferedEvents: batch.length + pendingEvents, pendingBatches: pending, batchesWritten });

  function send(res: ServerResponse, status: number, body: unknown, headers: Record<string, string> = {}): void {
    res.writeHead(status, { "content-type": "application/json", ...headers });
    res.end(JSON.stringify(body));
  }

  async function ingest(req: IncomingMessage, res: ServerResponse): Promise<void> {
    const type = (req.headers["content-type"] ?? "").split(";")[0]?.trim().toLowerCase();
    if (type !== "application/x-ndjson") {
      send(res, 415, { error: "send application/x-ndjson" });
      return;
    }
    if (pending >= maxPending) {
      // Shed load: the sink is behind, and this body would only wait in memory.
      send(res, 503, { error: "too busy, retry later" }, { "retry-after": "1" });
      return;
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
          await addEvent(parsed.json);
          return;
        }
        message = parsed.message;
      }
      rejected++;
      if (errors.length < 10) errors.push({ line: lineNo, message });
    };
    // Bytes are split into lines before decoding, so a character cut between two chunks is
    // decoded whole; a line over the limit is dropped as it streams by, never buffered. Each
    // line is awaited: while the sink is behind, the loop stops reading, and TCP slows the client.
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
  }

  async function handle(req: IncomingMessage, res: ServerResponse): Promise<void> {
    const url = new URL(req.url ?? "/", "http://localhost");
    try {
      if (url.pathname === "/events") {
        if (req.method === "POST") await ingest(req, res);
        else send(res, 405, { error: "method not allowed" }, { allow: "POST" });
      } else {
        send(res, 404, { error: "not found" });
      }
    } catch {
      if (!res.headersSent) send(res, 500, { error: "internal error" });
      else res.destroy();
    }
    if (!req.complete) req.resume();
  }

  const server = createServer((req, res) => {
    void handle(req, res);
  });

  return { server, flush, stats };
}
