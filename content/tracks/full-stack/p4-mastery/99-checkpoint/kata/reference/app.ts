// The users API of the expand release: old and new clients side by side.
import { createServer, type IncomingMessage, type Server, type ServerResponse } from "node:http";
import type { DatabaseSync } from "node:sqlite";
import { isEnabled, type Flag } from "./flags.ts";
import { usersRepo, type Rename } from "./users.ts";

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

async function readBody(request: IncomingMessage): Promise<string> {
  let body = "";
  for await (const chunk of request) body += chunk;
  return body;
}

/** The rename a PATCH body asks for, or `undefined` when it is not exactly one valid shape. */
export function parseRename(body: unknown): Rename | undefined {
  if (typeof body !== "object" || body === null || Array.isArray(body)) return undefined;
  const value = body as Record<string, unknown>;
  const old = "fullName" in value;
  const split = "firstName" in value || "lastName" in value;
  if (old === split) return undefined;
  if (old) {
    const fullName = value["fullName"];
    return typeof fullName === "string" && fullName.trim() !== "" ? { fullName } : undefined;
  }
  const { firstName, lastName } = value;
  if (typeof firstName !== "string" || typeof lastName !== "string" || firstName.trim() === "") return undefined;
  return { firstName, lastName };
}

export function createApp(options: AppOptions): Server {
  const users = usersRepo(options.db);
  const readSplit = (id: number): boolean => isEnabled(options.flags()[READ_SPLIT_NAME], id);

  async function handle(request: IncomingMessage, response: ServerResponse): Promise<void> {
    const path = new URL(request.url ?? "/", "http://localhost").pathname;
    const match = /^\/api\/users\/(\d+)$/.exec(path);
    if (!match) {
      sendJson(response, 404, { error: "not found" });
      return;
    }
    const id = Number(match[1]);
    if (request.method === "GET") {
      const user = users.get(id, readSplit(id));
      if (user) sendJson(response, 200, user);
      else sendJson(response, 404, { error: "user not found" });
    } else if (request.method === "PATCH") {
      let body: unknown;
      try {
        body = JSON.parse(await readBody(request));
      } catch {
        body = undefined;
      }
      const change = parseRename(body);
      if (!change) {
        sendJson(response, 422, { error: "send either fullName, or firstName and lastName" });
        return;
      }
      if (!users.rename(id, change)) {
        sendJson(response, 404, { error: "user not found" });
        return;
      }
      sendJson(response, 200, users.get(id, readSplit(id)));
    } else {
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
