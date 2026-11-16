// A small JSON API on node:http alone: routing, body parsing with limits, status codes.
import { createServer, type IncomingMessage, type Server, type ServerResponse } from "node:http";

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
  const type = req.headers["content-type"] ?? "";
  if (!/^application\/json\b/i.test(type)) {
    throw new HttpError(415, "expected a JSON body (content-type: application/json)");
  }
  const chunks: Buffer[] = [];
  let size = 0;
  for await (const chunk of req as AsyncIterable<Buffer>) {
    size += chunk.length;
    if (size <= limit) {
      chunks.push(chunk);
    }
  }
  if (size > limit) {
    throw new HttpError(413, `the body is larger than ${limit} bytes`);
  }
  const text = Buffer.concat(chunks).toString("utf8");
  if (text.trim() === "") {
    throw new HttpError(400, "the body is empty");
  }
  try {
    return JSON.parse(text);
  } catch {
    throw new HttpError(400, "the body is not valid JSON");
  }
}

/** The note text from a parsed body: a string of 1 to 280 characters after trimming, else 422. */
export function validateNote(body: unknown): string {
  const text =
    typeof body === "object" && body !== null && "text" in body && typeof body.text === "string"
      ? body.text.trim()
      : undefined;
  if (text === undefined || text === "" || text.length > MAX_TEXT_LENGTH) {
    throw new HttpError(422, `text must be a string of 1 to ${MAX_TEXT_LENGTH} characters`);
  }
  return text;
}

function methodNotAllowed(allowed: string): HttpError {
  return new HttpError(405, "method not allowed", { allow: allowed });
}

/** The API server, not listening yet. Notes live in memory; ids start at 1. */
export function createApp(): Server {
  const notes = new Map<number, Note>();
  let nextId = 1;

  async function route(req: IncomingMessage, res: ServerResponse): Promise<void> {
    const url = new URL(req.url ?? "/", "http://localhost");
    const method = req.method ?? "GET";

    if (url.pathname === "/health") {
      if (method !== "GET") throw methodNotAllowed("GET");
      sendJson(res, 200, { status: "ok" });
      return;
    }

    if (url.pathname === "/notes") {
      if (method === "GET") {
        const query = url.searchParams.get("q")?.toLowerCase();
        const list = [...notes.values()]
          .filter((note) => query === undefined || note.text.toLowerCase().includes(query))
          .sort((a, b) => a.id - b.id);
        sendJson(res, 200, { notes: list });
        return;
      }
      if (method === "POST") {
        const text = validateNote(await readJson(req));
        const note: Note = { id: nextId, text };
        nextId += 1;
        notes.set(note.id, note);
        sendJson(res, 201, note, { location: `/notes/${note.id}` });
        return;
      }
      throw methodNotAllowed("GET, POST");
    }

    const match = /^\/notes\/(\d+)$/.exec(url.pathname);
    if (match) {
      const id = Number(match[1]);
      if (method !== "GET" && method !== "DELETE") throw methodNotAllowed("GET, DELETE");
      const note = notes.get(id);
      if (note === undefined) throw new HttpError(404, `no note ${id}`);
      if (method === "GET") {
        sendJson(res, 200, note);
      } else {
        notes.delete(id);
        res.writeHead(204).end();
      }
      return;
    }

    throw new HttpError(404, "not found");
  }

  return createServer((req, res) => {
    route(req, res).catch((error: unknown) => {
      if (error instanceof HttpError) {
        sendJson(res, error.status, { error: error.message }, { ...error.headers });
      } else {
        console.error("unexpected error", error);
        if (!res.headersSent) {
          sendJson(res, 500, { error: "internal error" });
        } else {
          res.destroy();
        }
      }
    });
  });
}
