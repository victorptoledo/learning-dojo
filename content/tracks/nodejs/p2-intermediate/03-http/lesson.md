# HTTP Servers and Clients with node:http and fetch

Every Node.js web framework, Express, Fastify, Koa, NestJS, Hono on Node, sits on top of the
same module: `node:http`. Knowing what it does, and what it leaves to you, explains how those
frameworks work and how to debug them.

## A server

```ts
import { createServer } from "node:http";

const server = createServer((req, res) => {
  res.writeHead(200, { "content-type": "text/plain; charset=utf-8" });
  res.end("hello\n");
});

server.listen(3000, "127.0.0.1", () => console.log("listening on :3000"));
```

The callback is the server's `"request"` event. It gets two streams:

- `req`, an `IncomingMessage`: a **readable** stream of the body, plus `method`, `url` (the
  path and query string) and `headers` (lower-case names).
- `res`, a `ServerResponse`: a **writable** stream. Set the status and headers (`writeHead`,
  or `statusCode` and `setHeader`), write the body, and always `end()` it.

Headers go out with the first write. After that, `res.setHeader` throws "Cannot set headers
after they are sent". Each request gets **exactly one** response: a handler that forgets
`end()` leaves the client waiting until a timeout, and one that answers twice crashes.

`listen(0)` picks a free port (useful in tests); `server.address().port` tells you which.

## Routing

`node:http` has no router. The URL class parses the path and the query string:

```ts
const url = new URL(req.url ?? "/", "http://localhost");     // req.url is only "/users/7?x=1"
url.pathname;                       // "/users/7"
url.searchParams.get("x");          // "1"

const match = /^\/users\/(\d+)$/.exec(url.pathname);
if (req.method === "GET" && match) { /* ... */ }
```

### Status codes worth knowing

| Code | When |
| --- | --- |
| 200 OK, 201 Created (+ `Location`), 204 No Content | Success |
| 400 Bad Request | Malformed input (invalid JSON) |
| 401 Unauthorized / 403 Forbidden | Not authenticated / not allowed |
| 404 Not Found / 405 Method Not Allowed (+ `Allow`) | Wrong path / wrong method |
| 409 Conflict, 413 Content Too Large, 415 Unsupported Media Type, 422 Unprocessable Content | Conflicting state, body too big, wrong content type, valid JSON with invalid fields |
| 429 Too Many Requests (+ `Retry-After`) | Rate limited |
| 500 Internal Server Error | A bug: log it, do not leak details |
| 502 Bad Gateway / 503 Service Unavailable / 504 Gateway Timeout | A dependency failed / overloaded or shutting down / a dependency was too slow |

## Reading the body

The body is a stream and nothing limits it for you. Read it with a cap:

```ts
async function readBody(req: IncomingMessage, limit = 1_000_000): Promise<Buffer> {
  const chunks: Buffer[] = [];
  let size = 0;
  for await (const chunk of req as AsyncIterable<Buffer>) {
    size += chunk.length;
    if (size > limit) throw new HttpError(413, "body too large");
    chunks.push(chunk);
  }
  return Buffer.concat(chunks);
}
```

Then check `content-type` and parse inside `try`/`catch`. A body you do not need must still be
consumed (`req.resume()`) or the connection cannot be reused.

## Errors in handlers

A request listener that is `async` returns a promise **nobody awaits**. If it rejects, that is
an unhandled rejection, and the process exits. Wrap the routing in one place:

```ts
createServer((req, res) => {
  handle(req, res).catch((error) => {
    if (res.headersSent) return res.destroy();          // too late for a clean answer
    const status = error instanceof HttpError ? error.status : 500;
    if (status === 500) logger.error({ error }, "unexpected error");
    res.writeHead(status, { "content-type": "application/json" });
    res.end(JSON.stringify({ error: status === 500 ? "internal error" : error.message }));
  });
});
```

## Connections and timeouts

HTTP/1.1 keeps connections **alive** between requests; Node.js closes idle ones after
`server.keepAliveTimeout` (5 s). Put the keep-alive timeout of a load balancer **below** that
of the server behind it, or the balancer will reuse connections the server is closing.
Protect the server against slow clients:

| Setting | Default | Limits |
| --- | --- | --- |
| `server.headersTimeout` | 60 s | Time to receive the request headers |
| `server.requestTimeout` | 300 s | Time to receive the whole request |
| `server.keepAliveTimeout` | 5 s | How long an idle connection stays open |
| `maxHeaderSize` (option) | 16 KiB | Size of the headers |

`node:https` takes a certificate and key; in most deployments TLS ends at a load balancer or
proxy instead. `node:http2` speaks HTTP/2.

## The client: fetch

Node.js 18+ has the standard `fetch` (built on the undici library). It does not reject on 4xx
or 5xx, and it has **no default timeout** for a slow server:

```ts
const response = await fetch("https://api.example.com/rates", {
  headers: { accept: "application/json" },
  signal: AbortSignal.timeout(3_000),        // always set one
});
if (!response.ok) {
  throw new Error(`rates service answered ${response.status}`);
}
const rates: unknown = await response.json();  // validate before trusting it
```

Connections to the same origin are pooled and reused. Always consume or cancel the body
(`await response.text()`, `response.body?.cancel()`), or the connection stays busy.

## Frameworks

`node:http` gives you the transport. Frameworks add what every service writes again:

| Need | Express | Fastify |
| --- | --- | --- |
| Routing with parameters | `app.get("/users/:id", h)` | `app.get("/users/:id", h)` |
| Body parsing with limits | `express.json({ limit })` | Built in |
| Validation | Middleware (zod, joi) | JSON Schema per route |
| Error handling | Error middleware (async errors handled since Express 5) | Built in, typed |
| Plugins, hooks, logging | Middleware | Plugins, hooks, pino logging |

Express 5 is the most widely used; Fastify is faster and schema-first. Both are fine. What
matters is knowing which of the problems above each one solves for you.

## Summary

- `createServer((req, res) => ...)`: `req` is a readable stream, `res` a writable one; one
  response per request, headers before the body.
- Route with `new URL(req.url, base)`; use precise status codes.
- Read bodies with a size limit and a content-type check; parse safely.
- Catch handler errors in one place; never leak details in a 500.
- Configure timeouts; give every `fetch` an `AbortSignal.timeout`.

## Further reading

- Node.js API, "HTTP": <https://nodejs.org/api/http.html>
- Node.js, "Anatomy of an HTTP Transaction": <https://nodejs.org/en/learn/http/anatomy-of-an-http-transaction>
- Node.js API, `server.requestTimeout`: <https://nodejs.org/api/http.html#serverrequesttimeout>
- Node.js API, "Fetch": <https://nodejs.org/api/globals.html#fetch>
- MDN, "HTTP response status codes": <https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Status>
- RFC 9110, "HTTP Semantics": <https://www.rfc-editor.org/rfc/rfc9110>
- Express documentation: <https://expressjs.com/>
- Fastify documentation: <https://fastify.dev/docs/latest/>
