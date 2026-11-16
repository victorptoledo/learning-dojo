// The server half: a guestbook API and a server-rendered page, with node:http only.
import { createServer, type IncomingMessage, type Server, type ServerResponse } from "node:http";
import { renderEntries, type Entry } from "./client.ts";

/** Reads the whole request body as text. */
export async function readBody(request: IncomingMessage): Promise<string> {
  const chunks: Buffer[] = [];
  for await (const chunk of request) {
    chunks.push(chunk as Buffer);
  }
  return Buffer.concat(chunks).toString("utf8");
}

/** Sends a value as JSON with the given status and extra headers. */
export function sendJson(
  response: ServerResponse,
  status: number,
  value: unknown,
  headers: Record<string, string> = {},
): void {
  response.writeHead(status, { "Content-Type": "application/json", ...headers });
  response.end(JSON.stringify(value));
}

/** The trimmed string field `name` of a parsed body, or undefined when missing or blank. */
function field(body: unknown, name: string): string | undefined {
  if (typeof body !== "object" || body === null) {
    return undefined;
  }
  const value = (body as Record<string, unknown>)[name];
  return typeof value === "string" && value.trim() !== "" ? value.trim() : undefined;
}

/** A guestbook server (not listening yet) with its entries in memory: see README.md. */
export function createGuestbookServer(): Server {
  const entries: Entry[] = [];

  async function create(request: IncomingMessage, response: ServerResponse): Promise<void> {
    let body: unknown;
    try {
      body = JSON.parse(await readBody(request));
    } catch {
      sendJson(response, 400, { error: "the body is not valid JSON" });
      return;
    }
    const name = field(body, "name");
    const message = field(body, "message");
    if (name === undefined || message === undefined) {
      sendJson(response, 400, { error: "name and message must be non-blank strings" });
      return;
    }
    const entry: Entry = { id: entries.length + 1, name, message };
    entries.push(entry);
    sendJson(response, 201, entry, { Location: `/api/entries/${entry.id}` });
  }

  return createServer((request, response) => {
    const path = new URL(request.url ?? "/", "http://localhost").pathname;
    if (path === "/" && request.method === "GET") {
      response.writeHead(200, { "Content-Type": "text/html; charset=utf-8" });
      response.end(
        `<!doctype html><html><head><title>Guestbook</title></head>` +
          `<body><h1>Guestbook</h1><ul id="entries">${renderEntries(entries)}</ul></body></html>`,
      );
    } else if (path === "/api/entries" && request.method === "GET") {
      sendJson(response, 200, { entries });
    } else if (path === "/api/entries" && request.method === "POST") {
      create(request, response).catch(() => sendJson(response, 500, { error: "internal error" }));
    } else if (path === "/api/entries") {
      sendJson(response, 405, { error: "method not allowed" }, { Allow: "GET, POST" });
    } else {
      sendJson(response, 404, { error: "not found" });
    }
  });
}

// `npm start` (node server.ts) serves the guestbook on http://localhost:3000.
if (import.meta.main) {
  createGuestbookServer().listen(3000, () => console.log("http://localhost:3000"));
}
