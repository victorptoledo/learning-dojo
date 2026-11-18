// An event ingestion service: NDJSON streamed in over HTTP, validated line by line, grouped in
// batches and handed to a sink one batch at a time. The README of each stage lists the rules.
// Useful: node:http, node:fs/promises (mkdir, readdir, writeFile, rename) and Buffer.
import { createServer, type Server } from "node:http";

/** Where full batches go: one call per batch, never two at once. */
export interface Sink {
  write(lines: readonly string[]): Promise<void>;
}

export interface ServiceOptions {
  sink: Sink;
  batchSize?: number;
  flushMs?: number;
  maxLineBytes?: number;
}

export interface Service {
  server: Server;
  flush(): Promise<void>;
}

/** Writes each batch to dir/batch-NNNNNN.ndjson, numbered after the files already there. */
export function fileSink(dir: string): Sink {
  return {
    async write(lines) {
      throw new Error(`todo: write ${lines.length} lines to a new file in ${dir}`);
    },
  };
}

export function createService(options: ServiceOptions): Service {
  const server = createServer((req, res) => {
    // todo: POST /events
    res.writeHead(501, { "content-type": "application/json" });
    res.end(JSON.stringify({ error: `not implemented: ${req.method} ${req.url}` }));
  });
  return {
    server,
    async flush() {
      throw new Error("todo: write flush()");
    },
  };
}
