import assert from "node:assert/strict";
import { createServer, type IncomingHttpHeaders, type Server } from "node:http";
import type { AddressInfo } from "node:net";
import { test, type TestContext } from "node:test";
import { createApp, type LogLine } from "../app.ts";
import { BUCKETS, LatencyHistogram } from "../metrics.ts";
import { formatTraceparent, parseTraceparent } from "../trace.ts";

const TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
const PARENT = "00f067aa0ba902b7";

async function listen(t: TestContext, server: Server): Promise<string> {
  await new Promise<void>((resolve) => server.listen(0, "127.0.0.1", resolve));
  t.after(() => server.close());
  return `http://127.0.0.1:${(server.address() as AddressInfo).port}`;
}

interface Harness {
  base: string;
  logs: LogLine[];
  seen: IncomingHttpHeaders[];
  tick: (ms: number) => void;
}

/** The app with a fake inventory (status 200 unless `inventoryStatus`), a log sink and a clock. */
async function start(t: TestContext, inventoryStatus = 200, stepMs = 0): Promise<Harness> {
  const seen: IncomingHttpHeaders[] = [];
  const inventory = createServer((request, response) => {
    seen.push(request.headers);
    response.writeHead(inventoryStatus, { "Content-Type": "application/json" });
    response.end(JSON.stringify({ inStock: request.url === "/stock/42" ? 7 : 0 }));
  });
  const inventoryUrl = await listen(t, inventory);
  const logs: LogLine[] = [];
  let clock = 1_000_000;
  const app = createApp({
    inventoryUrl,
    log: (line) => logs.push(line),
    now: () => {
      const value = clock;
      clock += stepMs;
      return value;
    },
  });
  const base = await listen(t, app);
  return { base, logs, seen, tick: (ms) => (clock += ms) };
}

async function settle(): Promise<void> {
  await new Promise((resolve) => setTimeout(resolve, 20));
}

test("parseTraceparent refuses every malformed header", () => {
  for (const header of [
    undefined,
    "",
    `00-${TRACE}-${PARENT}`,
    `00-${TRACE.toUpperCase()}-${PARENT}-01`,
    `ff-${TRACE}-${PARENT}-01`,
    `00-${"0".repeat(32)}-${PARENT}-01`,
    `00-${TRACE}-${"0".repeat(16)}-01`,
    `00-${TRACE}x-${PARENT}-01`,
    `00-${TRACE}-${PARENT}-1`,
  ]) {
    assert.equal(parseTraceparent(header), undefined, `${header}`);
  }
  assert.deepEqual(parseTraceparent(`00-${TRACE}-${PARENT}-00`), { traceId: TRACE, parentId: PARENT, flags: "00" });
});

test("formatTraceparent builds the outgoing header", () => {
  assert.equal(formatTraceparent({ traceId: TRACE, spanId: "1111222233334444", flags: "01" }), `00-${TRACE}-1111222233334444-01`);
});

test("the histogram renders cumulative buckets, +Inf, sum and count", () => {
  const histogram = new LatencyHistogram();
  const labels = { method: "GET", route: "/api/orders/:id", status: "200" };
  for (const seconds of [0.001953125, 0.25, 0.25, 8]) {
    histogram.observe(labels, seconds);
  }
  const text = histogram.render();
  const prefix = 'http_server_request_duration_seconds_bucket{method="GET",route="/api/orders/:id",status="200",le=';
  const expected = [1, 1, 1, 1, 1, 3, 3, 3, 3, 3];
  BUCKETS.forEach((bound, i) => assert.ok(text.includes(`${prefix}"${String(bound)}"} ${expected[i]}\n`), `le=${bound}`));
  assert.ok(text.includes(`${prefix}"+Inf"} 4\n`));
  assert.ok(text.includes('http_server_request_duration_seconds_sum{method="GET",route="/api/orders/:id",status="200"} 8.501953125\n'));
  assert.ok(text.includes('http_server_request_duration_seconds_count{method="GET",route="/api/orders/:id",status="200"} 4\n'));
  assert.ok(text.startsWith("# HELP http_server_request_duration_seconds "));
  assert.ok(text.includes("# TYPE http_server_request_duration_seconds histogram\n"));
});

test("each label set is its own series, in first-seen order", () => {
  const histogram = new LatencyHistogram();
  histogram.observe({ method: "GET", route: "/b", status: "200" }, 0.001);
  histogram.observe({ method: "GET", route: "/a", status: "404" }, 0.001);
  histogram.observe({ method: "GET", route: "/b", status: "200" }, 0.001);
  const counts = [...histogram.render().matchAll(/_count\{(.*)\} (\d+)/g)].map((m) => `${m[1]} ${m[2]}`);
  assert.deepEqual(counts, ['method="GET",route="/b",status="200" 2', 'method="GET",route="/a",status="404" 1']);
});

test("an incoming trace is continued and propagated to the inventory", async (t) => {
  const { base, logs, seen } = await start(t);
  const response = await fetch(`${base}/api/orders/42`, { headers: { traceparent: `00-${TRACE}-${PARENT}-01` } });
  assert.equal(response.status, 200);
  assert.deepEqual(await response.json(), { id: "42", inStock: 7 });
  assert.equal(response.headers.get("x-trace-id"), TRACE);
  await settle();
  const outgoing = parseTraceparent(seen[0]?.["traceparent"] as string | undefined);
  assert.equal(outgoing?.traceId, TRACE);
  assert.equal(outgoing?.flags, "01");
  assert.notEqual(outgoing?.parentId, PARENT, "the inventory's parent is this request's span, not the browser's");
  assert.equal(outgoing?.parentId, logs[0]?.["spanId"]);
});

test("without a valid traceparent a new trace starts", async (t) => {
  const { base, logs, seen } = await start(t);
  const response = await fetch(`${base}/api/orders/1`, { headers: { traceparent: "garbage" } });
  await response.text();
  const traceId = response.headers.get("x-trace-id") ?? "";
  assert.match(traceId, /^[0-9a-f]{32}$/);
  await settle();
  assert.equal(parseTraceparent(seen[0]?.["traceparent"] as string | undefined)?.traceId, traceId);
  assert.equal(parseTraceparent(seen[0]?.["traceparent"] as string | undefined)?.flags, "01");
  assert.equal(logs[0]?.["traceId"], traceId);
});

test("every request writes one structured log line", async (t) => {
  const { base, logs } = await start(t, 200, 25);
  await (await fetch(`${base}/api/orders/42`, { headers: { traceparent: `00-${TRACE}-${PARENT}-00` } })).text();
  await settle();
  assert.equal(logs.length, 1);
  const line = logs[0] as Record<string, unknown>;
  assert.equal(line["level"], "info");
  assert.equal(line["msg"], "request");
  assert.equal(line["traceId"], TRACE);
  assert.match(String(line["spanId"]), /^[0-9a-f]{16}$/);
  assert.equal(line["method"], "GET");
  assert.equal(line["route"], "/api/orders/:id");
  assert.equal(line["status"], 200);
  assert.equal(line["durationMs"], 25);
});

test("a thrown error is a 500 with the trace id, logged as an error", async (t) => {
  const { base, logs } = await start(t);
  const response = await fetch(`${base}/api/boom`, { headers: { traceparent: `00-${TRACE}-${PARENT}-01` } });
  assert.equal(response.status, 500);
  assert.deepEqual(await response.json(), { error: "internal error", traceId: TRACE });
  await settle();
  assert.equal(logs[0]?.level, "error");
  assert.equal(logs[0]?.["status"], 500);
  assert.equal(logs[0]?.["route"], "/api/boom");
  assert.match(String(logs[0]?.["error"]), /does not add up/);
});

test("a failing inventory is a 502 with the trace id", async (t) => {
  const { base, logs } = await start(t, 503);
  const response = await fetch(`${base}/api/orders/42`, { headers: { traceparent: `00-${TRACE}-${PARENT}-01` } });
  assert.equal(response.status, 502);
  assert.deepEqual(await response.json(), { error: "inventory unavailable", traceId: TRACE });
  await settle();
  assert.equal(logs[0]?.level, "error");
});

test("an unreachable inventory is a 502 too", async (t) => {
  const logs: LogLine[] = [];
  const app = createApp({ inventoryUrl: "http://127.0.0.1:9", log: (line) => logs.push(line), now: () => 0 });
  const base = await listen(t, app);
  const response = await fetch(`${base}/api/orders/42`);
  assert.equal(response.status, 502);
  const body = (await response.json()) as { traceId: string };
  assert.equal(body.traceId, response.headers.get("x-trace-id"));
});

test("/metrics labels by route template and counts every request", async (t) => {
  const { base } = await start(t, 200, 30);
  for (const id of ["1", "2", "42"]) {
    await (await fetch(`${base}/api/orders/${id}`)).text();
  }
  await (await fetch(`${base}/nowhere/${Math.random()}`)).text();
  await settle();
  const response = await fetch(`${base}/metrics`);
  assert.equal(response.status, 200);
  assert.match(response.headers.get("content-type") ?? "", /^text\/plain; ?version=0\.0\.4/);
  const text = await response.text();
  assert.ok(text.includes('_count{method="GET",route="/api/orders/:id",status="200"} 3\n'), text);
  assert.ok(text.includes('_bucket{method="GET",route="/api/orders/:id",status="200",le="0.025"} 0\n'));
  assert.ok(text.includes('_bucket{method="GET",route="/api/orders/:id",status="200",le="0.05"} 3\n'));
  assert.ok(text.includes('_count{method="GET",route="unmatched",status="404"} 1\n'));
  assert.doesNotMatch(text, /route="\/api\/orders\/42"/);
  assert.doesNotMatch(text, /nowhere/);
});

test("unknown routes are a JSON 404 with a trace id", async (t) => {
  const { base, logs } = await start(t);
  const response = await fetch(`${base}/admin`);
  assert.equal(response.status, 404);
  assert.deepEqual(await response.json(), { error: "not found" });
  assert.match(response.headers.get("x-trace-id") ?? "", /^[0-9a-f]{32}$/);
  await settle();
  assert.equal(logs[0]?.["route"], "unmatched");
  assert.equal(logs[0]?.level, "info");
});
