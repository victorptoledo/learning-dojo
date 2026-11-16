# Node.js in Production: Lifecycle, Signals and Graceful Shutdown

A service that runs fine on a laptop can still drop requests on every deploy, ignore stop
signals, or be restarted in a loop by its orchestrator. This lesson is about the life of a
production Node.js process: how it starts, how it says it is healthy, and how it stops
without hurting anyone.

## Starting

```dockerfile
FROM node:26-slim
WORKDIR /app
ENV NODE_ENV=production
COPY package.json package-lock.json ./
RUN npm ci --omit=dev
COPY src ./src
USER node
CMD ["node", "src/main.ts"]
```

- **`npm ci --omit=dev`**: exactly the lockfile, without test and build tools.
- **`NODE_ENV=production`**: many libraries (Express among them) switch off development
  features with it. Do not use it for your own configuration; use explicit variables.
- **A non-root user** and a slim (or distroless) base image.
- **`node` is the process**: `CMD ["node", ...]` in exec form. With `CMD npm start` (or the
  shell form `CMD node src/main.ts`), PID 1 is npm or a shell, which may not forward
  `SIGTERM` to Node.js, so your shutdown code never runs and the container is killed after
  the grace period. As PID 1, Node.js also does not reap zombie processes; if you start
  children, add a tiny init (`docker run --init`, or tini).
- If you build TypeScript to JavaScript, ship the built files; if you run `.ts` with type
  stripping, keep `tsc` in CI.

Outside containers, the supervisor is systemd (with `Restart=on-failure`) or a process manager
like PM2. Whatever it is, **something must restart the process when it crashes**, because a
crash is the right reaction to a bug.

## Configuration and logs

Configuration comes from the environment (validated at startup, phase 2). Logs go to
**stdout**, one JSON object per line, and the platform collects them; the process does not
manage log files or rotation. pino is the usual fast JSON logger.

## Health checks

| Check | Question | Answer 200 when | On failure the platform |
| --- | --- | --- | --- |
| **Liveness** (`/live`) | Is the process stuck? | The event loop answers | Restarts the container |
| **Readiness** (`/ready`) | Should it get traffic? | Started, dependencies reachable, **not shutting down** | Stops routing to it |
| **Startup** | Has it finished starting? | Warm-up done | Waits before checking liveness |

Keep liveness **dumb**: if it checks the database, a database hiccup restarts every replica at
once and turns a small problem into an outage.

## Stopping

When Kubernetes (or Docker, or systemd) stops a container:

1. The pod is removed from the service's endpoints, and **at the same time** the process gets
   `SIGTERM`. Load balancers may keep sending traffic for a few seconds.
2. The process has `terminationGracePeriodSeconds` (default 30) to exit.
3. Then it gets `SIGKILL`: no cleanup, in-flight requests are cut.

Node.js's default for `SIGTERM` is to exit at once, which cuts in-flight requests. A
**graceful shutdown** replaces it:

```ts
process.once("SIGTERM", () => void shutdown("SIGTERM"));
process.once("SIGINT", () => void shutdown("SIGINT"));

async function shutdown(reason: string) {
  logger.info({ reason }, "shutting down");
  state.ready = false;                       // 1. readiness fails: no new traffic
  await sleep(5_000);                        // 2. give load balancers time to notice (optional)
  const forced = setTimeout(() => {          // 6. never outlive the grace period
    logger.error("shutdown timed out");
    process.exit(1);
  }, 20_000);
  forced.unref();
  await new Promise((resolve) => {
    server.close(resolve);                   // 3. stop accepting; wait for open connections
    server.closeIdleConnections();           //    idle keep-alive connections close now
  });
  await db.end();                            // 4. close resources in reverse order of creation
  await queue.close();
  logger.info("bye");
  process.exitCode = 0;                      // 5. let the process end by itself
}
```

Order matters: stop taking work before closing what the work needs. Keep the whole sequence
**shorter than the grace period**, with a forced exit as a safety net. A second `SIGINT`
(the impatient developer) should exit at once.

Workers, message consumers and cron-like jobs need the same treatment: stop fetching new
messages, finish (or hand back) the current ones, then exit.

## Deploys and upgrades

- **Rolling deploys** replace replicas a few at a time; graceful shutdown and readiness make
  them invisible to users.
- Long-lived connections (WebSockets, SSE) do not end by themselves: tell clients to
  reconnect, then close them.
- **Upgrade Node.js** along the LTS lines, before the one you run reaches end of life. Read
  the release notes for breaking changes, run the test suite on the new version in CI first,
  and pin the version in the image tag and `engines`.

## Summary

- Image: `npm ci --omit=dev`, non-root, `CMD ["node", ...]` so signals reach Node.js.
- A supervisor restarts crashed processes; logs go to stdout as JSON.
- Dumb liveness, honest readiness (false while shutting down).
- On `SIGTERM`: not ready → stop accepting → drain → close resources → exit, with a forced
  timeout below the grace period.

## Further reading

- Kubernetes, "Pod Lifecycle: Termination of Pods": <https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle/#pod-termination>
- Kubernetes, "Configure Liveness, Readiness and Startup Probes": <https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/>
- Node.js API, `server.close()` and `server.closeIdleConnections()`: <https://nodejs.org/api/http.html#serverclosecallback>
- Docker docs, "Best practices: CMD and ENTRYPOINT": <https://docs.docker.com/build/building/best-practices/#cmd>
- Node.js Docker image, "Best Practices": <https://github.com/nodejs/docker-node/blob/main/docs/BestPractices.md>
- The Twelve-Factor App, "Disposability": <https://12factor.net/disposability>
- pino documentation: <https://getpino.io/>
