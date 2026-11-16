// An inventory service: CSV uploads streamed line by line, NDJSON exports streamed back.
// Useful imports: createServer from "node:http", createInterface from "node:readline",
// Readable from "node:stream", pipeline from "node:stream/promises".
import type { Server } from "node:http";
import type { Readable } from "node:stream";

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

/** One CSV data row: exactly three fields, trimmed; an upper-case sku, a name and a whole qty >= 0. */
export function parseRow(line: string): Row {
  throw new Error("todo: write parseRow()");
}

/**
 * Reads CSV from the source line by line (never the whole body at once) into the inventory.
 * The first non-blank line must be the header (any case), else HttpError 400. Blank lines are
 * skipped. A valid row inserts or replaces its sku; an invalid one is reported with its
 * 1-based line number.
 */
export async function importCsv(source: Readable, inventory: Map<string, Item>): Promise<ImportReport> {
  throw new Error("todo: write importCsv()");
}

/** The items sorted by sku, one JSON line each. */
export function exportNdjson(inventory: ReadonlyMap<string, Item>): Readable {
  throw new Error("todo: write exportNdjson()");
}

/** The service, not listening yet. */
export function createInventoryServer(inventory: Map<string, Item> = new Map()): Server {
  throw new Error("todo: write createInventoryServer()");
}
