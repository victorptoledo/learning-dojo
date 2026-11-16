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

function redirect(response: ServerResponse, location: string): void {
  response.writeHead(303, { Location: location });
  response.end();
}

const NOTE_PATH = /^\/api\/notes\/(\d+)$/;
const DELETE_PATH = /^\/notes\/(\d+)\/delete$/;

async function route(repo: NotesRepo, request: IncomingMessage, response: ServerResponse): Promise<void> {
  const url = new URL(request.url ?? "/", "http://localhost");
  const method = request.method ?? "GET";
  const search = url.searchParams.get("q") ?? undefined;

  if (method === "GET" && url.pathname === "/") {
    sendHtml(response, 200, notesPage({ notes: repo.list(search), search }));
    return;
  }
  if (method === "POST" && url.pathname === "/notes") {
    const form = Object.fromEntries(new URLSearchParams(await readBody(request)));
    const result = validateNote(form);
    if (!result.ok) {
      sendHtml(response, 422, notesPage({ notes: repo.list(), errors: result.errors, values: form }));
      return;
    }
    repo.create(result.note);
    redirect(response, "/");
    return;
  }
  const toDelete = method === "POST" ? DELETE_PATH.exec(url.pathname) : null;
  if (toDelete) {
    if (repo.remove(Number(toDelete[1]))) {
      redirect(response, "/");
    } else {
      sendHtml(response, 404, "<!doctype html><title>Not found</title><h1>Note not found</h1>");
    }
    return;
  }
  if (method === "GET" && url.pathname === "/api/notes") {
    sendJson(response, 200, repo.list(search));
    return;
  }
  const one = method === "GET" ? NOTE_PATH.exec(url.pathname) : null;
  if (one) {
    const note = repo.get(Number(one[1]));
    if (note) {
      sendJson(response, 200, note);
    } else {
      sendJson(response, 404, { error: "not found" });
    }
    return;
  }
  if (method === "POST" && url.pathname === "/api/notes") {
    let body: unknown;
    try {
      body = JSON.parse(await readBody(request));
    } catch {
      sendJson(response, 400, { error: "the body is not valid JSON" });
      return;
    }
    const result = validateNote(typeof body === "object" && body !== null ? (body as Record<string, unknown>) : {});
    if (!result.ok) {
      sendJson(response, 422, { errors: result.errors });
      return;
    }
    const note = repo.create(result.note);
    sendJson(response, 201, note, { Location: `/api/notes/${note.id}` });
    return;
  }
  sendJson(response, 404, { error: "not found" });
}

/** The notes app (not listening yet): see the routes in README.md. */
export function createApp(repo: NotesRepo): Server {
  return createServer((request, response) => {
    route(repo, request, response).catch(() => sendJson(response, 500, { error: "internal error" }));
  });
}

// `npm start` (node app.ts) serves the app on http://localhost:3000, with the notes in notes.db.
if (import.meta.main) {
  createApp(openNotes("notes.db")).listen(3000, () => console.log("http://localhost:3000"));
}
