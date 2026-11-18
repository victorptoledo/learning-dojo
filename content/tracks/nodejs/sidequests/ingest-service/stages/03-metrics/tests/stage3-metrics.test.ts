import assert from "node:assert/strict";
import { describe, test } from "node:test";
import { event, events, post, postChunks, startService, until } from "./ingest.ts";
import { scrape } from "./prometheus.ts";

const FAMILIES: [string, string][] = [
  ["ingest_events_total", "counter"],
  ["ingest_requests_total", "counter"],
  ["ingest_request_duration_seconds", "histogram"],
  ["ingest_batches_written_total", "counter"],
  ["ingest_pending_batches", "gauge"],
  ["ingest_buffered_events", "gauge"],
  ["ingest_event_loop_delay_max_seconds", "gauge"],
];

const BUCKETS = ["0.005", "0.01", "0.025", "0.05", "0.1", "0.25", "0.5", "1", "2.5", "5", "10", "+Inf"];

describe("GET /metrics", () => {
  test("every family has HELP and TYPE, even before any traffic", async () => {
    const { base } = await startService();
    const metrics = await scrape(base);
    for (const [name, type] of FAMILIES) {
      assert.equal(metrics.types.get(name), type, `# TYPE ${name} ${type}`);
      assert.ok((metrics.help.get(name) ?? "") !== "", `# HELP ${name} ...`);
    }
    assert.equal(metrics.value("ingest_events_total", { result: "accepted" }), 0);
    assert.equal(metrics.value("ingest_events_total", { result: "rejected" }), 0);
    assert.equal(metrics.value("ingest_request_duration_seconds_count"), 0);
  });

  test("counts events by result and requests by route and status", async () => {
    const { base, port, service } = await startService();
    await post(port, `${events(4)}nope\n`);
    await post(port, `${event("x")}\n`);
    await post(port, `${event("x")}\n`, { contentType: "text/plain" });
    await fetch(`${base}/nowhere`);
    await fetch(`${base}/events`);
    await service.flush();
    const metrics = await scrape(base);
    assert.equal(metrics.value("ingest_events_total", { result: "accepted" }), 5);
    assert.equal(metrics.value("ingest_events_total", { result: "rejected" }), 1);
    assert.equal(metrics.value("ingest_requests_total", { route: "/events", status: "202" }), 2);
    assert.equal(metrics.value("ingest_requests_total", { route: "/events", status: "415" }), 1);
    assert.equal(metrics.value("ingest_requests_total", { route: "/events", status: "405" }), 1);
    assert.equal(metrics.value("ingest_requests_total", { route: "other", status: "404" }), 1);
    assert.equal(metrics.value("ingest_batches_written_total"), 1);
    const again = await scrape(base);
    assert.equal(again.value("ingest_requests_total", { route: "/metrics", status: "200" }), 1, "the scrape itself is counted");
  });

  test("a histogram of POST /events durations, with cumulative buckets", async () => {
    const { base, port } = await startService();
    await post(port, events(3));
    await post(port, events(3));
    await postChunks(port, [`${event("a")}\n`, `${event("b")}\n`, `${event("c")}\n`, `${event("d")}\n`], { gapMs: 110 });
    const metrics = await scrape(base);
    const counts = BUCKETS.map((le) => metrics.value("ingest_request_duration_seconds_bucket", { le }));
    for (let i = 1; i < counts.length; i++) {
      assert.ok((counts[i] ?? 0) >= (counts[i - 1] ?? 0), `buckets must be cumulative: ${counts.join(" ")}`);
    }
    assert.equal(counts.at(-1), 3);
    assert.equal(metrics.value("ingest_request_duration_seconds_count"), 3);
    assert.equal(metrics.value("ingest_request_duration_seconds_bucket", { le: "0.25" }), 2, "the slow upload took more than 0.25 s");
    const sum = metrics.value("ingest_request_duration_seconds_sum");
    assert.ok(sum > 0.3 && sum < 5, `sum ${sum}`);
  });

  test("gauges follow the sink: pending batches and buffered events", async () => {
    const { base, port, service, sink } = await startService({ batchSize: 5, flushMs: 60_000 });
    sink.close();
    await post(port, events(12));
    await until(() => service.stats().pendingBatches === 2, 2000, "two pending batches");
    const busy = await scrape(base);
    assert.equal(busy.value("ingest_pending_batches"), 2);
    assert.equal(busy.value("ingest_buffered_events"), 12);
    sink.open();
    await service.flush();
    const idle = await scrape(base);
    assert.equal(idle.value("ingest_pending_batches"), 0);
    assert.equal(idle.value("ingest_buffered_events"), 0);
    assert.equal(idle.value("ingest_batches_written_total"), 3);
  });

  test("reports the longest event loop delay since the previous scrape", async () => {
    const { base } = await startService();
    await scrape(base);
    await new Promise((resolve) => setTimeout(resolve, 50));
    const end = Date.now() + 250;
    while (Date.now() < end) {
      // block the event loop, as a slow synchronous handler would
    }
    await new Promise((resolve) => setTimeout(resolve, 50));
    const blocked = await scrape(base);
    const delay = blocked.value("ingest_event_loop_delay_max_seconds");
    assert.ok(delay >= 0.15 && delay < 5, `a 250 ms block showed as ${delay} s`);
    await new Promise((resolve) => setTimeout(resolve, 100));
    const calm = await scrape(base);
    assert.ok(calm.value("ingest_event_loop_delay_max_seconds") < 0.15, "the maximum starts again after each scrape");
  });
});
