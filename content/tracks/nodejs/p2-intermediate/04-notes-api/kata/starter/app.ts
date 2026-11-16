// A small JSON API on node:http alone: routing, body parsing with limits, status codes.
import type { IncomingMessage, Server, ServerResponse } from "node:http";
// You will also need: import { createServer } from "node:http";

export interface Note {
  id: number;
  text: string;
}

/** An error that becomes an HTTP response with this status and { error: message }. */
export class HttpError extends Error {
  override name = "HttpError";
  readonly status: number;
  readonly headers: Readonly<Record<string, string>>;

  constructor(status: number, message: string, headers: Record<string, string> = {}) {
    super(message);
    this.status = status;
    this.headers = headers;
  }
}

export const MAX_BODY_BYTES = 16 * 1024;
export const MAX_TEXT_LENGTH = 280;

/** Writes a JSON response with the right content type and length. */
export function sendJson(
  res: ServerResponse,
  status: number,
  body: unknown,
  headers: Record<string, string> = {},
): void {
  const text = JSON.stringify(body);
  res.writeHead(status, {
    ...headers,
    "content-type": "application/json; charset=utf-8",
    "content-length": Buffer.byteLength(text),
  });
  res.end(text);
}

/**
 * Reads the request body as JSON. 415 when the content type is not application/json, 413 when
 * the body is longer than `limit` bytes (the rest is read and thrown away), 400 when it is
 * empty or not valid JSON.
 */
export async function readJson(req: IncomingMessage, limit = MAX_BODY_BYTES): Promise<unknown> {
  throw new Error("todo: write readJson()");
}

/** The note text from a parsed body: a string of 1 to 280 characters after trimming, else 422. */
export function validateNote(body: unknown): string {
  throw new Error("todo: write validateNote()");
}

/** The API server, not listening yet. Notes live in memory; ids start at 1. */
export function createApp(): Server {
  throw new Error("todo: write createApp()");
}
