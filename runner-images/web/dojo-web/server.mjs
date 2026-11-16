// A small static file server for a kata folder, with optional fake API routes and extra headers.
// It listens on 127.0.0.1 inside the check container, which has no network: the page talks to
// this server only.
import { createServer } from "node:http";
import { readFile, stat } from "node:fs/promises";
import path from "node:path";

const TYPES = {
  ".html": "text/html; charset=utf-8",
  ".css": "text/css; charset=utf-8",
  ".js": "text/javascript; charset=utf-8",
  ".mjs": "text/javascript; charset=utf-8",
  ".json": "application/json; charset=utf-8",
  ".svg": "image/svg+xml",
  ".png": "image/png",
  ".jpg": "image/jpeg",
  ".jpeg": "image/jpeg",
  ".webp": "image/webp",
  ".gif": "image/gif",
  ".ico": "image/x-icon",
  ".txt": "text/plain; charset=utf-8",
  ".woff2": "font/woff2",
};

/**
 * The rules of a `_headers` file (the format Netlify and Cloudflare Pages read): a path pattern
 * on its own line ("/*", "/admin/*", "/index.html"), then indented "Name: value" lines.
 * Lines starting with # are comments.
 */
export function parseHeadersFile(text) {
  const rules = [];
  for (const raw of text.split(/\r?\n/)) {
    const line = raw.trimEnd();
    if (line.trim() === "" || line.trim().startsWith("#")) {
      continue;
    }
    if (!/^\s/.test(line)) {
      rules.push({ pattern: line.trim(), headers: {} });
      continue;
    }
    const colon = line.indexOf(":");
    if (colon > 0 && rules.length > 0) {
      const name = line.slice(0, colon).trim().toLowerCase();
      const value = line.slice(colon + 1).trim();
      const headers = rules.at(-1).headers;
      headers[name] = headers[name] ? `${headers[name]}, ${value}` : value;
    }
  }
  return rules;
}

function matches(pattern, pathname) {
  if (pattern.endsWith("*")) {
    return pathname.startsWith(pattern.slice(0, -1));
  }
  return pattern === pathname || (pattern.endsWith("/") && `${pattern}index.html` === pathname);
}

/** The headers a `_headers` file in root gives to a path ({} when there is no file). */
async function headersFor(root, pathname) {
  const text = await readFile(path.join(root, "_headers"), "utf8").catch(() => null);
  if (text === null) {
    return {};
  }
  const out = {};
  for (const rule of parseHeadersFile(text)) {
    if (matches(rule.pattern, pathname)) {
      Object.assign(out, rule.headers);
    }
  }
  return out;
}

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

async function readBody(request) {
  const chunks = [];
  for await (const chunk of request) {
    chunks.push(chunk);
  }
  return Buffer.concat(chunks).toString("utf8");
}

/**
 * Starts the server and resolves to it, with `origin` ("http://127.0.0.1:PORT") and `requests`
 * (every request seen: { method, path, query, headers, body }).
 *
 * - `routes`: { "GET /api/items": handler } where handler(request) returns
 *   { status = 200, headers = {}, json | body, delayMs = 0 } or a Promise of it. A route key
 *   without a method ("/api/items") matches every method.
 * - `headers`: extra headers on every static file, or a function (path) => headers.
 * A `_headers` file in root also applies to static files (see parseHeadersFile).
 */
export async function startServer({
  root = process.cwd(),
  routes = {},
  headers = {},
  host = "127.0.0.1",
  port = 0,
} = {}) {
  const base = path.resolve(root);
  const requests = [];

  const server = createServer(async (request, response) => {
    try {
      const url = new URL(request.url ?? "/", "http://localhost");
      const body = await readBody(request);
      const seen = {
        method: request.method ?? "GET",
        path: url.pathname,
        query: Object.fromEntries(url.searchParams),
        headers: request.headers,
        body,
      };
      requests.push(seen);

      const route = routes[`${seen.method} ${url.pathname}`] ?? routes[url.pathname];
      if (route) {
        const answer = (await route({ ...seen, url, json: () => JSON.parse(body || "null") })) ?? {};
        if (answer.delayMs) {
          await sleep(answer.delayMs);
        }
        const out = { ...(answer.headers ?? {}) };
        let payload = answer.body ?? "";
        if (answer.json !== undefined) {
          payload = JSON.stringify(answer.json);
          out["content-type"] ??= "application/json; charset=utf-8";
        }
        response.writeHead(answer.status ?? 200, out);
        response.end(payload);
        return;
      }

      let file = path.join(base, decodeURIComponent(url.pathname));
      if (!file.startsWith(base)) {
        response.writeHead(403).end("forbidden");
        return;
      }
      const info = await stat(file).catch(() => null);
      if (info?.isDirectory()) {
        file = path.join(file, "index.html");
      }
      const content = await readFile(file).catch(() => null);
      if (content === null) {
        response.writeHead(404, { "content-type": "text/plain" }).end("not found");
        return;
      }
      const extra = typeof headers === "function" ? headers(url.pathname) : headers;
      response.writeHead(200, {
        "content-type": TYPES[path.extname(file).toLowerCase()] ?? "application/octet-stream",
        "cache-control": "no-store",
        ...(await headersFor(base, url.pathname)),
        ...(extra ?? {}),
      });
      response.end(content);
    } catch (error) {
      response.writeHead(500, { "content-type": "text/plain" }).end(String(error));
    }
  });

  await new Promise((resolve) => server.listen(port, host, resolve));
  const address = server.address();
  server.origin = `http://127.0.0.1:${typeof address === "object" ? address.port : port}`;
  server.requests = requests;
  return server;
}
