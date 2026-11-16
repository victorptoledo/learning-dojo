# Performance and Real-Time: Budgets, CDNs, N+1, WebSockets and SSE

Performance is a feature with a number. A page is fast when real users on real devices see
content and can interact quickly; an optimisation that does not move a measured number is a
guess. This lesson goes along the request path, then adds live updates.

## Budgets and measurement

Set a **budget** per page and check it in CI: for example LCP ≤ 2.5 s and INP ≤ 200 ms at p75
on a mid-range phone, at most 170 KB of compressed JavaScript on the critical path, API p95 ≤
300 ms. Then measure:

- **field data** (what users get): real-user monitoring with the `web-vitals` library, the Chrome
  UX Report;
- **lab data** (reproducible): Lighthouse, the browser's network and performance panels with CPU
  and network throttling, and the server's traces.

Look for the **biggest** cost first. It is rarely where you guessed.

## The front end

- **JavaScript is the most expensive byte**: it is downloaded, parsed and executed on the main
  thread. Split code by route (`import()` for heavy, rarely used parts), remove unused
  dependencies, check the bundle with an analyser.
- **Images**: the right size (`srcset`, `sizes`), modern formats (AVIF, WebP), `loading="lazy"`
  below the fold, `fetchpriority="high"` for the LCP image, and `width`/`height` to avoid layout
  shifts.
- **Render-blocking resources**: inline the critical CSS, `defer` scripts, preload key fonts with
  `font-display: swap`.
- **Long tasks**: break up work over 50 ms so input is handled promptly (INP).

## The network

- A **CDN** serves static assets and cacheable pages from near the user; long TTLs for hashed
  assets (the data-fetching lesson).
- **Compression** with Brotli or gzip for text; HTTP/2 or HTTP/3 multiplex requests over one
  connection.
- **Fewer round trips**: avoid request waterfalls (fetch in parallel, at the route level), and
  `preconnect` to third-party origins you must use.

## The server and the database

Traces show where server time goes. The usual suspects:

- **N+1 queries** and **missing indexes** (read the plan with `EXPLAIN ANALYZE`).
- **Sequential calls** that could run in parallel (`Promise.all`, `CompletableFuture`).
- **Pool sizes**: too few database connections queue requests, too many overload PostgreSQL.
- **Payload size**: return what the screen needs, paginated.
- **Caching** the expensive, rarely changing results, with an invalidation plan.

## Live updates

| Technique | How | Good for |
| --- | --- | --- |
| **Polling** | `GET` every N seconds, ideally with `If-None-Match` so unchanged answers are 304 | low frequency, simplest, works everywhere |
| **Long polling** | the server holds the request until there is news | legacy fallback |
| **Server-Sent Events (SSE)** | one long `GET` with `Content-Type: text/event-stream`; the server writes `data: ...\n\n` events; `EventSource` reconnects by itself and resends `Last-Event-ID` | server → client feeds: notifications, order status, progress, streamed AI answers |
| **WebSockets** | an upgraded, two-way connection | chat, collaborative editing, games |

SSE is plain HTTP: it goes through proxies, works with cookies and HTTP/2, and is often all you
need. Choose WebSockets when the client also sends a steady stream of messages.

### At scale

- A user's connection lands on **one instance**, but the event happens on another. Fan out
  through a **pub/sub**: Redis pub/sub or streams, PostgreSQL `LISTEN/NOTIFY`, a message broker.
- Long-lived connections need **load balancer timeouts** above the heartbeat interval, and
  **heartbeats** (an SSE comment line, a WebSocket ping) to keep idle connections open.
- Clients must **reconnect with backoff and catch up** (SSE's `Last-Event-ID`, or refetch the
  current state after reconnecting), because connections drop all the time on mobile.
- **Authenticate** the connection like any request (cookies work for SSE and for the WebSocket
  handshake) and **check the `Origin`** of WebSocket upgrades.

## Summary

- Set budgets, measure in the field and the lab, fix the biggest cost first.
- Front end: less JavaScript, right-sized images, no render-blocking resources, short tasks.
- Network: CDN, compression, no waterfalls. Server: no N+1, indexes, parallel calls, sane pools.
- Push updates with polling, SSE or WebSockets; scale them with pub/sub, heartbeats and reconnects.

## Further reading

- web.dev, "Optimize Largest Contentful Paint": <https://web.dev/articles/optimize-lcp>
- web.dev, "Optimize Interaction to Next Paint": <https://web.dev/articles/optimize-inp>
- MDN, "Using server-sent events": <https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events/Using_server-sent_events>
- MDN, "The WebSocket API": <https://developer.mozilla.org/en-US/docs/Web/API/WebSockets_API>
- PostgreSQL documentation, "Using EXPLAIN": <https://www.postgresql.org/docs/current/using-explain.html>
