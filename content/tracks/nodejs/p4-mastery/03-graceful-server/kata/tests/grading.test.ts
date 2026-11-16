import assert from "node:assert/strict";
import { EventEmitter } from "node:events";
import { request, type IncomingHttpHeaders } from "node:http";
import type { AddressInfo } from "node:net";
import { describe, test } from "node:test";
import { setTimeout as sleep } from "node:timers/promises";
import { createLogger, createService, currentRequestId, installShutdown, requestContext, type Handler, type Service } from "../service.ts";

interface Answer {
  status: number;
  headers: IncomingHttpHeaders;
  body: string;
}

/** One request on a new connection (no keep-alive pool), so each test controls its sockets. */
function get(port: number, path: string, headers: Record<string, string> = {}): Promise<Answer> {
  return new Promise((resolve, reject) => {
    const req = request({ host: "127.0.0.1", port, path, headers, agent: false }, (res) => {
      let body = "";
      res.setEncoding("utf8");
      res.on("data", (chunk: string) => (body += chunk));
      res.on("end", () => resolve({ status: res.statusCode ?? 0, headers: res.headers, body }));
      res.on("error", reject);
    });
    req.on("error", reject);
    req.end();
  });
}

async function listen(service: Service): Promise<number> {
  await new Promise<void>((resolve) => service.server.listen(0, "127.0.0.1", resolve));
  return (service.server.address() as AddressInfo).port;
}

function setup(handler: Handler) {
  const lines: Record<string, unknown>[] = [];
  const logger = createLogger((line) => lines.push(JSON.parse(line)), () => new Date("2026-03-01T12:00:00.000Z"));
  const service = createService(handler, logger);
  return { lines, logger, service };
}

async function stop(service: Service): Promise<void> {
  service.server.closeAllConnections();
  await new Promise<void>((resolve) => service.server.close(() => resolve()));
}

describe("createLogger and requestContext", () => {
  test("writes JSON lines with time, level, msg and fields", () => {
    const lines: string[] = [];
    const logger = createLogger((line) => lines.push(line), () => new Date("2026-03-01T12:00:00.000Z"));
    logger.info("hello", { user: 7 });
    logger.error("oops");
    assert.deepEqual(lines.map((line) => JSON.parse(line)), [
      { time: "2026-03-01T12:00:00.000Z", level: "info", msg: "hello", user: 7 },
      { time: "2026-03-01T12:00:00.000Z", level: "error", msg: "oops" },
    ]);
  });

  test("adds the request id inside a context, across awaits and timers", async () => {
    const lines: Record<string, unknown>[] = [];
    const logger = createLogger((line) => lines.push(JSON.parse(line)));
    assert.equal(currentRequestId(), undefined);
    await requestContext.run({ requestId: "req-1" }, async () => {
      await sleep(5);
      await new Promise<void>((resolve) =>
        setTimeout(() => {
          logger.info("inside");
          resolve();
        }, 5),
      );
    });
    logger.info("outside");
    assert.equal(lines[0]?.requestId, "req-1");
    assert.equal("requestId" in (lines[1] ?? {}), false);
  });
});

describe("createService", () => {
  test("answers /ready and runs the handler inside the request's context", async () => {
    const { lines, logger, service } = setup(async (_req, res) => {
      await sleep(5);
      logger.info("handling");
      res.end(`id=${currentRequestId()}`);
    });
    const port = await listen(service);
    try {
      const ready = await get(port, "/ready");
      assert.equal(ready.status, 200);
      assert.deepEqual(JSON.parse(ready.body), { ready: true });

      const answer = await get(port, "/work", { "x-request-id": "abc-123" });
      assert.equal(answer.headers["x-request-id"], "abc-123");
      assert.equal(answer.body, "id=abc-123");
      const handling = lines.find((line) => line.msg === "handling");
      assert.equal(handling?.requestId, "abc-123");
    } finally {
      await stop(service);
    }
  });

  test("replaces a missing or unsafe x-request-id with a UUID", async () => {
    const { service } = setup((_req, res) => {
      res.end(String(currentRequestId()));
    });
    const port = await listen(service);
    try {
      const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;
      const none = await get(port, "/x");
      const unsafe = await get(port, "/x", { "x-request-id": "bad id\" injected" });
      assert.match(none.body, uuid);
      assert.match(unsafe.body, uuid);
      assert.notEqual(none.body, unsafe.body);
      assert.equal(unsafe.headers["x-request-id"], unsafe.body);
    } finally {
      await stop(service);
    }
  });

  test("logs one 'request' line per request with method, path and status", async () => {
    const { lines, service } = setup((_req, res) => {
      res.statusCode = 201;
      res.end();
    });
    const port = await listen(service);
    try {
      await get(port, "/things?x=1", { "x-request-id": "r-9" });
      await sleep(10);
      const line = lines.find((each) => each.msg === "request");
      assert.ok(line, "no 'request' line was logged");
      assert.equal(line.requestId, "r-9");
      assert.equal(line.method, "GET");
      assert.equal(line.path, "/things?x=1");
      assert.equal(line.status, 201);
      assert.equal(typeof line.ms, "number");
    } finally {
      await stop(service);
    }
  });

  test("a failing handler gives a 500 without details, and the error is logged", async () => {
    const { lines, service } = setup(async () => {
      throw new Error("database password is hunter2");
    });
    const port = await listen(service);
    try {
      const answer = await get(port, "/boom", { "x-request-id": "r-500" });
      assert.equal(answer.status, 500);
      assert.deepEqual(JSON.parse(answer.body), { error: "internal error", requestId: "r-500" });
      const logged = lines.find((line) => line.level === "error");
      assert.equal(logged?.requestId, "r-500");
      assert.match(String(logged?.error), /hunter2/);
      assert.equal((await get(port, "/ready")).status, 200, "the service must survive the error");
    } finally {
      await stop(service);
    }
  });
});

describe("installShutdown", () => {
  test("listens to SIGTERM and SIGINT on the given emitter", () => {
    const { logger, service } = setup((_req, res) => {
      res.end();
    });
    const signals = new EventEmitter();
    installShutdown(service, { logger, signals, exit: () => {} });
    assert.equal(signals.listenerCount("SIGTERM"), 1);
    assert.equal(signals.listenerCount("SIGINT"), 1);
  });

  test("on SIGTERM: not ready, no new connections, in-flight requests finish, exit 0", async () => {
    const { lines, logger, service } = setup(async (_req, res) => {
      await sleep(200);
      res.end("finished");
    });
    const port = await listen(service);
    const signals = new EventEmitter();
    const exits: number[] = [];
    installShutdown(service, { logger, signals, timeoutMs: 5000, exit: (code) => exits.push(code) });

    const slow = get(port, "/slow");
    await sleep(50);
    signals.emit("SIGTERM");
    assert.equal(service.ready, false);
    await sleep(20);
    await assert.rejects(get(port, "/ready"), { code: "ECONNREFUSED" });
    const answer = await slow;
    assert.equal(answer.status, 200);
    assert.equal(answer.body, "finished");
    await sleep(50);
    assert.deepEqual(exits, [0]);
    assert.ok(lines.some((line) => line.msg === "shutting down" && line.reason === "SIGTERM"));
  });

  test("cuts requests that outlive the timeout and exits with 1", async () => {
    const { logger, service } = setup(async (_req, res) => {
      await sleep(10_000);
      res.end();
    });
    const port = await listen(service);
    const exits: number[] = [];
    const { shutdown } = installShutdown(service, { logger, signals: new EventEmitter(), timeoutMs: 100, exit: (code) => exits.push(code) });
    const stuck = get(port, "/stuck");
    stuck.catch(() => {});
    await sleep(30);
    const started = performance.now();
    assert.equal(await shutdown("test"), 1);
    assert.ok(performance.now() - started < 1000);
    assert.deepEqual(exits, [1]);
    await assert.rejects(stuck);
  });

  test("a second signal exits with 1 at once; shutdown() twice gives the same promise", async () => {
    const { logger, service } = setup(async (_req, res) => {
      await sleep(10_000);
      res.end();
    });
    const port = await listen(service);
    const signals = new EventEmitter();
    const exits: number[] = [];
    const { shutdown } = installShutdown(service, { logger, signals, timeoutMs: 5000, exit: (code) => exits.push(code) });
    const stuck = get(port, "/stuck");
    stuck.catch(() => {});
    await sleep(30);
    signals.emit("SIGINT");
    const first = shutdown("again");
    assert.equal(shutdown("again"), first);
    signals.emit("SIGINT");
    assert.equal(await first, 1);
    assert.deepEqual(exits, [1]);
  });

  test("an idle service shuts down at once with 0", async () => {
    const { logger, service } = setup((_req, res) => {
      res.end("ok");
    });
    const port = await listen(service);
    assert.equal((await get(port, "/x")).body, "ok");
    const exits: number[] = [];
    const { shutdown } = installShutdown(service, { logger, signals: new EventEmitter(), timeoutMs: 5000, exit: (code) => exits.push(code) });
    const started = performance.now();
    assert.equal(await shutdown("deploy"), 0);
    assert.ok(performance.now() - started < 1000);
    assert.deepEqual(exits, [0]);
  });
});
