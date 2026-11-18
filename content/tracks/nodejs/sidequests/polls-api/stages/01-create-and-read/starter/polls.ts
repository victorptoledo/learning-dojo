// A polls API on node:http alone. The README of each stage lists the routes and the rules.
// Useful: createServer and STATUS_CODES from node:http, randomUUID from node:crypto, URL.
import { createServer, type Server } from "node:http";

export interface AppOptions {
  /** The clock; tests move it. */
  now?: () => Date;
  /** The largest request body accepted, in bytes (default 16384). */
  maxBodyBytes?: number;
}

export interface FieldError {
  path: string;
  message: string;
}

/** A new server for the API; the caller calls listen(). Each server keeps its own polls. */
export function createApp(options: AppOptions = {}): Server {
  return createServer((req, res) => {
    // todo: route the request
    res.writeHead(501, { "content-type": "text/plain" });
    res.end(`not implemented: ${req.method} ${req.url}\n`);
  });
}
