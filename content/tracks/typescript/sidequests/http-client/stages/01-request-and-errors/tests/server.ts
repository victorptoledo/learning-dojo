// A real HTTP server on 127.0.0.1 for the contract tests: it records every request it gets
// and answers with the handler of the test.
import { createServer, type IncomingHttpHeaders, type ServerResponse } from "node:http";
import type { AddressInfo } from "node:net";

export type Seen = { method: string; url: string; headers: IncomingHttpHeaders; body: string };

export type Handler = (request: Seen, response: ServerResponse, count: number) => void | Promise<void>;

export type TestServer = { url: string; seen: Seen[]; close(): Promise<void> };

export async function serve(handler: Handler): Promise<TestServer> {
  const seen: Seen[] = [];
  const server = createServer((incoming, response) => {
    const chunks: Buffer[] = [];
    incoming.on("data", (chunk: Buffer) => chunks.push(chunk));
    incoming.on("end", () => {
      const request: Seen = {
        method: incoming.method ?? "",
        url: incoming.url ?? "",
        headers: incoming.headers,
        body: Buffer.concat(chunks).toString("utf8"),
      };
      seen.push(request);
      Promise.resolve(handler(request, response, seen.length)).catch((error: unknown) => {
        response.statusCode = 599;
        response.end(String(error));
      });
    });
  });
  await new Promise<void>((resolve) => server.listen(0, "127.0.0.1", resolve));
  const { port } = server.address() as AddressInfo;
  return {
    url: `http://127.0.0.1:${port}`,
    seen,
    close: () =>
      new Promise<void>((resolve) => {
        server.closeAllConnections();
        server.close(() => resolve());
      }),
  };
}

/** Answers with JSON and the status. */
export function json(response: ServerResponse, status: number, value: unknown): void {
  response.writeHead(status, { "content-type": "application/json" });
  response.end(JSON.stringify(value));
}

/** A server for one test: closed when the test ends, whatever happens. */
export async function withServer(handler: Handler, run: (server: TestServer) => Promise<void>): Promise<void> {
  const server = await serve(handler);
  try {
    await run(server);
  } finally {
    await server.close();
  }
}

export const sleep = (ms: number) => new Promise<void>((resolve) => setTimeout(resolve, ms));
