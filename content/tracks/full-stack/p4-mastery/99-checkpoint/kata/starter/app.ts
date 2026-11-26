// The users API. This is the release running in production today: it only knows full_name.
// Turn it into the expand release: see README.md.
import { createServer, type IncomingMessage, type Server, type ServerResponse } from "node:http";
import type { DatabaseSync } from "node:sqlite";
import type { Flag } from "./flags.ts";
import type { Rename } from "./users.ts";

export const READ_SPLIT_NAME = "read-split-name";

export interface AppOptions {
  db: DatabaseSync;
  /** Read on every request, so a flag change needs no deploy. */
  flags: () => Record<string, Flag>;
}

function sendJson(response: ServerResponse, status: number, value: unknown): void {
  response.writeHead(status, { "Content-Type": "application/json" });
  response.end(JSON.stringify(value));
}

/** The rename a PATCH body asks for, or `undefined` when it is not exactly one valid shape. */
export function parseRename(body: unknown): Rename | undefined {
  throw new Error(`todo: read the rename in ${JSON.stringify(body)}`);
}

export function createApp(options: AppOptions): Server {
  const one = options.db.prepare("SELECT id, full_name, email FROM users WHERE id = ?");

  async function handle(request: IncomingMessage, response: ServerResponse): Promise<void> {
    const path = new URL(request.url ?? "/", "http://localhost").pathname;
    const match = /^\/api\/users\/(\d+)$/.exec(path);
    if (!match) {
      sendJson(response, 404, { error: "not found" });
      return;
    }
    if (request.method === "GET") {
      const row = one.get(Number(match[1])) as { id: number; full_name: string; email: string } | undefined;
      if (row) sendJson(response, 200, { id: row.id, fullName: row.full_name, email: row.email });
      else sendJson(response, 404, { error: "user not found" });
    } else {
      // todo: PATCH /api/users/:id
      sendJson(response, 405, { error: "method not allowed" });
    }
  }

  return createServer((request, response) => {
    handle(request, response).catch(() => {
      if (!response.headersSent) sendJson(response, 500, { error: "internal error" });
      else response.end();
    });
  });
}

if (import.meta.main) {
  const { DatabaseSync } = await import("node:sqlite");
  const { migrate } = await import("./migrations.ts");
  const db = new DatabaseSync(":memory:");
  migrate(db);
  db.prepare("INSERT INTO users (full_name, email) VALUES (?, ?)").run("Ana Maria Souza", "ana@example.com");
  const flags = { [READ_SPLIT_NAME]: { name: READ_SPLIT_NAME, enabled: true, percent: 10 } };
  createApp({ db, flags: () => flags }).listen(3000, () => console.log("http://localhost:3000/api/users/1"));
}
