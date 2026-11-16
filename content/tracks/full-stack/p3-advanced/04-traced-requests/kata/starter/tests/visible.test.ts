import assert from "node:assert/strict";
import { test } from "node:test";
import { parseTraceparent } from "../trace.ts";
import { LatencyHistogram } from "../metrics.ts";

test("parses a valid traceparent", () => {
  assert.deepEqual(parseTraceparent("00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"), {
    traceId: "4bf92f3577b34da6a3ce929d0e0e4736",
    parentId: "00f067aa0ba902b7",
    flags: "01",
  });
});

test("one observation fills the buckets from its bound up", () => {
  const histogram = new LatencyHistogram();
  histogram.observe({ method: "GET", route: "/x", status: "200" }, 0.02);
  const text = histogram.render();
  assert.match(text, /_bucket\{method="GET",route="\/x",status="200",le="0.01"\} 0\n/);
  assert.match(text, /_bucket\{method="GET",route="\/x",status="200",le="0.025"\} 1\n/);
});
