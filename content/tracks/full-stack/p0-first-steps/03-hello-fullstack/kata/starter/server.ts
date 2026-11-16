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

/** A guestbook server (not listening yet) with its entries in memory: see README.md. */
export function createGuestbookServer(): Server {
  const entries: Entry[] = [];
  return createServer((request, response) => {
    // todo: route GET /, GET and POST /api/entries, other methods (405) and other paths (404).
    // `entries` and `renderEntries` are here for you to use. Until then every request is a 501.
    void entries;
    void renderEntries;
    sendJson(response, 501, { error: `todo: ${request.method} ${request.url} is not written yet` });
  });
}

// `npm start` (node server.ts) serves the guestbook on http://localhost:3000.
if (import.meta.main) {
  createGuestbookServer().listen(3000, () => console.log("http://localhost:3000"));
}
