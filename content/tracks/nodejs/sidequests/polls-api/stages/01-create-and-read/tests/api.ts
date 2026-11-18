// Helpers for the contract tests: the app on a real port with a clock the test moves, and
// requests that return the status, the headers and the parsed body.
import assert from "node:assert/strict";
import { STATUS_CODES } from "node:http";
import type { AddressInfo } from "node:net";
import { after } from "node:test";
import { createApp, type AppOptions } from "../polls.ts";

export const START = new Date("2026-03-01T12:00:00.000Z");

export interface Reply {
  status: number;
  headers: Headers;
  /** The parsed JSON body (undefined when empty); `any` keeps the assertions short. */
  body: any;
  text: string;
}

export interface Api {
  base: string;
  /** Moves the app's clock forward. */
  advance(ms: number): void;
  now(): Date;
  call(method: string, path: string, options?: { json?: unknown; body?: string; headers?: Record<string, string> }): Promise<Reply>;
  post(path: string, json: unknown, headers?: Record<string, string>): Promise<Reply>;
  get(path: string, headers?: Record<string, string>): Promise<Reply>;
}

const servers: { close(): void; closeAllConnections(): void }[] = [];
after(() => {
  for (const server of servers) {
    server.closeAllConnections();
    server.close();
  }
});

export async function startApp(options: Omit<AppOptions, "now"> = {}): Promise<Api> {
  let current = START.getTime();
  const server = createApp({ ...options, now: () => new Date(current) });
  servers.push(server);
  await new Promise<void>((resolve) => server.listen(0, "127.0.0.1", resolve));
  const base = `http://127.0.0.1:${(server.address() as AddressInfo).port}`;
  const call: Api["call"] = async (method, path, { json, body, headers = {} } = {}) => {
    const init: RequestInit = { method, headers: { ...headers } };
    if (json !== undefined) {
      init.body = JSON.stringify(json);
      init.headers = { "content-type": "application/json", ...headers };
    } else if (body !== undefined) {
      init.body = body;
    }
    const response = await fetch(base + path, init);
    const text = await response.text();
    let parsed: unknown;
    if (text !== "") {
      try {
        parsed = JSON.parse(text);
      } catch {
        parsed = undefined;
      }
    }
    return { status: response.status, headers: response.headers, body: parsed, text };
  };
  return {
    base,
    advance: (ms) => {
      current += ms;
    },
    now: () => new Date(current),
    call,
    post: (path, json, headers) => call("POST", path, { json, headers }),
    get: (path, headers) => call("GET", path, { headers }),
  };
}

/** A valid poll body, with overrides. */
export function pollBody(overrides: Record<string, unknown> = {}): Record<string, unknown> {
  return { question: "Tabs or spaces?", options: ["Tabs", "Spaces"], ...overrides };
}

/** Asserts an RFC 9457 problem with this status and returns its body. */
export function assertProblem(reply: Reply, status: number): any {
  assert.equal(reply.status, status, `expected ${status}, got ${reply.status}: ${reply.text}`);
  assert.equal(reply.headers.get("content-type")?.split(";")[0], "application/problem+json");
  assert.equal(reply.body?.status, status);
  assert.equal(reply.body?.title, STATUS_CODES[status]);
  assert.equal(typeof reply.body?.type, "string");
  assert.equal(typeof reply.body?.detail, "string");
  return reply.body;
}

/** The paths of a 422 or 400 problem's errors, sorted. */
export function errorPaths(reply: Reply): string[] {
  const errors: unknown = reply.body?.errors;
  assert.ok(Array.isArray(errors), `expected an errors array: ${reply.text}`);
  for (const error of errors) {
    assert.equal(typeof error.path, "string");
    assert.ok(typeof error.message === "string" && error.message !== "", "each error has a message");
  }
  return errors.map((error: { path: string }) => error.path).sort();
}
