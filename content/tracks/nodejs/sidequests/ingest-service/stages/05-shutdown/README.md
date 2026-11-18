Deploys stop the old process with `SIGTERM`. Everything the service answered `202` for must
end up in the sink, and the process must then end by itself.

```ts
export interface ShutdownResult { drained: boolean; lostEvents: number }
// Service gains shutdown(options?: { timeoutMs?: number }): Promise<ShutdownResult>   (default 10000)
```

## shutdown()

1. Stop being ready: `/health/ready` answers `503` with the reason `"shutting down"`. Stop
   accepting connections (`server.close()`) and close the idle keep-alive ones
   (`server.closeIdleConnections()`). A new `POST /events` on a connection that is still open
   gets `503`.
2. Let the uploads in progress finish: each gets its `202`, with `Connection: close`.
3. Write the open batch and wait for every pending write (retries included).
4. Resolve `{ drained: true, lostEvents: 0 }`, leaving nothing behind that keeps the process
   alive: no timer, no `monitorEventLoopDelay`, no connection.

All of that must fit in `timeoutMs`. If the deadline comes first (an upload that never ends,
a sink that is stuck), stop waiting and retrying, close every connection, and resolve
`{ drained: false, lostEvents }` with the number of accepted events that were not written.

## main.ts

Change `main.ts` so that `SIGTERM` and `SIGINT` call `shutdown({ timeoutMs })`, with the
timeout from the environment variable `SHUTDOWN_TIMEOUT_MS` (default 10000):

- Drained: the process ends **by itself** with exit code 0, because nothing is left open
  (do not call `process.exit` here: if it does not end, something leaks).
- Not drained: exit with code 1.
- A **second** signal while shutting down: exit with code 1 at once (the operator insists).

Keep the line `listening on http://127.0.0.1:<port>` on stdout when it starts: the tests run
`node main.ts` with `PORT=0` and `INGEST_DIR` set, and read the port from that line.
