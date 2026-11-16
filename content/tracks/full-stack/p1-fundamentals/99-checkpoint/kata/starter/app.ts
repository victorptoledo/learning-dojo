// The routes: an HTML front end and a JSON API over the same repository and validation.
import { createServer, type IncomingMessage, type Server, type ServerResponse } from "node:http";
import { openNotes, type NotesRepo } from "./db.ts";
import { validateNote } from "./notes.ts";
import { notesPage } from "./views.ts";

/** Reads the whole request body as text. */
export async function readBody(request: IncomingMessage): Promise<string> {
  const chunks: Buffer[] = [];
  for await (const chunk of request) {
    chunks.push(chunk as Buffer);
  }
  return Buffer.concat(chunks).toString("utf8");
}

export function sendHtml(response: ServerResponse, status: number, html: string): void {
  response.writeHead(status, { "Content-Type": "text/html; charset=utf-8" });
  response.end(html);
}

export function sendJson(response: ServerResponse, status: number, value: unknown, headers: Record<string, string> = {}): void {
  response.writeHead(status, { "Content-Type": "application/json", ...headers });
  response.end(JSON.stringify(value));
}

/** The notes app (not listening yet): see the routes in README.md. */
export function createApp(repo: NotesRepo): Server {
  return createServer((request, response) => {
    // todo: the HTML routes (GET /, POST /notes, POST /notes/ID/delete) and the JSON API
    // (/api/notes). URLSearchParams parses both query strings and form bodies.
    void repo;
    void validateNote;
    void notesPage;
    sendJson(response, 501, { error: `todo: ${request.method} ${request.url} is not written yet` });
  });
}

// `npm start` (node app.ts) serves the app on http://localhost:3000, with the notes in notes.db.
if (import.meta.main) {
  createApp(openNotes("notes.db")).listen(3000, () => console.log("http://localhost:3000"));
}
