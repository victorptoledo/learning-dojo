// An inventory service: CSV uploads streamed line by line, NDJSON exports streamed back.
import { createServer, type IncomingMessage, type Server, type ServerResponse } from "node:http";
import { createInterface } from "node:readline";
import { Readable } from "node:stream";
import { pipeline } from "node:stream/promises";

export interface Item {
  sku: string;
  name: string;
  qty: number;
}

export type Row = { ok: true; item: Item } | { ok: false; reason: string };

export interface ImportReport {
  imported: number;
  rejected: { line: number; reason: string }[];
}

/** A failure the client caused; the server answers with this status and { error: message }. */
export class HttpError extends Error {
  override name = "HttpError";
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

export const HEADER = "sku,name,qty";
const SKU = /^[A-Z0-9-]{3,20}$/;

/** One CSV data row: exactly three fields, trimmed; an upper-case sku, a name and a whole qty >= 0. */
export function parseRow(line: string): Row {
  const fields = line.split(",").map((field) => field.trim());
  if (fields.length !== 3) {
    return { ok: false, reason: `expected 3 fields, got ${fields.length}` };
  }
  const [sku = "", name = "", qtyText = ""] = fields;
  if (!SKU.test(sku)) {
    return { ok: false, reason: `invalid sku: ${sku}` };
  }
  if (name === "") {
    return { ok: false, reason: "empty name" };
  }
  const qty = Number(qtyText);
  if (qtyText === "" || !Number.isInteger(qty) || qty < 0) {
    return { ok: false, reason: `invalid qty: ${qtyText}` };
  }
  return { ok: true, item: { sku, name, qty } };
}

/**
 * Reads CSV from the source line by line (never the whole body at once) into the inventory.
 * The first non-blank line must be the header (any case), else HttpError 400. Blank lines are
 * skipped. A valid row inserts or replaces its sku; an invalid one is reported with its
 * 1-based line number.
 */
export async function importCsv(source: Readable, inventory: Map<string, Item>): Promise<ImportReport> {
  const report: ImportReport = { imported: 0, rejected: [] };
  const lines = createInterface({ input: source, crlfDelay: Infinity });
  let lineNumber = 0;
  let sawHeader = false;
  for await (const line of lines) {
    lineNumber += 1;
    if (line.trim() === "") {
      continue;
    }
    if (!sawHeader) {
      if (line.trim().toLowerCase().replace(/\s*,\s*/g, ",") !== HEADER) {
        throw new HttpError(400, `the first line must be "${HEADER}"`);
      }
      sawHeader = true;
      continue;
    }
    const row = parseRow(line);
    if (row.ok) {
      inventory.set(row.item.sku, row.item);
      report.imported += 1;
    } else {
      report.rejected.push({ line: lineNumber, reason: row.reason });
    }
  }
  if (!sawHeader) {
    throw new HttpError(400, "the CSV is empty");
  }
  return report;
}

/** The items sorted by sku, one JSON line each. */
export function exportNdjson(inventory: ReadonlyMap<string, Item>): Readable {
  const skus = [...inventory.keys()].sort();
  function* lines(): Generator<string> {
    for (const sku of skus) {
      const item = inventory.get(sku);
      if (item !== undefined) {
        yield `${JSON.stringify(item)}\n`;
      }
    }
  }
  return Readable.from(lines());
}

function sendJson(res: ServerResponse, status: number, body: unknown, headers: Record<string, string> = {}): void {
  const text = JSON.stringify(body);
  res.writeHead(status, { ...headers, "content-type": "application/json; charset=utf-8", "content-length": Buffer.byteLength(text) });
  res.end(text);
}

/** The service, not listening yet. */
export function createInventoryServer(inventory: Map<string, Item> = new Map()): Server {
  async function route(req: IncomingMessage, res: ServerResponse): Promise<void> {
    const { pathname } = new URL(req.url ?? "/", "http://localhost");
    const method = req.method ?? "GET";

    if (pathname === "/import") {
      if (method !== "POST") {
        res.setHeader("allow", "POST");
        throw new HttpError(405, "method not allowed");
      }
      if (!/^text\/csv\b/i.test(req.headers["content-type"] ?? "")) {
        req.resume();
        throw new HttpError(415, "expected text/csv");
      }
      sendJson(res, 200, await importCsv(req, inventory));
      return;
    }

    if (pathname === "/items") {
      if (method !== "GET") {
        res.setHeader("allow", "GET");
        throw new HttpError(405, "method not allowed");
      }
      res.writeHead(200, { "content-type": "application/x-ndjson" });
      await pipeline(exportNdjson(inventory), res);
      return;
    }

    const match = /^\/items\/([^/]+)$/.exec(pathname);
    if (match) {
      if (method !== "GET") {
        res.setHeader("allow", "GET");
        throw new HttpError(405, "method not allowed");
      }
      const sku = decodeURIComponent(match[1] ?? "");
      const item = inventory.get(sku);
      if (item === undefined) {
        throw new HttpError(404, `no item ${sku}`);
      }
      sendJson(res, 200, item);
      return;
    }

    throw new HttpError(404, "not found");
  }

  return createServer((req, res) => {
    route(req, res).catch((error: unknown) => {
      req.resume(); // drop whatever is left of the body, so the client can read the answer
      if (res.headersSent) {
        res.destroy(error instanceof Error ? error : undefined);
      } else if (error instanceof HttpError) {
        sendJson(res, error.status, { error: error.message });
      } else {
        console.error("unexpected error", error);
        sendJson(res, 500, { error: "internal error" });
      }
    });
  });
}
